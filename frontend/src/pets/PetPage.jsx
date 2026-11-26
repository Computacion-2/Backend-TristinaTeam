import { useCallback } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Confirmation from '../components/Confirmation.jsx'
import Feedback from '../components/Feedback.jsx'
import { petFields, petSexes, petStatuses } from './fields.js'
import PhotoGallery from './PhotoGallery.jsx'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'

export default function PetPage() {
  const { petId } = useParams()
  if (!/^[1-9]\d*$/.test(petId) || !Number.isSafeInteger(Number(petId))) {
    return (
      <section className="panel">
        <h1>Mascota no encontrada</h1>
        <Link to="/mascotas">Volver a mis mascotas</Link>
      </section>
    )
  }
  return <PetDetails key={petId} petId={Number(petId)} />
}

function PetDetails({ petId }) {
  const { user, request } = useAuth()
  const ownerId = user.userId
  const navigate = useNavigate()
  const path = `/pets/${petId}`
  const load = useCallback(async (signal) => {
    if (!ownerId) throw new Error('Actualiza el backend de CuidaPet para gestionar tus mascotas.')
    const [pet, species] = await Promise.all([
      request(path, { signal }),
      request('/pet-species', { signal }),
    ])
    if (pet.ownerId !== ownerId) throw new Error('Esta mascota no pertenece a tu cuenta.')
    return { pet, species }
  }, [ownerId, path, request])
  const { result, reload, replace } = useRemoteData(load)
  const editor = useEditor()

  function save(values) {
    editor.submit(
      () => request(path, { method: 'PUT', json: values }),
      'Mascota actualizada.',
      (pet) => replace((current) => ({ ...current, pet })),
    )
  }

  return (
    <section className="panel">
      <Link to="/mascotas">Volver a mis mascotas</Link>
      <RemoteContent result={result} reload={reload} loading="Cargando datos de la mascota…">
        {({ pet, species }) => (
          <>
            <h1 className="pet-title">{pet.petName}</h1>
            <Feedback kind="success">{editor.notice}</Feedback>
            <dl className="account-info personal-details">
              {[
                ['Especie', species.find((item) => item.id === pet.speciesId)?.speciesName],
                ['Estado', petStatuses[pet.petStatus]],
                ['Raza', pet.breed],
                ['Fecha de nacimiento', pet.birthDate],
                ['Sexo', petSexes[pet.sex]],
                ['Peso', pet.weightKg && `${pet.weightKg} kg`],
                ['Temperamento', pet.temperament],
                ['Instrucciones de cuidado', pet.careInstructions],
                ['Alertas médicas', pet.medicalAlerts],
              ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}
            </dl>
            {!editor.edit && (
              <div className="form-actions">
                <Button onClick={() => editor.open('edit', pet)}>Editar mascota</Button>
                <Button variant="danger" onClick={() => editor.open('remove', pet)}>Eliminar mascota</Button>
              </div>
            )}
            {editor.edit?.mode === 'remove' && (
              <Confirmation title="Eliminar mascota" confirmLabel="Eliminar mascota" busyLabel="Eliminando…"
                busy={editor.busy} error={editor.error} onCancel={editor.close}
                onConfirm={() => editor.submit(
                  () => request(path, { method: 'DELETE' }), 'Mascota eliminada.', () => navigate('/mascotas', { replace: true }),
                )}
              >
                <p>Se eliminará el registro de {pet.petName} y sus fotos. Puedes editar su estado a Archivada si deseas conservarlos.</p>
              </Confirmation>
            )}
            {editor.edit?.mode === 'edit' && (
              <RecordForm title="Editar mascota" fields={petFields(species, pet)} record={editor.edit.record}
                busy={editor.busy} error={editor.error} onCancel={editor.close} onSave={save}
              />
            )}
            <PhotoGallery petId={pet.id} petName={pet.petName} />
          </>
        )}
      </RemoteContent>
    </section>
  )
}
