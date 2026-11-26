import { useCallback, useState } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import { formatCop } from '../formatters.js'
import useRemoteData from '../hooks/useRemoteData.js'

function CaregiverCard({ caregiverId, profile, services, own, favorite, pending, disabled, onToggle }) {
  const title = own ? 'Tu perfil de cuidador' : `Cuidador #${caregiverId}`

  return (
    <li className="record-card caregiver-card">
      <h2>{title}</h2>
      {profile ? (
        <>
          <p>{[profile.serviceCity, profile.serviceZone].filter(Boolean).join(' · ')}</p>
          <p>{profile.experienceYears} {profile.experienceYears === 1 ? 'año' : 'años'} de experiencia</p>
          <p className="caregiver-bio">{profile.biography}</p>
          <h3>Servicios ofrecidos</h3>
          {services.length > 0 ? (
            <ul>
              {services.map((service) => (
                <li key={service.serviceTypeId}>{service.name} · {formatCop(service.baseRate)}</li>
              ))}
            </ul>
          ) : <p>Sin servicios activos.</p>}
        </>
      ) : <p>Este perfil ya no está disponible en el directorio.</p>}
      {!own && (
        <Button
          variant={favorite ? 'secondary' : 'primary'}
          disabled={disabled}
          busy={pending}
          busyLabel={favorite ? 'Quitando…' : 'Guardando…'}
          onClick={() => onToggle(caregiverId, favorite)}
        >
          {favorite ? 'Quitar de favoritos' : 'Agregar a favoritos'}
        </Button>
      )}
    </li>
  )
}

export default function CaregiversPage() {
  const { user, request } = useAuth()
  const ownerId = user.userId
  const [favoritesOnly, setFavoritesOnly] = useState(false)
  const [pendingId, setPendingId] = useState(null)
  const [message, setMessage] = useState(null)

  const load = useCallback(async (signal) => {
    if (!ownerId) throw new Error('Actualiza el backend de CuidaPet para consultar el directorio.')
    const [profiles, offerings, types, favorites] = await Promise.all([
      request('/caregiver-profiles', { signal }),
      request('/caregiver-services', { signal }),
      request('/service-types', { signal }),
      request('/favorite-caregivers', { signal }),
    ])
    const profilesById = new Map(profiles.filter((item) => item.activeFlag === 'Y')
      .map((profile) => [profile.caregiverId, profile]))
    const typesById = new Map(types.filter((type) => type.activeFlag === 'Y')
      .map((type) => [type.id, type.serviceName]))
    const servicesByCaregiver = new Map()
    for (const offering of offerings) {
      const name = typesById.get(offering.serviceTypeId)
      if (offering.activeFlag !== 'Y' || !name || !profilesById.has(offering.caregiverId)) continue
      const services = servicesByCaregiver.get(offering.caregiverId) ?? []
      services.push({ ...offering, name })
      servicesByCaregiver.set(offering.caregiverId, services)
    }
    const favoriteIds = favorites.filter((favorite) => favorite.ownerId === ownerId)
      .map((favorite) => favorite.caregiverId)
    return { profilesById, servicesByCaregiver, favoriteIds }
  }, [request, ownerId])

  const { result, reload, replace } = useRemoteData(load)

  async function changeFavorite(caregiverId, favorite) {
    if (pendingId !== null) return
    setPendingId(caregiverId)
    setMessage(null)
    try {
      if (favorite) {
        await request(`/favorite-caregivers/${ownerId}/${caregiverId}`, { method: 'DELETE' })
      } else {
        await request('/favorite-caregivers', { method: 'POST', json: { ownerId, caregiverId } })
      }
      replace((current) => ({
        ...current,
        favoriteIds: favorite
          ? current.favoriteIds.filter((id) => id !== caregiverId)
          : [...current.favoriteIds, caregiverId],
      }))
      setMessage({ kind: 'success', text: favorite ? 'Cuidador quitado de favoritos.' : 'Cuidador agregado a favoritos.' })
    } catch (error) {
      setMessage({ kind: 'error', text: error.message })
      if (error.status === 404 || error.status === 409) reload()
    } finally {
      setPendingId(null)
    }
  }

  return (
    <section className="panel">
      <h1>Directorio de cuidadores</h1>
      <div className="form-actions directory-views" role="group" aria-label="Vista de cuidadores">
        <Button variant={favoritesOnly ? 'secondary' : 'primary'} aria-pressed={!favoritesOnly}
          onClick={() => setFavoritesOnly(false)}>Directorio</Button>
        <Button variant={favoritesOnly ? 'primary' : 'secondary'} aria-pressed={favoritesOnly}
          onClick={() => setFavoritesOnly(true)}>Mis favoritos</Button>
      </div>
      <Feedback kind={message?.kind}>{message?.text}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando cuidadores…">
        {({ profilesById, servicesByCaregiver, favoriteIds }) => {
          const caregiverIds = favoritesOnly ? favoriteIds : [...profilesById.keys()]
          if (caregiverIds.length === 0) {
            return <p>{favoritesOnly ? 'Aún no tienes cuidadores favoritos.' : 'No hay cuidadores en el directorio.'}</p>
          }
          return (
            <ul className="record-grid">
              {caregiverIds.map((id) => (
                <CaregiverCard key={id} caregiverId={id} profile={profilesById.get(id)}
                  services={servicesByCaregiver.get(id) ?? []} own={id === ownerId}
                  favorite={favoriteIds.includes(id)} pending={pendingId === id}
                  disabled={pendingId !== null} onToggle={changeFavorite} />
              ))}
            </ul>
          )
        }}
      </RemoteContent>
    </section>
  )
}
