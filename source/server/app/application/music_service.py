from app.integrations.music_assistant.client import MusicAssistantClient


def search(query: str) -> dict:
    client = MusicAssistantClient()

    try:
        results = client.search(query)

        items = []

        for track in results.get("tracks", []):
            items.append(
                {
                    "id": track.get("item_id"),
                    "title": track.get("name"),
                    "artist": (
                        track.get("artists", [{}])[0].get("name")
                        if track.get("artists")
                        else None
                    ),
                    "album": (
                        track.get("album", {}).get("name")
                        if track.get("album")
                        else None
                    ),
                    "duration": track.get("duration"),
                    "uri": track.get("uri"),
                }
            )

        return {"items": items}

    finally:
        client.close()

def get_now_playing(player_id: str) -> dict | None:
    client = MusicAssistantClient()

    try:
        return client.get_now_playing(player_id)
    finally:
        client.close()

def get_queue(player_id: str) -> dict:
    client = MusicAssistantClient()

    try:
        return {
            "player_id": player_id,
            "items": client.get_queue(player_id),
        }
    finally:
        client.close()

def play(player_id: str, uri: str) -> dict:
    client = MusicAssistantClient()

    try:
        client.play(player_id, uri)
        return {"success": True}
    finally:
        client.close()

def pause(player_id: str) -> dict:
    client = MusicAssistantClient()

    try:
        client.pause(player_id)
        return {"success": True}
    finally:
        client.close()

def next(player_id: str) -> dict:
    client = MusicAssistantClient()
    try:
        client.next(player_id)
        return {"success": True}
    finally:
        client.close()

def previous(player_id: str) -> dict:
    client = MusicAssistantClient()
    try:
        client.previous(player_id)
        return {"success": True}
    finally:
        client.close()
