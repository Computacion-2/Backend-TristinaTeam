import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <section className="panel">
      <h1>Página no encontrada</h1>
      <p>Revisa la dirección o vuelve al inicio de CuidaPet.</p>
      <Link className="action" to="/">Volver al inicio</Link>
    </section>
  )
}
