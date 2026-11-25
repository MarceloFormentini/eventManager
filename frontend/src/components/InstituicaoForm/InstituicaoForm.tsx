"use client"

import type React from "react"
import { useState } from "react"
import type { Instituicao, TipoInstituicao } from "../../App"
import "./InstituicaoForm.css"

interface InstituicaoFormProps {
  instituicoes: Instituicao[]
  setInstituicoes: (instituicoes: Instituicao[]) => void
  tiposInstituicao: TipoInstituicao[]
}

const InstituicaoForm: React.FC<InstituicaoFormProps> = ({ instituicoes, setInstituicoes, tiposInstituicao }) => {
  const [nome, setNome] = useState("")
  const [tipoInstituicaoId, setTipoInstituicaoId] = useState("")
  const [endereco, setEndereco] = useState("")
  const [telefone, setTelefone] = useState("")
  const [email, setEmail] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!nome.trim() || !tipoInstituicaoId) return

    if (editingId) {
      setInstituicoes(
        instituicoes.map((inst) =>
          inst.id === editingId ? { ...inst, nome, tipoInstituicaoId, endereco, telefone, email } : inst,
        ),
      )
      setEditingId(null)
    } else {
      const novaInstituicao: Instituicao = {
        id: Date.now().toString(),
        nome,
        tipoInstituicaoId,
        endereco,
        telefone,
        email,
        createdAt: new Date(),
      }
      setInstituicoes([...instituicoes, novaInstituicao])
    }

    resetForm()
  }

  const resetForm = () => {
    setNome("")
    setTipoInstituicaoId("")
    setEndereco("")
    setTelefone("")
    setEmail("")
  }

  const handleEdit = (instituicao: Instituicao) => {
    setNome(instituicao.nome)
    setTipoInstituicaoId(instituicao.tipoInstituicaoId)
    setEndereco(instituicao.endereco)
    setTelefone(instituicao.telefone)
    setEmail(instituicao.email)
    setEditingId(instituicao.id)
  }

  const handleDelete = (id: string) => {
    setInstituicoes(instituicoes.filter((inst) => inst.id !== id))
  }

  const cancelEdit = () => {
    setEditingId(null)
    resetForm()
  }

  const getTipoNome = (tipoId: string) => {
    return tiposInstituicao.find((tipo) => tipo.id === tipoId)?.nome || "Tipo não encontrado"
  }

  return (
    <div className="instituicao-form">
      <div className="form-card">
        <div className="form-header">
          <h3 className="form-title">{editingId ? "Editar Instituição" : "Nova Instituição"}</h3>
        </div>
        <div className="form-content">
          <form onSubmit={handleSubmit} className="form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="nome" className="form-label">
                  Nome da Instituição
                </label>
                <input
                  id="nome"
                  type="text"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Nome da instituição"
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="tipo" className="form-label">
                  Tipo de Instituição
                </label>
                <select
                  id="tipo"
                  value={tipoInstituicaoId}
                  onChange={(e) => setTipoInstituicaoId(e.target.value)}
                  className="form-select"
                  required
                >
                  <option value="">Selecione o tipo</option>
                  {tiposInstituicao.map((tipo) => (
                    <option key={tipo.id} value={tipo.id}>
                      {tipo.nome}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="endereco" className="form-label">
                Endereço
              </label>
              <input
                id="endereco"
                type="text"
                value={endereco}
                onChange={(e) => setEndereco(e.target.value)}
                placeholder="Endereço completo"
                className="form-input"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="telefone" className="form-label">
                  Telefone
                </label>
                <input
                  id="telefone"
                  type="tel"
                  value={telefone}
                  onChange={(e) => setTelefone(e.target.value)}
                  placeholder="(00) 00000-0000"
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label htmlFor="email" className="form-label">
                  E-mail
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email@exemplo.com"
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={tiposInstituicao.length === 0}>
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

          {tiposInstituicao.length === 0 && (
            <div className="warning-box">
              <p className="warning-text">
                ⚠️ Cadastre pelo menos um tipo de instituição antes de adicionar instituições.
              </p>
            </div>
          )}
        </div>
      </div>

      <div className="list-section">
        <h3 className="list-title">Instituições Cadastradas ({instituicoes.length})</h3>

        {instituicoes.length === 0 ? (
          <div className="empty-state">
            <p>Nenhuma instituição cadastrada ainda.</p>
          </div>
        ) : (
          <div className="items-grid">
            {instituicoes.map((instituicao) => (
              <div key={instituicao.id} className="item-card">
                <div className="item-content">
                  <div className="item-info">
                    <div className="item-header">
                      <span className="item-icon">🏢</span>
                      <h4 className="item-title">{instituicao.nome}</h4>
                    </div>

                    <div className="item-details">
                      <p className="item-detail">
                        <strong>Tipo:</strong> {getTipoNome(instituicao.tipoInstituicaoId)}
                      </p>
                      {instituicao.endereco && (
                        <p className="item-detail">
                          <span className="detail-icon">📍</span>
                          {instituicao.endereco}
                        </p>
                      )}
                      {instituicao.telefone && (
                        <p className="item-detail">
                          <span className="detail-icon">📞</span>
                          {instituicao.telefone}
                        </p>
                      )}
                      {instituicao.email && (
                        <p className="item-detail">
                          <span className="detail-icon">✉️</span>
                          {instituicao.email}
                        </p>
                      )}
                    </div>

                    <p className="item-date">Criado em: {instituicao.createdAt.toLocaleDateString("pt-BR")}</p>
                  </div>
                  <div className="item-actions">
                    <button onClick={() => handleEdit(instituicao)} className="btn btn-edit" title="Editar">
                      ✏️
                    </button>
                    <button onClick={() => handleDelete(instituicao.id)} className="btn btn-delete" title="Excluir">
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

export default InstituicaoForm
