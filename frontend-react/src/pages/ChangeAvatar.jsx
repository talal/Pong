import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api.js'
import './Theme.css'
import './ChangeAvatar.css'

export default function ChangeAvatar() {
  const [file, setFile] = useState(null)

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [inputKey, setInputKey] = useState(0)

  async function uploadAvatar(e) {
    e.preventDefault()
    if (busy || !file) return

    setBusy(true)
    setError('')
    setSuccess('')

    // avatar upload is multipart, so we do not set content-type manually
    const formData = new FormData()
    formData.append('file', file)

    try {
      // apiFetch keeps auth headers consistent for protected endpoints
      const resp = await apiFetch('/api/user/avatar', {
        method: 'POST',
        body: formData,
      })

      if (!resp.ok) {
        // backend may return json {message/error}, fallback keeps it readable
        let msg = 'Error uploading image'
        try {
          const j = await resp.json()
          msg = j?.error || j?.message || msg
        } catch {}
        setError(typeof msg === 'string' && msg.trim() ? msg : 'Error uploading image')
        return
      }

      setSuccess('Avatar updated successfully.')
      setFile(null)

      // file inputs keep an internal value, so a key forces a clean visual reset
      setInputKey((k) => k + 1)
    } catch {
      setError('Network error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel ca-panel">
          <div className="ca-topbar">
            <h1 className="cyber-title">Change Profile Picture</h1>
            <div className="cyber-pill">Avatar</div>
          </div>

          <form className="ca-form" onSubmit={uploadAvatar} noValidate>
            <div className="ca-field">
              <label className="cyber-label" htmlFor="avatar">
                New Picture
              </label>

              <input
                key={inputKey}
                id="avatar"
                type="file"
                className="ca-file"
                accept="image/png, image/jpeg"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
                required
              />

              <div className="ca-help text-muted">
                Must be less than 1 MiB in size and smaller than 3000 by 3000 pixels.
              </div>
            </div>

            <button type="submit" className="ca-btn ca-btn-cyan" disabled={busy || !file}>
              {busy ? 'Uploading…' : 'Upload'}
            </button>

            <Link to="/app/menu" className="ca-btn ca-btn-ghost">
              Return to Menu
            </Link>

            {error ? (
              <div className="ca-alert ca-alert-error card-glass" role="alert">
                {error}
              </div>
            ) : null}

            {success ? (
              <div className="ca-alert ca-alert-success card-glass" role="status">
                {success}
              </div>
            ) : null}
          </form>
        </div>
      </div>
    </div>
  )
}
