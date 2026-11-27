import { useCallback } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { formatCop } from '../formatters.js'
import { bookingStatuses, formatDate } from './requestDetails.js'

const statusFields = [
  { name: 'cancellationReason', label: 'Motivo de cancelación', as: 'textarea', rows: 3, maxLength: 500, required: true },
]
const addressFields = [
  { name: 'exactAddress', label: 'Dirección exacta del cuidado', maxLength: 500, required: true },
]
const updateFields = [
  { name: 'updateMessage', label: 'Novedad', as: 'textarea', rows: 4, maxLength: 1200, required: true },
]
const photoFields = [
  { name: 'photoUrl', label: 'URL de la foto', type: 'url', maxLength: 500, required: true },
]
const activeBookingStatuses = ['SCHEDULED', 'IN_PROGRESS', 'PENDING_CONFIRMATION']

function safeUrl(value) {
  if (!value) return ''
  try {
    const parsed = new URL(value)
    return ['http:', 'https:'].includes(parsed.protocol) ? value : ''
  } catch {
    return ''
  }
}

export default function BookingPage() {
  const { bookingId: rawBookingId } = useParams()
  const bookingId = Number(rawBookingId)
  if (!/^[1-9]\d*$/.test(rawBookingId) || !Number.isSafeInteger(bookingId)) {
    return <section className="panel"><h1>Reserva no encontrada</h1><Link to="/reservas">Volver a mis reservas</Link></section>
  }
  return <BookingDetail key={bookingId} bookingId={bookingId} />
}

function BookingDetail({ bookingId }) {
  const { user, request } = useAuth()
  const currentUserId = user.userId
  const bookingEditor = useEditor()
  const updateEditor = useEditor()
  const photoEditor = useEditor()

  const load = useCallback(async (signal) => {
    const booking = await request(`/care-bookings/${bookingId}`, { signal })
    const [careRequest, services, updates, photos] = await Promise.all([
      request(`/care-requests/${booking.requestId}`, { signal }),
      request('/service-types', { signal }),
      request('/care-updates', { signal }),
      request('/care-update-photos', { signal }),
    ])
    return {
      booking,
      careRequest,
      serviceName: services.find((item) => item.id === careRequest.serviceTypeId)?.serviceName ?? 'Cuidado de mascota',
      updates: updates.filter((item) => item.bookingId === bookingId),
      photos: photos.filter((item) => updates.some((update) => update.id === item.updateId && update.bookingId === bookingId)),
    }
  }, [request, bookingId])
  const { result, reload } = useRemoteData(load)

  function updateBooking(data, status, cancellationReason = null, exactAddress = data.booking.exactAddress) {
    bookingEditor.submit(() => request(`/care-bookings/${bookingId}`, {
      method: 'PUT', json: { bookingStatusCode: status, exactAddress, cancellationReason },
    }), status === 'CANCELLED' ? 'Reserva cancelada.' : 'Estado de la reserva actualizado.', reload)
  }

  function saveUpdate(values) {
    updateEditor.submit(() => request('/care-updates', {
      method: 'POST', json: { bookingId, updateMessage: values.updateMessage },
    }), 'Novedad publicada.', reload)
  }

  function savePhoto(values) {
    const photoUrl = safeUrl(values.photoUrl)
    if (!photoUrl) {
      photoEditor.submit(() => { throw new Error('Ingresa una URL HTTP o HTTPS válida.') }, '', reload)
      return
    }
    photoEditor.submit(() => request('/care-update-photos', {
      method: 'POST', json: { updateId: photoEditor.edit.record.id, photoUrl },
    }), 'Foto agregada a la novedad.', reload)
  }

  return (
    <section className="panel">
      <Link to="/reservas">Volver a mis reservas</Link>
      <RemoteContent result={result} reload={reload} loading="Cargando la reserva…">
        {(data) => {
          const { booking, careRequest } = data
          const owner = booking.ownerId === currentUserId
          const caregiver = booking.caregiverId === currentUserId
          const active = activeBookingStatuses.includes(booking.bookingStatusCode)
          const canTrack = caregiver && ['IN_PROGRESS', 'PENDING_CONFIRMATION'].includes(booking.bookingStatusCode)
          const canChangeAddress = owner && booking.bookingStatusCode === 'SCHEDULED'

          return (
            <>
              <h1 className="pet-title">{data.serviceName} · Reserva #{booking.id}</h1>
              <dl className="account-info personal-details">
                <div><dt>Estado</dt><dd>{bookingStatuses[booking.bookingStatusCode] ?? booking.bookingStatusCode}</dd></div>
                <div><dt>Rol en la reserva</dt><dd>{owner ? 'Propietario' : caregiver ? 'Cuidador' : 'Participante'}</dd></div>
                <div><dt>Contraparte</dt><dd>#{owner ? booking.caregiverId : booking.ownerId}</dd></div>
                <div><dt>Tarifa acordada</dt><dd>{formatCop(booking.agreedRate)}</dd></div>
                <div><dt>Inicio</dt><dd>{formatDate(careRequest.startAt)}</dd></div>
                <div><dt>Finalización</dt><dd>{formatDate(careRequest.endAt)}</dd></div>
                <div><dt>Ciudad y zona</dt><dd>{careRequest.city}{careRequest.zoneName ? ` · ${careRequest.zoneName}` : ''}</dd></div>
                <div><dt>Dirección exacta</dt><dd>{booking.exactAddress}</dd></div>
                {booking.cancellationReason && <div><dt>Motivo de cancelación</dt><dd>{booking.cancellationReason}</dd></div>}
                {careRequest.requestNotes && <div><dt>Notas de la solicitud</dt><dd>{careRequest.requestNotes}</dd></div>}
              </dl>

              <section className="personal-section" aria-labelledby="booking-actions-title">
                <h2 id="booking-actions-title">Acciones de la reserva</h2>
                <Feedback kind="success">{bookingEditor.notice}</Feedback>
                <div className="form-actions">
                  {caregiver && booking.bookingStatusCode === 'SCHEDULED' && <Button disabled={bookingEditor.busy} onClick={() => updateBooking(data, 'IN_PROGRESS')}>Iniciar cuidado</Button>}
                  {caregiver && booking.bookingStatusCode === 'IN_PROGRESS' && <Button disabled={bookingEditor.busy} onClick={() => updateBooking(data, 'PENDING_CONFIRMATION')}>Marcar cuidado terminado</Button>}
                  {owner && booking.bookingStatusCode === 'PENDING_CONFIRMATION' && <Button disabled={bookingEditor.busy} onClick={() => updateBooking(data, 'COMPLETED')}>Confirmar y finalizar</Button>}
                  {canChangeAddress && !bookingEditor.edit && <Button variant="secondary" onClick={() => bookingEditor.open('address')}>Cambiar dirección</Button>}
                  {active && !bookingEditor.edit && <Button variant="danger" onClick={() => bookingEditor.open('cancel')}>Cancelar reserva</Button>}
                </div>
                <Feedback kind="error">{bookingEditor.error}</Feedback>
                {bookingEditor.edit?.mode === 'cancel' && (
                  <RecordForm title="Cancelar reserva" fields={statusFields} busy={bookingEditor.busy} error={bookingEditor.error}
                    onCancel={bookingEditor.close} onSave={(values) => updateBooking(data, 'CANCELLED', values.cancellationReason)} />
                )}
                {bookingEditor.edit?.mode === 'address' && (
                  <RecordForm title="Actualizar dirección" fields={addressFields} record={{ exactAddress: booking.exactAddress }}
                    busy={bookingEditor.busy} error={bookingEditor.error} onCancel={bookingEditor.close}
                    onSave={(values) => updateBooking(data, booking.bookingStatusCode, null, values.exactAddress)} />
                )}
              </section>

              <section className="personal-section" aria-labelledby="care-updates-title">
                <h2 id="care-updates-title">Novedades y fotos</h2>
                <Feedback kind="success">{updateEditor.notice || photoEditor.notice}</Feedback>
                {data.updates.length === 0 ? <p>Aún no hay novedades del cuidado.</p> : (
                  <ul className="record-grid">
                    {data.updates.map((item) => (
                      <li className="record-card update-card" key={item.id}>
                        <p className="muted-text">{formatDate(item.createdAt)}</p>
                        <p className="caregiver-bio">{item.updateMessage}</p>
                        <div className="photo-gallery-inline">
                          {data.photos.filter((photo) => photo.updateId === item.id).map((photo) => (
                            <a key={photo.id} href={photo.photoUrl} target="_blank" rel="noreferrer">
                              <img src={photo.photoUrl} alt="Foto de una novedad del cuidado" loading="lazy" />
                            </a>
                          ))}
                        </div>
                        {canTrack && !photoEditor.edit && <Button variant="secondary" onClick={() => photoEditor.open('photo', item)}>Agregar foto</Button>}
                        {photoEditor.edit?.record.id === item.id && (
                          <RecordForm title="Agregar foto a esta novedad" fields={photoFields} busy={photoEditor.busy} error={photoEditor.error}
                            onCancel={photoEditor.close} onSave={savePhoto} />
                        )}
                      </li>
                    ))}
                  </ul>
                )}
                {caregiver && canTrack && !updateEditor.edit && <Button onClick={() => updateEditor.open('create')}>Publicar novedad</Button>}
                {updateEditor.edit && <RecordForm title="Publicar novedad" fields={updateFields} busy={updateEditor.busy} error={updateEditor.error}
                  onCancel={updateEditor.close} onSave={saveUpdate} />}
                {caregiver && !canTrack && <p>Las novedades se pueden publicar mientras la reserva está en curso o pendiente de confirmación.</p>}
              </section>

            </>
          )
        }}
      </RemoteContent>
    </section>
  )
}
