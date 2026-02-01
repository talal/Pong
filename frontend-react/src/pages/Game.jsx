// pong game screen
// renders the canvas game view, connects to the backend websocket using stomp,
// joins matchmaking, then receives game state updates from /topic/game/{gameId}.
// draws the game inside a requestanimationframe loop.
// uses refs for everything the loop reads to avoid stale values.
// shows a neon overlay inside the canvas area while connecting/waiting/finished
// pulls win rate + games played from /api/leaderboard and shows it under each player card.

import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import { auth } from '../auth.js'
import { getUsernameFromToken } from '../utils/jwt.js'
import './game.css'

export default function Game() {
  const navigate = useNavigate()

  // canvas refs
  // canvasRef is the dom element, ctxRef is the 2d context we draw on, rafRef is the animation frame id
  const canvasRef = useRef(null)
  const ctxRef = useRef(null)
  const rafRef = useRef(null)

  // websocket refs
  // clientRef holds the stomp client, matchSubRef listens for matchmaking results, gameSubRef listens for live game frames
  const clientRef = useRef(null)
  const matchSubRef = useRef(null)
  const gameSubRef = useRef(null)

  // loop refs
  // statusRef + score refs are mirrored from react state so the render loop always sees the latest values
  const statusRef = useRef('CONNECTING')
  const score1Ref = useRef(0)
  const score2Ref = useRef(0)

  // input refs
  // keysRef tracks arrow key press state without causing rerenders
  // myYRef stores the last y we sent (0..100 percent) so we can move smoothly frame-by-frame
  const keysRef = useRef({ ArrowUp: false, ArrowDown: false })
  const myYRef = useRef(50)

  // game state ref
  // last server-authoritative snapshot for ball and paddles (all values in percent: 0..100)
  const gameStateRef = useRef({ ballX: 50, ballY: 50, p1Y: 50, p2Y: 50 })

  // react ui state
  // status drives overlays and button behavior
  // names/scores are shown in the ui and also mirrored into refs for the canvas loop
  const [status, setStatus] = useState('CONNECTING')
  const [p1Name, setP1Name] = useState('Player 1')
  const [p2Name, setP2Name] = useState('Player 2')
  const [p1Score, setP1Score] = useState(0)
  const [p2Score, setP2Score] = useState(0)
  const [winner, setWinner] = useState('')

  // leaderboard stats displayed under avatars
  const [p1Stats, setP1Stats] = useState({ winRate: '—', gamesPlayed: '—' })
  const [p2Stats, setP2Stats] = useState({ winRate: '—', gamesPlayed: '—' })

  // responsive ui knobs
  // these are converted to css variables so the layout can scale down when viewport height is small
  const [ui, setUi] = useState({
    canvasW: 800,
    canvasH: 500,
    panelPad: 22,
    contentGap: 18,
    avatarSize: 88,
  })

  // keep loop refs in sync with react state
  // requestanimationframe reads these refs, not react state, to avoid stale closure issues
  useEffect(() => {
    statusRef.current = status
  }, [status])
  useEffect(() => {
    score1Ref.current = p1Score
  }, [p1Score])
  useEffect(() => {
    score2Ref.current = p2Score
  }, [p2Score])

  // avatar helper
  // backend serves images from /api/user/avatar?email=... and we fall back to a local file if missing
  function getAvatarUrl(emailOrUsername) {
    if (!emailOrUsername || emailOrUsername.includes('Player')) return '/default-avatar.jpg'
    return `/api/user/avatar?email=${encodeURIComponent(emailOrUsername)}`
  }

  function onImgError(e) {
    e.currentTarget.src = '/default-avatar.jpg'
  }

  // compute win/lose by comparing winner username with the local token username
  // this is used only to color and text the finished overlay
  const myUsername = auth.token ? getUsernameFromToken(auth.token) : ''
  const youWon =
    status === 'FINISHED' &&
    winner &&
    myUsername &&
    winner.toLowerCase() === myUsername.toLowerCase()

  // overlay model
  // shows the same styled overlay for connecting/waiting/error, and game over when finished
  const overlayModel = useMemo(() => {
    if (status === 'FINISHED') {
      return { title: 'Game Over!', sub: youWon ? 'You Win!' : 'You Lose!' }
    }
    if (status === 'WAITING') {
      return { title: 'Waiting for Players', sub: 'Matchmaking...' }
    }
    if (status === 'CONNECTING') {
      return { title: 'Connecting...', sub: 'Please wait' }
    }
    if (status === 'ERROR') {
      return { title: 'Connection Error', sub: 'Return to Menu' }
    }
    return null
  }, [status, youWon])

  // responsive sizing
  // the canvas always draws internally at 800x500, but we scale its displayed size in css (width/height).
  // we compute a target display size that fits the viewport height and the panel width.
  // we also scale padding/gaps/avatars down on short screens to keep everything visible.
  useEffect(() => {
    function computeUi() {
      const vw = window.innerWidth
      const vh = window.innerHeight

      // layout constants that affect available canvas width
      // these match the css grid columns and gap in .game-layout
      const sideCol = 35
      const gridGap = 30
      const layoutOverheadW = sideCol + sideCol + gridGap + gridGap

      // panel is capped at 980 and page has some padding
      const panelMaxW = Math.min(980, vw - 28)

      // keep canvas max display width reasonable so it stays inside the neon frame
      const maxCanvasW = Math.max(320, Math.min(860, panelMaxW - layoutOverheadW - 40))

      // reserve vertical space for topbar, button row, and avatar cards
      // reserve less when the viewport is already short
      const reserved = vh < 740 ? 320 : 390

      // compute a canvas display size that tries to keep the 16:10 ratio (800:500)
      let canvasH = Math.max(260, Math.min(500, vh - reserved))
      let canvasW = Math.round(canvasH * 1.6)

      // if the computed width would overflow, clamp width then recompute height
      if (canvasW > maxCanvasW) {
        canvasW = Math.round(maxCanvasW)
        canvasH = Math.round(canvasW / 1.6)
      }

      // scale down spacing elements for smaller heights
      let panelPad = 22
      let contentGap = 18
      let avatarSize = 88

      if (vh < 820) {
        panelPad = 18
        contentGap = 14
        avatarSize = 76
      }
      if (vh < 700) {
        panelPad = 14
        contentGap = 12
        avatarSize = 64
      }

      setUi({ canvasW, canvasH, panelPad, contentGap, avatarSize })
    }

    computeUi()
    window.addEventListener('resize', computeUi)
    return () => window.removeEventListener('resize', computeUi)
  }, [])

  // fetch win rate and games played
  // this hits /api/leaderboard once names are known (after match starts) and extracts the entries for both players.
  // keeps the call minimal: a single request and local lookup.
  useEffect(() => {
    let cancelled = false

    async function loadStats() {
      if (!auth.token) return

      const a = p1Name && !p1Name.includes('Player') ? p1Name : null
      const b = p2Name && !p2Name.includes('Player') ? p2Name : null
      if (!a && !b) return

      const res = await fetch('/api/leaderboard', {
        headers: { Authorization: 'Bearer ' + auth.token },
      })
      if (!res.ok) return

      const list = await res.json()

      const findEntry = (name) =>
        list.find((x) => String(x?.username || '').toLowerCase() === String(name).toLowerCase())

      const e1 = a ? findEntry(a) : null
      const e2 = b ? findEntry(b) : null

      if (cancelled) return

      if (e1) {
        setP1Stats({
          winRate: e1.winRate ?? '—',
          gamesPlayed: e1.gamesPlayed ?? '—',
        })
      }
      if (e2) {
        setP2Stats({
          winRate: e2.winRate ?? '—',
          gamesPlayed: e2.gamesPlayed ?? '—',
        })
      }
    }

    loadStats()
    return () => {
      cancelled = true
    }
  }, [p1Name, p2Name])

  // initial setup: auth guard, canvas context, keyboard listeners, websocket connect, start render loop
  useEffect(() => {
    // without a token we cannot authenticate websocket or protected endpoints
    if (!auth.token) {
      navigate('/app/login', { replace: true })
      return
    }

    // initial local label while waiting for match state
    setP1Name(getUsernameFromToken(auth.token))

    // store 2d context once
    ctxRef.current = canvasRef.current.getContext('2d')

    // arrow keys should not scroll the page; store press state in keysRef
    function onKeyDown(e) {
      if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
        e.preventDefault()
        keysRef.current[e.key] = true
      }
    }
    function onKeyUp(e) {
      if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
        e.preventDefault()
        keysRef.current[e.key] = false
      }
    }

    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)

    // create stomp client
    // brokerURL must match backend websocket endpoint
    // connectHeaders include bearer token for backend auth
    const c = new Client({
      brokerURL: 'ws://localhost:8080/websocket',
      connectHeaders: { Authorization: 'Bearer ' + auth.token },
      reconnectDelay: 2000,
      onWebSocketError: () => setStatus('ERROR'),
      onStompError: () => setStatus('ERROR'),
    })

    // on connect: subscribe for matchmaking results, then publish join request
    c.onConnect = () => {
      setStatus('WAITING')

      // backend sends a private message with gameId once matched
      matchSubRef.current = c.subscribe('/user/queue/match', (message) => {
        const { gameId } = JSON.parse(message.body)
        setStatus('PLAYING')

        // subscribe to the game topic to receive repeated state updates
        gameSubRef.current = c.subscribe(`/topic/game/${gameId}`, (m) => {
          const game = JSON.parse(m.body)

          // finished state is handled here; the overlay will show game over inside the canvas
          if (game.state === 'FINISHED') {
            setStatus('FINISHED')
            setWinner(
              game.player1.score > game.player2.score
                ? game.player1.username
                : game.player2.username
            )
          }

          // update ui state for names and scores
          setP1Name(game.player1.username)
          setP2Name(game.player2.username)
          setP1Score(game.player1.score)
          setP2Score(game.player2.score)

          // update the ref snapshot used by the render loop
          gameStateRef.current.ballX = game.ballX
          gameStateRef.current.ballY = game.ballY
          gameStateRef.current.p1Y = game.player1.y
          gameStateRef.current.p2Y = game.player2.y
        })
      })

      // join matchmaking
      c.publish({ destination: '/app/game.join', body: JSON.stringify({}) })
    }

    clientRef.current = c
    c.activate()

    // start the canvas render loop
    rafRef.current = requestAnimationFrame(renderLoop)

    // cleanup: remove listeners, stop animation, unsubscribe, disconnect
    return () => {
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('keyup', onKeyUp)
      cancelAnimationFrame(rafRef.current)
      matchSubRef.current?.unsubscribe()
      gameSubRef.current?.unsubscribe()
      clientRef.current?.deactivate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // paddle movement
  // moves y locally and publishes { y } to the backend while playing.
  // y is stored as percent (0..100), and clamped so the paddle stays inside the field.
  function updatePaddlePosition() {
    if (statusRef.current !== 'PLAYING') return

    const keys = keysRef.current
    let y = myYRef.current
    let changed = false

    const speed = 1.5

    if (keys.ArrowUp) {
      y -= speed
      changed = true
    }
    if (keys.ArrowDown) {
      y += speed
      changed = true
    }

    // paddle height is 15% -> half is 7.5% for clamping
    const half = 7.5
    y = Math.max(half, Math.min(100 - half, y))
    myYRef.current = y

    // backend move handler expects { "y": number } only
    if (changed && clientRef.current) {
      clientRef.current.publish({
        destination: '/app/game.move',
        body: JSON.stringify({ y }),
      })
    }
  }

  // render loop
  // clears the canvas, draws the field, scores, paddles, and ball using the latest ref snapshot.
  // the overlay is done in dom (css) so it can blur and glow without re-drawing text on canvas.
  function renderLoop() {
    const ctx = ctxRef.current
    if (!ctx) return

    updatePaddlePosition()

    // background
    ctx.fillStyle = '#05060a'
    ctx.fillRect(0, 0, 800, 500)

    // draw the game field when playing or finished
    // finished still draws the last frame behind the overlay
    if (statusRef.current === 'PLAYING' || statusRef.current === 'FINISHED') {
      const toX = (v) => (v / 100) * 800
      const toY = (v) => (v / 100) * 500
      const gs = gameStateRef.current

      // center dashed line
      ctx.strokeStyle = '#1a2236'
      ctx.lineWidth = 4
      ctx.setLineDash([10, 10])
      ctx.beginPath()
      ctx.moveTo(400, 0)
      ctx.lineTo(400, 500)
      ctx.stroke()
      ctx.setLineDash([])

      // scores
      ctx.fillStyle = '#aab1c3'
      ctx.font = 'bold 60px "Courier New", monospace'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'top'
      ctx.fillText(String(score1Ref.current), 350, 20)
      ctx.fillText(String(score2Ref.current), 450, 20)

      // paddles + ball
      ctx.fillStyle = '#e6e9f2'
      ctx.fillRect(toX(0), toY(gs.p1Y - 7.5), toX(2), toY(15))
      ctx.fillRect(toX(98), toY(gs.p2Y - 7.5), toX(2), toY(15))
      ctx.beginPath()
      ctx.arc(toX(gs.ballX), toY(gs.ballY), 8, 0, Math.PI * 2)
      ctx.fill()
    }

    rafRef.current = requestAnimationFrame(renderLoop)
  }

  function returnToMenu() {
    navigate('/app/menu')
  }

  // css variables keep styling clean while still letting js control responsive sizing
  const cssVars = useMemo(
    () => ({
      '--canvas-w': `${ui.canvasW}px`,
      '--canvas-h': `${ui.canvasH}px`,
      '--panel-pad': `${ui.panelPad}px`,
      '--content-gap': `${ui.contentGap}px`,
      '--avatar-size': `${ui.avatarSize}px`,
    }),
    [ui]
  )

  // overlay is visible any time we are not actively playing
  // finished uses a win/lose accent; waiting/connecting/error keep the neutral styling
  const overlayVisible = status !== 'PLAYING'
  const overlayTitle = overlayModel?.title ?? ''
  const overlaySub = overlayModel?.sub ?? ''

  return (
    <div className="game-page">
      <div className="game-wrap">
        <div className="game-panel" style={cssVars}>
          <div className="game-content">
            <div className="game-topbar">
              <h1 className="game-title">Pong</h1>
              <div className="status-pill">Status: {status}</div>
            </div>

            {/* the layout grid has equal side columns so the canvas stays centered
                the rotated names live inside those side columns to stay symmetric */}
            <div className="game-layout">
              {/* left column */}
              <div className="side-name">
                <h2 className="side-name-text side-left">{p1Name}</h2>
              </div>

              {/* center column */}
              <div className="center-cell">
                <div className="canvas-frame">
                  <div className="canvas-inner">
                    <canvas ref={canvasRef} width={800} height={500} className="pong-canvas" />

                    <div className={`game-overlay ${status !== 'PLAYING' ? 'is-visible' : ''}`}>
                      <div>
                        <p className="overlay-title">{overlayTitle}</p>
                        <p className={`overlay-sub ${status === 'FINISHED' ? (youWon ? 'win' : 'lose') : ''}`}>
                          {overlaySub}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

  {/* right column */}
  <div className="side-name">
    <h2 className="side-name-text side-right">{p2Name}</h2>
  </div>
</div>


            <div className="controls">
              <button className="btn-magenta" onClick={returnToMenu}>
                Return to Menu
              </button>
            </div>

            {/* small helper text under the button only while waiting
                the main waiting message is still inside the canvas overlay */}
            {status === 'WAITING' ? <div className="msg">Waiting for an opponent...</div> : null}

            <div className="avatar-row">
              <div className="avatar-card">
                <img
                  src={getAvatarUrl(p1Name)}
                  alt="p1 avatar"
                  className="avatar-img"
                  onError={onImgError}
                />
                <div>
                  <p className="meta-name">{p1Name}</p>
                  <p className="meta-stats">
                    Win rate: {p1Stats.winRate} · Games played: {p1Stats.gamesPlayed}
                  </p>
                </div>
              </div>

              <div className="avatar-card">
                <img
                  src={getAvatarUrl(p2Name)}
                  alt="p2 avatar"
                  className="avatar-img"
                  onError={onImgError}
                />
                <div>
                  <p className="meta-name">{p2Name}</p>
                  <p className="meta-stats">
                    Win rate: {p2Stats.winRate} · Games played: {p2Stats.gamesPlayed}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
