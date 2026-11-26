import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import { petFields, petStatuses } from './fields.js'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'

export default function PetsPage() {
  const { user, request } = useAuth()
  const ownerId = user.userId
  const load = useCallback(async (signal) => {
    if (!ownerId) throw new Error('Actualiza el backend de CuidaPet para gestionar tus mascotas.')
    const [pets, species] = await Promise.all([
      request('/pets', { signal }),
      request('/pet-species', { signal }),
    ])
    return { pets: pets.filter((pet) => pet.ownerId === ownerId), species }
  }, [ownerId, request])
  const { result, reload } = useRemoteData(load)
  const editor = useEditor()

  function save(values) {
    editor.submit(() => request('/pets', {
      method: 'POST', json: { ownerId, ...values },
    }), 'Mascota registrada.', reload)
  }

  return (
    <section className="panel">
      <h1>Mis mascotas</h1>
      <Feedback kind="success">{editor.notice}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando tus mascotas…">
        {({ pets, species }) => (
          <>
            {editor.edit ? (
              <RecordForm title="Registrar mascota" fields={petFields(species)} busy={editor.busy} error={editor.error}
                onCancel={editor.close} onSave={save}
              />
            ) : species.some((item) => item.activeFlag === 'Y') ? (
              <Button onClick={() => editor.open('create')}>Registrar mascota</Button>
            ) : <Feedback>No hay especies activas para registrar mascotas.</Feedback>}
            {pets.length === 0 && <p className="personal-section">Aún no tienes mascotas registradas.</p>}
            <ul className="record-grid">
              {pets.map((pet) => (
                <li className="record-card" key={pet.id}>
                  <h2><Link to={String(pet.id)}>{pet.petName}</Link></h2>
                  <p>{species.find((item) => item.id === pet.speciesId)?.speciesName ?? 'Especie no disponible'} · {petStatuses[pet.petStatus] ?? pet.petStatus}</p>
                </li>
              ))}
            </ul>
          </>
        )}
      </RemoteContent>
    </section>
  )
}
