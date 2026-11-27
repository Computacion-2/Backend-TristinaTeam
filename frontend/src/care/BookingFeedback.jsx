import { useCallback } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { formatDate } from './requestDetails.js'

const incidentFields = [
  { name: 'reportReason', label: 'Motivo', maxLength: 200, required: true },
  { name: 'reportDetails', label: 'Descripción', as: 'textarea', rows: 4, maxLength: 2000, required: true },
  { name: 'evidenceUrl', label: 'URL de evidencia (opcional)', type: 'url', maxLength: 500 },
]
const reviewFields = [
  { name: 'rating', label: 'Calificación', as: 'select', options: [['', 'Selecciona de 1 a 5'], [1, '1 estrella'], [2, '2 estrellas'], [3, '3 estrellas'], [4, '4 estrellas'], [5, '5 estrellas']], numeric: true, required: true },
  { name: 'reviewComment', label: 'Comentario (opcional)', as: 'textarea', rows: 4, maxLength: 1000 },
]

function safeUrl(value) {
  if (!value) return ''
  try {
    const parsed = new URL(value)
    return ['http:', 'https:'].includes(parsed.protocol) ? value : ''
  } catch {
    return ''
  }
}

export default function BookingFeedback({ booking }) {
  const { user, request } = useAuth()
  const currentUserId = user.userId
  const incidentEditor = useEditor()
  const reviewEditor = useEditor()
  const load = useCallback(async (signal) => {
    const [incidents, reviews] = await Promise.all([
      request('/incident-reports', { signal }),
      request('/reviews', { signal }),
    ])
    return {
      incidents: incidents.filter((item) => item.bookingId === booking.id),
      reviews: reviews.filter((item) => item.bookingId === booking.id),
    }
  }, [request, booking.id])
  const { result, reload } = useRemoteData(load)

  function saveIncident(values) {
    const existing = incidentEditor.edit?.mode === 'edit' ? incidentEditor.edit.record : null
    const evidenceUrl = safeUrl(values.evidenceUrl) || null
    const body = { ...values, evidenceUrl, reportStatus: 'OPEN', resolutionNotes: null }
    const action = existing
      ? () => request(`/incident-reports/${existing.id}`, { method: 'PUT', json: body })
      : () => request('/incident-reports', { method: 'POST', json: {
        reporterId: currentUserId,
        reportedUserId: booking.ownerId === currentUserId ? booking.caregiverId : booking.ownerId,
        requestId: booking.requestId,
        bookingId: booking.id,
        ...values,
        evidenceUrl,
      } })
    incidentEditor.submit(action, existing ? 'Reporte actualizado.' : 'Incidente reportado.', reload)
  }

  function saveReview(values) {
    const existing = reviewEditor.edit?.mode === 'edit' ? reviewEditor.edit.record : null
    const body = { rating: values.rating, reviewComment: values.reviewComment }
    const action = existing
      ? () => request(`/reviews/${existing.id}`, { method: 'PUT', json: body })
      : () => request('/reviews', { method: 'POST', json: { bookingId: booking.id, reviewerId: currentUserId, ...body } })
    reviewEditor.submit(action, existing ? 'Reseña actualizada.' : 'Reseña publicada.', reload)
  }

  return (
    <RemoteContent result={result} reload={reload} loading="Cargando reportes y reseñas…">
      {({ incidents, reviews }) => {
        const ownReview = reviews.find((item) => item.reviewerId === currentUserId)
        return (
          <>
            <section className="personal-section" aria-labelledby="incidents-title">
              <h2 id="incidents-title">Incidentes</h2>
              <Feedback kind="success">{incidentEditor.notice}</Feedback>
              {incidents.length === 0 ? <p>No has reportado incidentes asociados a esta reserva.</p> : (
                <ul className="record-grid">
                  {incidents.map((incident) => (
                    <li className="record-card" key={incident.id}>
                      <h3>{incident.reportReason}</h3>
                      <p>{incident.reportStatus === 'IN_REVIEW' ? 'En revisión' : incident.reportStatus === 'RESOLVED' ? 'Resuelto' : incident.reportStatus === 'DISMISSED' ? 'Cerrado' : 'Abierto'}</p>
                      <p className="caregiver-bio">{incident.reportDetails}</p>
                      {safeUrl(incident.evidenceUrl) && <a href={incident.evidenceUrl} target="_blank" rel="noreferrer">Ver evidencia</a>}
                      {incident.resolutionNotes && <p>Resolución: {incident.resolutionNotes}</p>}
                      {incident.reportStatus === 'OPEN' && !incidentEditor.edit && (
                        <Button variant="secondary" onClick={() => incidentEditor.open('edit', incident)}>Editar reporte abierto</Button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {!incidentEditor.edit && <Button variant="secondary" onClick={() => incidentEditor.open('create')}>Reportar incidente</Button>}
              {incidentEditor.edit?.mode === 'create' && (
                <RecordForm title="Reportar incidente" fields={incidentFields} busy={incidentEditor.busy} error={incidentEditor.error}
                  onCancel={incidentEditor.close} onSave={saveIncident} />
              )}
              {incidentEditor.edit?.mode === 'edit' && (
                <RecordForm title="Editar reporte abierto" fields={incidentFields} record={incidentEditor.edit.record}
                  busy={incidentEditor.busy} error={incidentEditor.error} onCancel={incidentEditor.close} onSave={saveIncident} />
              )}
            </section>

            <section className="personal-section" aria-labelledby="reviews-title">
              <h2 id="reviews-title">Reseñas del cuidado</h2>
              <Feedback kind="success">{reviewEditor.notice}</Feedback>
              {reviews.length === 0 ? <p>Esta reserva aún no tiene reseñas.</p> : (
                <ul className="record-grid">
                  {reviews.map((item) => (
                    <li className="record-card" key={item.id}>
                      <h3>{item.rating} de 5 estrellas</h3>
                      <p>Usuario #{item.reviewerId} reseñó a #{item.reviewedUserId}</p>
                      {item.reviewComment && <p className="caregiver-bio">{item.reviewComment}</p>}
                      <p className="muted-text">{formatDate(item.createdAt)}</p>
                      {item.reviewerId === currentUserId && !reviewEditor.edit && (
                        <Button variant="secondary" onClick={() => reviewEditor.open('edit', item)}>Editar reseña</Button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {booking.bookingStatusCode === 'COMPLETED' && !ownReview && !reviewEditor.edit && (
                <Button onClick={() => reviewEditor.open('create')}>Dejar reseña</Button>
              )}
              {(reviewEditor.edit?.mode === 'create' || reviewEditor.edit?.mode === 'edit') && (
                <RecordForm title={reviewEditor.edit.mode === 'create' ? 'Dejar reseña' : 'Editar reseña'} fields={reviewFields}
                  record={reviewEditor.edit.record ?? {}} busy={reviewEditor.busy} error={reviewEditor.error}
                  onCancel={reviewEditor.close} onSave={saveReview} />
              )}
            </section>
          </>
        )
      }}
    </RemoteContent>
  )
}
