import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import './Theme.css'
import './SignUp.css'

export default function SignUp() {
  // controlled inputs so the ui always reflects current values
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // file is stored separately because it must be sent as multipart/form-data
  const [file, setFile] = useState(null)

  // busy prevents double submits and gives instant feedback
  const [busy, setBusy] = useState(false)

  // error and success are mutually exclusive messages shown under the buttons
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function signup(e) {
    e.preventDefault()

    // avoids sending multiple requests if the user clicks twice
    if (busy) return

    setBusy(true)
    setError('')
    setSuccess('')

    // backend expects multipart so we can upload avatar + fields in one request
    const formData = new FormData()
    formData.append('email', email)
    formData.append('password', password)

    // only append the file if the user picked one
    if (file) formData.append('file', file)

    try {
      // do not set content-type manually; fetch will add the correct boundary
      const resp = await fetch('/api/auth/process_signup', {
        method: 'POST',
        body: formData,
      })

      // handle non-2xx responses without throwing
      if (!resp.ok) {
        // backend may return json {error|message}; fallback keeps it readable
        let msg = `Error: ${resp.status}`
        try {
          const j = await resp.json()
          msg = j?.error || j?.message || msg
        } catch {
          // ignore parse errors and use the fallback msg
        }

        setError(typeof msg === 'string' && msg.trim() ? msg : `Error: ${resp.status}`)
        return
      }

      // success state is displayed until the next submit
      setSuccess('You signed up successfully.')

      // reset inputs so the user sees a clean form again
      setEmail('')
      setPassword('')
      setFile(null)
    } catch {
      // fetch throws on network failures (server down, cors, offline)
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
                // trim avoids accidental leading/trailing spaces in the stored state
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
                // keeps the file picker limited to common image formats
                accept="image/png, image/jpeg"
                // store the first selected file or clear if the user cancels
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />

              <div className="signup-help text-muted">
                Must be less than 1 MiB in size and smaller than 3000 by 3000 pixels.
              </div>
            </div>

            <button type="submit" className="btn-magenta signup-btn" disabled={busy}>
              {/* shows a small feedback state while the request is running */}
              {busy ? 'Signing up…' : 'Sign Up'}
            </button>

            {/* link instead of button so it does not submit the form */}
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
