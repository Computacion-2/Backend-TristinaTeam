import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { applicationStatuses, formatDate } from './requestDetails.js'

export default function ApplicationsPage() {
  const { user, request } = useAuth()
  const caregiverId = user.userId
  const load = useCallback(async (signal) => {
    const [applications, requests, types] = await Promise.all([
      request('/care-applications', { signal }),
      request('/care-requests', { signal }),
      request('/service-types', { signal }),
    ])
    const ownApplications = applications.filter((item) => item.caregiverId === caregiverId)
    return {
      applications: ownApplications,
      requestsById: new Map(requests.map((item) => [item.id, item])),
      serviceNames: new Map(types.map((item) => [item.id, item.serviceName])),
    }
  }, [request, caregiverId])
  const { result, reload } = useRemoteData(load)

  return (
    <section className="panel">
      <h1>Mis postulaciones</h1>
      <RemoteContent result={result} reload={reload} loading="Cargando tus postulaciones…">
        {({ applications, requestsById, serviceNames }) => applications.length === 0 ? (
          <p>Aún no tienes postulaciones. <Link to="/solicitudes-abiertas">Explorar solicitudes abiertas</Link></p>
        ) : (
          <ul className="record-grid">
            {applications.map((item) => {
              const careRequest = requestsById.get(item.requestId)
              return (
                <li className="record-card" key={item.id}>
                  <h2><Link to={`/solicitudes-abiertas/${item.requestId}`}>{serviceNames.get(careRequest?.serviceTypeId) ?? 'Solicitud de cuidado'}</Link></h2>
                  {careRequest && <p>{careRequest.city} · {formatDate(careRequest.startAt)}</p>}
                  <p>{applicationStatuses[item.applicationStatus] ?? item.applicationStatus} · {formatCop(item.proposedRate)}</p>
                  {item.applicationMessage && <p className="caregiver-bio">{item.applicationMessage}</p>}
                  {item.responseReason && <p>Respuesta: {item.responseReason}</p>}
                  <Link className="action secondary" to={`/solicitudes-abiertas/${item.requestId}`}>Revisar solicitud</Link>
                </li>
              )
            })}
          </ul>
        )}
      </RemoteContent>
    </section>
  )
}
