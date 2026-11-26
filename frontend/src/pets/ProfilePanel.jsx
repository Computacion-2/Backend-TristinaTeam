import { useCallback } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Confirmation from '../components/Confirmation.jsx'
import Feedback from '../components/Feedback.jsx'
import { photoUrl, profileFields } from './fields.js'
import RecordForm from '../components/RecordForm.jsx'
import RemoteContent from '../components/RemoteContent.jsx'
import useEditor from '../hooks/useEditor.js'
import useRemoteData from '../hooks/useRemoteData.js'

export default function ProfilePanel() {
  const { user, request } = useAuth()
  const userId = user.userId
  const path = `/user-profiles/${userId}`
  const load = useCallback(async (signal) => {
    if (!userId) throw new Error('Actualiza el backend de CuidaPet para gestionar tu perfil.')
    try {
      return await request(path, { signal })
    } catch (error) {
      if (error.status === 404) return null
      throw error
    }
  }, [path, request, userId])
  const { result, reload } = useRemoteData(load)
  const editor = useEditor()

  function save(values) {
    const creating = editor.edit.mode === 'create'
    const url = creating ? '/user-profiles' : path
    const json = creating ? { userId, ...values } : values
    editor.submit(() => request(url, { method: creating ? 'POST' : 'PUT', json }), 'Perfil guardado.', reload)
  }

  return (
    <section className="personal-section" aria-labelledby="profile-title">
      <h2 id="profile-title">Mi perfil</h2>
      <Feedback kind="success">{editor.notice}</Feedback>
      <RemoteContent result={result} reload={reload} loading="Cargando tu perfil…">
        {(profile) => {
          const image = profile ? photoUrl(profile.photoUrl) : null
          return (
            <>
              {profile ? (
                <div className="profile-details">
                  {image && <img className="profile-photo" src={image} alt="Foto de perfil" referrerPolicy="no-referrer" />}
                  <dl className="account-info">
                    {profileFields.filter(({ name }) => name !== 'photoUrl').map(({ name, label }) => (
                      <div key={name}><dt>{label}</dt><dd>{profile[name] || '—'}</dd></div>
                    ))}
                  </dl>
                </div>
              ) : <p>Aún no tienes un perfil personal.</p>}
              {!editor.edit && (
                <div className="form-actions">
                  <Button onClick={() => editor.open(profile ? 'edit' : 'create', profile)}>
                    {profile ? 'Editar perfil' : 'Crear perfil'}
                  </Button>
                  {profile && <Button variant="danger" onClick={() => editor.open('remove', profile)}>Eliminar perfil</Button>}
                </div>
              )}
              {editor.edit?.mode === 'remove' && (
                <Confirmation title="Eliminar perfil" confirmLabel="Eliminar perfil" busyLabel="Eliminando…"
                  busy={editor.busy} error={editor.error} onCancel={editor.close}
                  onConfirm={() => editor.submit(() => request(path, { method: 'DELETE' }), 'Perfil eliminado.', reload)}
                >
                  <p>Se eliminarán tus datos de perfil. Tu cuenta seguirá activa.</p>
                </Confirmation>
              )}
              {editor.edit && editor.edit.mode !== 'remove' && (
                <RecordForm
                  title={profile ? 'Editar perfil' : 'Crear perfil'}
                  fields={profileFields} record={profile ?? undefined} busy={editor.busy} error={editor.error}
                  onCancel={editor.close} onSave={save}
                />
              )}
            </>
          )
        }}
      </RemoteContent>
    </section>
  )
}
