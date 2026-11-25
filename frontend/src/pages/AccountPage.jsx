import { Link } from 'react-router'
import { administrationAccess, hasAccess } from '../auth/access.js'
import { useAuth } from '../auth/authContext.js'
import ProfilePanel from '../pets/ProfilePanel.jsx'

const roleNames = { ADMIN: 'Administrador', OWNER: 'Propietario', CAREGIVER: 'Cuidador' }

export default function AccountPage() {
  const { user } = useAuth()
  const canAdminister = hasAccess(user, administrationAccess)

  return (
    <section className="panel">
      <h1>Mi cuenta</h1>
      <dl className="account-info">
        <dt>Correo electrónico</dt>
        <dd>{user.email}</dd>
        <dt>Roles</dt>
        <dd>{user.roles.map((role) => roleNames[role] || role).join(', ') || 'Sin roles asignados'}</dd>
      </dl>
      {canAdminister && <Link className="action" to="/administracion">Administración</Link>}
      <ProfilePanel />
    </section>
  )
}
