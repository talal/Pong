import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../auth.js'
import './Theme.css'
import './Login.css'

export default function Login() {
  const navigate = useNavigate()

  // controlled inputs so the ui always reflects the current form values
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // busy prevents double submits and keeps buttons disabled during the request
  const [busy, setBusy] = useState(false)

  // error is shown inside the form with aria-live so screen readers announce updates
  const [error, setError] = useState('')

  async function login(e) {
    e.preventDefault()

    // avoids sending multiple login requests if the user clicks twice
    if (busy) return

    setBusy(true)
    setError('')

    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },

        // backend expects json with email + password keys
        body: JSON.stringify({ email, password }),
      })

      // handle non-2xx responses without throwing
      if (!resp.ok) {
        // backend uses 401 when credentials are wrong
        if (resp.status === 401) {
          setError('Bad credentials')
          return
        }

        // try to read backend error json, fall back to http status text
        try {
          const data = await resp.json()
          setError(data?.error || data?.message || `Error: ${resp.status}`)
        } catch {
          setError(`Error: ${resp.status} ${resp.statusText}`)
        }
        return
      }

      // expected response: { accessToken: "..." }
      const data = await resp.json()

      // store token in the shared auth module so other pages can use it
      auth.token = data.accessToken

      // go to the menu after successful login
      navigate('/app/menu')
    } catch {
      // fetch throws on network errors (server down, cors blocked, offline, etc.)
      setError('Network error (CORS/connection)')
    } finally {
      // always clear busy so the ui unlocks even if we returned early above
      setBusy(false)
    }
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel login-panel">
          <div className="login-topbar">
            <h1 className="cyber-title">Login</h1>
            <div className="cyber-pill">JWT</div>
          </div>

          <form className="login-form" onSubmit={login} noValidate>
            <div className="login-field">
              <label className="cyber-label" htmlFor="email">
                E-Mail Address
              </label>
              <input
                id="email"
                value={email}
                // trim avoids accidental leading/trailing spaces, but keeps the input responsive
                onChange={(e) => setEmail(e.target.value.trim())}
                type="email"
                className="cyber-input"
                required
                autoComplete="email"
                placeholder="you@example.com"
              />
            </div>

            <div className="login-field">
              <label className="cyber-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type="password"
                className="cyber-input"
                required
                autoComplete="current-password"
                placeholder="••••••••"
              />
            </div>

            <button type="submit" className="btn-magenta login-btn" disabled={busy}>
              {/* shows a small feedback state while the request is running */}
              {busy ? 'Logging in…' : 'Login'}
            </button>

            {/* link instead of button so it does not submit the form */}
            <Link to="/app/menu" className="btn-ghost login-btn login-link">
              Return to Menu
            </Link>

            {error ? (
              <div className="login-error card-glass" aria-live="polite">
                {error}
              </div>
            ) : null}
          </form>
        </div>
      </div>
    </div>
  )
}
