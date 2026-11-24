import { useEffect, useRef, useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/authContext.js'

export default function LoginPage() {
  const { user, message, checkingSession, signIn } = useAuth()
  const { state } = useLocation()
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const activeRequest = useRef(null)

  useEffect(() => () => activeRequest.current?.abort(), [])

  if (checkingSession) return <p role="status">Verificando tu sesión…</p>
  if (user) return <Navigate to={state?.from || '/cuenta'} replace />

  async function handleSubmit(event) {
    event.preventDefault()
    if (activeRequest.current) return

    const fields = new FormData(event.currentTarget)
    const controller = new AbortController()
    activeRequest.current = controller
    setError('')
    setSubmitting(true)

    try {
      await signIn(fields.get('email').trim(), fields.get('password'), controller.signal)
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure.message)
    } finally {
      activeRequest.current = null
      if (!controller.signal.aborted) setSubmitting(false)
    }
  }

  return (
    <section className="panel auth-panel">
      <h1>Iniciar sesión</h1>
      {message && <p role="status">{message}</p>}
      <form className="login-form" onSubmit={handleSubmit}>
        <label htmlFor="email">Correo electrónico</label>
        <input id="email" name="email" type="email" autoComplete="username" maxLength={160} required />
        <label htmlFor="password">Contraseña</label>
        <input id="password" name="password" type="password" autoComplete="current-password" maxLength={72} required />
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="action" type="submit" disabled={submitting}>
          {submitting ? 'Iniciando sesión…' : 'Ingresar'}
        </button>
      </form>
    </section>
  )
}
