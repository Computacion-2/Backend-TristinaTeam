import { useCallback, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useRemoteData from '../hooks/useRemoteData.js'
import { formatDate } from './requestDetails.js'

export default function NotificationsPage() {
  const { request } = useAuth()
  const [actionState, setActionState] = useState({ error: '', pendingId: null })
  const load = useCallback((signal) => request('/notifications', { signal }), [request])
  const { result, reload, replace } = useRemoteData(load)
  const update = useCallback(async (item) => {
    setActionState({ error: '', pendingId: item.id })
    const readFlag = item.readFlag === 'Y' ? 'N' : 'Y'
    try {
      const saved = await request(`/notifications/${item.id}`, { method: 'PUT', json: { readFlag } })
      replace((current) => current.map((notification) => notification.id === item.id ? saved : notification))
    } catch (error) {
      setActionState({ error: error.message, pendingId: null })
      return
    }
    setActionState({ error: '', pendingId: null })
  }, [request, replace])

  return (
    <section className="panel">
      <h1>Notificaciones</h1>
      <Feedback kind="error">{actionState.error}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando notificaciones…">
        {(notifications) => notifications.length === 0 ? <p>No tienes notificaciones por ahora.</p> : (
          <ul className="record-grid">
            {notifications.map((item) => (
              <li className={`record-card notification-card${item.readFlag === 'Y' ? ' notification-read' : ''}`} key={item.id}>
                <div className="notification-heading">
                  <h2>{item.title}</h2>
                  <span>{item.readFlag === 'Y' ? 'Leída' : 'Sin leer'}</span>
                </div>
                <p>{item.messageText}</p>
                <p className="muted-text">{item.notificationType} · {formatDate(item.createdAt)}</p>
                {item.referenceEntity === 'CARE_BOOKING' && item.referenceId && (
                  <Link to={`/reservas/${item.referenceId}`}>Abrir reserva</Link>
                )}
                <Button variant="secondary" busy={actionState.pendingId === item.id} disabled={actionState.pendingId !== null} onClick={() => update(item)}>
                  {item.readFlag === 'Y' ? 'Marcar como no leída' : 'Marcar como leída'}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </RemoteContent>
    </section>
  )
}
