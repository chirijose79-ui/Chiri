from typing import Any

import httpx2

from app.config.settings import settings


class MusicAssistantClient:
    def __init__(self) -> None:
        self._client = httpx2.Client(
            base_url=settings.music_assistant_url,
            headers={
                "Authorization": f"Bearer {settings.music_assistant_token}",
                "Content-Type": "application/json",
            },
            timeout=10.0,
        )

    def _request(
        self,
        command: str,
        args: dict[str, Any] | None = None,
    ) -> Any:
        response = self._client.post(
            "/api",
            json={
                "message_id": "chiri-backend",
                "command": command,
                "args": args or {},
            },
        )
        response.raise_for_status()
        return response.json()

    def search(self, query: str) -> Any:
        return self._request(
            "music/search",
            {
                "search_query": query,
            },
        )

    def get_players(self) -> list[dict[str, Any]]:
        players = self._request("players/all")

        result = []

        for player in players:
            if player.get("hide_in_ui"):
                continue

            if player.get("private"):
                continue

            result.append(
                {
                    "id": player.get("player_id"),
                    "name": player.get("name"),
                    "available": player.get("available", False),
                }
            )

        return result

    def get_session_id(self, player_id: str) -> str | None:
        return self._request(
            "player_queues/get_session_id",
            {
                "queue_id": player_id,
            },
        )

    def get_now_playing(self, player_id: str) -> dict[str, Any] | None:
        players = self._request("players/all")

        for player in players:
            if player.get("player_id") != player_id:
                continue

            current_media = player.get("current_media")

            if not current_media:
                return {
                    "player_id": player_id,
                    "state": player.get("state", "idle"),
                    "track": None,
                    "elapsed": 0,
                }

            return {
                "player_id": player_id,
                "state": player.get("state", "idle"),
                "track": {
                    "id": current_media.get("uri", "").split("/")[-1],
                    "title": current_media.get("title"),
                    "artist": current_media.get("artist"),
                    "album": current_media.get("album"),
                    "duration": current_media.get("duration"),
                    "uri": current_media.get("uri"),
                },
                "elapsed": int(current_media.get("elapsed_time", 0)),
            }

        return None
    
    def get_queue(self, player_id: str) -> list[dict[str, Any]]:
        items = self._request(
            "player_queues/items",
            {
                "queue_id": player_id,
            },
        )

        queue = []

        for item in items:
            media_item = item.get("media_item") or {}
            artists = media_item.get("artists") or []
            album = media_item.get("album") or {}

            queue.append(
                {
                    "id": item.get("queue_item_id"),
                    "title": media_item.get("name"),
                    "artist": artists[0].get("name") if artists else None,
                    "album": album.get("name"),
                    "duration": item.get("duration"),
                }
            )

        return queue

    def play(self, player_id: str, uri: str) -> Any:
        return self._request(
            "player_queues/play_media",
            {
                "queue_id": player_id,
                "media": uri,
            },
        )

    def pause(self, player_id: str) -> Any:
        return self._request(
            "players/cmd/pause",
            {
                "player_id": player_id,
            },
        )

    def next(self, player_id: str) -> Any:
        return self._request(
            "players/cmd/next",
            {
                "player_id": player_id,
            },
        )

    def previous(self, player_id: str) -> Any:
        return self._request(
            "players/cmd/previous",
            {
                "player_id": player_id,
            },
        )

    def close(self) -> None:
            self._client.close()
