from app.integrations.music_assistant.client import MusicAssistantClient


def main() -> None:
    client = MusicAssistantClient()

    try:
        result = client._request("players/all")
        print(result)
    finally:
        client.close()


if __name__ == "__main__":
    main()