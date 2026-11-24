import { NavLink, Route, Routes } from 'react-router'
import HomePage from './pages/HomePage.jsx'
import AboutPage from './pages/AboutPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
    <>
      <header className="site-header">
        <div className="container header-content">
          <NavLink className="brand" to="/" end>CuidaPet Icesi</NavLink>
          <nav aria-label="Navegación principal">
            <NavLink to="/" end>Inicio</NavLink>
            <NavLink to="/como-funciona">Cómo funciona</NavLink>
          </nav>
        </div>
      </header>
      <main className="container main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/como-funciona" element={<AboutPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  )
}
