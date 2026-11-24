import { useEffect, useState } from 'react'
import { apiRequest } from '../api/apiClient.js'
import { AuthContext } from './authContext.js'

const storageKey = 'cuidapet.session'
const expiredMessage = 'Tu sesión venció o ya no es válida. Inicia sesión de nuevo.'

function signedOut(message = '') {
  return { credentials: null, user: null, message }
}

function readStoredSession() {
  try {
    const credentials = JSON.parse(sessionStorage.getItem(storageKey))
    if (!credentials?.accessToken || !Number.isFinite(credentials.expiresAt)) {
      return signedOut()
    }
    return credentials.expiresAt > Date.now()
      ? { credentials, user: null, message: '' }
      : signedOut(expiredMessage)
  } catch {
    return signedOut()
  }
}

export default function AuthProvider({ children }) {
  const [session, setSession] = useState(readStoredSession)
  const { credentials, user, message } = session
  const checkingSession = Boolean(credentials && !user)

  useEffect(() => {
    try {
      if (credentials) sessionStorage.setItem(storageKey, JSON.stringify(credentials))
      else sessionStorage.removeItem(storageKey)
    } catch {
      // La sesión sigue funcionando en memoria si el navegador bloquea el almacenamiento.
    }
  }, [credentials])

  useEffect(() => {
    if (!checkingSession) return

    const controller = new AbortController()
    apiRequest('/auth/me', { token: credentials.accessToken, signal: controller.signal })
      .then((currentUser) => {
        if (controller.signal.aborted) return
        setSession((active) => active.credentials === credentials
          ? { ...active, user: currentUser }
          : active)
      })
      .catch((error) => {
        if (controller.signal.aborted) return
        const reason = error.status === 401
          ? expiredMessage
          : 'No se pudo verificar tu sesión. Intenta iniciar sesión de nuevo.'
        setSession((active) => active.credentials === credentials ? signedOut(reason) : active)
      })

    return () => controller.abort()
  }, [checkingSession, credentials])

  useEffect(() => {
    if (!credentials) return

    const timer = window.setTimeout(() => {
      setSession((active) => active.credentials === credentials ? signedOut(expiredMessage) : active)
    }, Math.max(0, credentials.expiresAt - Date.now()))

    return () => window.clearTimeout(timer)
  }, [credentials])

  async function signIn(email, password, signal) {
    const response = await apiRequest('/auth/login', {
      method: 'POST',
      json: { email, password },
      signal,
    })
    const expiresAt = Date.parse(response.expiresAt)
    if (!response.accessToken || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      throw new Error('El servidor no emitió una sesión válida.')
    }

    const currentUser = await apiRequest('/auth/me', { token: response.accessToken, signal })
    if (signal?.aborted) return
    setSession({
      credentials: { accessToken: response.accessToken, expiresAt },
      user: currentUser,
      message: '',
    })
  }

  function signOut() {
    setSession(signedOut())
  }

  return (
    <AuthContext.Provider value={{ user, message, checkingSession, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}
