import { useEffect, useRef, useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import FormField from '../components/FormField.jsx'

export default function LoginPage() {
  const { user, message, checkingSession, signIn } = useAuth()
  const { state } = useLocation()
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const activeRequest = useRef(null)

  useEffect(() => () => activeRequest.current?.abort(), [])

  if (checkingSession) return <Feedback kind="loading">Verificando tu sesión…</Feedback>
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
      <Feedback>{message}</Feedback>
      <form className="login-form" onSubmit={handleSubmit}>
        <FormField label="Correo electrónico" name="email" type="email" autoComplete="username" maxLength={160} required />
        <FormField label="Contraseña" name="password" type="password" autoComplete="current-password" maxLength={72} required />
        <Feedback kind="error">{error}</Feedback>
        <Button type="submit" busy={submitting} busyLabel="Iniciando sesión…">Ingresar</Button>
      </form>
    </section>
  )
}
