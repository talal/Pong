import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../auth.js'
import './Theme.css'
import './Menu.css'

export default function Menu() {
  const navigate = useNavigate()

  // local state is used so the menu updates instantly after logout
  const [isLoggedIn, setIsLoggedIn] = useState(!!auth.token)

  useEffect(() => {
    // initialize from auth when the page mounts
    setIsLoggedIn(!!auth.token)
  }, [])

  function logout() {
    // clears token and any persisted auth state inside auth.js
    auth.logout()

    // update ui immediately without waiting for navigation
    setIsLoggedIn(false)

    // keep the user on the menu after logout
    navigate('/app/menu')
  }

  return (
    <div className="cyber-page">
      <div className="cyber-wrap">
        <div className="cyber-panel menu-panel">
          <div className="menu-topbar">
            <h1 className="cyber-title">Pong</h1>

            {/* this pill reflects auth state and helps confirm logout worked */}
            <div className="cyber-pill">{isLoggedIn ? 'Logged in' : 'Guest'}</div>
          </div>

          {isLoggedIn ? (
            <div className="menu-actions">
              {/* primary action when authenticated */}
              <Link to="/app/game" className="menu-btn menu-btn-primary">
                Play Pong
              </Link>

              <Link to="/app/leaderboard" className="menu-btn menu-btn-ghost">
                Leaderboard
              </Link>

              {/* two related profile actions shown side-by-side on larger screens */}
              <div className="menu-row">
                <Link to="/app/change-password" className="menu-btn menu-btn-pass">
                  Change Password
                </Link>
                <Link to="/app/change-avatar" className="menu-btn menu-btn-cyan">
                  Change Profile Picture
                </Link>
              </div>

              {/* button is used here because it performs an action (clears auth) */}
              <button type="button" onClick={logout} className="menu-btn menu-btn-logout">
                Logout
              </button>
            </div>
          ) : (
            <div className="menu-actions">
              {/* guest-only actions */}
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
