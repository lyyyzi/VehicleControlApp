import time
from uuid import uuid4

from fastapi import BackgroundTasks, FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()


# 模拟车辆当前状态
vehicle_state = {
    "vehicle_id": "vehicle-001",
    "locked": True,
    "online": True,
}


# 保存每一条命令及其执行状态
commands = {}


# 控制车辆模拟器的故障模式
simulator_config = {
    "failure_mode": "normal",
    "execution_delay_seconds": 2,
}


class CommandRequest(BaseModel):
    command_type: str


class ConnectivityRequest(BaseModel):
    online: bool


class FailureModeRequest(BaseModel):
    failure_mode: str


def execute_command(
    command_id: str,
    command_type: str,
) -> None:
    """模拟车辆在后台执行命令。"""

    commands[command_id]["status"] = "PROCESSING"

    # timeout 模式下，命令永远停留在 PROCESSING
    if simulator_config["failure_mode"] == "timeout":
        return

    # 模拟云端向车辆发送命令，以及车辆执行命令的时间
    time.sleep(
        simulator_config["execution_delay_seconds"]
    )

    # 车辆离线时，命令明确失败
    if not vehicle_state["online"]:
        commands[command_id]["status"] = "FAILED"
        commands[command_id]["message"] = "Vehicle is offline"
        return

    if command_type == "UNLOCK":
        vehicle_state["locked"] = False

    elif command_type == "LOCK":
        vehicle_state["locked"] = True

    commands[command_id]["status"] = "COMPLETED"
    commands[command_id]["vehicle"] = vehicle_state.copy()


@app.get("/")
def root():
    return {
        "message": "Vehicle backend is running",
        "failure_mode": simulator_config["failure_mode"],
    }


@app.get("/vehicles/vehicle-001")
def get_vehicle_status():
    return vehicle_state


@app.patch("/vehicles/vehicle-001/connectivity")
def update_vehicle_connectivity(
    request: ConnectivityRequest,
):
    vehicle_state["online"] = request.online

    return {
        "message": "Vehicle connectivity updated",
        "vehicle": vehicle_state,
    }


@app.patch("/simulator/failure-mode")
def update_failure_mode(
    request: FailureModeRequest,
):
    failure_mode = request.failure_mode.lower()

    allowed_modes = {
        "normal",
        "timeout",
    }

    if failure_mode not in allowed_modes:
        raise HTTPException(
            status_code=400,
            detail=(
                "Unsupported failure mode. "
                "Use 'normal' or 'timeout'."
            ),
        )

    simulator_config["failure_mode"] = failure_mode

    return {
        "message": "Failure mode updated",
        "failure_mode": failure_mode,
    }


@app.post(
    "/vehicles/vehicle-001/commands",
    status_code=202,
)
def send_vehicle_command(
    request: CommandRequest,
    background_tasks: BackgroundTasks,
):
    command_type = request.command_type.upper()

    if command_type not in {"LOCK", "UNLOCK"}:
        raise HTTPException(
            status_code=400,
            detail="Unsupported command",
        )

    command_id = f"cmd-{uuid4().hex[:8]}"

    commands[command_id] = {
        "command_id": command_id,
        "command_type": command_type,
        "status": "ACCEPTED",
    }

    background_tasks.add_task(
        execute_command,
        command_id,
        command_type,
    )

    return commands[command_id]


@app.get("/commands/{command_id}")
def get_command_status(command_id: str):
    if command_id not in commands:
        raise HTTPException(
            status_code=404,
            detail="Command not found",
        )

    return commands[command_id]


@app.post("/simulator/reset")
def reset_simulator():
    vehicle_state.update(
        {
            "vehicle_id": "vehicle-001",
            "locked": True,
            "online": True,
        }
    )

    commands.clear()

    simulator_config["failure_mode"] = "normal"
    simulator_config["execution_delay_seconds"] = 2

    return {
        "message": "Simulator reset",
        "vehicle": vehicle_state,
        "failure_mode": simulator_config["failure_mode"],
    }