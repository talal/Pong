import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { auth } from './auth.js'

import Menu from './pages/Menu.jsx'
import Login from './pages/Login.jsx'
import SignUp from './pages/SignUp.jsx'
import Game from './pages/Game.jsx'
import Leaderboard from './pages/Leaderboard.jsx'
import ChangePassword from './pages/ChangePassword.jsx'
import ChangeAvatar from './pages/ChangeAvatar.jsx'

function RequireAuth({ children }) {
  if (!auth.token) return <Navigate to="/app/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app/menu" replace />} />
      <Route path="/app/menu" element={<Menu />} />
      <Route path="/app/login" element={<Login />} />
      <Route path="/app/signup" element={<SignUp />} />

      <Route
        path="/app/game"
        element={
          <RequireAuth>
            <Game />
          </RequireAuth>
        }
      />
      <Route
        path="/app/leaderboard"
        element={
          <RequireAuth>
            <Leaderboard />
          </RequireAuth>
        }
      />
      <Route
        path="/app/change-password"
        element={
          <RequireAuth>
            <ChangePassword />
          </RequireAuth>
        }
      />
      <Route
        path="/app/change-avatar"
        element={
          <RequireAuth>
            <ChangeAvatar />
          </RequireAuth>
        }
      />

      <Route path="*" element={<Navigate to="/app/menu" replace />} />
    </Routes>
  )
}
