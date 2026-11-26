import { NextRequest, NextResponse } from "next/server"

export const dynamic = "force-dynamic"

type RouteContext = {
  params: Promise<{ path: string[] }>
}

async function proxy(request: NextRequest, context: RouteContext) {
  const { path } = await context.params
  const backendUrl = process.env.BACKEND_API_URL ?? "http://localhost:8080"
  const target = new URL(
    `/api/v1/${path.map(encodeURIComponent).join("/")}${request.nextUrl.search}`,
    backendUrl,
  )

  const headers = new Headers()
  const contentType = request.headers.get("content-type")
  if (contentType) headers.set("content-type", contentType)
  const requestId = request.headers.get("x-request-id")
  if (requestId) headers.set("x-request-id", requestId)

  const body =
    request.method === "GET" || request.method === "HEAD"
      ? undefined
      : await request.text()

  const response = await fetch(target, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
  })

  const responseBody =
    response.status === 204 ? null : await response.arrayBuffer()
  const responseHeaders = new Headers({
    "content-type":
      response.headers.get("content-type") ?? "application/json",
  })
  const responseRequestId = response.headers.get("x-request-id")
  if (responseRequestId) responseHeaders.set("x-request-id", responseRequestId)

  return new NextResponse(responseBody, {
    status: response.status,
    headers: responseHeaders,
  })
}

export const GET = proxy
export const POST = proxy
export const PUT = proxy
export const PATCH = proxy
export const DELETE = proxy
