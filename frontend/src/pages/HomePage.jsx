import { Link } from 'react-router'

export default function HomePage() {
  return (
    <section className="panel">
      <h1>Cuidado para tu mascota</h1>
      <p>CuidaPet conecta a propietarios de mascotas con cuidadores y reúne las solicitudes, reservas y novedades del cuidado en un solo lugar.</p>
      <Link className="action" to="/como-funciona">Cómo funciona</Link>
    </section>
  )
}
