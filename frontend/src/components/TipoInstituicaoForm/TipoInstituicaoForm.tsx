"use client"

import type React from "react"
import { useState } from "react"
import type { TipoInstituicao } from "../../App"
import "./TipoInstituicaoForm.css"

interface TipoInstituicaoFormProps {
  tiposInstituicao: TipoInstituicao[]
  setTiposInstituicao: (tipos: TipoInstituicao[]) => void
}

const TipoInstituicaoForm: React.FC<TipoInstituicaoFormProps> = ({ tiposInstituicao, setTiposInstituicao }) => {
  const [nome, setNome] = useState("")
  const [descricao, setDescricao] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!nome.trim()) return

    if (editingId) {
      setTiposInstituicao(tiposInstituicao.map((tipo) => (tipo.id === editingId ? { ...tipo, nome, descricao } : tipo)))
      setEditingId(null)
    } else {
      const novoTipo: TipoInstituicao = {
        id: Date.now().toString(),
        nome,
        descricao,
        createdAt: new Date(),
      }
      setTiposInstituicao([...tiposInstituicao, novoTipo])
    }

    setNome("")
    setDescricao("")
  }

  const handleEdit = (tipo: TipoInstituicao) => {
    setNome(tipo.nome)
    setDescricao(tipo.descricao)
    setEditingId(tipo.id)
  }

  const handleDelete = (id: string) => {
    setTiposInstituicao(tiposInstituicao.filter((tipo) => tipo.id !== id))
  }

  const cancelEdit = () => {
    setEditingId(null)
    setNome("")
    setDescricao("")
  }

  return (
    <div className="tipo-instituicao-form">
      <div className="form-card">
        <div className="form-header">
          <h3 className="form-title">{editingId ? "Editar Tipo de Instituição" : "Novo Tipo de Instituição"}</h3>
        </div>
        <div className="form-content">
          <form onSubmit={handleSubmit} className="form">
            <div className="form-group">
              <label htmlFor="nome" className="form-label">
                Nome do Tipo
              </label>
              <input
                id="nome"
                type="text"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Ex: Cooperativa, Agência, Posto de Atendimento"
                className="form-input"
                required
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
                placeholder="Descrição do tipo de instituição"
                className="form-textarea"
                rows={3}
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary">
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
        </div>
      </div>

      <div className="list-section">
        <h3 className="list-title">Tipos Cadastrados ({tiposInstituicao.length})</h3>

        {tiposInstituicao.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum tipo de instituição cadastrado ainda.</p>
          </div>
        ) : (
          <div className="items-grid">
            {tiposInstituicao.map((tipo) => (
              <div key={tipo.id} className="item-card">
                <div className="item-content">
                  <div className="item-info">
                    <h4 className="item-title">{tipo.nome}</h4>
                    {tipo.descricao && <p className="item-description">{tipo.descricao}</p>}
                    <p className="item-date">Criado em: {tipo.createdAt.toLocaleDateString("pt-BR")}</p>
                  </div>
                  <div className="item-actions">
                    <button onClick={() => handleEdit(tipo)} className="btn btn-edit" title="Editar">
                      ✏️
                    </button>
                    <button onClick={() => handleDelete(tipo.id)} className="btn btn-delete" title="Excluir">
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

export default TipoInstituicaoForm
