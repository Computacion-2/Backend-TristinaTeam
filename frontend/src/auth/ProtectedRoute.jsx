import { Link, Navigate, Outlet, useLocation } from 'react-router'
import Feedback from '../components/Feedback.jsx'
import { hasAccess } from './access.js'
import { useAuth } from './authContext.js'

export default function ProtectedRoute({ access }) {
  const { user, checkingSession } = useAuth()
  const { pathname, search, hash } = useLocation()

  if (checkingSession) return <Feedback kind="loading">Verificando tu sesión…</Feedback>

  if (!user) {
    return <Navigate to="/login" replace state={{ from: pathname + search + hash }} />
  }

  if (!hasAccess(user, access)) {
    return (
      <section className="panel">
        <h1>Acceso denegado</h1>
        <p>Tu cuenta no tiene permisos para ver esta página.</p>
        <Link className="action" to="/cuenta">Volver a mi cuenta</Link>
      </section>
    )
  }

  return <Outlet />
}
