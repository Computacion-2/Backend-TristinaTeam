export default function Feedback({ kind = 'info', children }) {
  if (!children) return null

  return (
    <p className={`feedback feedback--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  )
}
