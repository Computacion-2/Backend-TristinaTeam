import { useCallback } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'
import { applicationStatuses, formatDate, requestStatuses } from './requestDetails.js'

const applicationFields = [
  { name: 'proposedRate', label: 'Tarifa propuesta en COP', type: 'number', numeric: true, min: 0.01, max: 9999999999.99, step: 0.01, required: true },
  { name: 'applicationMessage', label: 'Mensaje para el propietario', as: 'textarea', rows: 4, maxLength: 1000 },
]

export default function OpenRequestPage() {
  const { requestId: rawRequestId } = useParams()
  const requestId = Number(rawRequestId)
  if (!/^[1-9]\d*$/.test(rawRequestId) || !Number.isSafeInteger(requestId)) {
    return <section className="panel"><h1>Solicitud no encontrada</h1><Link to="/solicitudes-abiertas">Volver a solicitudes abiertas</Link></section>
  }
  return <OpenRequestDetail key={requestId} requestId={requestId} />
}

function OpenRequestDetail({ requestId }) {
  const { user, request } = useAuth()
  const caregiverId = user.userId
  const editor = useEditor()
  const load = useCallback(async (signal) => {
    const [careRequest, types, offerings, applications] = await Promise.all([
      request(`/care-requests/${requestId}`, { signal }),
      request('/service-types', { signal }),
      request('/caregiver-services', { signal }),
      request('/care-applications', { signal }),
    ])
    return {
      careRequest,
      serviceName: types.find((item) => item.id === careRequest.serviceTypeId)?.serviceName ?? 'Cuidado de mascota',
      offering: offerings.find((item) => item.caregiverId === caregiverId && item.serviceTypeId === careRequest.serviceTypeId && item.activeFlag === 'Y'),
      application: applications.find((item) => item.requestId === requestId && item.caregiverId === caregiverId),
    }
  }, [request, caregiverId, requestId])
  const { result, reload } = useRemoteData(load)

  function validate(values, data) {
    if (!(Number(values.proposedRate) > 0)) return 'Indica una tarifa mayor que cero.'
    if (data.careRequest.maxBudget != null && Number(values.proposedRate) > Number(data.careRequest.maxBudget)) return 'La tarifa supera el presupuesto máximo de la solicitud.'
    if (new Date(data.careRequest.startAt).getTime() <= Date.now()) return 'La solicitud ya comenzó y no admite postulaciones.'
    return ''
  }

  function save(data, values) {
    const current = data.application
    if (editor.edit.mode === 'withdraw') {
      editor.submit(() => request(`/care-applications/${current.id}`, { method: 'PUT', json: {
        proposedRate: current.proposedRate,
        applicationMessage: current.applicationMessage,
        applicationStatus: 'WITHDRAWN',
        responseReason: null,
      } }), 'Postulación retirada.', reload)
      return
    }
    const body = { proposedRate: values.proposedRate, applicationMessage: values.applicationMessage }
    const action = current
      ? () => request(`/care-applications/${current.id}`, { method: 'PUT', json: { ...body, applicationStatus: 'PENDING', responseReason: null } })
      : () => request('/care-applications', { method: 'POST', json: { requestId, caregiverId, ...body } })
    editor.submit(action, current ? 'Postulación actualizada.' : 'Postulación enviada.', reload)
  }

  return (
    <section className="panel">
      <Link to="/solicitudes-abiertas">Volver a solicitudes abiertas</Link>
      <RemoteContent result={result} reload={reload} loading="Cargando el detalle…">
        {(data) => {
          const { careRequest, application, offering } = data
          const canApply = careRequest.requestStatus === 'OPEN' && new Date(careRequest.startAt).getTime() > Date.now() && Boolean(offering)
          const pending = application?.applicationStatus === 'PENDING'
          return (
            <>
              <h1 className="pet-title">{data.serviceName}</h1>
              <Feedback kind="success">{editor.notice}</Feedback>
              <dl className="account-info personal-details">
                {[
                  ['Estado', requestStatuses[careRequest.requestStatus] ?? careRequest.requestStatus],
                  ['Inicio', formatDate(careRequest.startAt)], ['Finalización', formatDate(careRequest.endAt)],
                  ['Ciudad', careRequest.city], ['Zona', careRequest.zoneName],
                  ['Referencia', careRequest.addressReference], ['Notas', careRequest.requestNotes],
                  ['Presupuesto máximo', careRequest.maxBudget == null ? 'Sin límite indicado' : formatCop(careRequest.maxBudget)],
                ].filter(([, value]) => value).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
              </dl>

              <section className="personal-section" aria-labelledby="your-application-title">
                <h2 id="your-application-title">Tu postulación</h2>
                {application ? (
                  <>
                    <p>{applicationStatuses[application.applicationStatus] ?? application.applicationStatus} · {formatCop(application.proposedRate)}</p>
                    {application.applicationMessage && <p className="caregiver-bio">{application.applicationMessage}</p>}
                    {application.responseReason && <p>Respuesta del propietario: {application.responseReason}</p>}
                    {!editor.edit && pending && <div className="form-actions">
                      <Button disabled={!canApply} onClick={() => editor.open('edit')}>Editar postulación</Button>
                      <Button variant="secondary" onClick={() => editor.open('withdraw')}>Retirar postulación</Button>
                    </div>}
                    {editor.edit?.mode === 'withdraw' && (
                      <div className="form-panel">
                        <h3>Retirar postulación</h3>
                        <p>Dejarás de participar en esta solicitud.</p>
                        <Feedback kind="error">{editor.error}</Feedback>
                        <div className="form-actions">
                          <Button variant="danger" busy={editor.busy} busyLabel="Retirando…" onClick={() => save(data, {})}>Confirmar retiro</Button>
                          <Button variant="secondary" disabled={editor.busy} onClick={editor.close}>Conservar postulación</Button>
                        </div>
                      </div>
                    )}
                  </>
                ) : canApply ? (
                  <p>La tarifa base que configuraste para este servicio es {formatCop(offering.baseRate)}. Puedes enviar una propuesta.</p>
                ) : !offering ? <p>Para postularte, primero activa este servicio desde <Link to="/panel-cuidador">tu panel</Link>.</p>
                  : <p>Esta solicitud ya no acepta postulaciones.</p>}
                {!editor.edit && !application && canApply && <Button onClick={() => editor.open('apply')}>Postularme</Button>}
                {(editor.edit?.mode === 'apply' || editor.edit?.mode === 'edit') && (
                  <RecordForm title={editor.edit.mode === 'apply' ? 'Enviar postulación' : 'Editar postulación'}
                    fields={applicationFields} record={editor.edit.mode === 'edit' ? application : { proposedRate: offering.baseRate }}
                    busy={editor.busy} error={editor.error} validate={(values) => validate(values, data)}
                    onCancel={editor.close} onSave={(values) => save(data, values)} />
                )}
              </section>
            </>
          )
        }}
      </RemoteContent>
    </section>
  )
}
