import { useId } from 'react'

export default function FormField({ as: Control = 'input', label, hint, children, ...props }) {
  const id = useId()

  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <Control {...props} id={id} className="form-control" aria-describedby={hint ? `${id}-hint` : undefined}>
        {children}
      </Control>
      {hint && <small id={`${id}-hint`}>{hint}</small>}
    </div>
  )
}
