const baseUrl = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/+$/, '')

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

export async function apiRequest(path, { token, json, headers, ...options } = {}) {
  const requestHeaders = new Headers(headers)
  requestHeaders.set('Accept', 'application/json')
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`)
  if (json !== undefined) requestHeaders.set('Content-Type', 'application/json')

  let response
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: requestHeaders,
      body: json === undefined ? options.body : JSON.stringify(json),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('No se pudo conectar con CuidaPet. Intenta de nuevo.', 0)
  }

  if (response.status === 204) return null

  const data = response.headers.get('Content-Type')?.includes('json')
    ? await response.json().catch(() => null)
    : null

  if (!response.ok) {
    const fallback = response.status === 403
      ? 'No tienes permiso para realizar esta acción.'
      : 'El servidor no pudo completar la solicitud.'
    throw new ApiError(data?.error || fallback, response.status)
  }
  if (data === null) {
    throw new ApiError('El API devolvió una respuesta inesperada.', response.status)
  }

  return data
}
