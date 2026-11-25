"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Trash2, Edit, Plus, Calendar, Clock, MapPin, Building2 } from "lucide-react"
import type { Evento, Local, Instituicao } from "../page"
import { apiRequest } from "@/lib/api"

interface EventoFormProps {
  eventos: Evento[]
  locais: Local[]
  instituicoes: Instituicao[]
  onRefresh: () => Promise<void>
}

export default function EventoForm({ eventos, locais, instituicoes, onRefresh }: EventoFormProps) {
  const [titulo, setTitulo] = useState("")
  const [descricao, setDescricao] = useState("")
  const [localId, setLocalId] = useState("")
  const [dataInicio, setDataInicio] = useState("")
  const [dataFim, setDataFim] = useState("")
  const [status, setStatus] = useState<Evento["status"]>("DRAFT")
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!titulo.trim() || !localId || !dataInicio || !dataFim) return
    if (new Date(dataFim) <= new Date(dataInicio)) {
      setError("A data de término deve ser posterior à data de início.")
      return
    }

    try {
      const currentEvent = editingId
        ? eventos.find((evento) => evento.id === editingId)
        : undefined
      await apiRequest(editingId ? `/events/${editingId}` : "/events", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify({
          title: titulo.trim(),
          description: descricao.trim() || null,
          locationId: Number(localId),
          startDate: new Date(dataInicio).toISOString(),
          endDate: new Date(dataFim).toISOString(),
          status,
        }),
      })
      if (editingId && currentEvent && currentEvent.status !== status) {
        await apiRequest(`/events/${editingId}/status`, {
          method: "PATCH",
          body: JSON.stringify({ status }),
        })
      }
      await onRefresh()
      setEditingId(null)
      setError("")
      resetForm()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Não foi possível salvar o evento.")
    }
  }

  const resetForm = () => {
    setTitulo("")
    setDescricao("")
    setLocalId("")
    setDataInicio("")
    setDataFim("")
    setStatus("DRAFT")
  }

  const handleEdit = (evento: Evento) => {
    setTitulo(evento.titulo)
    setDescricao(evento.descricao)
    setLocalId(evento.localId)
    setDataInicio(toLocalDateTime(evento.dataInicio))
    setDataFim(toLocalDateTime(evento.dataFim))
    setStatus(evento.status)
    setEditingId(evento.id)
  }

  const toLocalDateTime = (date: Date) => {
    const pad = (value: number) => String(value).padStart(2, "0")
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
  }

  const handleDelete = async (id: string) => {
    try {
      await apiRequest(`/events/${id}`, { method: "DELETE" })
      await onRefresh()
      setError("")
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Não foi possível excluir o evento.")
    }
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
      DRAFT: { color: "bg-yellow-100 text-yellow-800", label: "Rascunho" },
      PUBLISHED: { color: "bg-blue-100 text-blue-800", label: "Publicado" },
      CANCELLED: { color: "bg-red-100 text-red-800", label: "Cancelado" },
      COMPLETED: { color: "bg-green-100 text-green-800", label: "Concluído" },
    }

    const config = statusConfig[status as keyof typeof statusConfig]
    return <Badge className={config.color}>{config.label}</Badge>
  }

  return (
    <div className="space-y-6">
      <Card className="border-[#00A651]/20">
        <CardHeader>
          <CardTitle className="text-[#00A651]">{editingId ? "Editar Evento" : "Novo Evento"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="titulo">Título do Evento</Label>
              <Input
                id="titulo"
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
                placeholder="Ex: Reunião Mensal, Treinamento, Assembleia"
                className="focus:border-[#00A651]"
                maxLength={150}
                required
              />
            </div>

            <div>
              <Label htmlFor="local">Local</Label>
              <Select value={localId} onValueChange={setLocalId} required>
                <SelectTrigger className="focus:border-[#00A651]">
                  <SelectValue placeholder="Selecione o local" />
                </SelectTrigger>
                <SelectContent>
                  {locais.map((local) => (
                    <SelectItem key={local.id} value={local.id}>
                      {local.nome} - {getInstituicaoNome(local.id)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="dataInicio">Data e Hora de Início</Label>
                <Input
                  id="dataInicio"
                  type="datetime-local"
                  value={dataInicio}
                  onChange={(e) => setDataInicio(e.target.value)}
                  className="focus:border-[#00A651]"
                  required
                />
              </div>

              <div>
                <Label htmlFor="dataFim">Data e Hora de Fim</Label>
                <Input
                  id="dataFim"
                  type="datetime-local"
                  value={dataFim}
                  onChange={(e) => setDataFim(e.target.value)}
                  className="focus:border-[#00A651]"
                  required
                />
              </div>
            </div>

            <div>
              <Label htmlFor="status">Status</Label>
              <Select
                value={status}
                onValueChange={(value: Evento["status"]) => setStatus(value)}
              >
                <SelectTrigger className="focus:border-[#00A651]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DRAFT">Rascunho</SelectItem>
                  <SelectItem value="PUBLISHED">Publicado</SelectItem>
                  <SelectItem value="CANCELLED">Cancelado</SelectItem>
                  <SelectItem value="COMPLETED">Concluído</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="descricao">Descrição</Label>
              <Textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                placeholder="Descrição do evento, agenda, participantes esperados, etc."
                className="focus:border-[#00A651]"
                maxLength={1000}
                rows={3}
              />
            </div>

            <div className="flex gap-2">
              <Button
                type="submit"
                className="bg-[#00A651] hover:bg-[#008A44] flex items-center gap-2"
                disabled={locais.length === 0}
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

          {locais.length === 0 && (
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 text-sm">⚠️ Cadastre pelo menos um local antes de adicionar eventos.</p>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Eventos Cadastrados ({eventos.length})</h3>

        {eventos.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center text-gray-500">Nenhum evento cadastrado ainda.</CardContent>
          </Card>
        ) : (
          <div className="grid gap-4">
            {eventos.map((evento) => (
              <Card key={evento.id} className="border-l-4 border-l-[#00A651]">
                <CardContent className="py-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <Calendar className="w-5 h-5 text-[#00A651]" />
                        <h4 className="font-semibold text-gray-900">{evento.titulo}</h4>
                        {getStatusBadge(evento.status)}
                      </div>

                      <div className="space-y-1 text-sm text-gray-600">
                        <p className="flex items-center gap-1">
                          <MapPin className="w-4 h-4" />
                          <strong>Local:</strong> {getLocalNome(evento.localId)}
                        </p>
                        <p className="flex items-center gap-1">
                          <Building2 className="w-4 h-4" />
                          <strong>Instituição:</strong> {getInstituicaoNome(evento.localId)}
                        </p>
                        <p className="flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          <strong>Início:</strong> {evento.dataInicio.toLocaleString("pt-BR")}
                        </p>
                        <p className="flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          <strong>Fim:</strong> {evento.dataFim.toLocaleString("pt-BR")}
                        </p>
                        {evento.descricao && <p className="mt-2">{evento.descricao}</p>}
                      </div>

                      <p className="text-xs text-gray-400 mt-2">
                        Criado em: {evento.createdAt.toLocaleDateString("pt-BR")}
                      </p>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEdit(evento)}
                        disabled={evento.status === "CANCELLED" || evento.status === "COMPLETED"}
                        className="border-[#0066CC] text-[#0066CC] hover:bg-[#0066CC]/10"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDelete(evento.id)}
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
