"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Trash2, Edit, Plus, MapPin, Users } from "lucide-react"
import type { Local, Instituicao } from "../page"
import { apiRequest } from "@/lib/api"

interface LocalFormProps {
  locais: Local[]
  instituicoes: Instituicao[]
  onRefresh: () => Promise<void>
}

export default function LocalForm({ locais, instituicoes, onRefresh }: LocalFormProps) {
  const [nome, setNome] = useState("")
  const [instituicaoId, setInstituicaoId] = useState("")
  const [capacidade, setCapacidade] = useState("")
  const [descricao, setDescricao] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!nome.trim() || !instituicaoId) return

    try {
      await apiRequest(editingId ? `/locations/${editingId}` : "/locations", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify({
          name: nome.trim(),
          institutionId: Number(instituicaoId),
          capacity: Number.parseInt(capacidade, 10) || 0,
          description: descricao.trim() || null,
        }),
      })
      await onRefresh()
      setEditingId(null)
      setError("")
      resetForm()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Não foi possível salvar o local.")
    }
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

  const handleDelete = async (id: string) => {
    try {
      await apiRequest(`/locations/${id}`, { method: "DELETE" })
      await onRefresh()
      setError("")
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Não foi possível excluir o local.")
    }
  }

  const cancelEdit = () => {
    setEditingId(null)
    resetForm()
  }

  const getInstituicaoNome = (instituicaoId: string) => {
    return instituicoes.find((inst) => inst.id === instituicaoId)?.nome || "Instituição não encontrada"
  }

  return (
    <div className="space-y-6">
      <Card className="border-[#00A651]/20">
        <CardHeader>
          <CardTitle className="text-[#00A651]">{editingId ? "Editar Local" : "Novo Local"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="nome">Nome do Local</Label>
                <Input
                  id="nome"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Ex: Auditório Principal, Sala de Reuniões"
                  className="focus:border-[#00A651]"
                  maxLength={150}
                  required
                />
              </div>

              <div>
                <Label htmlFor="instituicao">Instituição</Label>
                <Select value={instituicaoId} onValueChange={setInstituicaoId} required>
                  <SelectTrigger className="focus:border-[#00A651]">
                    <SelectValue placeholder="Selecione a instituição" />
                  </SelectTrigger>
                  <SelectContent>
                    {instituicoes.map((instituicao) => (
                      <SelectItem key={instituicao.id} value={instituicao.id}>
                        {instituicao.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div>
              <Label htmlFor="capacidade">Capacidade (pessoas)</Label>
              <Input
                id="capacidade"
                type="number"
                value={capacidade}
                onChange={(e) => setCapacidade(e.target.value)}
                placeholder="Ex: 50"
                className="focus:border-[#00A651]"
                min="0"
              />
            </div>

            <div>
              <Label htmlFor="descricao">Descrição</Label>
              <Textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                placeholder="Descrição do local, equipamentos disponíveis, etc."
                className="focus:border-[#00A651]"
                maxLength={2000}
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <Button
                type="submit"
                className="bg-[#00A651] hover:bg-[#008A44] flex items-center gap-2"
                disabled={instituicoes.length === 0}
              >
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
          </form>
          {error && <p role="alert" className="mt-3 text-sm text-red-600">{error}</p>}

          {instituicoes.length === 0 && (
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 text-sm">
                ⚠️ Cadastre pelo menos uma instituição antes de adicionar locais.
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Locais Cadastrados ({locais.length})</h3>

        {locais.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center text-gray-500">Nenhum local cadastrado ainda.</CardContent>
          </Card>
        ) : (
          <div className="grid gap-4">
            {locais.map((local) => (
              <Card key={local.id} className="border-l-4 border-l-[#00A651]">
                <CardContent className="py-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <MapPin className="w-5 h-5 text-[#00A651]" />
                        <h4 className="font-semibold text-gray-900">{local.nome}</h4>
                      </div>

                      <div className="space-y-1 text-sm text-gray-600">
                        <p>
                          <strong>Instituição:</strong> {getInstituicaoNome(local.instituicaoId)}
                        </p>
                        {local.capacidade > 0 && (
                          <p className="flex items-center gap-1">
                            <Users className="w-4 h-4" />
                            Capacidade: {local.capacidade} pessoas
                          </p>
                        )}
                        {local.descricao && <p className="mt-2">{local.descricao}</p>}
                      </div>

                      <p className="text-xs text-gray-400 mt-2">
                        Criado em: {local.createdAt.toLocaleDateString("pt-BR")}
                      </p>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEdit(local)}
                        className="border-[#0066CC] text-[#0066CC] hover:bg-[#0066CC]/10"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDelete(local.id)}
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
