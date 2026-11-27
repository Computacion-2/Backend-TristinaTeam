import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { formatDate } from './requestDetails.js'

export default function OpenRequestsPage() {
  const { user, request } = useAuth()
  const caregiverId = user.userId
  const load = useCallback(async (signal) => {
    const [requests, applications, types, offerings] = await Promise.all([
      request('/care-requests', { signal }),
      request('/care-applications', { signal }),
      request('/service-types', { signal }),
      request('/caregiver-services', { signal }),
    ])
    return {
      requests: requests.filter((item) => item.requestStatus === 'OPEN'
        && item.ownerId !== caregiverId && new Date(item.startAt).getTime() > Date.now()),
      applications: applications.filter((item) => item.caregiverId === caregiverId),
      typeNames: new Map(types.map((item) => [item.id, item.serviceName])),
      offerings: offerings.filter((item) => item.caregiverId === caregiverId && item.activeFlag === 'Y'),
    }
  }, [request, caregiverId])
  const { result, reload } = useRemoteData(load)

  return (
    <section className="panel">
      <h1>Solicitudes abiertas</h1>
      <p>Explora cuidados que coinciden con tus servicios y consulta cada solicitud antes de postularte.</p>
      <RemoteContent result={result} reload={reload} loading="Buscando solicitudes abiertas…">
        {({ requests, applications, typeNames, offerings }) => {
          if (requests.length === 0) return <p>No hay solicitudes abiertas disponibles por ahora.</p>
          return (
            <ul className="record-grid">
              {requests.map((item) => {
                const application = applications.find((entry) => entry.requestId === item.id)
                const matchingOffering = offerings.find((entry) => entry.serviceTypeId === item.serviceTypeId)
                return (
                  <li className="record-card" key={item.id}>
                    <h2><Link to={String(item.id)}>{typeNames.get(item.serviceTypeId) ?? 'Cuidado de mascota'}</Link></h2>
                    <p>{item.city}{item.zoneName ? ` · ${item.zoneName}` : ''}</p>
                    <p>{formatDate(item.startAt)}–{formatDate(item.endAt)}</p>
                    <p>{item.maxBudget == null ? 'Sin presupuesto máximo' : `Presupuesto: ${formatCop(item.maxBudget)}`}</p>
                    {application && <p>Tu postulación: {application.applicationStatus === 'PENDING' ? 'Pendiente' : application.applicationStatus === 'ACCEPTED' ? 'Aceptada' : application.applicationStatus === 'WITHDRAWN' ? 'Retirada' : 'Rechazada'}</p>}
                    {!matchingOffering && <p>No ofreces este servicio en tu perfil.</p>}
                    <Link className="action secondary" to={String(item.id)}>Ver detalle</Link>
                  </li>
                )
              })}
            </ul>
          )
        }}
      </RemoteContent>
    </section>
  )
}
