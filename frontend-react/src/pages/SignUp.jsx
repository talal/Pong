import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import './Theme.css'
import './SignUp.css'

export default function SignUp() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [file, setFile] = useState(null)

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function signup(e) {
    e.preventDefault()
    if (busy) return

    setBusy(true)
    setError('')
    setSuccess('')

    // the backend expects multipart/form-data so the avatar can be uploaded together with the user
    const formData = new FormData()
    formData.append('email', email)
    formData.append('password', password)
    if (file) formData.append('file', file)

    try {
      // no content-type header here because fetch will set the multipart boundary automatically
      const resp = await fetch('/api/auth/process_signup', {
        method: 'POST',
        body: formData,
      })

      if (!resp.ok) {
        // backend sometimes returns json {message|error}; fallback keeps the error readable
        let msg = `Error: ${resp.status}`
        try {
          const j = await resp.json()
          msg = j?.error || j?.message || msg
        } catch {}
        setError(typeof msg === 'string' && msg.trim() ? msg : `Error: ${resp.status}`)
        return
      }

      setSuccess('You signed up successfully.')
      setEmail('')
      setPassword('')
      setFile(null)
    } catch {
      setError('Network error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel signup-panel">
          <div className="signup-topbar">
            <h1 className="cyber-title">Sign Up</h1>
            <div className="cyber-pill">User</div>
          </div>

          <form className="signup-form" onSubmit={signup} noValidate>
            <div className="signup-field">
              <label className="cyber-label" htmlFor="email">
                E-Mail Address
              </label>
              <input
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value.trim())}
                type="email"
                className="cyber-input"
                placeholder="name@example.com"
                required
                autoComplete="email"
              />
            </div>

            <div className="signup-field">
              <label className="cyber-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type="password"
                className="cyber-input"
                placeholder="••••••••"
                required
                autoComplete="new-password"
              />
            </div>

            <div className="signup-field">
              <label className="cyber-label" htmlFor="avatar">
                Profile Picture (Optional)
              </label>

              <input
                id="avatar"
                type="file"
                className="signup-file"
                accept="image/png, image/jpeg"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />

              <div className="signup-help text-muted">
                Must be less than 1 MiB in size and smaller than 3000 by 3000 pixels.
              </div>
            </div>

            <button type="submit" className="btn-magenta signup-btn" disabled={busy}>
              {busy ? 'Signing up…' : 'Sign Up'}
            </button>

            <Link to="/app/menu" className="btn-ghost signup-btn signup-link">
              Return to Menu
            </Link>

            {error ? (
              <div className="signup-alert signup-alert-error card-glass" role="alert">
                {error}
              </div>
            ) : null}

            {success ? (
              <div className="signup-alert signup-alert-success card-glass" role="status">
                {success}
              </div>
            ) : null}
          </form>
        </div>
      </div>
    </div>
  )
}
