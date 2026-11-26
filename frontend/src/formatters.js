const copFormatter = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' })

export function formatCop(amount) {
  return copFormatter.format(amount)
}
