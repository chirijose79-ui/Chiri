from typing import Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.api.dependencies import get_current_user
from app.application.home_service import get_home_data
from app.database.models.user import User


class HomeUser(BaseModel):
    display_name: str


class HomeStatus(BaseModel):
    status: Literal["operational", "attention", "offline"]


class QuickAction(BaseModel):
    id: Literal["music", "multimedia"]
    enabled: bool


class HomeInformation(BaseModel):
    connectivity: Literal["online", "offline"]
    server: Literal["online", "offline"]


class HomeResponse(BaseModel):
    user: HomeUser
    home: HomeStatus
    quick_actions: list[QuickAction]
    information: HomeInformation


router = APIRouter(
    prefix="/home",
    tags=["home"],
)


@router.get("", response_model=HomeResponse)
def get_home(
    current_user: User = Depends(get_current_user),
) -> HomeResponse:
    data = get_home_data(current_user)
    return HomeResponse.model_validate(data)
