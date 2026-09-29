#!/usr/bin/env python3
"""Convert legacy Flutter content (Dart) to JSON for :content module.

Reads legacy_flutter/lib/data/*.dart, writes content/src/main/resources/*.json.
Fails loudly on any unknown condition or unparsable block.

Condition DSL (evaluated by :core:engine):
  {"all": [...]}, {"any": [...]}, {"not": {...}},
  {"key": "<stateKey|special>", "op": "<|<=|>|>=|==|!=", "value": number},
  {"historyContains": "<text>"}
Special numeric keys: builtLandmarksCount, turnNumber, inGameYear,
  nationScore, tradeDealCount, allyCount, minRelation, isAtWar (1/0).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LEGACY = ROOT / "legacy_flutter" / "lib" / "data"
OUT = ROOT / "content" / "src" / "main" / "resources"


def norm_cond(text: str) -> str:
    text = re.sub(r"\(GameState s\)\s*=>", "", text).strip().rstrip(",").strip()
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r",\s*\)", ")", text)
    text = re.sub(r"\(\s+", "(", text)
    return text.strip()


def cmp(key, op, value):
    return {"key": key, "op": op, "value": value}


# Hand-mapped from the full inventory of conditions in legacy data files.
CONDITIONS = {
    "s.agriculture < 70": cmp("agriculture", "<", 70),
    "s.agriculture >= 45": cmp("agriculture", ">=", 45),
    "s.workersSatisfaction < 55": cmp("workersSatisfaction", "<", 55),
    "s.taxRate > 0.30": cmp("taxRate", ">", 0.30),
    "s.industry < 65": cmp("industry", "<", 65),
    "s.energy < 55": cmp("energy", "<", 55),
    "s.publicSatisfaction < 55 || s.economy < 45": {
        "any": [cmp("publicSatisfaction", "<", 55), cmp("economy", "<", 45)]
    },
    "s.health < 60": cmp("health", "<", 60),
    "s.treasuryCash > 200000": cmp("treasuryCash", ">", 200000),
    "s.cyberSecurity < 70": cmp("cyberSecurity", "<", 70),
    "s.cyberSecurity < 65": cmp("cyberSecurity", "<", 65),
    "s.technology >= 40": cmp("technology", ">=", 40),
    "s.industry >= 40": cmp("industry", ">=", 40),
    "s.energy >= 45": cmp("energy", ">=", 45),
    "s.technology >= 45 && s.treasuryCash > 250000": {
        "all": [cmp("technology", ">=", 45), cmp("treasuryCash", ">", 250000)]
    },
    "s.technology >= 50": cmp("technology", ">=", 50),
    "s.technology >= 55": cmp("technology", ">=", 55),
    "s.builtLandmarks.isNotEmpty": cmp("builtLandmarksCount", ">=", 1),
    "s.energy < 65": cmp("energy", "<", 65),
    "s.foodSecurity < 45": cmp("foodSecurity", "<", 45),
    "s.cyberSecurity < 75": cmp("cyberSecurity", "<", 75),
    "s.publicSatisfaction < 35": cmp("publicSatisfaction", "<", 35),
    "s.economy < 45 || s.debt > 400000": {
        "any": [cmp("economy", "<", 45), cmp("debt", ">", 400000)]
    },
    "!s.isAtWar && s.countries.values.any((DiplomacyState d) => d.relation < -50)": {
        "all": [cmp("isAtWar", "==", 0), cmp("minRelation", "<", -50)]
    },
    "s.builtLandmarks.length >= 5": cmp("builtLandmarksCount", ">=", 5),
    "s.builtLandmarks.length >= 10": cmp("builtLandmarksCount", ">=", 10),
    "s.economy >= 80": cmp("economy", ">=", 80),
    "s.publicSatisfaction >= 85": cmp("publicSatisfaction", ">=", 85),
    "s.digitalOpinion >= 85": cmp("digitalOpinion", ">=", 85),
    "s.cyberSecurity >= 80": cmp("cyberSecurity", ">=", 80),
    "s.cleanEnergyRatio >= 0.60": cmp("cleanEnergyRatio", ">=", 0.60),
    "s.foodSecurity >= 85": cmp("foodSecurity", ">=", 85),
    "s.education >= 75 && s.technology >= 75": {
        "all": [cmp("education", ">=", 75), cmp("technology", ">=", 75)]
    },
    "s.culture >= 80": cmp("culture", ">=", 80),
    "s.tourism >= 75": cmp("tourism", ">=", 75),
    "s.debt <= 0 && s.treasuryCash >= 500000 && s.turnNumber > 24": {
        "all": [
            cmp("debt", "<=", 0),
            cmp("treasuryCash", ">=", 500000),
            cmp("turnNumber", ">", 24),
        ]
    },
    "s.countries.values.where((DiplomacyState d) => d.hasTradeDeal).length >= 4": cmp(
        "tradeDealCount", ">=", 4
    ),
    "s.countries.values.where((DiplomacyState d) => d.isAlly).length >= 2": cmp(
        "allyCount", ">=", 2
    ),
    "!s.isAtWar && s.publicSatisfaction >= 50 && s.history.any((HistoryEntry h) => h.title.contains('نهاية الحرب'))": {
        "all": [
            cmp("isAtWar", "==", 0),
            cmp("publicSatisfaction", ">=", 50),
            {"historyContains": "نهاية الحرب"},
        ]
    },
    "s.inGameDate.year >= 2035": cmp("inGameYear", ">=", 2035),
    "s.nationScore >= 85": cmp("nationScore", ">=", 85),
}


def extract_blocks(text: str, marker: str):
    """Split top-level `marker(` ... matching-paren blocks."""
    blocks = []
    idx = 0
    while True:
        start = text.find(marker + "(", idx)
        if start < 0:
            return blocks
        depth = 0
        i = start + len(marker)
        in_str = None
        while i < len(text):
            ch = text[i]
            if in_str:
                if ch == in_str and text[i - 1] != "\\":
                    in_str = None
            elif ch in ("'", '"'):
                in_str = ch
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        if depth != 0:
            raise ValueError(f"unbalanced parens after {marker} at {start}")
        blocks.append(text[start : i + 1])
        idx = i + 1


def field_str(block: str, name: str) -> str:
    m = re.search(name + r":\s*'((?:[^'\\]|\\.)*)'", block)
    if not m:
        raise ValueError(f"missing string field {name} in: {block[:80]}")
    return m.group(1)


def field_opt_str(block: str, name: str):
    m = re.search(name + r":\s*'((?:[^'\\]|\\.)*)'", block)
    return m.group(1) if m else None


def field_num(block: str, name: str, default=0):
    m = re.search(name + r":\s*(-?[\d.]+)", block)
    return float(m.group(1)) if m else default


def field_int(block: str, name: str, default=0):
    return int(field_num(block, name, default))


def field_bool(block: str, name: str, default=False) -> bool:
    m = re.search(name + r":\s*(true|false)", block)
    return (m.group(1) == "true") if m else default


def field_enum(block: str, name: str, default=None):
    m = re.search(name + r":\s*([A-Za-z]+)\.([A-Za-z]+)", block)
    return m.group(2) if m else default


def field_map(block: str, name: str):
    m = re.search(name + r":\s*<String,\s*double>\{(.*?)\}", block, re.S)
    out = {}
    if not m:
        return out
    for key, val in re.findall(r"StateKeys\.(\w+)\s*:\s*(-?[\d.]+)", m.group(1)):
        out[key] = float(val)
    # country-id keyed maps (relationEffects)
    for key, val in re.findall(r"'(\w+)'\s*:\s*(-?[\d.]+)", m.group(1)):
        out[key] = float(val)
    return out


FIELD_NAMES = (
    "choices|id|titleAr|descriptionAr|gemReward|hidden|weight|category|minTurn|"
    "oncePerGame|cooldownTurns|condition|label|resultText|costCash|costGems|"
    "stateEffects|relationEffects|allRelationsDelta"
)


def split_condition(block: str):
    """Depth-aware extraction of the expression after `condition: ... =>`."""
    m = re.search(r"condition:\s*\(GameState s\)\s*=>", block)
    if not m:
        return None
    i = m.end()
    depth = 0
    in_str = None
    while i < len(block):
        ch = block[i]
        if in_str:
            if ch == in_str and block[i - 1] != "\\":
                in_str = None
        elif ch in ("'", '"'):
            in_str = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            if depth == 0:
                break
            depth -= 1
        elif ch == "," and depth == 0:
            rest = block[i + 1 :].lstrip()
            if re.match(rf"(?:\)|(?:{FIELD_NAMES})\s*:)", rest):
                break
        i += 1
    return block[m.end() : i]


def parse_condition(block: str):
    raw = split_condition(block)
    if raw is None:
        return None
    key = norm_cond(raw)
    if key not in CONDITIONS:
        raise ValueError(f"unknown condition: {key!r}")
    return CONDITIONS[key]


def parse_event(block: str):
    choices = []
    for c in extract_blocks(block, "EventChoice"):
        choices.append(
            {
                "label": field_str(c, "label"),
                "resultText": field_str(c, "resultText"),
                "costCash": field_num(c, "costCash"),
                "costGems": field_int(c, "costGems"),
                "stateEffects": field_map(c, "stateEffects"),
                "relationEffects": field_map(c, "relationEffects"),
                "allRelationsDelta": field_num(c, "allRelationsDelta"),
            }
        )
    if not choices:
        raise ValueError("event without choices")
    cond = parse_condition(block)
    event = {
        "id": field_str(block, "id"),
        "title": field_str(block, "title"),
        "description": field_str(block, "description"),
        "category": field_enum(block, "category"),
        "weight": field_int(block, "weight", 10),
        "minTurn": field_int(block, "minTurn", 0),
        "oncePerGame": field_bool(block, "oncePerGame"),
        "imageKey": "",
        "choices": choices,
    }
    if cond is not None:
        event["condition"] = cond
    cd = re.search(r"cooldownTurns:\s*(\d+)", block)
    if cd:
        event["cooldownTurns"] = int(cd.group(1))
    return event


def parse_landmark(block: str):
    return {
        "id": field_str(block, "id"),
        "nameAr": field_str(block, "nameAr"),
        "descriptionAr": field_str(block, "descriptionAr"),
        "kind": field_enum(block, "kind"),
        "costCash": field_num(block, "costCash"),
        "buildTurns": field_int(block, "buildTurns"),
        "onCompleteEffects": field_map(block, "onCompleteEffects"),
        "perTurnEffects": field_map(block, "perTurnEffects"),
        "tourismIncomePerTurn": field_num(block, "tourismIncomePerTurn"),
        "maintenancePerTurn": field_num(block, "maintenancePerTurn"),
        "requirements": field_map(block, "requirements"),
        "era": field_enum(block, "era", "modern"),
    }


def parse_country(block: str):
    return {
        "id": field_str(block, "id"),
        "nameAr": field_str(block, "nameAr"),
        "behavior": field_enum(block, "behavior"),
        "relation": field_num(block, "relation"),
        "economicPower": field_num(block, "economicPower"),
        "militaryPower": field_num(block, "militaryPower"),
    }


def parse_achievement(block: str):
    cond = parse_condition(block)
    if cond is None:
        raise ValueError("achievement without condition")
    return {
        "id": field_str(block, "id"),
        "titleAr": field_str(block, "titleAr"),
        "descriptionAr": field_str(block, "descriptionAr"),
        "gemReward": field_int(block, "gemReward"),
        "hidden": field_bool(block, "hidden"),
        "condition": cond,
    }


def convert_events():
    out = {"classic": [], "modern": [], "crisis": []}
    for name in ("classic", "modern", "crisis"):
        path = LEGACY / f"events_{name}.dart"
        text = path.read_text(encoding="utf-8")
        for b in extract_blocks(text, "GameEvent"):
            out[name].append(parse_event(b))
    return out


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    events = convert_events()
    for name, items in events.items():
        (OUT / f"events_{name}.json").write_text(
            json.dumps(items, ensure_ascii=False, indent=1), encoding="utf-8"
        )
        print(f"events_{name}.json: {len(items)} events")

    lm_text = (LEGACY / "landmarks_data.dart").read_text(encoding="utf-8")
    landmarks = [parse_landmark(b) for b in extract_blocks(lm_text, "Landmark")]
    (OUT / "landmarks.json").write_text(
        json.dumps(landmarks, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"landmarks.json: {len(landmarks)} landmarks")

    c_text = (LEGACY / "countries_data.dart").read_text(encoding="utf-8")
    countries = [
        parse_country(b)
        for b in extract_blocks(c_text, "CountrySeed")
        if "id:" in b and "'unknown'" not in b
    ]
    (OUT / "countries.json").write_text(
        json.dumps(countries, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"countries.json: {len(countries)} countries")

    a_text = (LEGACY / "achievements_data.dart").read_text(encoding="utf-8")
    achievements = [
        parse_achievement(b) for b in extract_blocks(a_text, "Achievement")
    ]
    (OUT / "achievements.json").write_text(
        json.dumps(achievements, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"achievements.json: {len(achievements)} achievements")

    total_events = sum(len(v) for v in events.values())
    print(f"TOTAL events: {total_events}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
