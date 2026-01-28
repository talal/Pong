import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../auth.js'
import './Theme.css'
import './Login.css'

export default function Login() {
  const navigate = useNavigate()

  // form state
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // ui state
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function login(e) {
    e.preventDefault()
    if (busy) return

    setBusy(true)
    setError('')

    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })

      if (!resp.ok) {
        if (resp.status === 401) {
          setError('Bad credentials')
          return
        }

        try {
          const data = await resp.json()
          setError(data?.error || data?.message || `Error: ${resp.status}`)
        } catch {
          setError(`Error: ${resp.status} ${resp.statusText}`)
        }
        return
      }

      const data = await resp.json()
      auth.token = data.accessToken
      navigate('/app/menu')
    } catch {
      setError('Network error (CORS/connection)')
    } finally {
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
              {busy ? 'Logging in…' : 'Login'}
            </button>

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
