"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Trash2, Edit, Plus, Building2, Phone, Mail, MapPin } from "lucide-react"
import type { Instituicao, TipoInstituicao } from "../page"
import { apiRequest } from "@/lib/api"

interface InstituicaoFormProps {
  instituicoes: Instituicao[]
  tiposInstituicao: TipoInstituicao[]
  onRefresh: () => Promise<void>
}

export default function InstituicaoForm({ instituicoes, tiposInstituicao, onRefresh }: InstituicaoFormProps) {
  const [nome, setNome] = useState("")
  const [tipoInstituicaoId, setTipoInstituicaoId] = useState("")
  const [endereco, setEndereco] = useState("")
  const [telefone, setTelefone] = useState("")
  const [email, setEmail] = useState("")
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!nome.trim() || !tipoInstituicaoId) return

    try {
      await apiRequest(editingId ? `/institutions/${editingId}` : "/institutions", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify({
          name: nome.trim(),
          typeInstitutionId: Number(tipoInstituicaoId),
          address: endereco.trim() || null,
          phone: telefone.trim() || null,
          email: email.trim() || null,
        }),
      })
      await onRefresh()
      setEditingId(null)
      setError("")
      resetForm()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Não foi possível salvar a instituição.")
    }
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

  const handleDelete = async (id: string) => {
    try {
      await apiRequest(`/institutions/${id}`, { method: "DELETE" })
      await onRefresh()
      setError("")
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Não foi possível excluir a instituição.")
    }
  }

  const cancelEdit = () => {
    setEditingId(null)
    resetForm()
  }

  const getTipoNome = (tipoId: string) => {
    return tiposInstituicao.find((tipo) => tipo.id === tipoId)?.nome || "Tipo não encontrado"
  }

  return (
    <div className="space-y-6">
      <Card className="border-[#00A651]/20">
        <CardHeader>
          <CardTitle className="text-[#00A651]">{editingId ? "Editar Instituição" : "Nova Instituição"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="nome">Nome da Instituição</Label>
                <Input
                  id="nome"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Nome da instituição"
                  className="focus:border-[#00A651]"
                  maxLength={150}
                  required
                />
              </div>

              <div>
                <Label htmlFor="tipo">Tipo de Instituição</Label>
                <Select value={tipoInstituicaoId} onValueChange={setTipoInstituicaoId} required>
                  <SelectTrigger className="focus:border-[#00A651]">
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                  <SelectContent>
                    {tiposInstituicao.map((tipo) => (
                      <SelectItem key={tipo.id} value={tipo.id}>
                        {tipo.nome}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div>
              <Label htmlFor="endereco">Endereço</Label>
              <Input
                id="endereco"
                value={endereco}
                onChange={(e) => setEndereco(e.target.value)}
                placeholder="Endereço completo"
                className="focus:border-[#00A651]"
                maxLength={300}
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="telefone">Telefone</Label>
                <Input
                  id="telefone"
                  value={telefone}
                  onChange={(e) => setTelefone(e.target.value)}
                  placeholder="(00) 00000-0000"
                  className="focus:border-[#00A651]"
                  maxLength={50}
                />
              </div>

              <div>
                <Label htmlFor="email">E-mail</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email@exemplo.com"
                  className="focus:border-[#00A651]"
                  maxLength={254}
                />
              </div>
            </div>

            <div className="flex gap-2">
              <Button
                type="submit"
                className="bg-[#00A651] hover:bg-[#008A44] flex items-center gap-2"
                disabled={tiposInstituicao.length === 0}
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

          {tiposInstituicao.length === 0 && (
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 text-sm">
                ⚠️ Cadastre pelo menos um tipo de instituição antes de adicionar instituições.
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Instituições Cadastradas ({instituicoes.length})</h3>

        {instituicoes.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center text-gray-500">Nenhuma instituição cadastrada ainda.</CardContent>
          </Card>
        ) : (
          <div className="grid gap-4">
            {instituicoes.map((instituicao) => (
              <Card key={instituicao.id} className="border-l-4 border-l-[#00A651]">
                <CardContent className="py-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <Building2 className="w-5 h-5 text-[#00A651]" />
                        <h4 className="font-semibold text-gray-900">{instituicao.nome}</h4>
                      </div>

                      <div className="space-y-1 text-sm text-gray-600">
                        <p>
                          <strong>Tipo:</strong> {getTipoNome(instituicao.tipoInstituicaoId)}
                        </p>
                        {instituicao.endereco && (
                          <p className="flex items-center gap-1">
                            <MapPin className="w-4 h-4" />
                            {instituicao.endereco}
                          </p>
                        )}
                        {instituicao.telefone && (
                          <p className="flex items-center gap-1">
                            <Phone className="w-4 h-4" />
                            {instituicao.telefone}
                          </p>
                        )}
                        {instituicao.email && (
                          <p className="flex items-center gap-1">
                            <Mail className="w-4 h-4" />
                            {instituicao.email}
                          </p>
                        )}
                      </div>

                      <p className="text-xs text-gray-400 mt-2">
                        Criado em: {instituicao.createdAt.toLocaleDateString("pt-BR")}
                      </p>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEdit(instituicao)}
                        className="border-[#0066CC] text-[#0066CC] hover:bg-[#0066CC]/10"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDelete(instituicao.id)}
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
