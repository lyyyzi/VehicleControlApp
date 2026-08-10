import pytest
from fastapi.testclient import TestClient

import main


client = TestClient(main.app)


@pytest.fixture(autouse=True)
def reset_simulator_state():
    """
    每个测试开始前恢复默认状态，
    防止前一个测试影响后一个测试。
    """

    main.vehicle_state.update(
        {
            "vehicle_id": "vehicle-001",
            "locked": True,
            "online": True,
        }
    )

    main.commands.clear()

    main.simulator_config["failure_mode"] = "normal"
    main.simulator_config["execution_delay_seconds"] = 0

    yield


def test_backend_is_running():
    response = client.get("/")

    assert response.status_code == 200
    assert response.json()["message"] == (
        "Vehicle backend is running"
    )


def test_get_vehicle_status():
    response = client.get(
        "/vehicles/vehicle-001"
    )

    assert response.status_code == 200

    vehicle = response.json()

    assert vehicle["vehicle_id"] == "vehicle-001"
    assert vehicle["locked"] is True
    assert vehicle["online"] is True


def test_unlock_vehicle_successfully():
    create_response = client.post(
        "/vehicles/vehicle-001/commands",
        json={
            "command_type": "UNLOCK",
        },
    )

    assert create_response.status_code == 202

    accepted_command = create_response.json()

    assert accepted_command["status"] == "ACCEPTED"
    assert accepted_command["command_type"] == "UNLOCK"

    command_id = accepted_command["command_id"]

    status_response = client.get(
        f"/commands/{command_id}"
    )

    assert status_response.status_code == 200

    completed_command = status_response.json()

    assert completed_command["status"] == "COMPLETED"
    assert completed_command["vehicle"]["locked"] is False


def test_command_fails_when_vehicle_is_offline():
    connectivity_response = client.patch(
        "/vehicles/vehicle-001/connectivity",
        json={
            "online": False,
        },
    )

    assert connectivity_response.status_code == 200

    create_response = client.post(
        "/vehicles/vehicle-001/commands",
        json={
            "command_type": "UNLOCK",
        },
    )

    command_id = create_response.json()["command_id"]

    status_response = client.get(
        f"/commands/{command_id}"
    )

    failed_command = status_response.json()

    assert failed_command["status"] == "FAILED"
    assert failed_command["message"] == (
        "Vehicle is offline"
    )

    vehicle_response = client.get(
        "/vehicles/vehicle-001"
    )

    assert vehicle_response.json()["locked"] is True


def test_command_remains_processing_in_timeout_mode():
    failure_mode_response = client.patch(
        "/simulator/failure-mode",
        json={
            "failure_mode": "timeout",
        },
    )

    assert failure_mode_response.status_code == 200

    create_response = client.post(
        "/vehicles/vehicle-001/commands",
        json={
            "command_type": "UNLOCK",
        },
    )

    command_id = create_response.json()["command_id"]

    status_response = client.get(
        f"/commands/{command_id}"
    )

    command = status_response.json()

    assert command["status"] == "PROCESSING"

    vehicle_response = client.get(
        "/vehicles/vehicle-001"
    )

    assert vehicle_response.json()["locked"] is True


def test_invalid_command_is_rejected():
    response = client.post(
        "/vehicles/vehicle-001/commands",
        json={
            "command_type": "OPEN_TRUNK",
        },
    )

    assert response.status_code == 400
    assert response.json()["detail"] == (
        "Unsupported command"
    )


def test_unknown_command_id_returns_404():
    response = client.get(
        "/commands/cmd-does-not-exist"
    )

    assert response.status_code == 404
    assert response.json()["detail"] == (
        "Command not found"
    )