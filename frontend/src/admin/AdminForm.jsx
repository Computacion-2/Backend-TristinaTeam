import { useEffect, useRef, useState } from 'react'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import FormField from '../components/FormField.jsx'
import { catalogs } from './resources.js'

function AdminField({ field, record, lookups }) {
  const { name, label, type, catalog, multiple, options, activeOnly, ...constraints } = field
  const value = record?.[name] ?? ''
  const choices = catalog
    ? lookups[catalog].filter((entry) => !activeOnly || entry.activeFlag === 'Y' || entry.id === value)
    : []

  if (multiple) {
    return (
      <fieldset className="choice-group">
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

  if (catalog || options) {
    return (
      <FormField as="select" label={label} name={name} defaultValue={value} required={constraints.required}>
        <option value="">Selecciona una opción</option>
        {catalog
          ? choices.map((entry) => <option key={entry.id} value={entry.id}>{entry[catalogs[catalog].label]}</option>)
          : Object.entries(options).map(([key, text]) => <option key={key} value={key}>{text}</option>)}
      </FormField>
    )
  }

  if (type === 'textarea') {
    return <FormField as="textarea" label={label} name={name} defaultValue={value} rows={3} {...constraints} />
  }

  return (
    <FormField
      label={label}
      name={name}
      type={type ?? 'text'}
      defaultValue={value}
      autoComplete={type === 'password' ? 'new-password' : undefined}
      {...constraints}
    />
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
    <form className="form-panel" onSubmit={submit}>
      <h3 ref={heading} tabIndex={-1}>{title}</h3>
      <fieldset className="form-fields" disabled={saving}>
        {fields.map((field) => <AdminField key={field.name} field={field} record={record} lookups={lookups} />)}
      </fieldset>
      <Feedback kind="error">{validation || error}</Feedback>
      <div className="form-actions">
        <Button type="submit" busy={saving} busyLabel="Guardando…">Guardar</Button>
        <Button variant="secondary" disabled={saving} onClick={onCancel}>Cancelar</Button>
      </div>
    </form>
  )
}
