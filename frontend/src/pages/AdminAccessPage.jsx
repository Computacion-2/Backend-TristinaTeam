import { useAuth } from '../auth/authContext.js'

const permissionNames = { MANAGE_USERS: 'Gestionar usuarios', MANAGE_ROLES: 'Gestionar roles y permisos' }

export default function AdminAccessPage() {
  const { user } = useAuth()

  return (
    <section className="panel">
      <h1>Administración</h1>
      <h2>Permisos de tu cuenta</h2>
      <ul>
        {user.permissions.map((permission) => (
          <li key={permission}>{permissionNames[permission] || permission}</li>
        ))}
      </ul>
    </section>
  )
}
