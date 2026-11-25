const text = (name, label, maxLength, options = {}) => ({ name, label, maxLength, ...options })
const longText = (name, label, maxLength) => text(name, label, maxLength, { as: 'textarea', rows: 3 })
const select = (name, label, options, extra = {}) => ({ name, label, as: 'select', options, ...extra })

export const profileFields = [
  text('fullName', 'Nombre completo', 140, { required: true, autoComplete: 'name' }),
  text('photoUrl', 'URL de la foto de perfil', 500, { inputMode: 'url' }),
  text('phoneNumber', 'Teléfono', 30, { type: 'tel', autoComplete: 'tel' }),
  text('city', 'Ciudad', 100, { autoComplete: 'address-level2' }),
  text('academicProgram', 'Programa académico', 140),
  text('emergencyContact', 'Contacto de emergencia', 140),
  text('emergencyPhone', 'Teléfono de emergencia', 30, { type: 'tel' }),
]

export const petStatuses = {
  ACTIVE: 'Activa',
  ARCHIVED: 'Archivada',
  DELETED: 'Eliminada',
}

export const petSexes = {
  FEMALE: 'Hembra',
  MALE: 'Macho',
  UNKNOWN: 'Desconocido',
}

export function petFields(species, pet) {
  const available = species.filter((item) => item.activeFlag === 'Y' || item.id === pet?.speciesId)
  const speciesOptions = available.map((item) => [
    item.id, item.activeFlag === 'Y' ? item.speciesName : `${item.speciesName} (inactiva)`,
  ])
  const now = new Date()
  const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)

  return [
    text('petName', 'Nombre', 100, { required: true }),
    select('speciesId', 'Especie', [['', 'Selecciona una especie'], ...speciesOptions], { numeric: true, required: true }),
    text('breed', 'Raza', 100),
    { name: 'birthDate', label: 'Fecha de nacimiento', type: 'date', max: localDate },
    select('sex', 'Sexo', [['', 'Sin especificar'], ...Object.entries(petSexes)]),
    { name: 'weightKg', label: 'Peso en kg', type: 'number', numeric: true, min: 0.01, max: 9999.99, step: 0.01 },
    longText('temperament', 'Temperamento', 500),
    longText('careInstructions', 'Instrucciones de cuidado', 1000),
    longText('medicalAlerts', 'Alertas médicas', 1000),
    ...(pet ? [select('petStatus', 'Estado', Object.entries(petStatuses), { required: true })] : []),
  ]
}

export const photoFields = [
  text('photoUrl', 'URL de la foto', 500, { type: 'url', pattern: 'https?://[^\\s]+', required: true }),
  select('mainPhotoFlag', 'Foto principal', [['N', 'No'], ['Y', 'Sí']], { required: true }),
]

export function photoUrl(value) {
  try {
    const url = new URL(value)
    return ['https:', 'http:'].includes(url.protocol) ? url.href : null
  } catch {
    return null
  }
}
