import { useCallback } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Confirmation from '../components/Confirmation.jsx'
import Feedback from '../components/Feedback.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'

const weekDays = [
  [1, 'Lunes'], [2, 'Martes'], [3, 'Miércoles'], [4, 'Jueves'],
  [5, 'Viernes'], [6, 'Sábado'], [7, 'Domingo'],
]
const profileFields = [
  { name: 'biography', label: 'Presentación', as: 'textarea', rows: 4, maxLength: 1200, required: true },
  { name: 'experienceYears', label: 'Años de experiencia', type: 'number', numeric: true, min: 0, max: 999, required: true },
  { name: 'serviceCity', label: 'Ciudad de servicio', maxLength: 100, required: true },
  { name: 'serviceZone', label: 'Zona de servicio', maxLength: 160 },
  { name: 'activeFlag', label: 'Visibilidad en el directorio', as: 'select', options: [['Y', 'Activo'], ['N', 'Pausado']], required: true },
]
const newProfileFields = profileFields.filter(({ name }) => name !== 'activeFlag')
const availabilityFields = [
  { name: 'dayOfWeek', label: 'Día', as: 'select', options: [['', 'Selecciona un día'], ...weekDays], numeric: true, required: true },
  { name: 'startTime', label: 'Hora de inicio', type: 'time', required: true },
  { name: 'endTime', label: 'Hora de finalización', type: 'time', required: true },
  { name: 'validFrom', label: 'Vigente desde (opcional)', type: 'date' },
  { name: 'validUntil', label: 'Vigente hasta (opcional)', type: 'date' },
]

function availabilityFormFields(editing) {
  return editing ? [...availabilityFields, {
    name: 'activeFlag', label: 'Horario', as: 'select', options: [['Y', 'Activo'], ['N', 'Inactivo']], required: true,
  }] : availabilityFields
}

function formRecord(record) {
  return {
    ...record,
    startTime: record.startTime?.slice(0, 5),
    endTime: record.endTime?.slice(0, 5),
  }
}

export default function CaregiverPanelPage() {
  const { user, request } = useAuth()
  const caregiverId = user.userId
  const editor = useEditor()

  const load = useCallback(async (signal) => {
    if (!caregiverId) throw new Error('La sesión no incluye el identificador de la cuenta.')
    const [profileResult, availabilities, offerings, serviceTypes] = await Promise.all([
      request(`/caregiver-profiles/${caregiverId}`, { signal }).then((profile) => ({ profile })).catch((error) => {
        if (error.status === 404) return { profile: null }
        throw error
      }),
      request('/caregiver-availabilities', { signal }),
      request('/caregiver-services', { signal }),
      request('/service-types', { signal }),
    ])
    return {
      ...profileResult,
      availabilities: availabilities.filter((item) => item.caregiverId === caregiverId),
      offerings: offerings.filter((item) => item.caregiverId === caregiverId),
      serviceTypes: serviceTypes.filter((item) => item.activeFlag === 'Y'),
    }
  }, [request, caregiverId])
  const { result, reload } = useRemoteData(load)

  function save(data, values) {
    const { mode, record } = editor.edit
    const submit = (action, message) => editor.submit(action, message, reload)
    if (mode === 'profile') {
      const body = data.profile ? values : { ...values, caregiverId }
      if (!data.profile) delete body.activeFlag
      submit(() => request(data.profile ? `/caregiver-profiles/${caregiverId}` : '/caregiver-profiles', {
        method: data.profile ? 'PUT' : 'POST', json: body,
      }), data.profile ? 'Perfil actualizado.' : 'Perfil creado.')
      return
    }
    if (mode === 'availability') {
      const body = { ...values, caregiverId }
      submit(() => request('/caregiver-availabilities', { method: 'POST', json: body }), 'Horario agregado.')
      return
    }
    if (mode === 'editAvailability') {
      const body = { ...values }
      submit(() => request(`/caregiver-availabilities/${record.id}`, { method: 'PUT', json: body }), 'Horario actualizado.')
      return
    }
    if (mode === 'offering') {
      submit(() => request('/caregiver-services', { method: 'POST', json: { caregiverId, ...values } }), 'Servicio agregado.')
      return
    }
    const body = { baseRate: values.baseRate, activeFlag: values.activeFlag }
    submit(() => request(`/caregiver-services/${caregiverId}/${record.serviceTypeId}`, { method: 'PUT', json: body }), 'Servicio actualizado.')
  }

  function removeAvailability(item) {
    editor.submit(() => request(`/caregiver-availabilities/${item.id}`, { method: 'DELETE' }), 'Horario eliminado.', reload)
  }

  function validateTime(values) {
    if (values.startTime >= values.endTime) return 'La hora de inicio debe ser anterior a la hora de finalización.'
    if (values.validFrom && values.validUntil && values.validUntil < values.validFrom) return 'La fecha final debe ser posterior a la fecha inicial.'
    return ''
  }

  return (
    <section className="panel">
      <h1>Panel del cuidador</h1>
      <p>Administra tu presentación, los horarios disponibles y las tarifas que ven los propietarios.</p>
      <Feedback kind="success">{editor.notice}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando tu perfil de cuidador…">
        {(data) => {
          const typeById = new Map(data.serviceTypes.map((item) => [item.id, item.serviceName]))
          const editingAvailability = editor.edit?.mode === 'availability' || editor.edit?.mode === 'editAvailability'
          const isEditingOffering = editor.edit?.mode === 'editOffering'
          const isProfileEditor = editor.edit?.mode === 'profile'
          const canManageServices = data.profile?.activeFlag === 'Y'

          return (
            <>
              <section className="personal-section" aria-labelledby="caregiver-profile-title">
                <h2 id="caregiver-profile-title">Perfil público</h2>
                {data.profile ? (
                  <>
                    <p>Estado: {data.profile.activeFlag === 'Y' ? 'Visible en el directorio' : 'Pausado'}</p>
                    <dl className="account-info personal-details">
                      <div><dt>Ciudad</dt><dd>{data.profile.serviceCity}</dd></div>
                      <div><dt>Zona</dt><dd>{data.profile.serviceZone || 'Sin zona especificada'}</dd></div>
                      <div><dt>Experiencia</dt><dd>{data.profile.experienceYears} años</dd></div>
                    </dl>
                    <p className="caregiver-bio">{data.profile.biography}</p>
                  </>
                ) : <p>Aún no has creado tu perfil. Completa la información para aparecer en el directorio.</p>}
                {!editor.edit && <Button onClick={() => editor.open('profile')}>{data.profile ? 'Editar perfil' : 'Crear perfil'}</Button>}
                {isProfileEditor && (
                  <RecordForm title={data.profile ? 'Editar perfil' : 'Crear perfil'} fields={data.profile ? profileFields : newProfileFields}
                    record={data.profile ?? { activeFlag: 'Y', experienceYears: 0 }} busy={editor.busy} error={editor.error}
                    onCancel={editor.close} onSave={(values) => save(data, values)} />
                )}
              </section>

              <section className="personal-section" aria-labelledby="caregiver-availability-title">
                <h2 id="caregiver-availability-title">Horarios de disponibilidad</h2>
                {!canManageServices && <Feedback>Activa tu perfil para agregar horarios y servicios.</Feedback>}
                {data.availabilities.length === 0 ? <p>No has registrado horarios.</p> : (
                  <ul className="record-grid">
                    {data.availabilities.map((item) => (
                      <li className="record-card" key={item.id}>
                        <h3>{weekDays.find(([day]) => day === item.dayOfWeek)?.[1] ?? `Día ${item.dayOfWeek}`}</h3>
                        <p>{item.startTime.slice(0, 5)}–{item.endTime.slice(0, 5)}</p>
                        <p>{item.validFrom || 'Sin inicio'} · {item.validUntil || 'Sin vencimiento'}</p>
                        <p>{item.activeFlag === 'Y' ? 'Activo' : 'Inactivo'}</p>
                        <div className="form-actions">
                          <Button variant="secondary" disabled={Boolean(editor.edit)} onClick={() => editor.open('editAvailability', item)}>Editar</Button>
                          <Button variant="danger" disabled={Boolean(editor.edit || editor.busy)} onClick={() => editor.open('deleteAvailability', item)}>Eliminar</Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
                {canManageServices && !editor.edit && <Button onClick={() => editor.open('availability')}>Agregar horario</Button>}
                {editingAvailability && (
                  <RecordForm title={editor.edit.mode === 'availability' ? 'Agregar horario' : 'Editar horario'}
                    fields={availabilityFormFields(editor.edit.mode === 'editAvailability')}
                    record={editor.edit.mode === 'editAvailability' ? formRecord(editor.edit.record) : {}}
                    busy={editor.busy} error={editor.error} validate={validateTime} onCancel={editor.close}
                    onSave={(values) => save(data, values)} />
                )}
              </section>

              <section className="personal-section" aria-labelledby="caregiver-services-title">
                <h2 id="caregiver-services-title">Servicios y tarifas</h2>
                {data.offerings.length === 0 ? <p>Aún no has configurado servicios.</p> : (
                  <ul className="record-grid">
                    {data.offerings.map((item) => (
                      <li className="record-card" key={item.serviceTypeId}>
                        <h3>{typeById.get(item.serviceTypeId) ?? 'Servicio no disponible'}</h3>
                        <p>{formatCop(item.baseRate)} · {item.activeFlag === 'Y' ? 'Activo' : 'Inactivo'}</p>
                        <Button variant="secondary" disabled={Boolean(editor.edit)} onClick={() => editor.open('editOffering', item)}>Editar tarifa o estado</Button>
                      </li>
                    ))}
                  </ul>
                )}
                {canManageServices && !editor.edit && data.serviceTypes.some((type) => !data.offerings.some((item) => item.serviceTypeId === type.id)) && (
                  <Button onClick={() => editor.open('offering')}>Agregar servicio</Button>
                )}
                {editor.edit?.mode === 'offering' && (
                  <RecordForm title="Agregar servicio" busy={editor.busy} error={editor.error} onCancel={editor.close}
                    onSave={(values) => save(data, values)} fields={[
                      { name: 'serviceTypeId', label: 'Servicio', as: 'select', required: true, numeric: true,
                        options: [['', 'Selecciona un servicio'], ...data.serviceTypes.filter((type) => !data.offerings.some((item) => item.serviceTypeId === type.id)).map((type) => [type.id, type.serviceName])] },
                      { name: 'baseRate', label: 'Tarifa base en COP', type: 'number', numeric: true, min: 0.01, max: 9999999999.99, step: 0.01, required: true },
                    ]} />
                )}
                {isEditingOffering && (
                  <RecordForm title={`Editar ${typeById.get(editor.edit.record.serviceTypeId) ?? 'servicio'}`}
                    fields={[
                      { name: 'baseRate', label: 'Tarifa base en COP', type: 'number', numeric: true, min: 0.01, max: 9999999999.99, step: 0.01, required: true },
                      { name: 'activeFlag', label: 'Disponibilidad', as: 'select', options: [['Y', 'Activo'], ['N', 'Inactivo']], required: true },
                    ]} record={editor.edit.record} busy={editor.busy} error={editor.error} onCancel={editor.close}
                    onSave={(values) => save(data, values)} />
                )}
              </section>
              {editor.edit?.mode === 'deleteAvailability' && (
                <Confirmation title="Eliminar horario" confirmLabel="Eliminar" busyLabel="Eliminando…" busy={editor.busy}
                  error={editor.error} onCancel={editor.close} onConfirm={() => removeAvailability(editor.edit.record)}>
                  <p>Este horario dejará de aparecer como disponibilidad.</p>
                </Confirmation>
              )}
            </>
          )
        }}
      </RemoteContent>
    </section>
  )
}
