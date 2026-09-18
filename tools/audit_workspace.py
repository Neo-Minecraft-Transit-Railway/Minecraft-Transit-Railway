import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path


CORE = Path(__file__).resolve().parents[1]
ADDONS = ("Filters-API", "London-Underground", "Russian-Metro", "Joban-Client-Mod", "MSD", "Tianjin-Metro", "Yunzhu-Transit")
IDENTIFIER = re.compile(r"^#?[a-z0-9_.-]+:[a-z0-9_./-]+$")
LEGACY = {"recipes", "loot_tables", "advancements", "structures", "functions"}


def audit_jars(report):
    artifacts = []
    for project in [CORE, *(CORE.parent / "addons" / name for name in ADDONS)]:
        if not project.exists():
            continue
        module = project if project.name == "Filters-API" else project / "fabric"
        directory = project / "build/release" if project == CORE else module / "build/libs"
        candidates = sorted(directory.glob("*1.21.11.jar"))
        if len(candidates) != 1:
            report["errors"].append(f"{project.name}: expected one current runtime JAR in {directory}, found {len(candidates)}")
            continue
        jar = candidates[0]
        try:
            with zipfile.ZipFile(jar) as archive:
                names = set(archive.namelist())
                metadata = json.loads(archive.read("fabric.mod.json"))

                def require(condition, reason):
                    if not condition:
                        report["errors"].append(f"{project.name}/{jar.name}: {reason}")

                require(metadata.get("depends", {}).get("minecraft") == "1.21.11", "wrong Minecraft dependency")
                if project != CORE and project.name != "Filters-API":
                    require(metadata.get("depends", {}).get("mtr") == "4.0.5", "wrong MTR dependency")
                if project != CORE:
                    require(not any(name.startswith("org/mtr/") for name in names), "vendored MTR classes/resources")
                for name in names:
                    parts = Path(name).parts
                    if len(parts) > 2 and parts[0] == "data":
                        require(parts[2] not in LEGACY, f"legacy data directory: {name}")
                        if len(parts) > 3 and parts[2] == "tags":
                            require(parts[3] not in {"blocks", "items", "entity_types", "fluids"}, f"legacy tag directory: {name}")
                for entries in metadata.get("entrypoints", {}).values():
                    for entry in entries:
                        classname = entry if isinstance(entry, str) else entry["value"]
                        require(classname.replace(".", "/") + ".class" in names, f"missing entrypoint class: {classname}")
                for config in metadata.get("mixins", []):
                    config = config if isinstance(config, str) else config["config"]
                    mixin = json.loads(archive.read(config))
                    for group in ("mixins", "client", "server"):
                        for classname in mixin.get(group, []):
                            entry = (mixin["package"] + "." + classname).replace(".", "/") + ".class"
                            require(entry in names, f"missing mixin class: {entry}")
                    if mixin.get("refmap"):
                        require(mixin["refmap"] in names, f"missing refmap: {mixin['refmap']}")
                classes = [name for name in names if name.endswith(".class") and not name.startswith("META-INF/")]
                if project == CORE:
                    classes = [name for name in classes if name.startswith(("org/mtr/mod/", "org/mtr/init/", "org/mtr/mixin/"))]
                    website = archive.read("org/mtr/mod/generated/WebserverResources.class")
                    require(b"index.html" in website and b".js" in website, "empty embedded creator website")
                maximum = max((int.from_bytes(archive.read(name)[6:8], "big") for name in classes), default=0)
                require(0 < maximum <= 65, f"own classes must target Java 21 or earlier, found major version {maximum}")
                artifacts.append({"module": project.name, "file": str(jar), "version": metadata["version"], "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "own_classes": len(classes), "max_class_major": maximum})
        except (OSError, ValueError, KeyError, zipfile.BadZipFile) as exception:
            report["errors"].append(f"{project.name}/{jar.name}: {exception}")
    report["artifacts"] = artifacts


def audit(built=False, minecraft_jar=None):
    errors = []
    summaries = []
    modules = []
    for project in [CORE, *(CORE.parent / "addons" / name for name in ADDONS)]:
        if not project.exists():
            continue
        module = project if project.name == "Filters-API" else project / "fabric"
        resources = module / ("build/resources/main" if built else "src/main/resources")
        if not resources.is_dir():
            errors.append(f"{project.name}: missing resource directory {resources}")
            continue
        modules.append((project.name, module, resources))
    available = set()
    for _, module, resources in modules:
        roots = [resources]
        if not built:
            roots.append(module / "build/generated/resources")
        for root in roots:
            available.update(path.relative_to(root).as_posix() for path in root.rglob("*") if path.is_file())
    if minecraft_jar and minecraft_jar.is_file():
        with zipfile.ZipFile(minecraft_jar) as archive:
            available.update(archive.namelist())

    def exists(identifier, kind):
        namespace, name = (identifier if ":" in identifier else "minecraft:" + identifier).split(":", 1)
        if namespace == "minecraft" and not minecraft_jar:
            return True
        if kind == "tag":
            return f"data/{namespace}/tags/item/{name}.json" in available
        return f"assets/{namespace}/models/{name}.json" in available or name.startswith("builtin/")

    for name, module, resources in modules:
        start = len(errors)
        recipes = 0
        documents = 0

        def problem(path, reason):
            errors.append(f"{name}/{path.relative_to(resources)}: {reason}")

        for path in sorted(resources.rglob("*.json")):
            relative = path.relative_to(resources)
            parts = relative.parts
            text = path.read_text(encoding="utf-8-sig")
            if not built and any(token in text for token in ("${legacy_unicode}", "${mc_door_blockstate}")):
                continue
            try:
                data = json.loads(text)
            except ValueError as exception:
                problem(path, f"invalid JSON: {exception}")
                continue
            documents += 1
            if parts[0] == "data":
                if len(parts) > 2 and parts[2] in LEGACY:
                    problem(path, "legacy data-pack directory")
                if len(parts) > 3 and parts[2] == "tags" and parts[3] in {"blocks", "items", "entity_types", "fluids"}:
                    problem(path, "legacy tag directory")
                if len(parts) > 2 and parts[2] == "recipe":
                    recipes += 1
                    result = data.get("result", {})
                    if not isinstance(result, dict) or not IDENTIFIER.fullmatch(result.get("id", "")):
                        problem(path, "recipe result requires a namespaced id")
                    ingredients = list(data.get("key", {}).values()) + data.get("ingredients", [])
                    if data.get("type") == "minecraft:crafting_shapeless" and not 1 <= len(ingredients) <= 9:
                        problem(path, "shapeless recipes require 1 to 9 slots")
                    if "pattern" in data:
                        pattern = data["pattern"]
                        if not 1 <= len(pattern) <= 3 or any(not 1 <= len(row) <= 3 for row in pattern) or len({len(row) for row in pattern}) != 1:
                            problem(path, "invalid shaped crafting grid")
                        symbols = set("".join(pattern)) - {" "}
                        if symbols != set(data.get("key", {})):
                            problem(path, "recipe keys do not match the pattern")
                    for ingredient in ingredients:
                        values = ingredient if isinstance(ingredient, list) else [ingredient]
                        if not values or any(not isinstance(value, str) or not IDENTIFIER.fullmatch(value) for value in values):
                            problem(path, f"invalid 1.21.11 ingredient: {ingredient}")
                        else:
                            for value in values:
                                if value.startswith("#") and not exists(value[1:], "tag"):
                                    problem(path, f"missing item tag {value}")
            if len(parts) > 2 and parts[0] == "assets":
                references = []
                if parts[2] in {"items", "blockstates"}:
                    def collect(value):
                        if isinstance(value, dict):
                            if isinstance(value.get("model"), str):
                                references.append(value["model"])
                            for child in value.values():
                                collect(child)
                        elif isinstance(value, list):
                            for child in value:
                                collect(child)
                    collect(data)
                if parts[2] == "models" and isinstance(data.get("parent"), str):
                    references.append(data["parent"])
                for reference in references:
                    if not exists(reference, "model"):
                        problem(path, f"missing model {reference}")
            if relative.as_posix() == "fabric.mod.json":
                if not (resources / data["icon"]).is_file():
                    problem(path, "missing mod icon")
                for entries in data.get("entrypoints", {}).values():
                    for entry in entries:
                        classname = entry if isinstance(entry, str) else entry["value"]
                        source = module / "src/main/java" / (classname.replace(".", "/") + ".java")
                        if not source.is_file():
                            problem(path, f"missing entrypoint {classname}")
                for config in data.get("mixins", []):
                    mixin_path = resources / (config if isinstance(config, str) else config["config"])
                    if not mixin_path.is_file():
                        problem(path, f"missing mixin configuration {config}")
                        continue
                    mixin_data = json.loads(mixin_path.read_text(encoding="utf-8-sig"))
                    for group in ("mixins", "client", "server"):
                        for mixin in mixin_data.get(group, []):
                            classname = mixin_data["package"] + "." + mixin
                            if not (module / "src/main/java" / (classname.replace(".", "/") + ".java")).is_file():
                                problem(path, f"missing mixin class {classname}")
        summaries.append({"module": name, "json_files": documents, "recipes": recipes, "errors": len(errors) - start})
    return {"built_resources": built, "modules": summaries, "errors": errors}


def main():
    parser = argparse.ArgumentParser(description="Read-only validation of the MTR 1.21.11 workspace.")
    parser.add_argument("--built", action="store_true", help="Validate processed resources after Gradle builds.")
    parser.add_argument("--jars", action="store_true", help="Also validate final runtime JARs; implies --built.")
    parser.add_argument("--minecraft-jar", type=Path)
    parser.add_argument("--report", type=Path)
    arguments = parser.parse_args()
    minecraft_jar = arguments.minecraft_jar
    if minecraft_jar is None:
        cached = Path.home() / ".gradle/caches/fabric-loom/1.21.11/minecraft-client.jar"
        minecraft_jar = cached if cached.is_file() else None
    if minecraft_jar and not minecraft_jar.is_file():
        parser.error(f"Minecraft jar not found: {minecraft_jar}")
    report = audit(arguments.built or arguments.jars, minecraft_jar)
    if arguments.jars:
        audit_jars(report)
    for module in report["modules"]:
        print(f"{module['module']}: {module['json_files']} JSON, {module['recipes']} recipes, {module['errors']} errors")
    for error in report["errors"][:30]:
        print(error, file=sys.stderr)
    for artifact in report.get("artifacts", []):
        print(f"{artifact['module']}: JAR {artifact['version']}, {artifact['own_classes']} own classes, major version {artifact['max_class_major']}")
    if arguments.report:
        arguments.report.parent.mkdir(parents=True, exist_ok=True)
        arguments.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return bool(report["errors"])


if __name__ == "__main__":
    sys.exit(main())
