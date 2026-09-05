from app.database.models.user import User


def get_home_data(current_user: User) -> dict:
    return {
        "user": {
            "display_name": current_user.username,
        },
        "home": {
            "status": "operational",
        },
        "quick_actions": [
            {
                "id": "music",
                "enabled": True,
            },
            {
                "id": "multimedia",
                "enabled": True,
            },
        ],
        "information": {
            "connectivity": "online",
            "server": "online",
        },
    }
