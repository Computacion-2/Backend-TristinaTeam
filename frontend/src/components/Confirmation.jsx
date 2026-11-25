import { useEffect, useId, useRef } from 'react'
import Button from './Button.jsx'
import Feedback from './Feedback.jsx'

export default function Confirmation({
  title, children, confirmLabel, busyLabel, busy, error, onCancel, onConfirm,
}) {
  const id = useId()
  const heading = useRef(null)

  useEffect(() => heading.current?.focus(), [])

  return (
    <section className="form-panel confirmation" aria-labelledby={id}>
      <h3 id={id} ref={heading} tabIndex={-1}>{title}</h3>
      {children}
      <Feedback kind="error">{error}</Feedback>
      <div className="form-actions">
        <Button variant="danger" busy={busy} busyLabel={busyLabel} onClick={onConfirm}>
          {confirmLabel}
        </Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>Cancelar</Button>
      </div>
    </section>
  )
}
