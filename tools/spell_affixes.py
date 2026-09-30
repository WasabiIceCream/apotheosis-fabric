#!/usr/bin/env python3
"""Gameoverse addition (not upstream data): affixes for the apotheosis:spell_weapon category (Spell Engine staves/wands).

Writes src/main/resources/data/apotheosis/apotheosis/affixes/spell/** and adds spell_weapon to a few existing
school-neutral affixes (lucky, experienced). Lang entries are merged into assets/apotheosis/lang/en_us.json.
Re-run after changing a value here; it rewrites every file it owns:
    python3 tools/spell_affixes.py

Values: spell_power school attributes have base 1 and staves/wands add 3-8 flat, so add_multiplied_base scales
(1 + weapon power); RPG Series robes give +20-35% per piece, the Jewelry gems +2.4-21.6%. Haste/crit chance/crit damage
have base 100 (+5 crit chance, +50 crit damage), so 0.05 add_multiplied_base is about 5 percentage points; RPG Series armor
gives 2-5 per piece. spell_power:generic scales every school's flat power (0.10 = +10%).
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources"
AFFIXES = ROOT / "data/apotheosis/apotheosis/affixes"
LANG = ROOT / "assets/apotheosis/lang/en_us.json"
CAT = "apotheosis:spell_weapon"
RARITIES = ["common", "uncommon", "rare", "epic", "mythic"]


def rng(lo, hi, step=0.01):
    return {"min": lo, "max": hi, "step": step}


def values(table):
    return {f"apotheosis:{r}": rng(*v) for r, v in table.items()}


SCHOOL_POWER = dict(zip(RARITIES, [(0.08, 0.12), (0.10, 0.15), (0.12, 0.20), (0.16, 0.25), (0.20, 0.30)]))
SECONDARY = dict(zip(RARITIES, [(0.02, 0.04), (0.03, 0.05), (0.04, 0.06), (0.05, 0.08), (0.06, 0.10)]))

# school: (prefix, suffix)
SCHOOLS = {
    "arcane": ("Runic", "of the Arcane Weave"),
    "fire": ("Blazing", "of Cinders"),
    "frost": ("Frigid", "of Winter"),
    "healing": ("Hallowed", "of Mending"),
    "lightning": ("Stormcalled", "of Thunder"),
    "soul": ("Soulbound", "of Souls"),
}

files = {}
lang = {}


def attr_affix(path, attribute, affix_type, table, name, suffix, require=False, exclusive=()):
    d = {
        "type": "apotheosis:optional_attribute",
        "attribute": attribute,
        "categories": [CAT],
        "definition": {
            "affix_type": affix_type,
            "exclusive_set": sorted(exclusive),
            "weights": {"quality": 0.1, "weight": 25},
        },
        "operation": "add_multiplied_base",
        "values": values(table),
    }
    if require:
        d["require_item_attribute"] = True
    files[path] = d
    lang[f"affix.apotheosis:{path}"] = name
    lang[f"affix.apotheosis:{path}.suffix"] = suffix


school_ids = {s: f"spell/attribute/{s}" for s in SCHOOLS}
for school, (name, suffix) in SCHOOLS.items():
    # One school affix per item: a multi-school staff (the Wizard Staff) still rolls only one.
    others = {f"apotheosis:{p}" for s, p in school_ids.items() if s != school}
    attr_affix(school_ids[school], f"spell_power:{school}", "stat", SCHOOL_POWER, name, suffix, require=True, exclusive=others)

attr_affix("spell/attribute/quickened", "spell_power:haste", "stat", SECONDARY, "Quickened", "of Haste")
attr_affix("spell/attribute/focused", "spell_power:critical_chance", "stat", SECONDARY, "Focused", "of Precision")
attr_affix("spell/attribute/archmages", "spell_power:generic", "ability",
           {"epic": (0.08, 0.12), "mythic": (0.12, 0.18)}, "Archmage's", "of the Archmage")
attr_affix("spell/attribute/devastating", "spell_power:critical_damage", "ability",
           {"epic": (0.10, 0.15), "mythic": (0.15, 0.20)}, "Devastating", "of Ruin")


def effect(dur, amp, cooldown=None):
    d = {"duration": dur, "amplifier": amp}
    if cooldown is not None:
        d["cooldown"] = cooldown
    return d


def effect_affix(path, mob_effect, target, table, name, suffix):
    files[path] = {
        "type": "apotheosis:mob_effect",
        "definition": {"affix_type": "basic_effect", "exclusive_set": [], "weights": {"quality": 0.1, "weight": 25}},
        "mob_effect": mob_effect,
        "target": target,
        "types": [CAT],
        "values": {f"apotheosis:{r}": v for r, v in table.items()},
    }
    lang[f"affix.apotheosis:{path}"] = name
    lang[f"affix.apotheosis:{path}.suffix"] = suffix


# Same numbers as the bow affixes they mirror (ensnaring, fleeting, blighted); weakness uses ensnaring's.
SLOW = {
    "uncommon": effect(rng(40.0, 80.0, 20.0), 0.0, 160),
    "rare": effect(rng(40.0, 100.0, 20.0), 0.0, 160),
    "epic": effect(rng(40.0, 120.0, 20.0), rng(0.0, 1.0, 0.25), 160),
    "mythic": effect(rng(80.0, 160.0, 20.0), rng(0.0, 2.0, 0.25), 160),
}
effect_affix("spell/mob_effect/binding", "minecraft:slowness", "projectile_target", SLOW, "Binding", "of Chains")
effect_affix("spell/mob_effect/hexing", "minecraft:weakness", "projectile_target", SLOW, "Hexing", "of Frailty")
effect_affix("spell/mob_effect/necrotic", "minecraft:wither", "projectile_target", {
    "epic": effect(rng(160.0, 200.0, 20.0), rng(0.0, 1.0, 0.25), 300),
    "mythic": effect(rng(160.0, 200.0, 20.0), rng(0.0, 3.0, 0.25), 300),
}, "Necrotic", "of Decay")
effect_affix("spell/mob_effect/flowing", "minecraft:speed", "projectile_self", {
    "uncommon": effect(rng(100.0, 200.0, 20.0), 0.0),
    "rare": effect(rng(100.0, 200.0, 20.0), 0.0),
    "epic": effect(rng(100.0, 200.0, 20.0), rng(0.0, 1.0, 0.25)),
    "mythic": effect(rng(100.0, 300.0, 20.0), rng(0.0, 2.0, 0.25)),
}, "Flowing", "of the Current")

lang["loot_category.apotheosis.spell_weapon"] = "Spell Weapon"
lang["loot_category.apotheosis.spell_weapon.plural"] = "Spell Weapons"

# Existing school-neutral affixes that suit a caster.
SHARED = ["generic/attribute/lucky.json", "breaker/attribute/experienced.json"]


def dump(d):
    return json.dumps(d, indent=4, ensure_ascii=False)


def main():
    out_dir = AFFIXES / "spell"
    for old in out_dir.rglob("*.json"):
        if str(old.relative_to(AFFIXES))[:-5] not in files:
            old.unlink()
    for path, d in files.items():
        f = AFFIXES / f"{path}.json"
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(dump(d))
    for rel in SHARED:
        f = AFFIXES / rel
        text = f.read_text()
        d = json.loads(text)
        if CAT not in d["categories"]:
            d["categories"].append(CAT)
            f.write_text(dump(d) + ("\n" if text.endswith("\n") else ""))
    # The lang file is hand-formatted (blank-line groups, repeated "comment_id" keys), so edit it as text: replace a
    # key's line if present, else add the missing keys as one group at the end.
    lines = LANG.read_text().split("\n")
    missing = dict(lang)
    for i, line in enumerate(lines):
        stripped = line.strip()
        for k in list(missing):
            if stripped.startswith(json.dumps(k) + ":"):
                comma = "," if stripped.endswith(",") else ""
                lines[i] = f"    {json.dumps(k)}: {json.dumps(missing.pop(k), ensure_ascii=False)}{comma}"
    if missing:
        end = max(i for i, l in enumerate(lines) if l.strip() == "}")
        last = max(i for i in range(end) if lines[i].strip())
        if not lines[last].rstrip().endswith(","):
            lines[last] = lines[last].rstrip() + ","
        add = ["", '    "comment_id": "Gameoverse spell weapon affixes (tools/spell_affixes.py)",']
        add += [f"    {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}," for k, v in missing.items()]
        add[-1] = add[-1][:-1]
        lines[last + 1:last + 1] = add
    LANG.write_text("\n".join(lines))
    json.loads(LANG.read_text())
    print(f"{len(files)} spell affixes, {len(lang)} lang entries")


if __name__ == "__main__":
    main()
