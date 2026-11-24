const userStatuses = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  SUSPENDED: 'Suspendido',
  DELETED: 'Eliminado',
}
const roleStatuses = { Y: 'Activo', N: 'Inactivo' }

export const catalogs = {
  users: { path: '/users', label: 'email' },
  roles: { path: '/roles', label: 'roleName' },
  permissions: { path: '/permissions', label: 'permissionCode' },
}

function field(name, label, options = {}) {
  return { name, label, ...options }
}

const email = field('email', 'Correo electrónico', { type: 'email', required: true, maxLength: 160 })
const password = field('password', 'Contraseña', { type: 'password', minLength: 8, maxLength: 72 })
const roleName = field('roleName', 'Nombre del rol', { required: true, maxLength: 60 })
const description = field('description', 'Descripción', { type: 'textarea', maxLength: 300 })
const permissionName = field('permissionName', 'Nombre del permiso', { required: true, maxLength: 120 })

const userRoleFields = [
  field('userId', 'Usuario', { catalog: 'users', required: true }),
  field('roleId', 'Rol', { catalog: 'roles', required: true, activeOnly: true }),
]
const rolePermissionFields = [
  field('roleId', 'Rol', { catalog: 'roles', required: true }),
  field('permissionId', 'Permiso', { catalog: 'permissions', required: true }),
]

export const adminResources = {
  usuarios: {
    title: 'Usuarios',
    singular: 'usuario',
    path: '/users',
    catalogs: ['roles'],
    columns: [email, field('accountStatus', 'Estado', { options: userStatuses })],
    create: [email, { ...password, required: true }, field('roleIds', 'Roles iniciales', {
      catalog: 'roles', multiple: true, activeOnly: true,
    })],
    edit: [email, { ...password, label: 'Nueva contraseña (opcional)' }, field('accountStatus', 'Estado', {
      options: userStatuses, required: true,
    })],
    removeText: 'La cuenta quedará en estado Eliminado y perderá el acceso.',
  },
  roles: {
    title: 'Roles',
    singular: 'rol',
    path: '/roles',
    catalogs: ['permissions'],
    columns: [roleName, field('activeFlag', 'Estado', { options: roleStatuses }), description],
    create: [roleName, description, field('permissionIds', 'Permisos iniciales', {
      catalog: 'permissions', multiple: true,
    })],
    edit: [roleName, description, field('activeFlag', 'Estado', { options: roleStatuses, required: true })],
  },
  permisos: {
    title: 'Permisos',
    singular: 'permiso',
    path: '/permissions',
    catalogs: [],
    columns: [field('permissionCode', 'Código'), permissionName, description],
    create: [field('permissionCode', 'Código', { required: true, maxLength: 100 }), permissionName, description],
    edit: [permissionName, description],
  },
  'roles-de-usuarios': {
    title: 'Roles de usuarios',
    singular: 'rol de usuario',
    path: '/user-roles',
    ids: ['userId', 'roleId'],
    catalogs: ['users', 'roles'],
    columns: userRoleFields,
    create: userRoleFields,
    edit: userRoleFields,
  },
  'permisos-de-roles': {
    title: 'Permisos de roles',
    singular: 'permiso de rol',
    path: '/role-permissions',
    ids: ['roleId', 'permissionId'],
    catalogs: ['roles', 'permissions'],
    columns: rolePermissionFields,
    create: rolePermissionFields,
    edit: rolePermissionFields,
  },
}

export function itemPath(resource, item) {
  const ids = resource.ids ?? ['id']
  return [resource.path, ...ids.map((key) => item[key])].join('/')
}

export function displayValue(column, item, lookups) {
  const value = item[column.name]
  if (column.catalog) {
    const label = catalogs[column.catalog].label
    return lookups[column.catalog].find((option) => option.id === value)?.[label] ?? String(value)
  }
  return column.options?.[value] ?? (value || '—')
}
