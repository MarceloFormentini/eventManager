"use client"

import { useEffect, useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Building2, MapPin, Calendar, Settings } from "lucide-react"
import TipoInstituicaoForm from "./components/tipo-instituicao-form"
import InstituicaoForm from "./components/instituicao-form"
import LocalForm from "./components/local-form"
import EventoForm from "./components/evento-form"
import Header from "./components/header"
import { apiRequest } from "@/lib/api"

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
  status: "DRAFT" | "PUBLISHED" | "CANCELLED" | "COMPLETED"
  createdAt: Date
}

type ApiType = {
  id: number
  name: string
  description: string | null
  createdAt: string
}
type ApiInstitution = {
  id: number
  name: string
  typeInstitutionId: number
  address: string | null
  phone: string | null
  email: string | null
  createdAt: string
}
type ApiLocation = {
  id: number
  name: string
  institutionId: number
  capacity: number
  description: string | null
  createdAt: string
}
type ApiEvent = {
  id: number
  title: string
  description: string | null
  locationId: number
  startDate: string
  endDate: string
  status: Evento["status"]
  createdAt: string
}

export default function TodoSystem() {
  const [tiposInstituicao, setTiposInstituicao] = useState<TipoInstituicao[]>([])
  const [instituicoes, setInstituicoes] = useState<Instituicao[]>([])
  const [locais, setLocais] = useState<Local[]>([])
  const [eventos, setEventos] = useState<Evento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  async function refreshData() {
    try {
      const [types, institutions, locations, events] = await Promise.all([
        apiRequest<ApiType[]>("/institution-types"),
        apiRequest<ApiInstitution[]>("/institutions"),
        apiRequest<ApiLocation[]>("/locations"),
        apiRequest<ApiEvent[]>("/events"),
      ])
      setTiposInstituicao(types.map((item) => ({
        id: String(item.id),
        nome: item.name,
        descricao: item.description ?? "",
        createdAt: new Date(item.createdAt),
      })))
      setInstituicoes(institutions.map((item) => ({
        id: String(item.id),
        nome: item.name,
        tipoInstituicaoId: String(item.typeInstitutionId),
        endereco: item.address ?? "",
        telefone: item.phone ?? "",
        email: item.email ?? "",
        createdAt: new Date(item.createdAt),
      })))
      setLocais(locations.map((item) => ({
        id: String(item.id),
        nome: item.name,
        instituicaoId: String(item.institutionId),
        capacidade: item.capacity,
        descricao: item.description ?? "",
        createdAt: new Date(item.createdAt),
      })))
      setEventos(events.map((item) => ({
        id: String(item.id),
        titulo: item.title,
        descricao: item.description ?? "",
        localId: String(item.locationId),
        dataInicio: new Date(item.startDate),
        dataFim: new Date(item.endDate),
        status: item.status,
        createdAt: new Date(item.createdAt),
      })))
      setError("")
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Falha ao carregar os dados.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refreshData()
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />

      <main className="container mx-auto px-4 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Sistema de Gestão</h1>
          <p className="text-gray-600">Gerencie tipos de instituição, instituições, locais e eventos</p>
        </div>

        {error && (
          <div role="alert" className="mb-6 rounded-md border border-red-300 bg-red-50 p-4 text-red-800">
            <p>{error}</p>
            <button type="button" className="mt-2 font-semibold underline" onClick={() => void refreshData()}>
              Tentar novamente
            </button>
          </div>
        )}
        {loading && <p className="mb-4 text-gray-600">Carregando dados...</p>}

        <Tabs defaultValue="tipos" className="w-full">
          <TabsList className="grid w-full grid-cols-4 mb-8">
            <TabsTrigger value="tipos" className="flex items-center gap-2">
              <Settings className="w-4 h-4" />
              Tipos
            </TabsTrigger>
            <TabsTrigger value="instituicoes" className="flex items-center gap-2">
              <Building2 className="w-4 h-4" />
              Instituições
            </TabsTrigger>
            <TabsTrigger value="locais" className="flex items-center gap-2">
              <MapPin className="w-4 h-4" />
              Locais
            </TabsTrigger>
            <TabsTrigger value="eventos" className="flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              Eventos
            </TabsTrigger>
          </TabsList>

          <TabsContent value="tipos">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="w-5 h-5" />
                  Tipos de Instituição
                </CardTitle>
              </CardHeader>
              <CardContent>
                <TipoInstituicaoForm tiposInstituicao={tiposInstituicao} onRefresh={refreshData} />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="instituicoes">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Building2 className="w-5 h-5" />
                  Instituições
                </CardTitle>
              </CardHeader>
              <CardContent>
                <InstituicaoForm
                  instituicoes={instituicoes}
                  tiposInstituicao={tiposInstituicao}
                  onRefresh={refreshData}
                />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="locais">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MapPin className="w-5 h-5" />
                  Locais
                </CardTitle>
              </CardHeader>
              <CardContent>
                <LocalForm locais={locais} instituicoes={instituicoes} onRefresh={refreshData} />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="eventos">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Calendar className="w-5 h-5" />
                  Eventos
                </CardTitle>
              </CardHeader>
              <CardContent>
                <EventoForm eventos={eventos} locais={locais} instituicoes={instituicoes} onRefresh={refreshData} />
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}
