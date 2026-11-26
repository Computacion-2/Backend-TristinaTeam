import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { formatDate, loadRequestCatalogs, requestFormFields, requestStatuses, requestValues, validateDates } from './requestDetails.js'

export default function RequestsPage() {
  const { user, request } = useAuth()
  const ownerId = user.userId
  const load = useCallback(async (signal) => {
    if (!ownerId) throw new Error('Actualiza el backend de CuidaPet para gestionar tus solicitudes.')
    const [requests, catalogs] = await Promise.all([
      request('/care-requests', { signal }),
      loadRequestCatalogs(request, ownerId, signal),
    ])
    return { ...catalogs, requests: requests.filter((item) => item.ownerId === ownerId) }
  }, [request, ownerId])
  const { result, reload } = useRemoteData(load)
  const editor = useEditor()

  function publish(values) {
    editor.submit(
      () => request('/care-requests', { method: 'POST', json: { ownerId, ...requestValues(values) } }),
      'Solicitud publicada.',
      reload,
    )
  }

  return (
    <section className="panel">
      <h1>Mis solicitudes</h1>
      <Feedback kind="success">{editor.notice}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando solicitudes…">
        {({ requests, pets, services }) => (
          <>
            {editor.edit ? (
              <RecordForm title="Publicar solicitud" fields={requestFormFields(pets, services)}
                busy={editor.busy} error={editor.error} validate={validateDates}
                onCancel={editor.close} onSave={publish} />
            ) : !pets.some((pet) => pet.petStatus === 'ACTIVE') ? (
              <p>Necesitas una mascota activa para publicar. <Link to="/mascotas">Ir a mis mascotas</Link></p>
            ) : !services.some((service) => service.activeFlag === 'Y') ? (
              <Feedback>No hay tipos de servicio activos.</Feedback>
            ) : <Button onClick={() => editor.open('create')}>Publicar solicitud</Button>}

            {requests.length === 0 && <p className="personal-section">Aún no tienes solicitudes de cuidado.</p>}
            {requests.length > 0 && (
              <ul className="record-grid">
                {requests.map((item) => {
                  const name = pets.find((pet) => pet.id === item.petId)?.petName ?? 'Mascota'
                  const service = services.find((type) => type.id === item.serviceTypeId)?.serviceName ?? 'Servicio'
                  return (
                    <li className="record-card" key={item.id}>
                      <h2><Link to={String(item.id)}>{service} para {name}</Link></h2>
                      <p>{formatDate(item.startAt)} · {requestStatuses[item.requestStatus] ?? item.requestStatus}</p>
                    </li>
                  )
                })}
              </ul>
            )}
          </>
        )}
      </RemoteContent>
    </section>
  )
}
