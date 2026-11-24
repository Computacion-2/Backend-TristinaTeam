import { NavLink, Outlet } from 'react-router'
import { adminResources } from '../admin/resources.js'

export default function AdminAccessPage() {
  return (
    <section className="panel admin-panel">
      <h1>Administración</h1>
      <nav className="admin-nav" aria-label="Secciones de administración">
        {Object.entries(adminResources).map(([segment, resource]) => (
          <NavLink key={segment} to={segment} end>{resource.title}</NavLink>
        ))}
      </nav>
      <Outlet />
    </section>
  )
}
