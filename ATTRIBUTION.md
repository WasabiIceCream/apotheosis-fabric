# Asset attribution

This project's Java code is a from-scratch Fabric port and is licensed under
this repo's own `LICENSE` (MIT). The files below are **not** original work;
they're binary/JSON assets pulled from other projects, tracked here with
exact provenance since they carry their own licensing.

## Upstream Apotheosis, pre-license-split (MIT)

Verified directly against `Shadows-of-Fire/Apotheosis`'s GitHub history: the
whole repo (code *and* assets) was plain MIT, copyright reassigned to
Stormraven Studios, LLC, no separate asset restriction, up through commit
[`f12308b`](https://github.com/Shadows-of-Fire/Apotheosis/commit/f12308bfc74ba3f600dcd04b58bc48b9bb6dfc96)
(2025-03-07). The next commit,
[`6ef3e3c`](https://github.com/Shadows-of-Fire/Apotheosis/commit/6ef3e3c904dac132c4ec60dd90b3190a7bf85f3d)
(2025-03-13, "Update licenses"), introduced `LICENSE_ASSETS`
(all-rights-reserved) and split assets out of the MIT grant. Confirmed via
the actual commit diff, not just the file's later existence.

Every file below was fetched from `f12308b` (raw.githubusercontent.com),
i.e. the last commit before the split, and is MIT under upstream's license
as it stood at that time:

- `textures/block/gem_cutting_table.png`
- `textures/block/reforging/{table_top,table_side,table_lit,simple_table_top,simple_table_side,simple_table_lit}.png`
- `textures/block/augmenting/{top,side,bottom,top_lit}.png`
- `textures/block/salvaging_table_{top,bottom,front,side}.png`
- `textures/gui/{gem_cutting,reforge,reforge_animation,salvage,augmenting}.png`
- `textures/gui/sprites/widget/{button,button_disabled,button_highlighted}.png(+.mcmeta)`
- `textures/item/gem_dust.png(+.mcmeta)`, `gem_fused_slate.png`, `potion_charm.png`,
  `gems/default.png`
- `textures/item/{mysterious_scrap_metal,timeworn_fabric,luminous_crystal_shard,arcane_sands,godforged_pearl}.png`
 , renamed from upstream's `{common,uncommon,rare,epic,mythic}_material.png`
  to match this port's item IDs; `godforged_pearl.png` also carries
  upstream's `mythic_material.png.mcmeta` (animated texture)
- `textures/item/sigils/{enhancement,rebirth,socketing,unnaming,withdrawal}.png`
- `blockstates/{gem_cutting_table,reforging_table,simple_reforging_table,salvaging_table,augmenting_table}.json`
- `models/block/{gem_cutting_table,reforging_table,simple_reforging_table,salvaging_table,augmenting_table}.json`
  (the `gem_cutting_table` one is upstream's real Blockbench model, not
  Zenith's approximation, see below)
- `models/item/{gem_cutting_table,reforging_table,simple_reforging_table,salvaging_table,augmenting_table}.json`

These **replace** the equivalent files that were briefly sourced from Zenith
Renewed (still MIT, but likely redistributed official art with murkier
provenance, see the git history / README status sections for that earlier
reasoning). Upstream-at-the-time-it-was-MIT is a strictly cleaner source
than a third-party fork's copy of the same art, so everything Zenith
supplied that also existed pre-split was swapped out in favor of it.

**Not covered by this pre-split source**, these features were added to
upstream *after* March 2025, so no MIT-era version of them exists:

- Gem Case / Ender Gem Case (block textures, item icons, GUI background)
- `sigils/malice.png`, `sigils/supremacy.png`

## CurseForge, "Apotheosis 8.5.4 Legacy Textures" (Chive_X, listed MIT)

Still used for the files the pre-split source doesn't cover:

- `textures/block/{basic_gem_case,ender_gem_case}.png`
- `textures/item/{gem_case_icon,ender_gem_case_icon}.png`
- `textures/item/sigils/{malice,supremacy}.png`

Downloaded via CurseForge's CDN (file ID 8480272). Same provenance caveat as
before (see README): listed MIT by the redistributor, but is explicitly the
*official* pre-8.6.0 Apotheosis art, so likely redistributed rather than
independent work. Used with the user's explicit go-ahead for this private
(non-redistributed) server.

## Not in this public repo

The 6 files listed just above (the "Apotheosis 8.5.4 Legacy Textures"
CurseForge pack files) are excluded from this repository and from its
GitHub Releases jar, even though the actual mod running on the Gameoverse
server includes them. The user's original go-ahead for using these was
scoped specifically to that private server, not to public redistribution
via a public repo/release, a meaningfully different thing, since the
pack's own MIT claim is plausible but unverified (likely redistributed
official Apotheosis art, see the note above). They're listed in
`.gitignore` and kept locally so the mod can still be built for real use;
a `git blame`/history check won't find them either, since the repo's very
first commit was already amended to exclude them before any push. The
built jar attached to this project's GitHub Releases is built with these
files stripped, so it is **not** byte-identical to what's deployed on the
live server, every other file is.

## `GemCaseScreen`'s GUI background, still unsourced

No free source (pre-split upstream, Zenith, or the CurseForge pack) has a
Gem Case GUI sheet, the feature postdates the pre-split cutoff, Zenith
never implemented it, and the CurseForge pack has no GUI-sheet textures at
all. `GemCaseScreen` will render with the missing-texture checkerboard for
its background until this is sourced or hand-authored.
