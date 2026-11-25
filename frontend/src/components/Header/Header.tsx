import type React from "react"
import "./Header.css"

const Header: React.FC = () => {
  return (
    <header className="header">
      <div className="header-container">
        <div className="header-content">
          <div className="header-brand">
            <div className="header-logo">
              <span className="logo-text">C</span>
            </div>
            <div className="header-info">
              <h1 className="header-title">Sistema de Gestão</h1>
              <p className="header-subtitle">Gestão Institucional</p>
            </div>
          </div>
          <div className="header-user">
            <p className="user-welcome">Bem-vindo ao sistema</p>
            <p className="user-version">Versão 1.0</p>
          </div>
        </div>
      </div>
    </header>
  )
}

export default Header
