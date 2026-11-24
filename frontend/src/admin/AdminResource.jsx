import { useEffect, useState } from 'react'
import { useAuth } from '../auth/authContext.js'
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

  function openEditor(mode, record) {
    setError('')
    setNotice('')
    setEditor({ mode, record })
  }

  async function edit(row) {
    if (busy) return
    setError('')
    setNotice('')
    setBusy(true)
    try {
      openEditor('edit', await request(itemPath(resource, row)))
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
        {list?.rows && !editor && (
          <button className="action" type="button" disabled={busy} onClick={() => openEditor('create')}>
            Crear {resource.singular}
          </button>
        )}
      </div>
      {notice && <p role="status">{notice}</p>}
      {!list && <p role="status">Cargando {resource.title.toLowerCase()}…</p>}
      {list?.error && (
        <div>
          <p className="form-error" role="alert">{list.error}</p>
          <button className="action secondary" type="button" onClick={reload}>Reintentar</button>
        </div>
      )}
      {!editor && error && <p className="form-error" role="alert">{error}</p>}
      {editor?.mode === 'remove' && (
        <section className="admin-editor" aria-label="Confirmar eliminación">
          <h3>Eliminar {resource.singular}</h3>
          <p>
            {resource.columns.map((column) => displayValue(column, editor.record, list.lookups)).slice(0, 2).join(' · ')}
          </p>
          <p>{resource.removeText ?? 'Confirma que quieres eliminar este registro.'}</p>
          {error && <p className="form-error" role="alert">{error}</p>}
          <div className="admin-actions">
            <button className="action danger" type="button" disabled={busy} onClick={() => save()}>
              {busy ? 'Eliminando…' : 'Confirmar eliminación'}
            </button>
            <button className="action secondary" type="button" disabled={busy} onClick={() => setEditor(null)}>Cancelar</button>
          </div>
        </section>
      )}
      {editor && editor.mode !== 'remove' && (
        <AdminForm
          fields={resource[editor.mode]}
          record={editor.record}
          lookups={list.lookups}
          title={`${editor.mode === 'create' ? 'Crear' : 'Editar'} ${resource.singular}`}
          saving={busy}
          error={error}
          onCancel={() => { setEditor(null); setError('') }}
          onSave={save}
        />
      )}
      {list?.rows?.length === 0 && <p>Aún no hay registros.</p>}
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
                    <div className="admin-actions">
                      <button className="action secondary" type="button" disabled={busy || Boolean(editor)} onClick={() => edit(row)}>
                        Editar
                      </button>
                      <button className="action danger" type="button" disabled={busy || Boolean(editor) || row.accountStatus === 'DELETED'} onClick={() => openEditor('remove', row)}>
                        Eliminar
                      </button>
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
