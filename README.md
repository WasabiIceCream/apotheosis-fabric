# Apotheosis (Adventure module), Fabric port

A Fabric port of the "Adventure" part of [Apotheosis](https://github.com/Shadows-of-Fire/Apotheosis)
by Shadows-of-Fire, built for the Gameoverse Minecraft server (Fabric 26.1.2).

Apotheosis normally ships as six interconnected mods covering rarity/loot
tiers, dynamic affixes, gem sockets, custom enchanting, spawners, and
boss-summoning gateways, all NeoForge-only. This port covers just the
**Adventure module**: rarities, affixes, gem sockets and cutting, the
loot injection that lets mobs/chests drop affixed gear, (since 0.4.0)
the mob features: invaders, elites and augmented monsters, and (since 0.4.1)
the rogue spawner and boss dungeon worldgen. Enchanting, Apothic Spawners,
gateways and the tower structures are out of scope.

## What it adds

- Rarity tiers (Common through Mythic) applied to gear
- Dynamic affixes that roll onto items, from simple stat boosts to
  on-hit effects
- Gems that socket into gear for extra bonuses, plus a cutting table to
  make them
- Reforging, salvaging, and augmenting tables to reroll, break down, or
  upgrade affixed gear
- Loot tables updated so this content actually drops in the world
- Invaders: named bosses in affixed gear that sometimes replace a natural
  monster spawn (from the Frontier World Tier up), announced to nearby players
- Elites: rare minibosses (Undead Knight, Honeyed Archer, Withering Archer,
  Craig) that ordinary spawns can turn into
- Monsters that sometimes spawn wearing a random affixed item, more often at
  higher World Tiers
- Rogue spawners underground: a preset monster spawner (brutal zombies, husks
  and pillagers, or fast spider/silverfish/baby zombie swarms) over a loot
  chest, sometimes a valuable one with affixed gear and gems
- Boss dungeons: small stone-brick rooms with dungeon chests and a Caged
  Invader that releases an invader when a player comes within 8 blocks
- Server additions (Gameoverse): a Spell Weapon category for Spell Engine staves and wands with school-matched spell
  power, spell haste, crit and projectile-effect affixes, and invaders that crit, life steal and pierce armor at higher
  rarities (see `DEVLOG.md` 0.4.3)
- Server addition (Gameoverse, 0.4.4): Potion Charms work in the Trinkets "Charm" slot when
  [Trinkets Updated](https://modrinth.com/mod/trinkets-updated) is installed (optional; upstream's Curios charm slot)
- `/apoth spawn_boss` and `/apoth spawn_elite` for operators, and the
  `apotheosis:is_invader` / `apotheosis:is_elite` entity predicates for
  advancements (see `DEVLOG.md` for the JSON)

Depends on [Placebo](https://github.com/WasabiIceCream/placebo-fabric) (0.1.2+), a
Fabric port of Apotheosis's own base library, and, since 0.3.0,
[Apothic Attributes](https://github.com/WasabiIceCream/Apothic-Attributes) (Fabric port), the
attribute library upstream Apotheosis requires. Both are composite builds: clone them next to this
repository (`../placebo-fabric`, `../apothic-attributes-fabric`) to build.

## License and assets

Code is MIT, same as upstream, see `LICENSE`.

Apotheosis's own assets (textures/models) were also MIT until March 2025,
when upstream split them out under a separate all-rights-reserved license.
This port's assets are sourced from upstream's own last commit while
everything was still MIT, not from the newer restricted release. A handful
of assets added after that split (mainly the Gem Case) came from a
separately-licensed community texture pack instead, used only on our own
server and deliberately excluded from this public repo and its releases.
Full per-file provenance is in `ATTRIBUTION.md`.

## Known gaps

Not everything ported cleanly:

- 2 gems and 2 affixes are blocked by a registry limitation in this port's
  datapack system (they need real dynamic-registry context this port
  doesn't provide)
- One gem uses a NeoForge-only wildcard with no Fabric equivalent
- The Gem Case screen has no background texture, and the reforging/
  augmenting tables' floating 3D props aren't rendered (no free asset
  source for either)
- Hoppers and pipes can't use the salvaging/reforging/augmenting tables or
  gem cases (upstream's automation handlers aren't ported)
- Cosmetic client features not ported: the gem icon row in socket tooltips (text lines instead),
  equipment comparison, radial mining outlines
- `DEVLOG.md` (0.4.4) has the full list of upstream integration points and
  how each is connected

Loot beams (since 0.4.6) use Loot Beams Refork's beam, textures and drop sound (CC0), see `DEVLOG.md`.

Everything else works: rarities, affixes, gem sockets/cutting/case
storage, reforging/salvaging/augmenting, and loot injection are all live
and confirmed booting cleanly on a dedicated server.

## Development history

For the full porting log (every bug found, every NeoForge-to-Fabric API
decision, in order), see `DEVLOG.md`.
