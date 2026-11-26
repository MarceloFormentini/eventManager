export async function apiRequest<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api/backend${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...init?.headers,
      },
      cache: "no-store",
    })
  } catch {
    throw new Error("Não foi possível conectar ao servidor. Tente novamente.")
  }

  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null)
    const errorBody =
      typeof body === "object" && body !== null
        ? (body as { error?: unknown; message?: unknown })
        : null
    const message =
      typeof errorBody?.error === "string"
        ? errorBody.error
        : getStatusMessage(response.status)
    throw new Error(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

type ApiPage<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export async function apiRequestAll<T>(path: string): Promise<T[]> {
  const items: T[] = []
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const separator = path.includes("?") ? "&" : "?"
    const result = await apiRequest<ApiPage<T>>(
      `${path}${separator}page=${page}&size=100`,
    )
    items.push(...result.content)
    totalPages = result.totalPages
    page += 1
  }

  return items
}

function getStatusMessage(status: number): string {
  switch (status) {
    case 400:
      return "Os dados informados são inválidos. Verifique os campos e tente novamente."
    case 404:
      return "O registro solicitado não foi encontrado."
    case 409:
      return "Não foi possível concluir a operação por causa de um conflito."
    case 500:
    case 502:
    case 503:
      return "Ocorreu um erro no servidor. Tente novamente mais tarde."
    default:
      return "Não foi possível concluir a operação. Tente novamente."
  }
}
