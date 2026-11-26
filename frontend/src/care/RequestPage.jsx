import { useCallback } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'
import RequestApplications from './RequestApplications.jsx'
import RequestEditor from './RequestEditor.jsx'
import { formatDate, formatPrice, loadRequestCatalogs, requestStatuses, requestValues, toRequestForm } from './requestDetails.js'

export default function RequestPage() {
  const { requestId } = useParams()
  if (!/^[1-9]\d*$/.test(requestId) || !Number.isSafeInteger(Number(requestId))) {
    return (
      <section className="panel">
        <h1>Solicitud no encontrada</h1>
        <Link to="/solicitudes">Volver a mis solicitudes</Link>
      </section>
    )
  }
  return <RequestDetail key={requestId} requestId={Number(requestId)} />
}

function RequestDetail({ requestId }) {
  const { user, request } = useAuth()
  const ownerId = user.userId
  const navigate = useNavigate()
  const path = `/care-requests/${requestId}`
  const load = useCallback(async (signal) => {
    if (!ownerId) throw new Error('Actualiza el backend de CuidaPet para gestionar tus solicitudes.')
    const careRequest = await request(path, { signal })
    if (careRequest.ownerId !== ownerId) throw new Error('Esta solicitud no pertenece a tu cuenta.')
    const [catalogs, applications, bookings] = await Promise.all([
      loadRequestCatalogs(request, ownerId, signal),
      request('/care-applications', { signal }),
      request('/care-bookings', { signal }),
    ])
    return {
      careRequest,
      ...catalogs,
      applications: applications.filter((item) => item.requestId === requestId),
      booking: bookings.find((item) => item.requestId === requestId),
      upcoming: new Date(careRequest.startAt).getTime() > Date.now(),
    }
  }, [request, ownerId, requestId, path])
  const { result, reload } = useRemoteData(load)
  const editor = useEditor()

  function save(careRequest, values) {
    const { mode, record } = editor.edit
    const submit = (url, method, json, notice) => editor.submit(() => request(url, { method, json }), notice, reload)

    if (mode === 'remove') {
      editor.submit(() => request(path, { method: 'DELETE' }), 'Solicitud eliminada.',
        () => navigate('/solicitudes', { replace: true }))
      return
    }
    if (mode === 'accept') {
      submit('/care-bookings', 'POST', { applicationId: record.id, exactAddress: values.exactAddress }, 'Reserva creada.')
      return
    }
    if (mode === 'reject') {
      submit(`/care-applications/${record.id}`, 'PUT', {
        proposedRate: record.proposedRate,
        applicationMessage: record.applicationMessage,
        applicationStatus: 'REJECTED',
        responseReason: values.responseReason,
      }, 'Postulación rechazada.')
      return
    }

    const statuses = { edit: 'OPEN', cancel: 'CANCELLED', close: 'CLOSED' }
    const notices = { edit: 'Solicitud actualizada.', cancel: 'Solicitud cancelada.', close: 'Solicitud cerrada.' }
    const updated = mode === 'edit' ? values : toRequestForm(careRequest)
    submit(path, 'PUT', {
      ...requestValues(updated, careRequest),
      requestStatus: statuses[mode],
      cancellationReason: mode === 'cancel' ? values.cancellationReason : null,
    }, notices[mode])
  }

  return (
    <section className="panel">
      <Link to="/solicitudes">Volver a mis solicitudes</Link>
      <RemoteContent result={result} reload={reload} loading="Cargando la solicitud…">
        {(data) => {
          const { careRequest, pets, services, applications, booking, upcoming } = data
          const name = pets.find((pet) => pet.id === careRequest.petId)?.petName ?? 'mascota'
          const service = services.find((item) => item.id === careRequest.serviceTypeId)?.serviceName ?? 'Servicio'
          const open = careRequest.requestStatus === 'OPEN'
          const canAccept = open && !booking && upcoming
          const canDelete = !booking && careRequest.requestStatus !== 'ASSIGNED'
          return (
            <>
              <h1 className="pet-title">{service} para {name}</h1>
              <Feedback kind="success">{editor.notice}</Feedback>
              <dl className="account-info personal-details">
                {[
                  ['Estado', requestStatuses[careRequest.requestStatus] ?? careRequest.requestStatus],
                  ['Inicio', formatDate(careRequest.startAt)],
                  ['Finalización', formatDate(careRequest.endAt)],
                  ['Ciudad', careRequest.city],
                  ['Zona', careRequest.zoneName],
                  ['Referencia de ubicación', careRequest.addressReference],
                  ['Notas', careRequest.requestNotes],
                  ['Presupuesto máximo', formatPrice(careRequest.maxBudget)],
                  ['Motivo de cancelación', careRequest.cancellationReason],
                ].filter(([, value]) => value).map(([label, value]) => (
                  <div key={label}><dt>{label}</dt><dd>{value}</dd></div>
                ))}
              </dl>
              {!editor.edit && (
                <div className="form-actions">
                  {open && (
                    <>
                      <Button onClick={() => editor.open('edit')}>Editar solicitud</Button>
                      <Button variant="secondary" onClick={() => editor.open('close')}>Cerrar solicitud</Button>
                      <Button variant="secondary" onClick={() => editor.open('cancel')}>Cancelar solicitud</Button>
                    </>
                  )}
                  {canDelete && <Button variant="danger" onClick={() => editor.open('remove')}>Eliminar solicitud</Button>}
                </div>
              )}
              <RequestEditor editor={editor} data={data} onSave={(values) => save(careRequest, values)} />
              <RequestApplications applications={applications} booking={booking} open={open}
                canAccept={canAccept} editing={Boolean(editor.edit)} onDecision={editor.open} />
            </>
          )
        }}
      </RemoteContent>
    </section>
  )
}
