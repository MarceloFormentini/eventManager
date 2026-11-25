"use client"

import { useState } from "react"
import Header from "./components/Header/Header"
import TipoInstituicaoForm from "./components/TipoInstituicaoForm/TipoInstituicaoForm"
import InstituicaoForm from "./components/InstituicaoForm/InstituicaoForm"
import LocalForm from "./components/LocalForm/LocalForm"
import EventoForm from "./components/EventoForm/EventoForm"
import "./App.css"

export interface TipoInstituicao {
  id: string
  nome: string
  descricao: string
  createdAt: Date
}

export interface Instituicao {
  id: string
  nome: string
  tipoInstituicaoId: string
  endereco: string
  telefone: string
  email: string
  createdAt: Date
}

export interface Local {
  id: string
  nome: string
  instituicaoId: string
  capacidade: number
  descricao: string
  createdAt: Date
}

export interface Evento {
  id: string
  titulo: string
  descricao: string
  localId: string
  dataInicio: Date
  dataFim: Date
  status: "pendente" | "em-andamento" | "concluido"
  createdAt: Date
}

function App() {
  const [activeTab, setActiveTab] = useState("tipos")
  const [tiposInstituicao, setTiposInstituicao] = useState<TipoInstituicao[]>([])
  const [instituicoes, setInstituicoes] = useState<Instituicao[]>([])
  const [locais, setLocais] = useState<Local[]>([])
  const [eventos, setEventos] = useState<Evento[]>([])

  return (
    <div className="app">
      <Header />

      <main className="main-container">
        <div className="page-header">
          <h1 className="page-title">Sistema de Gestão</h1>
          <p className="page-subtitle">Gerencie tipos de instituição, instituições, locais e eventos</p>
        </div>

        <div className="tabs-container">
          <div className="tabs-list">
            <button
              className={`tab-trigger ${activeTab === "tipos" ? "active" : ""}`}
              onClick={() => setActiveTab("tipos")}
            >
              <span className="tab-icon">⚙️</span>
              Tipos
            </button>
            <button
              className={`tab-trigger ${activeTab === "instituicoes" ? "active" : ""}`}
              onClick={() => setActiveTab("instituicoes")}
            >
              <span className="tab-icon">🏢</span>
              Instituições
            </button>
            <button
              className={`tab-trigger ${activeTab === "locais" ? "active" : ""}`}
              onClick={() => setActiveTab("locais")}
            >
              <span className="tab-icon">📍</span>
              Locais
            </button>
            <button
              className={`tab-trigger ${activeTab === "eventos" ? "active" : ""}`}
              onClick={() => setActiveTab("eventos")}
            >
              <span className="tab-icon">📅</span>
              Eventos
            </button>
          </div>

          <div className="tab-content">
            {activeTab === "tipos" && (
              <div className="card">
                <div className="card-header">
                  <h2 className="card-title">
                    <span className="card-icon">⚙️</span>
                    Tipos de Instituição
                  </h2>
                </div>
                <div className="card-content">
                  <TipoInstituicaoForm tiposInstituicao={tiposInstituicao} setTiposInstituicao={setTiposInstituicao} />
                </div>
              </div>
            )}

            {activeTab === "instituicoes" && (
              <div className="card">
                <div className="card-header">
                  <h2 className="card-title">
                    <span className="card-icon">🏢</span>
                    Instituições
                  </h2>
                </div>
                <div className="card-content">
                  <InstituicaoForm
                    instituicoes={instituicoes}
                    setInstituicoes={setInstituicoes}
                    tiposInstituicao={tiposInstituicao}
                  />
                </div>
              </div>
            )}

            {activeTab === "locais" && (
              <div className="card">
                <div className="card-header">
                  <h2 className="card-title">
                    <span className="card-icon">📍</span>
                    Locais
                  </h2>
                </div>
                <div className="card-content">
                  <LocalForm locais={locais} setLocais={setLocais} instituicoes={instituicoes} />
                </div>
              </div>
            )}

            {activeTab === "eventos" && (
              <div className="card">
                <div className="card-header">
                  <h2 className="card-title">
                    <span className="card-icon">📅</span>
                    Eventos
                  </h2>
                </div>
                <div className="card-content">
                  <EventoForm eventos={eventos} setEventos={setEventos} locais={locais} instituicoes={instituicoes} />
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}

export default App
