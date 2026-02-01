import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import './Theme.css'
import './Leaderboard.css'

export default function Leaderboard() {
  // players is the parsed array from /api/leaderboard
  const [players, setPlayers] = useState([])

  // loading controls the spinner vs table rendering
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // prevents state updates if the component unmounts before fetch completes
    let cancelled = false

    ;(async () => {
      try {
        // apiFetch is expected to attach auth headers and handle base url/proxy settings
        const response = await apiFetch('/api/leaderboard')

        // on error status we stop and show the empty table state
        if (!response.ok) return

        const data = await response.json()

        // backend should return an array, this avoids runtime errors
        if (!cancelled) setPlayers(Array.isArray(data) ? data : [])
      } catch (e) {
        // network errors, server down, bad proxy, etc.
        console.error('failed to load leaderboard', e)
      } finally {
        // always stop the spinner, even if the request failed
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
              {/* decorative spinner */}
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
                    // username can repeat in edge cases, adding index keeps the key stable enough for this table
                    <tr key={`${p?.username || 'player'}-${i}`}>
                      <td className="lb-rank">{i + 1}</td>
                      <td className="lb-player">{p.username}</td>
                      <td className="lb-winrate">{p.winRate}</td>
                      <td className="lb-wins">{p.gamesWon}</td>
                      <td className="lb-played">{p.gamesPlayed}</td>
                    </tr>
                  ))}

                  {/* empty state when the backend returns [] or the request failed */}
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
