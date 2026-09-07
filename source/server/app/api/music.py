from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.api.dependencies import get_current_user
from app.application.music_service import (
    get_now_playing,
    get_players,
    get_queue,
    next,
    pause,
    play,
    previous,
    search,
)
from app.database.models.user import User


class MusicSearchItem(BaseModel):
    id: str
    title: str
    artist: str | None = None
    album: str | None = None
    duration: int | None = None
    uri: str


class MusicSearchResponse(BaseModel):
    items: list[MusicSearchItem]


class MusicPlayerItem(BaseModel):
    id: str
    name: str
    available: bool


class MusicPlayersResponse(BaseModel):
    items: list[MusicPlayerItem]


class MusicNowPlayingTrack(BaseModel):
    id: str
    title: str
    artist: str | None = None
    album: str | None = None
    duration: int | None = None
    uri: str
    stream_url: str | None = None


class MusicNowPlayingResponse(BaseModel):
    player_id: str
    state: str
    track: MusicNowPlayingTrack | None = None
    elapsed: int


class MusicPlayRequest(BaseModel):
    player_id: str
    uri: str


class MusicPlayerRequest(BaseModel):
    player_id: str


router = APIRouter(
    prefix="/music",
    tags=["music"],
)


@router.get("/search", response_model=MusicSearchResponse)
def music_search(
    q: str = Query(..., min_length=1),
    current_user: User = Depends(get_current_user),
) -> MusicSearchResponse:
    result = search(q)
    return MusicSearchResponse.model_validate(result)


@router.get("/players", response_model=MusicPlayersResponse)
def music_players(
    current_user: User = Depends(get_current_user),
) -> MusicPlayersResponse:
    result = get_players()
    return MusicPlayersResponse.model_validate(result)


@router.get("/now-playing", response_model=MusicNowPlayingResponse)
def music_now_playing(
    player_id: str = Query(..., min_length=1),
    current_user: User = Depends(get_current_user),
) -> MusicNowPlayingResponse:
    result = get_now_playing(player_id)

    if result is None:
        from fastapi import HTTPException

        raise HTTPException(
            status_code=404,
            detail="Player not found",
        )

    return MusicNowPlayingResponse.model_validate(result)


@router.get("/queue")
def music_queue(
    player_id: str = Query(..., min_length=1),
    current_user: User = Depends(get_current_user),
) -> dict:
    result = get_queue(player_id)

    return result


@router.post("/play")
def music_play(
    request: MusicPlayRequest,
    current_user: User = Depends(get_current_user),
) -> dict:
    return play(request.player_id, request.uri)


@router.post("/pause")
def music_pause(
    request: MusicPlayerRequest,
    current_user: User = Depends(get_current_user),
) -> dict:
    return pause(request.player_id)


@router.post("/next")
def music_next(
    request: MusicPlayerRequest,
    current_user: User = Depends(get_current_user),
) -> dict:
    return next(request.player_id)

@router.post("/previous")
def music_previous(
    request: MusicPlayerRequest,
    current_user: User = Depends(get_current_user),
) -> dict:
    return previous(request.player_id)
