import httpx
import os
import random
from contextlib import asynccontextmanager
from dotenv import load_dotenv
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import Response

load_dotenv()

# Конфигурация
MONOLITH_URL = os.getenv("MONOLITH_URL", "http://localhost:8080").rstrip("/")
MOVIES_SERVICE_URL = os.getenv("MOVIES_SERVICE_URL", "http://localhost:8081").rstrip("/")
EVENTS_SERVICE_URL = os.getenv("EVENTS_SERVICE_URL", "http://localhost:8082").rstrip("/")

GRADUAL_MIGRATION = (os.getenv("GRADUAL_MIGRATION", "true").lower() == "true")
MOVIES_MIGRATION_PERCENT = int(os.getenv("MOVIES_MIGRATION_PERCENT", "50"))

if not 0 <= MOVIES_MIGRATION_PERCENT <= 100:
    raise ValueError("MOVIES_MIGRATION_PERCENT must be between 0 and 100")


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.http_client = httpx.AsyncClient(timeout=10.0)
    yield
    await app.state.http_client.aclose()


app = FastAPI(
    title="API Gateway",
    description="API Gateway для маршрутизации запросов и разделения трафика между монолитом и новыми сервисами",
    lifespan=lifespan,
)


def get_movies_target() -> str:
    """ Распределение трафика в процентном соотношении между монолитом и movies сервисом """

    if not GRADUAL_MIGRATION:
        return MONOLITH_URL

    if random.random() * 100 < MOVIES_MIGRATION_PERCENT:
        return MOVIES_SERVICE_URL

    return MONOLITH_URL


async def proxy_request(request: Request, target_url: str):
    """ Перенаправление запросов к нужному сервису """

    if not target_url:
        raise HTTPException(
            status_code=503,
            detail="Target service URL is not configured",
        )

    url = f"{target_url}{request.url.path}"

    if request.url.query:
        url += f"?{request.url.query}"

    # Forward headers, excluding connection-specific headers.
    excluded_headers = {
        "host",
        "connection",
        "content-length",
        "transfer-encoding",
    }

    headers = {
        key: value
        for key, value in request.headers.items()
        if key.lower() not in excluded_headers
    }

    try:
        client = request.app.state.http_client

        response = await client.get(
            url,
            headers=headers,
        )

        # Avoid forwarding hop-by-hop response headers.
        excluded_response_headers = {
            "connection",
            "transfer-encoding",
            "content-length",
        }

        response_headers = {
            key: value
            for key, value in response.headers.items()
            if key.lower() not in excluded_response_headers
        }

        return Response(
            content=response.content,
            status_code=response.status_code,
            headers=response_headers,
        )

    except httpx.RequestError:
        raise HTTPException(
            status_code=502,
            detail="Failed to connect to target service",
        )


# 1. Health check
@app.get("/health")
async def health():
    return {"status": "ok"}


# 2. Movies endpoint
@app.get("/api/movies")
async def get_movies(request: Request):
    target = get_movies_target()
    return await proxy_request(request, target)


# 3. Users endpoint
@app.get("/api/users")
async def get_users(request: Request):
    return await proxy_request(request, MONOLITH_URL)
