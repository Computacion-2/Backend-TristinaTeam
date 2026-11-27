export const administrationAccess = {
  roles: ['ADMIN'],
  permissions: ['MANAGE_USERS', 'MANAGE_ROLES'],
}

export const caregiverAccess = { roles: ['CAREGIVER'] }
export const ownerAccess = { roles: ['OWNER'] }
export const careParticipantAccess = { roles: ['OWNER', 'CAREGIVER'] }

export function hasAccess(user, { roles = [], permissions = [] } = {}) {
  const hasRole = roles.length === 0 || roles.some((role) => user.roles.includes(role))
  return hasRole && permissions.every((permission) => user.permissions.includes(permission))
}
