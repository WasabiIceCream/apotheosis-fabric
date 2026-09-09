# Apotheosis (Adventure module), Fabric port

A Fabric port of the "Adventure" part of [Apotheosis](https://github.com/Shadows-of-Fire/Apotheosis)
by Shadows-of-Fire, built for the Gameoverse Minecraft server (Fabric 26.1.2).

Apotheosis normally ships as six interconnected mods covering rarity/loot
tiers, dynamic affixes, gem sockets, custom enchanting, spawners, and
boss-summoning gateways, all NeoForge-only. This port covers just the
**Adventure module**: rarities, affixes, gem sockets and cutting, and the
loot injection that lets mobs/chests drop affixed gear. Enchanting,
spawners, and bosses are out of scope.

## What it adds

- Rarity tiers (Common through Mythic) applied to gear
- Dynamic affixes that roll onto items, from simple stat boosts to
  on-hit effects
- Gems that socket into gear for extra bonuses, plus a cutting table to
  make them
- Reforging, salvaging, and augmenting tables to reroll, break down, or
  upgrade affixed gear
- Loot tables updated so this content actually drops in the world

Depends on [Placebo](https://github.com/WasabiIceCream/placebo-fabric), a
Fabric port of Apotheosis's own base library.

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

Everything else works: rarities, affixes, gem sockets/cutting/case
storage, reforging/salvaging/augmenting, and loot injection are all live
and confirmed booting cleanly on a dedicated server.

## Development history

For the full porting log (every bug found, every NeoForge-to-Fabric API
decision, in order), see `DEVLOG.md`.
