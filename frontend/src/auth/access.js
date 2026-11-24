export const administrationAccess = {
  roles: ['ADMIN'],
  permissions: ['MANAGE_USERS', 'MANAGE_ROLES'],
}

export function hasAccess(user, { roles = [], permissions = [] } = {}) {
  const hasRole = roles.length === 0 || roles.some((role) => user.roles.includes(role))
  return hasRole && permissions.every((permission) => user.permissions.includes(permission))
}
