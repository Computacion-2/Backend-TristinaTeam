export const requestStatuses = {
  OPEN: 'Abierta',
  ASSIGNED: 'Asignada',
  CANCELLED: 'Cancelada',
  CLOSED: 'Cerrada',
}

export const applicationStatuses = {
  PENDING: 'Pendiente',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada',
  WITHDRAWN: 'Retirada',
}

export const bookingStatuses = {
  SCHEDULED: 'Programada',
  IN_PROGRESS: 'En curso',
  PENDING_CONFIRMATION: 'Pendiente de confirmación',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
}

const dateFormatter = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
const priceFormatter = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' })

export function formatDate(date) {
  return dateFormatter.format(new Date(date))
}

export function formatPrice(amount) {
  return amount == null ? 'Sin límite indicado' : priceFormatter.format(amount)
}

export async function loadRequestCatalogs(request, ownerId, signal) {
  const [pets, services] = await Promise.all([
    request('/pets', { signal }),
    request('/service-types', { signal }),
  ])
  return { pets: pets.filter((pet) => pet.ownerId === ownerId), services }
}

export function requestFormFields(pets, services, careRequest) {
  const petOptions = pets.filter((pet) => pet.petStatus === 'ACTIVE' || pet.id === careRequest?.petId)
    .map((pet) => [pet.id, pet.petStatus === 'ACTIVE' ? pet.petName : `${pet.petName} (no activa)`])
  const serviceOptions = services.filter((service) => service.activeFlag === 'Y' || service.id === careRequest?.serviceTypeId)
    .map((service) => [service.id, service.activeFlag === 'Y' ? service.serviceName : `${service.serviceName} (inactivo)`])

  return [
    { name: 'petId', label: 'Mascota', as: 'select', options: [['', 'Selecciona una mascota'], ...petOptions], numeric: true, required: true },
    { name: 'serviceTypeId', label: 'Tipo de servicio', as: 'select', options: [['', 'Selecciona un servicio'], ...serviceOptions], numeric: true, required: true },
    { name: 'startAt', label: 'Fecha y hora de inicio', type: 'datetime-local', required: true },
    { name: 'endAt', label: 'Fecha y hora de finalización', type: 'datetime-local', required: true },
    { name: 'city', label: 'Ciudad', maxLength: 100, required: true },
    { name: 'zoneName', label: 'Zona', maxLength: 160 },
    { name: 'addressReference', label: 'Referencia de ubicación', maxLength: 500 },
    { name: 'requestNotes', label: 'Notas de la solicitud', as: 'textarea', rows: 3, maxLength: 1500 },
    { name: 'maxBudget', label: 'Presupuesto máximo en COP', type: 'number', numeric: true, min: 0.01, max: 9999999999.99, step: 0.01 },
  ]
}

export function toRequestForm(careRequest) {
  return Object.fromEntries(requestFormFields([], []).map(({ name }) => {
    const value = careRequest[name]
    return [name, (name === 'startAt' || name === 'endAt') ? value.slice(0, 16) : value]
  }))
}

export function requestValues(form, careRequest) {
  const values = { ...form }
  for (const name of ['startAt', 'endAt']) {
    if (careRequest && values[name] === careRequest[name].slice(0, 16)) {
      values[name] = careRequest[name]
    } else if (values[name]?.length === 16) {
      values[name] += ':00'
    }
  }
  return values
}

export function validateDates(form, careRequest) {
  const { startAt, endAt } = requestValues(form, careRequest)
  const start = new Date(startAt).getTime()
  const end = new Date(endAt).getTime()
  if (!(end > start)) return 'La fecha de finalización debe ser posterior a la de inicio.'
  if ((startAt !== careRequest?.startAt || endAt !== careRequest?.endAt) && !(start > Date.now())) {
    return 'La fecha de inicio debe ser futura.'
  }
  return ''
}
