import json
import re
import time
import urllib.request
import urllib.error
from pathlib import Path
from datetime import date


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "build/client-testing" / date.today().isoformat()


class TestApi:
    def __init__(self, client):
        log = ROOT / ("fabric/client-run/logs/latest.log" if client else "fabric/run/logs/latest.log")
        label = "Client" if client else "Local"
        matches = re.findall(label + r" test control listening at (http://127\.0\.0\.1:\d+) with token (\w+)", log.read_text())
        if not matches:
            raise RuntimeError(f"No {label} test endpoint in {log}")
        self.endpoint, self.token = matches[-1]

    def request(self, route, payload=None):
        body = None if payload is None else (json.dumps(payload) if isinstance(payload, dict) else payload).encode()
        request = urllib.request.Request(self.endpoint + route, data=body, headers={"X-MTR-Test-Token": self.token, "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=40) as response:
                result = json.load(response)
        except urllib.error.HTTPError as exception:
            raise RuntimeError(exception.read().decode()) from exception
        if not result.get("ok", False):
            raise RuntimeError(result)
        return result

    def action(self, action, **values):
        return self.request("/action", {"action": action, **values})

    def command(self, command):
        return self.request("/command", command)

    def capture(self, name):
        time.sleep(1)
        state = self.request("/state")
        REPORT.mkdir(parents=True, exist_ok=True)
        (REPORT / (name + ".json")).write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n")
        screenshot = ROOT / "fabric/client-run/screenshots" / (name + ".png")
        previous_time = screenshot.stat().st_mtime_ns if screenshot.exists() else -1
        self.action("screenshot", name=name + ".png")
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if screenshot.exists() and screenshot.stat().st_mtime_ns != previous_time and screenshot.stat().st_size > 0:
                return state
            time.sleep(0.1)
        raise TimeoutError("Screenshot was not saved: " + str(screenshot))

    def click_text(self, text):
        widgets = self.request("/state")["widgets"]
        widget = next(value for value in widgets if value["text"] == text and value["visible"] and value["active"])
        return self.action("click", x=widget["x"] + widget["width"] / 2, y=widget["y"] + widget["height"] / 2)


if __name__ == "__main__":
    print(json.dumps(TestApi(True).request("/state"), ensure_ascii=False, indent=2))
