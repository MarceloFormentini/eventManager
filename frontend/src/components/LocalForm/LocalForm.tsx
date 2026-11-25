"use client"

import type React from "react"
import { useState } from "react"
import type { Local, Instituicao } from "../../App"
import "./LocalForm.css"

interface LocalFormProps {
  locais: Local[]
  setLocais: (locais: Local[]) => void
  instituicoes: Instituicao[]
}

const LocalForm: React.FC<LocalFormProps> = ({ locais, setLocais, instituicoes }) => {
  const [nome, setNome] = useState("")
  const [instituicaoId, setInstituicaoId] = useState("")
  const [capacidade, setCapacidade] = useState("")
  const [descricao, setDescricao] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!nome.trim() || !instituicaoId) return

    if (editingId) {
      setLocais(
        locais.map((local) =>
          local.id === editingId
            ? {
                ...local,
                nome,
                instituicaoId,
                capacidade: Number.parseInt(capacidade) || 0,
                descricao,
              }
            : local,
        ),
      )
      setEditingId(null)
    } else {
      const novoLocal: Local = {
        id: Date.now().toString(),
        nome,
        instituicaoId,
        capacidade: Number.parseInt(capacidade) || 0,
        descricao,
        createdAt: new Date(),
      }
      setLocais([...locais, novoLocal])
    }

    resetForm()
  }

  const resetForm = () => {
    setNome("")
    setInstituicaoId("")
    setCapacidade("")
    setDescricao("")
  }

  const handleEdit = (local: Local) => {
    setNome(local.nome)
    setInstituicaoId(local.instituicaoId)
    setCapacidade(local.capacidade.toString())
    setDescricao(local.descricao)
    setEditingId(local.id)
  }

  const handleDelete = (id: string) => {
    setLocais(locais.filter((local) => local.id !== id))
  }

  const cancelEdit = () => {
    setEditingId(null)
    resetForm()
  }

  const getInstituicaoNome = (instituicaoId: string) => {
    return instituicoes.find((inst) => inst.id === instituicaoId)?.nome || "Instituição não encontrada"
  }

  return (
    <div className="local-form">
      <div className="form-card">
        <div className="form-header">
          <h3 className="form-title">{editingId ? "Editar Local" : "Novo Local"}</h3>
        </div>
        <div className="form-content">
          <form onSubmit={handleSubmit} className="form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="nome" className="form-label">
                  Nome do Local
                </label>
                <input
                  id="nome"
                  type="text"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Ex: Auditório Principal, Sala de Reuniões"
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="instituicao" className="form-label">
                  Instituição
                </label>
                <select
                  id="instituicao"
                  value={instituicaoId}
                  onChange={(e) => setInstituicaoId(e.target.value)}
                  className="form-select"
                  required
                >
                  <option value="">Selecione a instituição</option>
                  {instituicoes.map((instituicao) => (
                    <option key={instituicao.id} value={instituicao.id}>
                      {instituicao.nome}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="capacidade" className="form-label">
                Capacidade (pessoas)
              </label>
              <input
                id="capacidade"
                type="number"
                value={capacidade}
                onChange={(e) => setCapacidade(e.target.value)}
                placeholder="Ex: 50"
                className="form-input"
                min="0"
              />
            </div>

            <div className="form-group">
              <label htmlFor="descricao" className="form-label">
                Descrição
              </label>
              <textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                placeholder="Descrição do local, equipamentos disponíveis, etc."
                className="form-textarea"
                rows={3}
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={instituicoes.length === 0}>
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

          {instituicoes.length === 0 && (
            <div className="warning-box">
              <p className="warning-text">⚠️ Cadastre pelo menos uma instituição antes de adicionar locais.</p>
            </div>
          )}
        </div>
      </div>

      <div className="list-section">
        <h3 className="list-title">Locais Cadastrados ({locais.length})</h3>

        {locais.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum local cadastrado ainda.</p>
          </div>
        ) : (
          <div className="items-grid">
            {locais.map((local) => (
              <div key={local.id} className="item-card">
                <div className="item-content">
                  <div className="item-info">
                    <div className="item-header">
                      <span className="item-icon">📍</span>
                      <h4 className="item-title">{local.nome}</h4>
                    </div>

                    <div className="item-details">
                      <p className="item-detail">
                        <strong>Instituição:</strong> {getInstituicaoNome(local.instituicaoId)}
                      </p>
                      {local.capacidade > 0 && (
                        <p className="item-detail">
                          <span className="detail-icon">👥</span>
                          Capacidade: {local.capacidade} pessoas
                        </p>
                      )}
                      {local.descricao && <p className="item-description">{local.descricao}</p>}
                    </div>

                    <p className="item-date">Criado em: {local.createdAt.toLocaleDateString("pt-BR")}</p>
                  </div>
                  <div className="item-actions">
                    <button onClick={() => handleEdit(local)} className="btn btn-edit" title="Editar">
                      ✏️
                    </button>
                    <button onClick={() => handleDelete(local.id)} className="btn btn-delete" title="Excluir">
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

export default LocalForm
