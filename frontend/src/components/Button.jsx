export default function Button({
  children,
  variant = 'primary',
  type = 'button',
  disabled = false,
  busy = false,
  busyLabel,
  ...props
}) {
  return (
    <button
      {...props}
      type={type}
      className={variant === 'primary' ? 'action' : `action ${variant}`}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
    >
      {busy && busyLabel ? busyLabel : children}
    </button>
  )
}
