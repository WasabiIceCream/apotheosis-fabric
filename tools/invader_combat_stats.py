#!/usr/bin/env python3
"""Gameoverse change (not upstream data): give Apotheosis invaders Apothic Attributes combat stats by rarity.

Adds crit chance, crit damage, life steal and armor pierce modifiers to each invader's
stats.<rarity>.attribute_modifiers. Values are deliberately small: players here start with 3 hearts.
Idempotent: an attribute already present in a rarity's list is left alone. Run from anywhere:
    python3 tools/invader_combat_stats.py [--check]
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data/apotheosis/apotheosis/apothic_invaders"

# All add_value: crit_chance and life_steal are fractions (0.05 = 5%), crit_damage is a multiplier
# (base 1.5 = +50% crit damage, +0.25 = +25 points), armor_pierce is flat armor ignored.
STATS = {
    "apotheosis:rare": {"apothic_attributes:crit_chance": 0.05},
    "apotheosis:epic": {
        "apothic_attributes:crit_chance": 0.10,
        "apothic_attributes:life_steal": 0.05,
        "apothic_attributes:armor_pierce": 2.0,
    },
    "apotheosis:mythic": {
        "apothic_attributes:crit_chance": 0.15,
        "apothic_attributes:crit_damage": 0.25,
        "apothic_attributes:life_steal": 0.10,
        "apothic_attributes:armor_pierce": 4.0,
    },
}


def dump(d):
    return json.dumps(d, indent=4, ensure_ascii=False)


def main():
    check = "--check" in sys.argv
    changed = []
    for f in sorted(ROOT.rglob("*.json")):
        text = f.read_text()
        d = json.loads(text)
        for rarity, mods in STATS.items():
            stats = d.get("stats", {}).get(rarity)
            if stats is None:
                continue
            lst = stats.setdefault("attribute_modifiers", [])
            have = {m["attribute"] for m in lst}
            for attr, value in mods.items():
                if attr not in have:
                    lst.append({"attribute": attr, "operation": "add_value", "value": value})
        out = dump(d) + ("\n" if text.endswith("\n") else "")
        if out != text:
            changed.append(f.relative_to(ROOT))
            if not check:
                f.write_text(out)
    for c in changed:
        print(("needs update: " if check else "updated: ") + str(c))
    if check and changed:
        sys.exit(1)


if __name__ == "__main__":
    main()
