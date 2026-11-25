"use client"

import type React from "react"
import { useState } from "react"
import type { Evento, Local, Instituicao } from "../../App"
import "./EventoForm.css"

interface EventoFormProps {
  eventos: Evento[]
  setEventos: (eventos: Evento[]) => void
  locais: Local[]
  instituicoes: Instituicao[]
}

const EventoForm: React.FC<EventoFormProps> = ({ eventos, setEventos, locais, instituicoes }) => {
  const [titulo, setTitulo] = useState("")
  const [descricao, setDescricao] = useState("")
  const [localId, setLocalId] = useState("")
  const [dataInicio, setDataInicio] = useState("")
  const [dataFim, setDataFim] = useState("")
  const [status, setStatus] = useState<"pendente" | "em-andamento" | "concluido">("pendente")
  const [editingId, setEditingId] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!titulo.trim() || !localId || !dataInicio) return

    if (editingId) {
      setEventos(
        eventos.map((evento) =>
          evento.id === editingId
            ? {
                ...evento,
                titulo,
                descricao,
                localId,
                dataInicio: new Date(dataInicio),
                dataFim: dataFim ? new Date(dataFim) : new Date(dataInicio),
                status,
              }
            : evento,
        ),
      )
      setEditingId(null)
    } else {
      const novoEvento: Evento = {
        id: Date.now().toString(),
        titulo,
        descricao,
        localId,
        dataInicio: new Date(dataInicio),
        dataFim: dataFim ? new Date(dataFim) : new Date(dataInicio),
        status,
        createdAt: new Date(),
      }
      setEventos([...eventos, novoEvento])
    }

    resetForm()
  }

  const resetForm = () => {
    setTitulo("")
    setDescricao("")
    setLocalId("")
    setDataInicio("")
    setDataFim("")
    setStatus("pendente")
  }

  const handleEdit = (evento: Evento) => {
    setTitulo(evento.titulo)
    setDescricao(evento.descricao)
    setLocalId(evento.localId)
    setDataInicio(evento.dataInicio.toISOString().slice(0, 16))
    setDataFim(evento.dataFim.toISOString().slice(0, 16))
    setStatus(evento.status)
    setEditingId(evento.id)
  }

  const handleDelete = (id: string) => {
    setEventos(eventos.filter((evento) => evento.id !== id))
  }

  const cancelEdit = () => {
    setEditingId(null)
    resetForm()
  }

  const getLocalNome = (localId: string) => {
    return locais.find((local) => local.id === localId)?.nome || "Local não encontrado"
  }

  const getInstituicaoNome = (localId: string) => {
    const local = locais.find((local) => local.id === localId)
    if (!local) return "Instituição não encontrada"
    return instituicoes.find((inst) => inst.id === local.instituicaoId)?.nome || "Instituição não encontrada"
  }

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      pendente: { className: "status-badge status-pendente", label: "Pendente" },
      "em-andamento": { className: "status-badge status-andamento", label: "Em Andamento" },
      concluido: { className: "status-badge status-concluido", label: "Concluído" },
    }

    const config = statusConfig[status as keyof typeof statusConfig]
    return <span className={config.className}>{config.label}</span>
  }

  return (
    <div className="evento-form">
      <div className="form-card">
        <div className="form-header">
          <h3 className="form-title">{editingId ? "Editar Evento" : "Novo Evento"}</h3>
        </div>
        <div className="form-content">
          <form onSubmit={handleSubmit} className="form">
            <div className="form-group">
              <label htmlFor="titulo" className="form-label">
                Título do Evento
              </label>
              <input
                id="titulo"
                type="text"
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
                placeholder="Ex: Reunião Mensal, Treinamento, Assembleia"
                className="form-input"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="local" className="form-label">
                Local
              </label>
              <select
                id="local"
                value={localId}
                onChange={(e) => setLocalId(e.target.value)}
                className="form-select"
                required
              >
                <option value="">Selecione o local</option>
                {locais.map((local) => (
                  <option key={local.id} value={local.id}>
                    {local.nome} - {getInstituicaoNome(local.id)}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="dataInicio" className="form-label">
                  Data e Hora de Início
                </label>
                <input
                  id="dataInicio"
                  type="datetime-local"
                  value={dataInicio}
                  onChange={(e) => setDataInicio(e.target.value)}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="dataFim" className="form-label">
                  Data e Hora de Fim
                </label>
                <input
                  id="dataFim"
                  type="datetime-local"
                  value={dataFim}
                  onChange={(e) => setDataFim(e.target.value)}
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="status" className="form-label">
                Status
              </label>
              <select
                id="status"
                value={status}
                onChange={(e) => setStatus(e.target.value as "pendente" | "em-andamento" | "concluido")}
                className="form-select"
              >
                <option value="pendente">Pendente</option>
                <option value="em-andamento">Em Andamento</option>
                <option value="concluido">Concluído</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="descricao" className="form-label">
                Descrição
              </label>
              <textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                placeholder="Descrição do evento, agenda, participantes esperados, etc."
                className="form-textarea"
                rows={3}
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={locais.length === 0}>
                <span className="btn-icon">➕</span>
                {editingId ? "Atualizar" : "Adicionar"}
              </button>
              {editingId && (
                <button type="button" onClick={cancelEdit} className="btn btn-secondary">
                  Cancelar
                </button>
              )}
            </div>
          </form>

          {locais.length === 0 && (
            <div className="warning-box">
              <p className="warning-text">⚠️ Cadastre pelo menos um local antes de adicionar eventos.</p>
            </div>
          )}
        </div>
      </div>

      <div className="list-section">
        <h3 className="list-title">Eventos Cadastrados ({eventos.length})</h3>

        {eventos.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum evento cadastrado ainda.</p>
          </div>
        ) : (
          <div className="items-grid">
            {eventos.map((evento) => (
              <div key={evento.id} className="item-card">
                <div className="item-content">
                  <div className="item-info">
                    <div className="item-header">
                      <span className="item-icon">📅</span>
                      <h4 className="item-title">{evento.titulo}</h4>
                      {getStatusBadge(evento.status)}
                    </div>

                    <div className="item-details">
                      <p className="item-detail">
                        <span className="detail-icon">📍</span>
                        <strong>Local:</strong> {getLocalNome(evento.localId)}
                      </p>
                      <p className="item-detail">
                        <span className="detail-icon">🏢</span>
                        <strong>Instituição:</strong> {getInstituicaoNome(evento.localId)}
                      </p>
                      <p className="item-detail">
                        <span className="detail-icon">🕐</span>
                        <strong>Início:</strong> {evento.dataInicio.toLocaleString("pt-BR")}
                      </p>
                      <p className="item-detail">
                        <span className="detail-icon">🕐</span>
                        <strong>Fim:</strong> {evento.dataFim.toLocaleString("pt-BR")}
                      </p>
                      {evento.descricao && <p className="item-description">{evento.descricao}</p>}
                    </div>

                    <p className="item-date">Criado em: {evento.createdAt.toLocaleDateString("pt-BR")}</p>
                  </div>
                  <div className="item-actions">
                    <button onClick={() => handleEdit(evento)} className="btn btn-edit" title="Editar">
                      ✏️
                    </button>
                    <button onClick={() => handleDelete(evento.id)} className="btn btn-delete" title="Excluir">
                      🗑️
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default EventoForm
