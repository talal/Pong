// frontend-react/src/pages/Menu.jsx
// main menu using the shared cyber theme and a compact action layout

import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../auth.js'
import './Theme.css'
import './Menu.css'

export default function Menu() {
  const navigate = useNavigate()
  const [isLoggedIn, setIsLoggedIn] = useState(!!auth.token)

  useEffect(() => {
    setIsLoggedIn(!!auth.token)
  }, [])

  function logout() {
    auth.logout()
    setIsLoggedIn(false)
    navigate('/app/menu')
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel menu-panel">
          <div className="menu-topbar">
            <h1 className="cyber-title">Pong</h1>
            <div className="cyber-pill">{isLoggedIn ? 'Logged in' : 'Guest'}</div>
          </div>

          {isLoggedIn ? (
            <div className="menu-actions">
              <Link to="/app/game" className="menu-btn menu-btn-primary">
                Play Pong
              </Link>

              <Link to="/app/leaderboard" className="menu-btn menu-btn-ghost">
                Leaderboard
              </Link>

              <div className="menu-row">
                <Link to="/app/change-password" className="menu-btn menu-btn-warn">
                  Change Password
                </Link>
                <Link to="/app/change-avatar" className="menu-btn menu-btn-cyan">
                  Change Profile Picture
                </Link>
              </div>

              <button type="button" onClick={logout} className="menu-btn menu-btn-danger">
                Logout
              </button>
            </div>
          ) : (
            <div className="menu-actions">
              <Link to="/app/signup" className="menu-btn menu-btn-primary">
                Sign Up
              </Link>

              <Link to="/app/login" className="menu-btn menu-btn-ghost">
                Sign In
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
