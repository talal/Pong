import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import './Theme.css'
import './Leaderboard.css'

export default function Leaderboard() {
  const [players, setPlayers] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      try {
        const response = await apiFetch('/api/leaderboard')
        if (!response.ok) return
        const data = await response.json()
        if (!cancelled) setPlayers(Array.isArray(data) ? data : [])
      } catch (e) {
        console.error('failed to load leaderboard', e)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel lb-panel">
          <div className="lb-topbar">
            <h1 className="cyber-title">Leaderboard</h1>
            <div className="cyber-pill">Top Players</div>
          </div>

          {loading ? (
            <div className="lb-loading card-glass">
              <div className="lb-spinner" aria-hidden="true" />
              <div className="text-muted">Loading…</div>
            </div>
          ) : (
            <div className="card-glass lb-table-wrap">
              <table className="lb-table">
                <thead>
                  <tr>
                    <th className="lb-col-rank">#</th>
                    <th>Player</th>
                    <th className="lb-col-winrate">Win Rate</th>
                    <th className="lb-col-wins">Wins</th>
                    <th className="lb-col-played">Played</th>
                  </tr>
                </thead>

                <tbody>
                  {players.map((p, i) => (
                    <tr key={`${p?.username || 'player'}-${i}`}>
                      <td className="lb-rank">{i + 1}</td>
                      <td className="lb-player">{p.username}</td>
                      <td className="lb-winrate">{p.winRate}</td>
                      <td className="lb-wins">{p.gamesWon}</td>
                      <td className="lb-played">{p.gamesPlayed}</td>
                    </tr>
                  ))}

                  {players.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="lb-empty text-muted">
                        No games played yet.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          )}

          <div className="lb-actions">
            <Link to="/app/menu" className="btn-magenta lb-btn">
              Back to Menu
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
