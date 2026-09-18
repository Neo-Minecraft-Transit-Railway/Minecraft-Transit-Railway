import json

from client_test_api import REPORT, ROOT, TestApi
from client_smoke_test import equip, prepare_block, verify_block, wait_for


def click_widget(client, widget):
    return client.action("click", x=widget["x"] + widget["width"] / 2, y=widget["y"] + widget["height"] / 2)


def fields(state):
    return sorted((widget for widget in state["widgets"] if "value" in widget and widget["visible"]), key=lambda widget: widget["y"])


def edit_field(client, widget, text):
    click_widget(client, widget)
    client.action("key", key=65, modifiers=2)
    client.action("text", text=text)


def open_editor(client):
    client.action("use", x=0, y=120, z=0, face="south")
    return wait_for(client, lambda state: state.get("screen") is not None)


def verify_data(server, block, expected_data, label):
    position = (0, 120, 1) if block in {"mtr:pids_1", "msd:yuuni_pids", "londonunderground:pids_northern"} else (0, 120, 0)
    predicate = block + "{mtrData:{" + expected_data + "}}"
    verify_block(server, predicate, label, position=position)


def save_and_reopen(client, server, name, block, expected_data, save_button=None):
    client.capture(name + "-edited")
    if save_button:
        client.click_text(save_button)
    else:
        client.action("close")
    wait_for(client, lambda state: state.get("screen") is None)
    verify_data(server, block, expected_data, name + "_saved")
    open_editor(client)
    return client.capture(name + "-reopened")


def test_pids(client, server, name, block):
    prepare_block(client, server, block)
    equip(client, server, "mtr:brush")
    state = open_editor(client)
    message = name.upper() + " CLIENT QA"
    message_field = fields(state)[0 if name == "jcm-config" else 1]
    edit_field(client, message_field, message)
    state = save_and_reopen(client, server, name, block, "message0:" + json.dumps(message), "Save Settings" if name == "jcm-config" else None)
    assert message in [widget["value"] for widget in fields(state)], state
    if name == "jcm-config":
        edit_field(client, fields(state)[0], "DISCARD THIS CHANGE")
        client.click_text("Discard Settings")
        wait_for(client, lambda value: value.get("screen") is None)
        verify_block(server, block + "{mtrData:{message0:" + json.dumps(message) + "}}", name + "_discarded")
        state = open_editor(client)
        assert fields(state)[0]["value"] == message, state


def test_color(client, server, name, block):
    prepare_block(client, server, block)
    equip(client, server, "mtr:brush")
    state = open_editor(client)
    checkbox = next(widget for widget in state["widgets"] if "checked" in widget)
    if checkbox["checked"]:
        click_widget(client, checkbox)
    edit_field(client, fields(state)[0], "336699")
    state = save_and_reopen(client, server, name, block, "color:3368601")
    assert fields(state)[0]["value"].upper() == "336699", state
    assert not next(widget["checked"] for widget in state["widgets"] if "checked" in widget), state


def test_floor(client, server, name, block):
    prepare_block(client, server, block)
    equip(client, server, "mtr:brush")
    state = open_editor(client)
    for widget, text in zip(fields(state), ["B2", "Client QA Concourse"]):
        edit_field(client, widget, text)
    checkbox = next(widget for widget in state["widgets"] if "checked" in widget)
    if not checkbox["checked"]:
        click_widget(client, checkbox)
    state = save_and_reopen(client, server, name, block, 'floor_number:"B2",floor_description:"Client QA Concourse",should_ding:1b')
    assert [widget["value"] for widget in fields(state)] == ["B2", "Client QA Concourse"], state
    assert next(widget["checked"] for widget in state["widgets"] if "checked" in widget), state


def test_catenary(client, server, name, block):
    prepare_block(client, server, block)
    equip(client, server, "mtr:brush")
    state = open_editor(client)
    slider = state["widgets"][0]
    client.action("click", x=slider["x"] + slider["width"] - 2, y=slider["y"] + slider["height"] / 2)
    state = save_and_reopen(client, server, name, block, "msd_offset_position_x:0.5d")
    assert state["widgets"][0]["text"] == "8", state


def main():
    properties = (ROOT / "fabric/run/server.properties").read_text()
    if "level-name=mtr-test-world\n" not in properties:
        raise RuntimeError("Refusing to modify a non-test world")
    client = TestApi(True)
    server = TestApi(False)
    if client.request("/state").get("world") != "minecraft:overworld":
        raise RuntimeError("Join the isolated test server first")
    client.action("test_options")
    cases = [
        (test_pids, "mtr-config", "mtr:pids_1"),
        (test_pids, "jcm-config", "jsblock:lcd_pids"),
        (test_pids, "msd-pids-config", "msd:yuuni_pids"),
        (test_pids, "london-config", "londonunderground:pids_northern"),
        (test_color, "tianjin-config", "tjmetro:custom_color_concrete"),
        (test_catenary, "msd-catenary-config", "msd:new_catenary_node"),
        (test_floor, "yte-config", "yte:lift_track_empty_floor"),
    ]
    results = []
    for test, name, block in cases:
        result = {"name": name, "block": block}
        try:
            test(client, server, name, block)
            client.action("close")
            result["status"] = "passed"
        except Exception as exception:
            result["status"] = "failed"
            result["error"] = str(exception)
        results.append(result)
        REPORT.mkdir(parents=True, exist_ok=True)
        (REPORT / "config-roundtrip.json").write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n")
        print(json.dumps(result, ensure_ascii=False), flush=True)
    if any(result["status"] != "passed" for result in results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
