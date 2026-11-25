import { useEffect, useRef } from 'react'
import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'
import FormField from '../components/FormField.jsx'

export default function RecordForm({ title, fields, record = {}, busy, error, onCancel, onSave }) {
  const heading = useRef(null)
  useEffect(() => heading.current?.focus(), [])

  function handleSubmit(event) {
    event.preventDefault()
    const values = new FormData(event.currentTarget)
    const payload = Object.fromEntries(fields.map(({ name, numeric }) => {
      const value = values.get(name).trim()
      return [name, value === '' ? null : numeric ? Number(value) : value]
    }))
    onSave(payload)
  }

  return (
    <form className="form-panel" onSubmit={handleSubmit}>
      <h3 ref={heading} tabIndex={-1}>{title}</h3>
      <fieldset className="form-fields" disabled={busy}>
        {fields.map(({ name, options, numeric, ...field }) => {
          const value = record[name] ?? ''
          return (
            <FormField key={name} name={name} defaultValue={numeric ? String(value) : value} {...field}>
              {options?.map(([key, label]) => <option key={key} value={key}>{label}</option>)}
            </FormField>
          )
        })}
      </fieldset>
      <Feedback kind="error">{error}</Feedback>
      <div className="form-actions">
        <Button type="submit" busy={busy} busyLabel="Guardando…">Guardar</Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>Cancelar</Button>
      </div>
    </form>
  )
}
