import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/authContext.js'
import Button from '../components/Button.jsx'
import Confirmation from '../components/Confirmation.jsx'
import Feedback from '../components/Feedback.jsx'
import AdminForm from './AdminForm.jsx'
import { catalogs, displayValue, itemPath } from './resources.js'

export default function AdminResource({ resource }) {
  const { request } = useAuth()
  const [list, setList] = useState(null)
  const [revision, setRevision] = useState(0)
  const [editor, setEditor] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const editorTrigger = useRef(null)
  const actionsDisabled = busy || Boolean(editor)

  useEffect(() => {
    const controller = new AbortController()
    const options = { signal: controller.signal }
    const references = resource.catalogs.map(async (name) => [
      name, await request(catalogs[name].path, options),
    ])

    Promise.all([request(resource.path, options), ...references])
      .then(([rows, ...entries]) => {
        if (!controller.signal.aborted) setList({ rows, lookups: Object.fromEntries(entries) })
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setList({ error: failure.message })
      })

    return () => controller.abort()
  }, [request, resource, revision])

  function reload() {
    setList(null)
    setRevision((value) => value + 1)
  }

  function openEditor(mode, record, trigger) {
    editorTrigger.current = trigger
    setError('')
    setNotice('')
    setEditor({ mode, record })
  }

  function cancelEditor() {
    const trigger = editorTrigger.current
    setEditor(null)
    setError('')
    requestAnimationFrame(() => trigger?.focus())
  }

  async function edit(row, trigger) {
    if (busy) return
    setError('')
    setNotice('')
    setBusy(true)
    try {
      openEditor('edit', await request(itemPath(resource, row)), trigger)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  async function save(payload) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const creating = editor.mode === 'create'
      const removing = editor.mode === 'remove'
      await request(creating ? resource.path : itemPath(resource, editor.record), {
        method: creating ? 'POST' : removing ? 'DELETE' : 'PUT',
        json: payload,
      })
      setEditor(null)
      setNotice(removing ? 'Registro eliminado.' : 'Cambios guardados.')
      reload()
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section aria-labelledby="admin-resource-title">
      <div className="admin-resource-header">
        <h2 id="admin-resource-title">{resource.title}</h2>
        {list?.rows && (
          <Button disabled={actionsDisabled} onClick={(event) => openEditor('create', null, event.currentTarget)}>
            Crear {resource.singular}
          </Button>
        )}
      </div>
      <Feedback kind="success">{notice}</Feedback>
      {!list && <Feedback kind="loading">Cargando {resource.title.toLowerCase()}…</Feedback>}
      {list?.error && (
        <div>
          <Feedback kind="error">{list.error}</Feedback>
          <Button variant="secondary" onClick={reload}>Reintentar</Button>
        </div>
      )}
      {!editor && <Feedback kind="error">{error}</Feedback>}
      {editor?.mode === 'remove' && (
        <Confirmation
          title={`Eliminar ${resource.singular}`}
          confirmLabel="Confirmar eliminación"
          busyLabel="Eliminando…"
          busy={busy}
          error={error}
          onConfirm={() => save()}
          onCancel={cancelEditor}
        >
          <p>
            {resource.columns.map((column) => displayValue(column, editor.record, list.lookups)).slice(0, 2).join(' · ')}
          </p>
          <p>{resource.removeText ?? 'Confirma que quieres eliminar este registro.'}</p>
        </Confirmation>
      )}
      {editor && editor.mode !== 'remove' && (
        <AdminForm
          fields={resource[editor.mode]}
          record={editor.record}
          lookups={list.lookups}
          title={`${editor.mode === 'create' ? 'Crear' : 'Editar'} ${resource.singular}`}
          saving={busy}
          error={error}
          onCancel={cancelEditor}
          onSave={save}
        />
      )}
      {list?.rows?.length === 0 && <Feedback>Aún no hay registros.</Feedback>}
      {list?.rows?.length > 0 && (
        <div className="admin-table-scroll">
          <table className="admin-table" aria-labelledby="admin-resource-title">
            <thead>
              <tr>
                {resource.columns.map((column) => <th scope="col" key={column.name}>{column.label}</th>)}
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {list.rows.map((row) => (
                <tr key={itemPath(resource, row)}>
                  {resource.columns.map((column) => (
                    <td key={column.name}>{displayValue(column, row, list.lookups)}</td>
                  ))}
                  <td>
                    <div className="form-actions">
                      <Button variant="secondary" disabled={actionsDisabled} onClick={(event) => edit(row, event.currentTarget)}>
                        Editar
                      </Button>
                      <Button
                        variant="danger"
                        disabled={actionsDisabled || row.accountStatus === 'DELETED'}
                        onClick={(event) => openEditor('remove', row, event.currentTarget)}
                      >
                        Eliminar
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
