import { useEffect, useRef, useState } from 'react'
import { catalogs } from './resources.js'

function FormField({ field, record, lookups }) {
  const { name, label, type, catalog, multiple, options, activeOnly, ...constraints } = field
  const value = record?.[name] ?? ''
  const id = `admin-${name}`
  const choices = catalog
    ? lookups[catalog].filter((entry) => !activeOnly || entry.activeFlag === 'Y' || entry.id === value)
    : []

  if (multiple) {
    return (
      <fieldset className="admin-choices">
        <legend>{label}</legend>
        {choices.length === 0 && <p>No hay opciones disponibles.</p>}
        {choices.map((entry) => (
          <label key={entry.id}>
            <input type="checkbox" name={name} value={entry.id} />
            {entry[catalogs[catalog].label]}
          </label>
        ))}
      </fieldset>
    )
  }

  return (
    <div className="admin-field">
      <label htmlFor={id}>{label}</label>
      {(catalog || options) ? (
        <select id={id} name={name} defaultValue={value} required={constraints.required}>
          <option value="">Selecciona una opción</option>
          {catalog
            ? choices.map((entry) => <option key={entry.id} value={entry.id}>{entry[catalogs[catalog].label]}</option>)
            : Object.entries(options).map(([key, text]) => <option key={key} value={key}>{text}</option>)}
        </select>
      ) : type === 'textarea' ? (
        <textarea id={id} name={name} defaultValue={value} rows={3} {...constraints} />
      ) : (
        <input
          id={id}
          name={name}
          type={type ?? 'text'}
          defaultValue={value}
          autoComplete={type === 'password' ? 'new-password' : undefined}
          {...constraints}
        />
      )}
    </div>
  )
}

export default function AdminForm({ fields, record, lookups, title, saving, error, onCancel, onSave }) {
  const heading = useRef(null)
  const [validation, setValidation] = useState('')

  useEffect(() => heading.current.focus(), [])

  function submit(event) {
    event.preventDefault()
    const values = new FormData(event.currentTarget)
    const unselected = fields.find((field) => field.multiple && !values.has(field.name))
    if (unselected) {
      setValidation(`Selecciona al menos una opción en ${unselected.label.toLowerCase()}.`)
      return
    }
    setValidation('')
    const payload = Object.fromEntries(fields.map(({ name, type, catalog, multiple }) => {
      if (multiple) return [name, values.getAll(name).map(Number)]
      const value = values.get(name)
      if (catalog) return [name, Number(value)]
      if (type === 'password') return [name, value || null]
      return [name, value.trim()]
    }))
    onSave(payload)
  }

  return (
    <form className="admin-editor" onSubmit={submit}>
      <h3 ref={heading} tabIndex={-1}>{title}</h3>
      <fieldset className="admin-fields" disabled={saving}>
        {fields.map((field) => <FormField key={field.name} field={field} record={record} lookups={lookups} />)}
      </fieldset>
      {(validation || error) && <p className="form-error" role="alert">{validation || error}</p>}
      <div className="admin-actions">
        <button className="action" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</button>
        <button className="action secondary" type="button" disabled={saving} onClick={onCancel}>Cancelar</button>
      </div>
    </form>
  )
}
