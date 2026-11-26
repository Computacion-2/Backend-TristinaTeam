import Confirmation from '../components/Confirmation.jsx'
import RecordForm from '../components/RecordForm.jsx'
import { formatPrice, requestFormFields, toRequestForm, validateDates } from './requestDetails.js'

const decisionForms = {
  cancel: {
    title: 'Cancelar solicitud',
    fields: [{ name: 'cancellationReason', label: 'Motivo de cancelación', as: 'textarea', rows: 3, maxLength: 500, required: true }],
  },
  reject: {
    title: 'Rechazar postulación',
    fields: [{ name: 'responseReason', label: 'Motivo del rechazo (opcional)', as: 'textarea', rows: 3, maxLength: 500 }],
  },
  accept: {
    title: 'Aceptar postulación y crear reserva',
    fields: [{ name: 'exactAddress', label: 'Dirección exacta del cuidado', hint: 'Se comparte con el cuidador de la reserva.', maxLength: 500, required: true }],
  },
}

export default function RequestEditor({ editor, data, onSave }) {
  if (!editor.edit) return null
  const { careRequest, pets, services } = data
  const { mode, record } = editor.edit
  const common = { busy: editor.busy, error: editor.error, onCancel: editor.close }

  if (mode === 'remove' || mode === 'close') {
    const deleting = mode === 'remove'
    const title = deleting ? 'Eliminar solicitud' : 'Cerrar solicitud'
    return (
      <Confirmation {...common} title={title} confirmLabel={title}
        busyLabel={deleting ? 'Eliminando…' : 'Cerrando…'} onConfirm={() => onSave()}>
        <p>{deleting ? 'Se eliminará la solicitud junto con sus postulaciones.'
          : 'La solicitud se cerrará y las postulaciones pendientes quedarán rechazadas.'}</p>
      </Confirmation>
    )
  }

  if (mode === 'edit') {
    return (
      <RecordForm {...common} title="Editar solicitud" fields={requestFormFields(pets, services, careRequest)}
        record={toRequestForm(careRequest)} validate={(values) => validateDates(values, careRequest)} onSave={onSave} />
    )
  }

  const { title, fields } = decisionForms[mode]
  function validateDecision() {
    return mode === 'accept' && new Date(careRequest.startAt).getTime() <= Date.now()
      ? 'La solicitud ya comenzó.'
      : ''
  }
  return (
    <div>
      {mode === 'accept' && <p>Cuidador #{record.caregiverId} · Tarifa {formatPrice(record.proposedRate)}</p>}
      <RecordForm {...common} title={title} fields={fields} validate={validateDecision} onSave={onSave} />
    </div>
  )
}
