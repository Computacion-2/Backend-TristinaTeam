import { useCallback } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Confirmation from '../components/Confirmation.jsx'
import Feedback from '../components/Feedback.jsx'
import { photoFields, photoUrl } from './fields.js'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'

export default function PhotoGallery({ petId, petName }) {
  const { request } = useAuth()
  const load = useCallback(async (signal) => {
    const photos = await request('/pet-photos', { signal })
    return photos.filter((photo) => photo.petId === petId)
      .sort((first, second) => Number(second.mainPhotoFlag === 'Y') - Number(first.mainPhotoFlag === 'Y'))
  }, [petId, request])
  const { result, reload } = useRemoteData(load)
  const editor = useEditor()

  function save(values) {
    const creating = editor.edit.mode === 'create'
    const path = creating ? '/pet-photos' : `/pet-photos/${editor.edit.record.id}`
    const json = creating ? { petId, ...values } : values
    editor.submit(() => request(path, { method: creating ? 'POST' : 'PUT', json }), 'Foto guardada.', reload)
  }

  return (
    <section className="personal-section" aria-labelledby="photos-title">
      <h2 id="photos-title">Fotos</h2>
      <Feedback kind="success">{editor.notice}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando fotos…">
        {(photos) => (
          <>
            {!editor.edit && <Button onClick={() => editor.open('create', { mainPhotoFlag: 'N' })}>Agregar foto</Button>}
            {editor.edit?.mode === 'remove' && (
              <Confirmation title="Quitar foto" confirmLabel="Quitar foto" busyLabel="Quitando…"
                busy={editor.busy} error={editor.error} onCancel={editor.close}
                onConfirm={() => editor.submit(
                  () => request(`/pet-photos/${editor.edit.record.id}`, { method: 'DELETE' }), 'Foto eliminada.', reload,
                )}
              >
                <p>Se quitará el enlace de la galería de {petName}.</p>
              </Confirmation>
            )}
            {editor.edit && editor.edit.mode !== 'remove' && (
              <RecordForm title={editor.edit.mode === 'create' ? 'Agregar foto' : 'Editar foto'}
                fields={photoFields} record={editor.edit.record} busy={editor.busy} error={editor.error}
                onCancel={editor.close} onSave={save}
              />
            )}
            {photos.length === 0 && <p className="personal-section">Aún no hay fotos.</p>}
            <ul className="record-grid photo-grid">
              {photos.map((photo, index) => {
                const image = photoUrl(photo.photoUrl)
                return (
                  <li className="record-card" key={photo.id}>
                    {image && (
                      <a href={image} target="_blank" rel="noreferrer" aria-label={`Abrir foto ${index + 1} de ${petName}`}>
                        <img src={image} alt={`Foto ${index + 1} de ${petName}`} loading="lazy" referrerPolicy="no-referrer" />
                      </a>
                    )}
                    <p>{photo.mainPhotoFlag === 'Y' ? 'Foto principal' : `Foto ${index + 1}`}</p>
                    <div className="form-actions">
                      <Button variant="secondary" disabled={Boolean(editor.edit)} onClick={() => editor.open('edit', photo)}>Editar</Button>
                      <Button variant="danger" disabled={Boolean(editor.edit)} onClick={() => editor.open('remove', photo)}>Quitar</Button>
                    </div>
                  </li>
                )
              })}
            </ul>
          </>
        )}
      </RemoteContent>
    </section>
  )
}
