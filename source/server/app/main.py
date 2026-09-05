from fastapi import FastAPI

from app.api.auth import router as auth_router
from app.api.home import router as home_router


app = FastAPI(
    title="Chiri Platform API",
    version="1.0.0",
)


app.include_router(auth_router)
app.include_router(home_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
