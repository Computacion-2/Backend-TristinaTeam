import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router'
import AdminResource from './admin/AdminResource.jsx'
import { adminResources } from './admin/resources.js'
import { administrationAccess } from './auth/access.js'
import { useAuth } from './auth/authContext.js'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import HomePage from './pages/HomePage.jsx'
import AboutPage from './pages/AboutPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import AccountPage from './pages/AccountPage.jsx'
import AdminAccessPage from './pages/AdminAccessPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import PetPage from './pets/PetPage.jsx'
import PetsPage from './pets/PetsPage.jsx'
import RequestPage from './care/RequestPage.jsx'
import RequestsPage from './care/RequestsPage.jsx'
import CaregiversPage from './caregivers/CaregiversPage.jsx'

export default function App() {
  const { user, checkingSession, signOut } = useAuth()
  const navigate = useNavigate()

  function closeSession() {
    signOut()
    navigate('/login', { replace: true })
  }

  return (
    <>
      <header className="site-header">
        <div className="container header-content">
          <NavLink className="brand" to="/" end>CuidaPet Icesi</NavLink>
          <nav aria-label="Navegación principal">
            <NavLink to="/" end>Inicio</NavLink>
            <NavLink to="/como-funciona">Cómo funciona</NavLink>
            {user && (
              <>
                <NavLink to="/cuenta">Mi cuenta</NavLink>
                <NavLink to="/mascotas">Mis mascotas</NavLink>
                <NavLink to="/solicitudes">Mis solicitudes</NavLink>
                <NavLink to="/cuidadores">Cuidadores</NavLink>
                <button className="nav-button" type="button" onClick={closeSession}>Cerrar sesión</button>
              </>
            )}
            {!user && !checkingSession && <NavLink to="/login">Iniciar sesión</NavLink>}
          </nav>
        </div>
      </header>
      <main className="container main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/como-funciona" element={<AboutPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/cuenta" element={<AccountPage />} />
            <Route path="/mascotas" element={<PetsPage />} />
            <Route path="/mascotas/:petId" element={<PetPage />} />
            <Route path="/solicitudes" element={<RequestsPage />} />
            <Route path="/solicitudes/:requestId" element={<RequestPage />} />
            <Route path="/cuidadores" element={<CaregiversPage />} />
          </Route>
          <Route element={<ProtectedRoute access={administrationAccess} />}>
            <Route path="/administracion" element={<AdminAccessPage />}>
              <Route index element={<Navigate to="usuarios" replace />} />
              {Object.entries(adminResources).map(([segment, resource]) => (
                <Route key={segment} path={segment} element={<AdminResource key={segment} resource={resource} />} />
              ))}
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  )
}
