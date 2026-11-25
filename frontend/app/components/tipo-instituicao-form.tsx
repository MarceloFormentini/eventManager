"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Trash2, Edit, Plus } from "lucide-react"
import type { TipoInstituicao } from "../page"
import { apiRequest } from "@/lib/api"

interface TipoInstituicaoFormProps {
  tiposInstituicao: TipoInstituicao[]
  onRefresh: () => Promise<void>
}

export default function TipoInstituicaoForm({ tiposInstituicao, onRefresh }: TipoInstituicaoFormProps) {
  const [nome, setNome] = useState("")
  const [descricao, setDescricao] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!nome.trim()) return

    try {
      await apiRequest(editingId ? `/institution-types/${editingId}` : "/institution-types", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify({ name: nome.trim(), description: descricao.trim() || null }),
      })
      await onRefresh()
      setEditingId(null)
      setError("")
      setNome("")
      setDescricao("")
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Não foi possível salvar o tipo.")
    }
  }

  const handleEdit = (tipo: TipoInstituicao) => {
    setNome(tipo.nome)
    setDescricao(tipo.descricao)
    setEditingId(tipo.id)
  }

  const handleDelete = async (id: string) => {
    try {
      await apiRequest(`/institution-types/${id}`, { method: "DELETE" })
      await onRefresh()
      setError("")
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Não foi possível excluir o tipo.")
    }
  }

  const cancelEdit = () => {
    setEditingId(null)
    setNome("")
    setDescricao("")
  }

  return (
    <div className="space-y-6">
      <Card className="border-[#00A651]/20">
        <CardHeader>
          <CardTitle className="text-[#00A651]">
            {editingId ? "Editar Tipo de Instituição" : "Novo Tipo de Instituição"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="nome">Nome do Tipo</Label>
              <Input
                id="nome"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Ex: Cooperativa, Agência, Posto de Atendimento"
                className="focus:border-[#00A651]"
                maxLength={100}
                required
              />
            </div>

            <div>
              <Label htmlFor="descricao">Descrição</Label>
              <Textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                placeholder="Descrição do tipo de instituição"
                className="focus:border-[#00A651]"
                maxLength={2000}
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <Button type="submit" className="bg-[#00A651] hover:bg-[#008A44] flex items-center gap-2">
                <Plus className="w-4 h-4" />
                {editingId ? "Atualizar" : "Adicionar"}
              </Button>
              {editingId && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={cancelEdit}
                  className="border-[#00A651] text-[#00A651] hover:bg-[#00A651]/10"
                >
                  Cancelar
                </Button>
              )}
            </div>
            {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
          </form>
        </CardContent>
      </Card>

      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Tipos Cadastrados ({tiposInstituicao.length})</h3>

        {tiposInstituicao.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center text-gray-500">
              Nenhum tipo de instituição cadastrado ainda.
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-4">
            {tiposInstituicao.map((tipo) => (
              <Card key={tipo.id} className="border-l-4 border-l-[#00A651]">
                <CardContent className="py-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h4 className="font-semibold text-gray-900">{tipo.nome}</h4>
                      {tipo.descricao && <p className="text-gray-600 mt-1">{tipo.descricao}</p>}
                      <p className="text-xs text-gray-400 mt-2">
                        Criado em: {tipo.createdAt.toLocaleDateString("pt-BR")}
                      </p>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEdit(tipo)}
                        className="border-[#0066CC] text-[#0066CC] hover:bg-[#0066CC]/10"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDelete(tipo.id)}
                        className="border-red-500 text-red-500 hover:bg-red-50"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
