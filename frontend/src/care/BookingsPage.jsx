import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { bookingStatuses, formatDate } from './requestDetails.js'

export default function BookingsPage() {
  const { user, request } = useAuth()
  const load = useCallback(async (signal) => {
    const [bookings, requests, services] = await Promise.all([
      request('/care-bookings', { signal }),
      request('/care-requests', { signal }),
      request('/service-types', { signal }),
    ])
    return {
      bookings,
      requestsById: new Map(requests.map((item) => [item.id, item])),
      serviceNames: new Map(services.map((item) => [item.id, item.serviceName])),
    }
  }, [request])
  const { result, reload } = useRemoteData(load)

  return (
    <section className="panel">
      <h1>Mis reservas</h1>
      <RemoteContent result={result} reload={reload} loading="Cargando tus reservas…">
        {({ bookings, requestsById, serviceNames }) => bookings.length === 0 ? (
          <p>Aún no tienes reservas de cuidado.</p>
        ) : (
          <ul className="record-grid">
            {bookings.map((booking) => {
              const careRequest = requestsById.get(booking.requestId)
              return (
                <li className="record-card" key={booking.id}>
                  <h2><Link to={String(booking.id)}>{serviceNames.get(careRequest?.serviceTypeId) ?? 'Reserva de cuidado'} #{booking.id}</Link></h2>
                  <p>{careRequest ? `${careRequest.city}${careRequest.zoneName ? ` · ${careRequest.zoneName}` : ''}` : `Solicitud #${booking.requestId}`}</p>
                  {careRequest && <p>{formatDate(careRequest.startAt)}–{formatDate(careRequest.endAt)}</p>}
                  <p>{bookingStatuses[booking.bookingStatusCode] ?? booking.bookingStatusCode} · {formatCop(booking.agreedRate)}</p>
                  <p>{user.userId === booking.ownerId ? `Cuidador #${booking.caregiverId}` : `Propietario #${booking.ownerId}`}</p>
                  <Link className="action secondary" to={String(booking.id)}>Ver reserva</Link>
                </li>
              )
            })}
          </ul>
        )}
      </RemoteContent>
    </section>
  )
}
