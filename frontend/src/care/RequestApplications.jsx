import Button from '../components/Button.jsx'
import { applicationStatuses, bookingStatuses, formatPrice } from './requestDetails.js'

export default function RequestApplications({ applications, booking, open, canAccept, editing, onDecision }) {
  return (
    <>
      <section className="personal-section" aria-labelledby="care-applications-title">
        <h2 id="care-applications-title">Postulaciones</h2>
        {applications.length === 0 ? <p>Aún no hay postulaciones para esta solicitud.</p> : (
          <ul className="record-grid">
            {applications.map((application) => (
              <li className="record-card application-card" key={application.id}>
                <h3>Cuidador #{application.caregiverId}</h3>
                <p>{applicationStatuses[application.applicationStatus] ?? application.applicationStatus} · {formatPrice(application.proposedRate)}</p>
                {application.applicationMessage && <p>{application.applicationMessage}</p>}
                {application.responseReason && <p>Motivo: {application.responseReason}</p>}
                {open && application.applicationStatus === 'PENDING' && (
                  <div className="form-actions">
                    {canAccept && <Button disabled={editing} onClick={() => onDecision('accept', application)}>Aceptar</Button>}
                    <Button variant="secondary" disabled={editing} onClick={() => onDecision('reject', application)}>Rechazar</Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
      {booking && (
        <section className="personal-section" aria-labelledby="care-booking-title">
          <h2 id="care-booking-title">Reserva #{booking.id}</h2>
          <dl className="account-info personal-details">
            <div><dt>Estado</dt><dd>{bookingStatuses[booking.bookingStatusCode] ?? booking.bookingStatusCode}</dd></div>
            <div><dt>Cuidador</dt><dd>#{booking.caregiverId}</dd></div>
            <div><dt>Tarifa acordada</dt><dd>{formatPrice(booking.agreedRate)}</dd></div>
            <div><dt>Dirección exacta</dt><dd>{booking.exactAddress}</dd></div>
          </dl>
        </section>
      )}
    </>
  )
}
