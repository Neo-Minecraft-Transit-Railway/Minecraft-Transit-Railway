import json
import time

from client_test_api import REPORT, ROOT, TestApi


def wait_for(client, predicate, timeout=15):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        state = client.request("/state")
        if predicate(state):
            return state
        time.sleep(0.25)
    raise AssertionError(f"Client did not reach expected state: {state}")


def equip(client, server, item):
    server.command("item replace entity TestClient weapon.mainhand with " + item)
    wait_for(client, lambda state: state.get("hand") == item)


def verify_block(server, block, label, position=(0, 120, 0), timeout=5):
    log = ROOT / "fabric/run/logs/latest.log"
    offset = log.stat().st_size
    marker = f"CLIENT_TEST_{label}_{time.time_ns()}"
    deadline = time.monotonic() + timeout
    coordinates = " ".join(map(str, position))
    while time.monotonic() < deadline:
        server.command(f"execute if block {coordinates} {block} run say {marker}")
        with log.open() as stream:
            stream.seek(offset)
            if marker in stream.read():
                REPORT.mkdir(parents=True, exist_ok=True)
                (REPORT / (label + "-assertion.json")).write_text(json.dumps({"position": position, "predicate": block, "marker": marker}, ensure_ascii=False, indent=2) + "\n")
                return
        time.sleep(0.25)
    raise AssertionError("Server did not confirm placed block: " + block)


def prepare_block(client, server, block):
    client.action("close")
    server.command("fill -2 120 -2 2 124 2 minecraft:air")
    server.command("tp TestClient 0.5 120 3.5 180 15")
    equip(client, server, block)
    time.sleep(0.4)
    client.action("use", x=0, y=119, z=0, face="up")
    verify_block(server, block, block.replace(":", "_"))


def main():
    client = TestApi(True)
    server = TestApi(False)
    state = client.request("/state")
    if state.get("world") != "minecraft:overworld":
        raise RuntimeError("Join the isolated test server first")
    properties = (ROOT / "fabric/run/server.properties").read_text()
    if "level-name=mtr-test-world\n" not in properties:
        raise RuntimeError("Refusing to modify a non-test world")
    client.action("test_options")
    server.command("fill -4 119 -4 4 119 4 minecraft:smooth_stone")
    cases = [
        ("mtr-pids", "mtr:pids_1", "mtr:brush", "org.mtr.mod.screen."),
        ("jcm-pids", "jsblock:lcd_pids", "mtr:brush", "com.lx862.jcm.mod."),
        ("msd-pids", "msd:yuuni_pids", "mtr:brush", "org.mtr.mod.screen."),
        ("tianjin-color", "tjmetro:custom_color_concrete", "mtr:brush", "ziyue.tjmetro.mod.screen."),
        ("yte-floor", "yte:lift_track_empty_floor", "mtr:brush", "top.xfunny.mod."),
        ("london-pids", "londonunderground:pids_northern", "mtr:brush", "org.mtr.mod.screen."),
        ("russian-ticket", "russianmetro:moscow_new_ticket_machine", "minecraft:air", "org.mtr.mod.screen."),
        ("msd-catenary", "msd:new_catenary_node", "mtr:brush", "top.mcmtr.mod.screen."),
    ]
    results = []
    for name, block, tool, prefix in cases:
        result = {"name": name, "block": block}
        try:
            prepare_block(client, server, block)
            result["placed"] = True
            client.capture(name + "-placed")
            equip(client, server, tool)
            client.action("use", x=0, y=120, z=0, face="south")
            state = wait_for(client, lambda value: (value.get("screen") or "").startswith(prefix))
            result["screen"] = state["screen"]
            result["widgets"] = len(state["widgets"])
            client.capture(name + "-screen")
            client.action("close")
            wait_for(client, lambda value: value.get("screen") is None)
            server.command("data get block 0 120 0")
            result["status"] = "passed"
        except Exception as exception:
            result["status"] = "failed"
            result["error"] = str(exception)
        results.append(result)
        REPORT.mkdir(parents=True, exist_ok=True)
        (REPORT / "gui-smoke.json").write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n")
        print(json.dumps(result, ensure_ascii=False), flush=True)
    if any(result["status"] != "passed" for result in results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
