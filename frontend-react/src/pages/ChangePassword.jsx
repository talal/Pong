import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import './Theme.css'
import './ChangePassword.css'

export default function ChangePassword() {
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function changePassword(e) {
    e.preventDefault()
    if (busy) return

    setBusy(true)
    setError('')
    setSuccess('')

    try {
      // apiFetch keeps auth headers consistent across protected endpoints
      const resp = await apiFetch('/api/user/change_password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ oldPassword, newPassword }),
      })

      if (!resp.ok) {
        // backend sometimes returns json {message|error}; fallback keeps it readable
        let msg = 'Error changing password'
        try {
          const j = await resp.json()
          msg = j?.error || j?.message || msg
        } catch {}
        setError(typeof msg === 'string' && msg.trim() ? msg : 'Error changing password')
        return
      }

      setSuccess('Password changed successfully.')
      setOldPassword('')
      setNewPassword('')
    } catch {
      // catches fetch/network/cors issues, not backend validation
      setError('Network error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel cp-panel">
          <div className="cp-topbar">
            <h1 className="cyber-title">Change Password</h1>
            <div className="cyber-pill">Account</div>
          </div>

          <form className="cp-form" onSubmit={changePassword} noValidate>
            <div className="cp-field">
              <label className="cyber-label" htmlFor="oldPassword">
                Old Password
              </label>
              <input
                id="oldPassword"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                type="password"
                className="cyber-input"
                autoComplete="current-password"
                required
              />
            </div>

            <div className="cp-field">
              <label className="cyber-label" htmlFor="newPassword">
                New Password
              </label>
              <input
                id="newPassword"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                type="password"
                className="cyber-input"
                autoComplete="new-password"
                required
              />
            </div>

            <button type="submit" className="cp-btn cp-btn-warn" disabled={busy}>
              {busy ? 'Changing…' : 'Change Password'}
            </button>

            <Link to="/app/menu" className="cp-btn cp-btn-ghost">
              Return to Menu
            </Link>

            {error ? (
              <div className="cp-alert cp-alert-error card-glass" role="alert">
                {error}
              </div>
            ) : null}

            {success ? (
              <div className="cp-alert cp-alert-success card-glass" role="status">
                {success}
              </div>
            ) : null}
          </form>
        </div>
      </div>
    </div>
  )
}
