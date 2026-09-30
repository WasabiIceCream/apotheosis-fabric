# Apotheosis (Fabric Adventure-module port)

Unofficial Fabric 26.1.2 port of the **Adventure module** (affixes, rarities,
gems, sockets) from [Apotheosis](https://github.com/Shadows-of-Fire/Apotheosis)
by Shadows_of_Fire, originally NeoForge-only. MIT licensed upstream — see
`LICENSE`.

**Not in scope**: Apothic-Enchanting (full enchanting overhaul), Apothic-Spawners,
Gateways (boss portals), Apothic-Attributes as a dependency. This scoping was
worked out ahead of time in a separate planning pass covering the full
research, scope rationale, and NeoForge→Fabric API mapping table (not
included in this repo).

*Scope update (0.3.0, 0.4.0): Apothic-Attributes became a dependency in 0.3.0. The
mob features (invaders, elites, augmentations; upstream's `mobs` package) were
wrongly filed under "Gateways/boss scope" in the entries below; they are part of
the Adventure module and were ported in 0.4.0. Gateways, spawners, enchanting and
the boss dungeon/rogue spawner worldgen remain out of scope.*

## Status (2026-09-07)

`./gradlew compileJava` succeeds — 16 files ported. `attachments` (1 file) and
`tiers` (10 files) are done; wired to `placebo-fabric` as a Gradle composite
build (see `settings.gradle.kts`), which is now feature-complete enough for
this port's scope (see that project's own README). Supporting pieces added
along the way: `util/ApothMiscUtil`, `util/AttributeTooltipContext` (a
straight structural port of NeoForge's own type — no Fabric/vanilla
equivalent exists), `compat/GameStagesCompat` (ported as the already-stubbed
no-op it is upstream), and `Apotheosis.java` gained `loc`/`lang`/`langKey`
helpers, `STAGES_LOADED`, and `getCurrentServer()` (tracks the running server
via `ServerLifecycleEvents`, replacing NeoForge's `CommonHooks.resolveLookup`).

**`tiers/WorldTier.java` is now fully un-stubbed** (19 files total). The
player's current world tier lives in `tiers/WorldTierComponent`, a Cardinal
Components API player component (registered by `tiers/WorldTierComponents`
under the `"cardinal-components"` fabric.mod.json entrypoint) — replacing
NeoForge's two `Apoth.Attachments` (`WORLD_TIER`, `TIER_AUGMENTS_APPLIED`,
combined into one component) with free client sync via `AutoSyncedComponent`
(no hand-rolled `WorldTierPayload` needed) and copy-on-death via
`RespawnableComponent`/`RespawnCopyStrategy.ALWAYS_COPY`. Started `Apoth.java`
(upstream's ~740-line central object-holder, spanning both in-scope and
out-of-scope content — being built incrementally as each piece is actually
needed, not ported wholesale) with just `Advancements`/`Stats`.

**Dependency snag worth knowing about**: Cardinal Components' Modrinth bundle
jar-in-jars its submodules like `fabric-api` does, but Loom doesn't expose
those nested jars on the compile classpath automatically the way it does for
`fabric-api`. Fixed by adding Ladysnake's own maven
(`https://maven.ladysnake.org/releases`) and depending on
`cardinal-components-base`/`-entity` directly (plain `implementation`, not
`include()` — the mod is already separately installed on the target server).
See `TODO.md` at the repo root for full detail on this and two other
real-vanilla-API-change discoveries from porting `tiers`
(`Attribute.toComponent` removed in favor of `ItemAttributeModifiers.Display`;
`ClientAdvancements` has no public progress accessor).

## Status (2026-09-07, later — the `affix` spine)

46 files now, `./gradlew compileJava` still clean. The `affix` package's
core spine is ported and real: `Affix`/`AffixInstance`/`AffixHelper`/
`AffixRegistry`/`AffixDefinition`/`AffixType`/`AffixBuilder`/`ItemAffixes`/
`AttributeAffix`/`AttributeProvidingAffix`. Only the `attribute` concrete
affix subtype is registered so far — the other 18 (`affix.effect.*`, ~2500
lines: mob_effect, multi_attr, damage_reduction, catalyzing, cleaving,
enlightened, executing, festive, magical, omnetic, psychic, radial,
retreating, spectral, telepathic, thunderstruck, enchantment, stoneforming)
aren't ported yet.

**Resolved the "known coupling point" flagged below**: built a real
Fabric-native replacement for Apothic-Attributes' `StackAttributeModifiersEvent`
— see `affix/StackAttributeModifiersEvent.java` and its paired
`mixin/ItemStackAttributesMixin.java` (injects into both of vanilla's
independent `ItemStack.forEachModifier` overloads — verified via bytecode
inspection that neither delegates to the other, so no double-fire). This was
the single riskiest piece of new infrastructure in the whole port so far:
get it wrong and every attribute-modifying affix either doesn't apply in
combat or renders wrong tooltips.

Also added this pass: `loot.LootCategory`/`LootRarity`/`RarityRenderData`/
`RarityOverride(Registry)`/`RarityRegistry`/`LootRule`/`LootController`/
`AffixLootEntry`/`AffixLootRegistry` (needed because `affix` and `loot` are
mutually dependent, not a clean tree), `Apoth.BuiltInRegs`/`Components`/
`LootCategories` (using vanilla `EquipmentSlotGroup` in place of
Apothic-Attributes' `EntitySlotGroup`), a new `DeferredHelper.registry()` in
`placebo-fabric` (custom Fabric registries via `FabricRegistryBuilder`), and
`util.PersistentDataComponent` (NeoForge's `Entity#getPersistentData()` turned
out to be a NeoForge-only patch, not vanilla — replaced with a Cardinal
Components component). Full detail, including two more real vanilla-API
removals found (`Attribute.toComponent`/`toValueComponent`), is in `TODO.md`
at the repo root.

## Status (2026-09-07, later still — all 19 affix types)

71 files, `./gradlew build` (full build, mixin annotation processing
included) succeeds. Went through all 18 remaining `affix.effect.*` subtypes
in one pass — every concrete affix type upstream ships is now ported and
registered. Three new mixins: `mixin.EnchantmentHelperMixin` (the
`EnchantmentAffix` dynamic-level-bonus hook the plan flagged as needing one —
turned out simpler than expected, all three NeoForge event modes reduce to a
per-query decision) and `mixin.LivingEntityInvoker` (`@Invoker` accessors for
a few private `LivingEntity` methods). Two new Cardinal Components
(`util.RadialMiningComponent`, a player preference; `util.AbilityCooldowns`,
built on `util.PersistentDataComponent`). Three new static damage types
(`data/apotheosis/damage_type/execute|psychic|thunderstruck.json`) replacing
NeoForge's per-hit damage-tag mutation with datapack tag-file merging. Two
new accesswidener entries. Full detail in `TODO.md` at the repo root.

Five affix methods were dropped with a TODO rather than faked — all the same
shape (a NeoForge event with no Fabric equivalent: drop-list modification,
break-speed, harvest-check, block-break interception, entity-invulnerability
checks). Worth a dedicated pass later since several affixes need the same
underlying mixin.

Not yet ported: `socket` (48 files), `loot`'s remainder (a few files still
depend on `socket`), `item` (5 files), `client`/`net`/`mixin` (partial). Next:
`socket`, per the plan's dependency ordering.

## Toolchain

Same generic Fabric 26.x Loom setup as `mod-dev/creeper-overhaul-fabric/`
(see that project's README for the underlying discoveries — no published
mappings for 26.x, `fabric.loom.disableObfuscation=true`, Java 25 source/target,
no `mappings(...)` call). Package name kept as the original
`dev.shadowsoffire.apotheosis` (matching how the Zenith Renewed fork did it)
to ease diffing against upstream.

## Dependencies added beyond the base template

- **Cardinal Components API** (`maven.modrinth:cardinal-components-api:d78LiKJ8`,
  version 8.0.1, already installed on the live Gameoverse server) — replaces
  NeoForge's DataAttachments for affix/gem state on items and entities.
  Confirmed as the right choice by reading Zenith Renewed's own `cca` package
  (`ZenithComponents.java`) before committing to it.
- **Loot Table Modifier** (`maven.modrinth:loot-table-modifier:HQO9NFWe`,
  version 2.3.1+fabric+26.1) — predicate/action-based loot table modification,
  closer to NeoForge's `GlobalLootModifier` than bare `fabric-loot-api-v3`.
  Not yet installed on the live server — needs adding alongside this mod once
  there's something to test.

## Reference material

- Upstream source (cloned read-only for research, not part of this project):
  the actual `26.1` branch of Apotheosis, used to measure scope and read the
  real NeoForge API usage before porting anything.
- Zenith Renewed (community Fabric fork, stuck at MC 1.20, unofficial/closed
  process but MIT and open-source), read as a reference for Fabric-API
  mapping decisions (Cardinal Components usage, mixin targets for the
  enchantment-level-query gap), not copied wholesale. Its own `adventure`
  package (135 files) closely matches this port's scope, which is
  reassuring cross-validation of the boundary, not a guarantee its
  implementation is correct or complete.

Both of the above are scratch clones under this session's job tmp directory,
not durable — if picking this project back up in a future session and they're
gone, re-clone from `https://github.com/Shadows-of-Fire/Apotheosis` (branch
`26.1`) and `https://github.com/Stalemated/ZenithRenewed`.

## Status (2026-09-07, later still — `socket` package underway)

103 files, `./gradlew compileJava` succeeds. `socket`'s core (`SocketHelper`,
`SocketedGems`, `gem/Gem`, `gem/GemClass`, `gem/GemView`, `gem/UnsocketedGem`,
`gem/GemRegistry`, `gem/PurityWeightsRegistry`, `gem/ExtraGemBonusRegistry`,
`gem/GemInstance`, `gem/Purity`, `gem/GemItem`) and **all 14 concrete
`gem/bonus/*` types** are done and registered (`attribute`, `multi_attribute`,
`durability`, `damage_reduction`, `enchantment`, plus the 8
`gem/bonus/special/*` types: `bloody_arrow`, `leech_block`, `all_stats`,
`drop_transform`, `mageslayer`, `mob_effect`, `omnetic`, `radial`). Only
`frozen_drops` (`FrozenDropsBonus`) is deferred — it hard-depends on
Apothic-Attributes' `COLD_DAMAGE` attribute and an attachment tracking cold
damage taken, both out of scope; TODO'd in `GemBonus.initCodecs()`.

**The gem-bonus/affix integration mixins are now real, not just compiling**:
`mixin.ItemStackAttributesMixin` calls `SocketHelper.getGems(stack).addModifiers(event)`
alongside the existing affix loop (both `forEachModifier` overloads), and
`mixin.EnchantmentHelperMixin` checks `SocketHelper.getGems(stack).streamValidGems()`
for `EnchantmentBonus` the same way it already checked affixes for
`EnchantmentAffix` — gem-granted attribute modifiers and enchantment-level
bonuses actually apply now, not just gem-bonus tooltips.

Also added: `util.SizedIngredient` (self-contained reimplementation of
NeoForge's `common.crafting.SizedIngredient` — a plain `Ingredient` + count
pair with no NeoForge-specific behavior, needed by the gem-cutting recipes).

**`socket/gem/cutting`'s server-side logic is ported** (`GemCuttingRecipe`,
`BasicGemCuttingRecipe`, `PurityUpgradeRecipe`, `GemCuttingRecipeCache`,
`GemCuttingBlock`, `GemCuttingMenu`), rebacked against Placebo-Fabric's real
`InternalItemHandler` API (`getStackInSlot`/`setStackInSlot`, not NeoForge
`transfer`'s `ItemResource`-based one) and registered in `Apoth.java`
(`Blocks.GEM_CUTTING_TABLE`, `Menus.GEM_CUTTING`, `RecipeTypes.GEM_CUTTING`,
`RecipeSerializers.BASIC_GEM_CUTTING`/`PURITY_UPGRADE`). Two things
deliberately left out: the advancement-trigger firing on a successful cut
(advancements are out of scope), and `GemCuttingScreen` (client-side GUI
rendering — deferred to the later client/net pass along with the rest of
`client`). **Upstream's own blockstate/model/texture assets for this block
can't be used** — they exist in the upstream tree (confirmed directly:
`assets/apotheosis/{blockstates,models}/gem_cutting_table.json`), but
upstream ships two separate licenses — `LICENSE` (MIT, the code) and
`LICENSE_ASSETS` ("All Rights Reserved, Copyright Stormraven Studios, LLC",
the assets), confirmed split exactly that way in upstream's own
`build.gradle`. **Resolved 2026-09-07 from a different source** — see the
asset-sourcing status entry further down this file.

**`socket/gem/storage`'s server-side logic is also ported** (`GemCaseTile`,
`GemCaseBlock`, `GemCaseBlockItem`, `GemCaseMenu`, `GemCaseSlot`,
`GemUpgradeMatch`, `GemCaseAnimationState` — the last one is visual-only state
but lives in shared, not client-only, code since `GemCaseTile.clientTick`
references it directly). Registered in `Apoth.java` (`Blocks.GEM_CASE`/
`ENDER_GEM_CASE`, `Tiles.GEM_CASE`/`ENDER_GEM_CASE`, `Menus.GEM_CASE`,
`Items.GEM_CASE`/`ENDER_GEM_CASE`). One deliberate simplification: drops the
inner `GemCaseItemHandler` (a NeoForge `transfer.ResourceHandler<ItemResource>`
exposing the safe's contents to hoppers/pipes) — automation access is outside
this port's core gameplay scope (affixes/rarities/gems/sockets); the direct
gameplay API used by `GemCaseMenu` is unaffected, and a Fabric
`Storage<ItemVariant>` could be added back later without touching anything
else. Also found and worked around three more real vanilla API changes this
pass: `BlockEntityType.BlockEntitySupplier` is now package-private (use a
plain `BiFunction<BlockPos, BlockState, T>` instead), `BlockEntity#onDataPacket`
no longer exists (the client now applies a synced tag straight through the
normal `loadAdditional` path), and `Block#getCloneItemStack` moved to
`BlockBehaviour` and dropped its `Player` parameter. Same asset situation as
the gem cutting table — resolved the same way, see further down.

**3 of the 4 root `socket/*.java` recipe files are done**: `AddSocketsRecipe`,
`ReactiveSmithingRecipe` (interface), `SocketingRecipe` — plus the shared
`util.ApothSmithingRecipe` base (its `BASE_PLACEHOLDER` simplifies upstream's
NeoForge `ICustomIngredient` — which existed only to make the smithing-table
UI accept anything in the base slot — down to a plain vanilla `Ingredient`
built from every non-air item, since the real per-recipe matching always
happened in `matches()`, never in the placeholder itself). `WithdrawalRecipe`
is deferred — it needs `Items.SIGIL_OF_WITHDRAWAL`, which belongs to the
not-yet-ported `item` package.

**With this, the `socket` package (48 files in upstream's scope) is
functionally complete** modulo: `WithdrawalRecipe` (blocked on `item`), the
`frozen_drops` gem bonus (blocked on excluded Apothic-Attributes), and
client-side rendering (`GemCuttingScreen`, `GemCaseScreen`,
`GemCaseTileRenderer`, `GemCaseSelectButton` — deferred to the later
`client`/`net` pass). 114 files total, `./gradlew build` (full mixin build)
succeeds.

## Status (2026-09-07, later still — `item` package, and `socket` fully closed out)

119 files, `./gradlew build` clean. Ported 4 of the `item` package's 5 files
(`GlowyItem`, `PotionCharmItem`, `TooltipBlockItem`, `TooltipItem`) —
`BossSummonerItem` excluded (boss/spawner scope). Also registered the sigil
items in `Apoth.Items` (`SIGIL_OF_SOCKETING`/`WITHDRAWAL`/`REBIRTH`/
`ENHANCEMENT`/`UNNAMING`/`MALICE`/`SUPREMACY`, `POTION_CHARM`, `GOD_FUSED_PEARL`,
`GEM_DUST`, `GEM_FUSED_SLATE`) — the sigils need no dedicated behavior class
upstream either, they're plain `TooltipItem`s. Not yet registered: the
rarity-tier salvage materials (need `SalvageItem`, unported), spawner runes
and `BOSS_SUMMONER` (excluded), smithing-template icon-list items (need
`SmithingTemplateItem` factory helpers, unported).

**This unblocked `WithdrawalRecipe`** (needed `Items.SIGIL_OF_WITHDRAWAL`),
now ported along with `Apoth.RecipeSerializers.WITHDRAWAL` — **closing out
all 4 root `socket/*.java` recipe files and, with it, the entire `socket`
package's in-scope Java logic** (modulo the deferred `frozen_drops` bonus,
client-side rendering, and block assets already noted above).

Found two more real NeoForge-only `Item`/`Item.Properties` extensions this
pass, confirmed absent via javap: `isPrimaryItemFor`/`supportsEnchantment`/
`getMaxDamage(ItemStack)` don't exist on vanilla `Item` at all (enchantment
applicability is now data-driven via tags, not a Java override; `getMaxDamage`
had no state-dependent equivalent, so `PotionCharmItem` now uses only the
static `durability(192)` property), and `Item.Properties#setNoCombineRepair()`
doesn't exist either (dropped — the charm can now be anvil-combined like any
other damageable item, a minor cosmetic mismatch).

## Status (2026-09-07, later still — `loot` package closed out)

139 files, `./gradlew build` (full mixin build) succeeds. Ported all 14
remaining `loot` files across `conditions/` (3), `entry/` (3), `functions/`
(3), and `modifiers/` (5, including the base class) — **`loot` is now
completely done.**

**The `modifiers/*` package needed real new infrastructure, not just fixes**:
NeoForge's `IGlobalLootModifier`/`LootModifier` (a post-generation hook that
mutates a loot table's *already-rolled* item list, filtered by json
conditions) has no Fabric equivalent at all — not even the "Loot Table
Modifier" mod dependency already in `build.gradle.kts`, which turned out to
operate at table-*build* time (editing pools/entries before any roll
happens), a fundamentally different point in the pipeline than what
`AffixLootModifier`/`AffixConvertLootModifier`/`AffixHookLootModifier`/
`GemLootModifier` actually need. Built a from-scratch replacement:

- `loot.modifiers.GlobalLootModifier` — our own base (conditions + priority +
  `doApply`), mirroring NeoForge's own shape closely enough that the four
  concrete modifiers port with only their `codec()` return type and import
  changed.
- `loot.modifiers.GlobalLootModifierRegistry` — a `placebo` `DynamicRegistry`
  using a type-keyed `SubtypedSerializer` (the same polymorphic-codec pattern
  already used for `Affix`/`GemBonus`), loading `data/<ns>/loot_modifier/*.json`.
- `mixin.LootTableGlobalModifiersMixin` — wraps the item consumer of
  `LootTable#getRandomItemsRaw(LootContext, Consumer)`, confirmed via
  bytecode inspection to be the single method every real drop/chest-fill/
  trade query funnels through exactly once. A per-thread depth counter
  excludes recursive loot-table references (which reuse the same
  `LootContext` overload) so modifiers see the complete result set for the
  whole query exactly once, not once per referenced sub-table.

**Also discovered mid-pass**: `LootContext#getQueriedLootTableId()` — the
method every one of these classes (plus `util.LootPatternMatcher`) relies on
to know "which loot table is this" — no longer exists anywhere in this MC
version (confirmed via javap: gone from `LootContext`/`LootParams`/
`LootTable` entirely). Loot tables are registry entries now
(`LootTable.CODEC` is `Codec<Holder<LootTable>>`), but a `LootTable`
instance carries no self-reference to its own key. Fixed with
`util.LootTableQueryTracker`, a small thread-local stack that the mixin
pushes to (via a reverse `Registry<LootTable>#getKey` lookup, resolved once
per outermost query) and everything else reads from instead of calling the
now-gone method.

**Also fixed a pre-existing gap affecting the whole port, not just `loot`**:
none of this project's 9 `DynamicRegistry` subclasses (`AffixRegistry`,
`RarityRegistry`, `RarityOverrideRegistry`, `AffixLootRegistry`,
`GemRegistry`, `PurityWeightsRegistry`, `ExtraGemBonusRegistry`,
`TierAugmentRegistry`, and now `GlobalLootModifierRegistry`) were ever
actually calling `registerToBus()` — meaning **none of them were loading
their datapack JSON content at all**, despite all being fully ported and
compiling. Added `Apotheosis.registerDynamicRegistries()`, called from
`onInitialize()`, which registers all nine. This was a real functional gap,
not a `loot`-specific one — without it the mod would boot with zero
affixes/rarities/gems/tier-augments/loot-entries available at runtime no
matter how much Java-side logic was ported.

Also ported (needed by the above): `util.LootPatternMatcher` (clean,
zero-coupling regex table-id matcher) and a trimmed `util.NameHelper` (item
flavor-naming only — upstream's boss-entity name generation and its
`Configuration`-backed `load()` are boss/spawner-scoped, dropped; also drops
the `ItemAbilities`-based tool-type fallback checks, NeoForge-only, in favor
of the vanilla item-tag checks alone).

**With `socket`, `item`, and now `loot` all done, only `client`/`net`/`mixin`
(partial) remain** from the plan's package list — plus the two deferred GUI
screens (`GemCuttingScreen`, `GemCaseScreen`).

## Status (2026-09-07, later still — two more root `mixin` pieces)

140 files, `./gradlew build` clean. Picked two `mixin`/`net` items out of the
remaining ~30 (root `mixin` + `mixin/client` + `net`), chosen because they
make already-ported code actually functional rather than just compiling —
same bar as the `socket`/`EnchantmentHelperMixin` gem-bonus fix earlier:

- **`mixin.SmithingMenuMixin`** — fires `ReactiveSmithingRecipe#onCraft` when
  a smithing-table result is taken. Without it, `WithdrawalRecipe`'s "return
  withdrawn gems to the player's inventory" side effect (and any future
  `ReactiveSmithingRecipe`) would silently never run, even though the recipe
  itself resolves correctly. Zero NeoForge coupling upstream — ported
  unchanged.
- **Server-side radial mining hook**, registered from `Apotheosis.onInitialize()`
  via Fabric API's `PlayerBlockBreakEvents.AFTER` — replaces
  `RadialAffix`/`RadialBonus`'s dropped `onBreak(BreakBlockEvent)` static
  hooks (confirmed by the plan's own earlier research that this specific case
  — unlike break-*speed* — is covered by this Fabric event, no mixin needed).
  This is the server-authoritative half only (extra blocks actually break);
  the client-side visual piece (`mixin.client.MultiPlayerGameModeMixin` +
  `client.RadialProgressTracker`, for instant client-side break-progress
  prediction on the extra blocks) is still deferred along with the rest of
  `client`.

Left for later, and why: the remaining root `mixin` files are boss/spawner-
scoped (`AbstractSkeletonMixin`, `BaseSpawnerAccessor`, `EnderDragonFightMixin`,
`WitherSkullBlockMixin`) or NeoForge-infrastructure mixins with no Fabric
target at all (`AnyHolderSetMixin`, `GLMProviderMixin` — both replaced by
this port's own `DynamicRegistry`/`SubtypedSerializer` machinery already).
`WandererSpawnerMixin` is wandering-trader spawn-frequency tuning gated by an
unported config flag — cosmetic, not Adventure-module scope, skipped.
`net`'s `GemCaseSelectPayload`/`RerollResultPayload` are genuinely blocked on
their client screens (`GemCaseScreen`, the augmenting table's
`AugmentingScreen` — the whole augmenting-table system isn't ported yet
either); `BossSpawnPayload` is boss scope; `RadialStatePayload`/
`WorldTierPayload` were already replaced by Cardinal Components
`AutoSyncedComponent` sync in earlier sessions. ## Status (2026-09-07, later still — item linking)

143 files, `./gradlew build` clean (including `validateAccessWidener`).
Ported `LinkItemToChatPayload` end to end: the payload itself, `util.ItemLinking`
(cooldown tracker + client trigger), a trimmed `client.AdventureKeys` (just
`LINK_ITEM_TO_CHAT` — the other 3 upstream keybinds still need unported
screens/components, see the note above), and wired registration into
`Apotheosis.onInitialize()`/`ApotheosisClient.onInitializeClient()`.

Found three more real vanilla API changes along the way: `Screen.hasShiftDown()`
no longer exists (replaced with a direct `InputConstants.isKeyDown` check on
both shift keys), `AbstractContainerScreen#getHoveredSlot()` is now private
and takes `(double, double)` instead of being a no-arg accessor (worked
around with a new accesswidener entry exposing the underlying `hoveredSlot`
field directly — the third accesswidener entry in this port), and Fabric
API's keybinding helper lives at
`net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper` in the
installed version, not the more commonly-referenced `KeyBindingHelper`.
`PayloadProvider` also turned out not to declare a `getSupportedProtocols()`
method (upstream's NeoForge equivalent does) — dropped, matching the
established pattern of removing NeoForge-only interface members that don't
exist on the Fabric-native replacement.

## Status (2026-09-07, later still — reforging/salvaging/augmenting: gear progression)

159 files, `./gradlew build` clean. Found and ported a whole scope gap the
plan's original file table didn't clearly separate out: `affix/reforging`,
`affix/salvaging`, and `affix/augmenting` — 20 upstream files, the core
"improve your gear" progression loop (reforge an item to a new rarity,
salvage items back into rarity materials, upgrade/reroll individual affixes)
that isn't boss/spawner-scoped at all. Ported the server-side logic for all
three (block, tile, menu, recipe types) — 16 new files — following the same
pattern established for `socket.gem.cutting`/`socket.gem.storage`:

- `ReforgingRecipe`/`SalvagingRecipe` + their client-side recipe caches:
  clean, zero NeoForge coupling, ported verbatim.
- `ReforgingTableTile`/`SalvagingTableTile`/`AugmentingTableTile`: dropped
  the automation-facing `ResourceHandler` wrappers (hopper/pipe access) —
  same call as `GemCaseTile` — and rebacked inventory persistence on
  `ValueOutput#list`/`ValueInput#list` with `ItemStack.CODEC` instead of
  upstream's `output.putChild("inventory", this.inv)`, which doesn't exist
  on vanilla `ValueOutput` in this version either (confirmed via javap —
  only a `Codec`/`MapCodec`-based `store`, no raw-object `putChild`).
- `ReforgingMenu`'s `ReforgingResultSlot` (extended NeoForge's
  `ResourceHandlerSlot`) now extends vanilla `Slot` directly, backed by
  `InternalItemHandler#asContainer()` — same pattern as
  `socket.gem.storage.GemCaseSlot`. Its reforge-seed persistence
  (`player.getPersistentData()`) now uses `util.PersistentDataComponent`.
- `SalvagingMenu#salvageAll` replaces NeoForge `transfer` API transaction
  calls with a plain "first slot with room, respecting max stack size" loop
  — no transactional rollback, matching this port's established
  simplification for gameplay-only inventory writes.
- `AugmentingMenu` needed `net.RerollResultPayload` (the "you got affix X"
  notification sent after a reroll) — ported with a `TODO` stub client
  handler, since the augmenting table's own client screen isn't ported yet;
  the send is fully functional regardless (`ServerPlayNetworking.send`
  replacing NeoForge's `PacketDistributor.sendToPlayer`).

Also added: `affix.salvaging.SalvageItem` (rarity-tinted flavor-text item,
used both directly and via `Apoth.Items.rarityMat(...)` for the 5 rarity-tier
salvage materials), 4 new `AdventureConfig` cost fields, and
`Apoth.Sounds.REFORGE_ITEM_REFORGED`.

**Still deferred, and why**: the 3 client screens (`ReforgingScreen`,
`SalvagingScreen`, `AugmentingScreen`) and 2 tile renderers
(`ReforgingTableTileRenderer`, `AugmentingTableTileRenderer`) — client
rendering, same bucket as `GemCuttingScreen`/`GemCaseScreen`.

## Status (2026-09-07, later still — smithing recipes + a full debug command suite)

170 files, `./gradlew build` clean. Found the top-level `recipe`, `commands`,
`event`, and `particle` packages hadn't been surveyed yet; triaged all four:

- **`recipe/`** (3 files, ported): `MaliceRecipe`/`SupremacyRecipe` are more
  smithing-table `ReactiveSmithingRecipe`s in the same family as
  `SocketingRecipe`/`WithdrawalRecipe` (Sigil of Malice/Supremacy — high-end
  affix-manipulation items, not boss-scoped) — both call into
  `AffixHelper.applyMalice`/`applySupremacy`, which turned out to already be
  fully ported and Fabric-native (`PersistentDataComponent`-backed) from the
  earlier `affix` package pass, so these needed no new adaptation, just
  registration. `PotionCharmRecipe` (a shaped recipe dipping a Potion Charm
  in any valid potion) is entirely vanilla-typed, zero NeoForge coupling —
  ported verbatim except a private-field workaround (`ShapedRecipe#pattern`
  is private with no getter in this version; stores its own copy instead).
- **`commands/`** (8 of 9 files, ported): a full `/apoth` debug/testing
  command suite — `affix apply/list/list_alternatives`, `set_rarity`,
  `set_sockets`, `set_world_tier`, `reforge`, `gem fromPreset/random`,
  `loot_category`, and `debug weights ...`. Genuinely useful for verifying
  the port actually works once deployed, independent of any unbuilt GUI.
  `BossCommand` excluded (boss scope); `DebugWeightCommand` drops its
  `elites`/`invaders` subcommands (Gateway boss-wave registries, excluded).
  *(Correction, 0.4.0: invaders and elites are the Adventure module's own
  natural-spawn features, not Gateways content; both subcommands and a
  ported `BossCommand` are back, see the 0.4.0 entry.)*
  Replaces NeoForge's `ApotheosisCommandEvent` (a custom event wrapping a
  root literal — the "known coupling point" flagged unresolved for most of
  this port, now resolved) with Fabric API's `CommandRegistrationCallback`.
  Also replaces two more Apothic-Attributes-only calls:
  `ApothicAttributes.getTooltipFlag()` → this port's own
  `util.AttributeTooltipContext.getTooltipFlag()`, and `AttributeHelper.list()`
  (a bullet-point component builder) → a plain `"- "` literal prefix.
- **`event/`** (not ported): all 3 files are the `GetItemSocketsEvent`/
  `CanSocketGemEvent`/`ItemSocketingEvent` NeoForge extension points already
  confirmed dropped back in `socket.SocketHelper`'s own port note — nothing
  new here, just confirms that decision was complete.
- **`particle/`** (not ported): `RarityParticleData` is client-rendering
  support with no server-side use in this scope — deferred to the client
  pass along with everything else particle/rendering-related.

## Known coupling point to watch

`Apotheosis.java`'s root-level `AdventureEvents.java` (the NeoForge event-bus
wiring hub, not yet ported) imports from `apothic_attributes` for
`StackAttributeModifiersEvent` (**resolved** — see the 2026-09-07 status
update above, `affix/StackAttributeModifiersEvent.java`) and
`ApotheosisCommandEvent` (**resolved** — see the `commands/` status update
below, replaced with Fabric API's `CommandRegistrationCallback`).

## Status (2026-09-07, later still — real block/item assets, sourced and attributed)

Resolved the asset-licensing gap noted throughout this file above. Two
sources, both used with the user's explicit go-ahead for this private
(Gameoverse, not publicly redistributed) server:

- **[Zenith Renewed](https://github.com/Stalemated/ZenithRenewed)** (the
  community Fabric fork already used as a research reference elsewhere in
  this port) — MIT licensed as a whole repo, no separate asset carve-out
  (unlike upstream Apotheosis, which explicitly splits `LICENSE`/
  `LICENSE_ASSETS`). Its own README credits "Faellynna: Artist of
  Apotheosis" by name, so these are very likely the same/derived official
  art, just redistributed under Zenith's blanket MIT rather than
  independently redrawn — flagged to the user as a real provenance
  ambiguity before use, same as the CurseForge pack below. Supplied full
  blockstate + block model + textures for `gem_cutting_table`,
  `reforging_table`, `simple_reforging_table`, `salvaging_table`, and
  `augmenting_table` (copied with only a `zenith:` → `apotheosis:` namespace
  swap in the JSON — geometry/UVs untouched), plus GUI textures
  (`textures/gui/gem_cutting.png`, `gui/augmenting.png`) for whenever the
  matching screens get ported.
- **["Apotheosis 8.5.4 Legacy Textures"](https://www.curseforge.com/minecraft/texture-packs/apotheosis-8-5-4-legacy-textures)**
  (CurseForge, author Chive_X, listed MIT) — a resource pack of *official*
  pre-8.6.0 Apotheosis textures, built and tagged for exactly this mod's
  current 26.1.2 version. Same provenance caveat as Zenith (a third party's
  MIT label on what looks like official art, not independently redrawn) —
  flagged to the user, who approved private use. Downloaded directly via
  CurseForge's CDN (`mediafilez.forgecdn.net/files/8480/272/...`, file ID
  8480272). Supplied `gem_case`/`ender_gem_case`/`basic_gem_case` block
  textures (Zenith has no gem-case equivalent at all — it never reached that
  Apotheosis feature) plus a full set of item icons: every rarity-tier
  salvage material, `gem_dust`, `gem_fused_slate`, `potion_charm`, all 7
  sigils, and a generic gem icon (`gems/default.png` — the per-gem-type
  textures need actual gem datapack JSON to select between, which doesn't
  exist in this port yet; see the note below).

**What got wired up**: blockstate + block model + item model for all 7
in-scope blocks (`gem_cutting_table`, `gem_case`, `ender_gem_case`,
`reforging_table`, `simple_reforging_table`, `salvaging_table`,
`augmenting_table`) — the first 5 use Zenith's real geometry verbatim; the
two gem-case variants use a plain `cube_all` model (no source model existed
for them anywhere) with the block's 3D texture and a separate flat 2D item
icon, since a chest-like case block conventionally uses a different icon
than its placed appearance. Also added item models for 15 previously-iconless
items (see above). All JSON validated (`python3 -m json.tool` per file) and
the built jar's contents confirmed (`unzip -l`) to include every new
blockstate/model/texture at the right path. **Not runtime-verified** — no
way to actually launch the game and look at these from this environment;
first real test happens whenever this gets deployed.

**Found while sourcing this**: neither Zenith nor the legacy-textures pack
gets us all the way to a populated mod — this port has **zero datapack game
content** (no gem/affix/rarity JSON definitions) despite the Java-side
registries (`GemRegistry`, `AffixRegistry`, `RarityRegistry`, etc.) being
fully wired and reload-listener-registered. That's a separate, larger gap
than "block textures" — worth its own pass, not attempted here.

## Status (2026-09-07, later still — all 5 client GUI screens)

Ported the client-side GUI for every in-scope block: `GemCuttingScreen`,
`GemCaseScreen`, `ReforgingScreen`, `SalvagingScreen`, `AugmentingScreen`,
plus their shared infrastructure — `AdventureContainerScreen` (dark-mode base
class), `SimpleTexButton` (textured button widget), `DropDownList` (the
augmenting table's affix picker), `PipelinedRenderer` (ghost/gray fake-item
preview helper), `GemCaseSelectButton`, and `GemCaseSelectPayload` (a new
client↔server payload for Gem Case selection sync, following the existing
`ClientPlayNetworking`/`ServerPlayNetworking` pattern). `placebo-fabric`
gained the missing `DrawsOnLeft` interface and its universal
`AbstractContainerScreenMixin`. `GemCaseTileRenderer` (the floating-gems
block-entity renderer) is also ported and registered.

This is the first point in the port where upstream's own source (checked
against the real `26.1` NeoForge branch, not guessed) turned out to already
be written against a screen-rendering API — `extractBackground`,
`extractRenderState`, `extractSlot`, `GuiGraphicsExtractor`, etc. — that
looks a lot like this Fabric build's own mapped jar. That similarity was
useful but not exact, and porting these 5 screens surfaced several real,
verified (via `javap` against the actual merged jar, not assumed) API
differences between NeoForge's copy of this MC version and this Fabric
build's:

- **`AbstractContainerScreen.getLeftPos()`/`getTopPos()` don't exist** in
  this jar — only the protected `leftPos`/`topPos` fields do. Every screen
  was rewritten to use the fields directly (legal for a direct subclass);
  `DrawsOnLeft`, an unrelated interface mixed onto the class, needed the
  fields opened via a new `placebo.accesswidener` entry instead, since plain
  Java protected-access rules don't extend to a same-class-via-mixin
  interface at compile time.
- **`imageWidth`/`imageHeight` are `final`** on this version's
  `AbstractContainerScreen` — upstream's `GemCuttingScreen`/`GemCaseScreen`
  assign them post-`super()` in their constructors (legal on NeoForge's
  copy, where they're mutable); rewritten to pass both through the 5-arg
  super constructor instead.
- **`extractSlotHighlightBack`/`extractSlotHighlightFront` are `private`**
  here, not overridable — `AdventureContainerScreen`'s dark-mode slot
  highlight tint (a cosmetic override of these two methods) is dropped;
  slots fall back to the vanilla highlight. Documented inline.
- **No `renderSlotContents(...)` hook exists** — `GemCaseScreen` upstream
  overrides it to draw the fake gem stack + count overlay per-slot; this
  version collapsed per-slot rendering into
  `extractSlot(GuiGraphicsExtractor, Slot, int, int)` with no equivalent
  seam, so `GemCaseScreen` (and `ReforgingScreen`, which already targeted
  this shape upstream) override `extractSlot` directly instead, falling
  through to `super.extractSlot(...)` for non-custom slots.
- **No FG-color concept on `AbstractWidget`** — `getFGColor()`/
  `UNSET_FG_COLOR` don't exist in this jar (removed once text color moved
  fully onto `Component` `Style`s upstream of both loaders). `SimpleTexButton`'s
  manual FG-color override in `extractDefaultLabel` is dropped; button text
  color now comes purely from the `Component`'s own style.
- **`KeyMapping.isActiveAndMatches(Key)` doesn't exist** — `GemCaseScreen`'s
  filter-box vs. inventory-key-close conflict check now calls
  `KeyMapping.matches(KeyEvent)` directly on the raw event instead.

`DrawsOnLeft`'s NeoForge-only call
(`net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents`, which
turns split `FormattedText` lines into `ClientTooltipComponent`s) has no
Fabric equivalent either, and is replaced with the plain vanilla path:
`Language.getInstance().getVisualOrder(FormattedText)` →
`ClientTooltipComponent.create(FormattedCharSequence)` per line, then
`gfx.tooltip(Font, List<ClientTooltipComponent>, int, int,
ClientTooltipPositioner, Identifier)` — a method that, satisfyingly, has the
exact same 6-argument shape on both loaders.

**GUI textures**: copied `reforge.png` (256×384), `reforge_animation.png`
(127×2240), and `salvage.png` (256×256) from Zenith Renewed — all three
matched their expected `blit()` dimensions in the ported screen code
exactly, no resizing needed. **`GemCaseScreen`'s GUI texture is still
missing.** Zenith's closest-named asset, `textures/gui/socket.png`, turned
out to be a 9×9 icon, not a GUI background sheet — Zenith's older feature
set apparently never had a full "Gem Safe" storage-block screen like current
upstream's 307×256 `gem_case.png`, so there's nothing to source from it. The
CurseForge legacy pack has no GUI-sheet textures at all (item/block icons
only). Upstream's own real `textures/gui/gem_case.png` exists in the
research checkout but is under the same `LICENSE_ASSETS` restriction as
every other still-excluded upstream asset (see above) — not used. Net
effect: `GemCaseScreen` currently blits a texture path with nothing behind
it and will render with Minecraft's missing-texture checkerboard for the
background until a real source is found or one gets hand-authored.

**Tile renderers**: only `GemCaseTileRenderer` (floating gem items above the
Gem Case, driven by real `ItemStackRenderState`s — no external asset
dependency beyond the gem items' own models) is ported and registered.
`ReforgingTableTileRenderer` and `AugmentingTableTileRenderer` are **not**
ported — both render a floating animated 3D prop (a spinning hammer /
rising-and-spinning star cube) via NeoForge's `ModelEvent.RegisterStandalone`
+ `StandaloneModelKey` system, registered in the not-ported
`AdventureModuleClient`. Fabric has no equivalent standalone-model
registration path, and neither Zenith nor the CurseForge pack has real 3D
geometry for either prop (both sources are 2D textures only). The reforging
and augmenting tables are otherwise fully functional — this is purely the
floating cosmetic prop above them that's missing.

As with the asset pass: **nothing here is runtime-verified.** All of it
compiles clean (`./gradlew build` on both `placebo-fabric` and
`apotheosis-fabric`) and every API assumption above was checked against the
real merged jar via `javap`, not guessed — but no screen has actually been
opened in a running client from this environment.

## Status (2026-09-07, later still — asset re-sourcing: pre-license-split upstream)

The user asked whether Apotheosis's `LICENSE_ASSETS` split was recent enough
that a pre-split version of the assets could be used instead of Zenith/
CurseForge. Checked `Shadows-of-Fire/Apotheosis`'s real commit history (not
guessed): the split landed in commit `6ef3e3c` on **2025-03-13**. Before
that, verified via the actual diff, **the whole repo — code and assets —
was plain MIT**, no separate asset restriction at all.

Checked the tree at the last pre-split commit (`f12308b`, 2025-03-07)
against everything this port's screens/blocks/items need, and it covers
almost all of it — including the exact GUI textures at the exact pixel
dimensions the ported screen code expects (`reforge.png` 256×384,
`reforge_animation.png` 127×2240, `salvage.png` 256×256, `augmenting.png`
256×307), real per-block textures for all 5 tables, item textures for the
rarity materials/sigils/gem dust, and even upstream's real Blockbench
`gem_cutting_table` model (previously using Zenith's copy of the same
asset). Swapped all of it in, replacing the equivalent Zenith-sourced files
— strictly better provenance (upstream itself, at a point when it actually
was MIT, vs. a third-party fork's copy of the same art under murkier
circumstances). All 40 PNGs verified to open correctly and match expected
dimensions before being copied in; all 15 blockstate/model JSON files
validated. `./gradlew build` clean on `apotheosis-fabric` afterward.

**Still not covered by the pre-split source** (both features postdate
2025-03), so still CurseForge-sourced: Gem Case / Ender Gem Case block
textures + item icons, and the `malice`/`supremacy` sigil icons. Full
per-file provenance — pre-split-upstream vs. CurseForge, with exact commit
hashes and source paths — is now tracked in
`mod-dev/apotheosis-fabric/ATTRIBUTION.md` rather than scattered across
these status notes.

## Status (2026-09-07, later still — datapack content: gems, affixes, rarities, loot injection)

Closed most of the "zero datapack game content" gap flagged in every status
section above. All of this is `data/`, not `assets/` — datapack JSON (like
Java code) was **never** covered by `LICENSE_ASSETS` at all, on any branch,
at any point — so this was pulled straight from the current upstream `26.1`
branch tip (commit `ae0ef78`), no licensing caveat needed.

**Copied 172 files** (validated as parseable JSON, none rejected) into
`data/apotheosis/apotheosis/{affixes,affix_loot_entries,gems,rarities,rarity_override,purity_weights}/`:
94 affix definitions, 50 affix-loot-entry templates (which items an affixed
drop can be), 21 gems, the 5 rarity tiers, 1 rarity-override rule, 1
purity-weights table. Before bulk-copying, checked every polymorphic `type`
key actually used across the batch against what our registries have
registered (`AffixRegistry`'s 18 affix subtypes, `GemBonus`'s 12 subtypes,
`LootRule`'s 7 subtypes) — full coverage, nothing unknown.

**Found and fixed a real bug in the process**: `GlobalLootModifierRegistry`
(the from-scratch registry that replaces NeoForge's `IGlobalLootModifier`
system — see its javadoc) was registering ad-hoc type keys (`affix`,
`affix_convert`, `affix_hook`, `gem`) that don't match what any real
Apotheosis datapack — including upstream's own — actually ships. Checked
upstream's real `loot_modifiers/*.json` and fixed the keys to match exactly:
`affix_loot`, `affix_conversion`, `code_hook`, `gems`. Without this fix, the
loot-injection JSON below would have silently failed to load (unknown
type), and no affixed gear or gems would ever actually drop in-world despite
everything else being wired.

Ported 4 of upstream's 6 real `loot_modifiers/*.json` files (the ones that
give affixed gear and raw gems a chance to spawn in chest loot and
structure loot — this is the actual "make the mod do something" wiring):
`affix_loot_injection.json`, `gem_loot_injection.json`, `affix_hook.json`
(applies gem/affix on-hit/on-loot effects), `affix_conversion.json`
(upgrades plain drops into affixed ones). **Not ported**: upstream's other
2 files (`gem_entity_drops.json`, `gem_entity_drops_from_real_players.json`
— gem drops from mob kills) depend on a custom loot condition
(`apotheosis:killed_by_real_player`) and entity sub-predicate
(`apotheosis:is_monster`) that aren't registered in this port yet. Chest/
structure loot injection works without them; mob-kill gem drops don't yet.

Net effect: with this in place, the mod should now actually generate
affixed gear and gems in vanilla chest loot and (if present)
Twilight Forest structure loot, not just have empty registries backing
functional GUIs. **Still not runtime-verified** — same standing caveat as
every prior status section; `./gradlew build` is clean but nothing has
been launched from this environment.

**Remaining known gaps**, in rough priority order: the Gem Case GUI texture;
the reforging/augmenting tables' floating 3D props; and, underneath all of
it, actual runtime verification.

## Status (2026-09-07, later still — mob-kill gem drops)

Closed the last piece of the loot-injection gap. Upstream's other 2
`loot_modifiers/*.json` files (`gem_entity_drops.json`,
`gem_entity_drops_from_real_players.json` — gems dropping from killed
monsters) needed 2 custom registrations that weren't ported yet:

- **`KilledByRealPlayerCondition`** (`loot.conditions`) — a `LootItemCondition`
  checking the loot context's attacking entity is a real player. Port note:
  upstream also excludes NeoForge's `FakePlayer` (used by various mods for
  non-player actors like quarries); Fabric has no equivalent standardized
  fake-player marker, so this simplifies to a plain `instanceof ServerPlayer`
  check. Loot generation is always server-side, so this is still "a real
  connected player," just without the fake-player exclusion — a minor,
  documented simplification, not a functional gap.
- **`MonsterPredicate`** (`advancements.predicates`) — an `EntitySubPredicate`
  checking the target `instanceof Monster`, used inside a vanilla
  `minecraft:entity_properties` condition's `type_specific` field. Both
  classes are pure vanilla types with no NeoForge dependency at all (unlike
  most of this port, no Fabric-API mapping was even needed here), so both
  are verbatim ports of upstream's real source.

Registered both via `DeferredHelper`'s existing `lootCondition(...)` and
`custom(...)` helpers (`BuiltInRegs.KILLED_BY_REAL_PLAYER`/`IS_MONSTER` in
`Apoth.java`) — `custom` already supported this exact "register a value
into an arbitrary vanilla `BuiltInRegistries` field" case, no new
`DeferredHelper` API needed. Then copied the 2 JSON files in, same as the
other 4. All 6 of upstream's real `loot_modifiers/*.json` files are now
ported — the loot-injection gap flagged in every status section since the
datapack-content pass is fully closed as far as this scope goes.

`./gradlew build` clean. Still not runtime-verified — same standing caveat.

**Remaining known gaps**: the Gem Case GUI texture, the reforging/
augmenting tables' floating 3D props, and actual runtime verification.

## Status (2026-09-07, later still — first successful runtime launch)

Every prior status section ended with "still not runtime-verified" — this
one changes that. Ran `./gradlew runServer` (Loom's dev dedicated-server
task) repeatedly, fixing what broke each time, until the server reached
`Done` cleanly and stayed up. This is the **first time any part of this
port has actually run**, and it surfaced a cluster of real bugs that no
amount of static review or `./gradlew build` could have caught — every one
of these is a genuine defect, not a hypothetical:

1. **Dead dependency crashed mod resolution before anything else could
   run.** `fabric.mod.json` hard-depended on `loot-table-modifier` (pulled
   in during an earlier design exploration, later abandoned in favor of
   the from-scratch `GlobalLootModifierRegistry` — see above — but never
   removed from the manifest). Its own dependency, `monkeylib538`, isn't
   installed, so Fabric Loader refused to even start. Removed the dead
   dependency from both `fabric.mod.json` and `build.gradle.kts`.
2. **Static field ordering bug crashed `Apotheosis.onInitialize`.**
   `GlobalLootModifierRegistry` declared `INSTANCE = new
   GlobalLootModifierRegistry()` *before* the `SERIALIZER` field it needs —
   Java initializes static fields in textual order, so the constructor ran
   with `SERIALIZER` still null, throwing a `NullPointerException` at
   class-load time. Swapped the declaration order. Checked every other
   `DynamicRegistry` subclass for the same pattern — none had it.
3. **Missing Cardinal Components mod-metadata block crashed entity-component
   registration.** CCA requires every component ID to be pre-declared in a
   `"custom": {"cardinal-components": [...]}` array in `fabric.mod.json` —
   a *separate* declaration from the `"cardinal-components"` entrypoint
   list — and this port never had one. Found the exact schema by checking
   Zenith Renewed's real, working `fabric.mod.json` (a proven-working CCA
   consumer) rather than guessing. Added the block listing
   `apotheosis:world_tier`, `apotheosis:persistent_data`,
   `apotheosis:radial_mining_mode`.
4. **`GemClass`'s (and two other codecs') use of vanilla
   `RegistryCodecs.homogeneousList` silently broke every multi-entry
   `HolderSet` field.** That vanilla helper needs `RegistryOps` (a
   `HolderGetter`) to resolve tag/list references — but Placebo's
   `DynamicRegistry` (this whole datapack-content system) decodes with
   plain `JsonOps.INSTANCE`, since it's a bespoke Fabric-side reload
   listener, not a real vanilla dynamic registry with real `RegistryOps`
   context. Against `JsonOps`, `homogeneousList` fails for any list with
   more than one entry — and because Mojang's list codec in this MC
   version *silently drops failed elements instead of erroring* (confirmed
   by temporarily wrapping the decode in `.promotePartial(...)` to surface
   what was actually happening), this didn't show up as a decode error at
   all: it just made every multi-type `GemClass`, every `StoneformingAffix`
   candidate list, and every `AllStatsBonus` attribute list decode to an
   *empty* `HolderSet`, which then failed downstream precondition checks
   with misleading messages ("No bonuses were provided") that had nothing
   directly to do with the real cause. Fixed all three
   (`GemClass.EXPLICIT_CODEC`, `StoneformingAffix.CODEC`,
   `AllStatsBonus.CODEC`) by swapping to `Registry.holderByNameCodec()` —
   a plain name→value lookup that doesn't need `RegistryOps` — for the
   registries this port owns outright (`BuiltInRegs.LOOT_CATEGORY`) or that
   are real vanilla `BuiltInRegistries` (`BLOCK`, `ATTRIBUTE`). **Left
   unfixed**: `Constraints.biomes` and the vanilla-`Registries.ENCHANTMENT`
   uses in `EnchantmentBonus`/`EnchantmentAffix` — `Registries.BIOME` and
   `Registries.ENCHANTMENT` are *real* dynamic registries with no
   `BuiltInRegistries` static equivalent (they're datapack-driven, only
   reachable through actual `RegistryOps`/`RegistryAccess`), so this same
   fix doesn't apply to them. `Constraints.biomes` is latent (nothing in
   the currently-ported content uses it); the enchantment one is live —
   see the datapack-content section below.
5. **Two `GemBonus` subtypes' registry lookups were also genuinely
   NeoForge/RegistryOps-dependent** and got the same `holderByNameCodec()`
   treatment as #4 once found.
6. **Missing invader sounds blocked 4 of 5 rarity tiers entirely.** The
   real ported `rarities/*.json` (see the datapack-content section) each
   reference an `invader_sound` SoundEvent id
   (`apotheosis:invader_{uncommon,rare,epic,mythic}`) that was never
   registered — `LootRarity`'s codec resolves that field strictly whenever
   it's present (registry-key lookup, unrelated to whether the actual
   `.ogg` audio file exists), so all 4 non-`common` rarities failed to
   parse outright. Registered the 4 `SoundEvent`s in `Apoth.Sounds` (same
   pattern as the existing reforge/malice sounds — no audio asset exists
   yet either, same cosmetic-only gap).
7. **`AffixRegistry` and `RarityOverrideRegistry` had no declared
   reload-listener dependency on `RarityRegistry`**, despite decoding
   `DynamicHolder<LootRarity>` references. `DynamicRegistry`'s default
   `getFabricDependencies()` only depends on the tag manager, so Fabric's
   reload scheduler had no guarantee rarities finished binding first —
   every affix and the one rarity-override file failed with "Trying to
   access unbound value" `NullPointerException`s. Overrode
   `getFabricDependencies()` on both to explicitly depend on
   `RarityRegistry`.
8. **`GemBonus.initCodecs()` — the method that populates the polymorphic
   `type`-dispatch registry for every gem bonus subtype — was never
   called anywhere.** `LootRule.initCodecs()` (the equivalent method for
   loot rules) was wired into `Apotheosis.onInitialize()`; the identical
   call for `GemBonus` simply wasn't. Combined with #4's silent-failure
   behavior, this made gems the single worst-affected piece of content:
   with an empty `GemBonus` type registry, *every* bonus in *every* gem
   failed to decode (silently, per #4), so *every* gem in the entire
   ported set failed its "no bonuses provided" precondition — 0 of 21
   loaded. Adding the missing `GemBonus.initCodecs()` call alone fixed the
   majority of them.

**Also found while diagnosing, and fixed as part of the same pass** (not a
code bug — a datapack-content scoping issue): 17 of the 21 ported gems and
24 of the 94 ported affixes referenced `apothic_attributes:`/
`apothic_enchanting:` content (custom attributes, enchantments, mob
effects) — upstream can freely do this because Apothic-Attributes and
Apothic-Enchanting are *hard, required* dependencies of real Apotheosis,
but both are explicitly out of scope for this port (see the top of this
README). These 41 files were never going to work here regardless of any
codec fix, so they were removed from the ported content set rather than
worked around.

**Final registry-load tally after all of the above**, from the actual
server log: `affix_loot_entries` 50/50, `loot_modifier` 6/6,
`purity_weights` 1/1, `rarities` 5/5, `rarity_override` 1/1, `affixes`
61/70, `gems` 2/4. Server reaches `Done` and stays up. The remaining
failures are the two genuinely-deeper issues called out in point 4 above
(vanilla `Registries.ENCHANTMENT` needs real `RegistryOps`, which
`DynamicRegistry` doesn't provide — affects 2 gems + 2 affixes) plus one
single file (`the_end/endersurge`) using a NeoForge-only
`{"type": "neoforge:any"}` holder-set wildcard this port's simplified
`GemClass` codec has no equivalent for.

Not touched this pass, still open: the Gem Case GUI texture and the
reforging/augmenting floating 3D props (both asset gaps, not code bugs);
actual gameplay testing (a client was never connected — this was a
dedicated-server boot-and-stay-up test only, driven from a headless
environment with no display).

## 2026-09-15: `armor/attribute/aquatic` remapped to an installed mod's attribute

After `placebo-fabric`'s `RegistryOps` fix (see that project's DEVLOG) got
`affixes` to 68/70, the two remaining affix failures were `aquatic` and
`unbound`, both referencing genuine NeoForge-only attributes
(`neoforge:swim_speed`/`neoforge:creative_flight`) that were never registered
on Fabric — not a codec bug, a missing-content one.

Checked whether any mod already in this server's modpack (not
`apothic_attributes`/`apothic_enchanting` — still explicitly out of scope,
see above) happens to register an equivalent attribute we could point at
instead of dropping the content. Found one: **Artifacts**
(`artifacts-fabric-15.1.3.jar`, already installed) registers its own
`artifacts:swim_speed` attribute — decompiled `artifacts.registry.ModAttributes`/
`ModAttributesFabric` to confirm the exact registry id and its base
value/range (`1.0` base, `0.0`–`1024.0`, multiplicative — the same convention
NeoForge's own `swim_speed` uses), and confirmed Artifacts implements it via
its own `LivingEntity` mixin (`artifacts.fabric.mixin.attribute.swimspeed`),
so it's a real, functioning attribute on Fabric, not just a registered-but-inert
id. Remapped `aquatic.json`'s `"attribute"` field from `neoforge:swim_speed` to
`artifacts:swim_speed` — no other change needed, since the existing
`add_multiplied_total` operation and `0.2`–`0.7` value ranges already assumed a
multiplicative, base-1.0 attribute, which is exactly what Artifacts' version is
too.

This makes `aquatic` (and this port generally) soft-depend on Artifacts being
installed — acceptable since Artifacts is already a permanent part of this
server's curated modpack, same as Cardinal Components or Balm, which this port
already assumes. Made that assumption visible instead of silent: added
`"artifacts": "*"` under a new `"recommends"` block in `fabric.mod.json`, so
Fabric Loader prints the same "recommends X, which is missing" warning at boot
we already see for things like modmenu, if Artifacts is ever removed — the
affix itself would just fail to parse again (same non-fatal degrade as today),
not crash anything.

**`unbound` (creative flight) has no equivalent and was left alone.** Checked
every installed mod's lang files and bytecode for an attribute that grants
flight; none exist. NeoForge's `creative_flight` attribute works because
NeoForge itself patches player-ability-tick code to check that attribute value
and toggle `abilities.flying`/`mayfly` — an engine-level hook, not a number.
Reproducing it would mean writing that hook ourselves (a mixin into `Player`'s
ability tick honoring a custom attribute), a real feature port on its own, not
a remap. Not done here.

Confirmed on a local `fabric 26.1/` boot test: `apotheosis:affixes` now
**69/70** (up from 68/70), `gems` unchanged at `3/4`. Only `unbound` and
`the_end/endersurge` remain — both genuinely out of scope, not bugs.

## 2026-09-15: World Tier `summit`/`pinnacle` boss-kill requirements swapped back

The user recalled swapping which boss each World Tier advancement requires at
some earlier point and asked to revert it: `data/apotheosis/advancement/
progression/summit.json` (the "Summit" tier, gated on a full set of Rare gear)
had `kill_ender_dragon`; `progression/pinnacle.json` (the "Pinnacle" tier,
gated on Epic/Mythic gear, the harder/later tier) had `kill_wither`. Swapped
so `summit` now requires `kill_wither` and `pinnacle` requires
`kill_ender_dragon` — the Ender Dragon kill now gates the *later* tier, which
also matches it being the vanilla "final boss" relative to the Wither.
Nothing in `mod-dev/apotheosis-fabric`'s own Java source hardcodes either
boss — this is purely datapack-driven (grepped for `ender_dragon`/`wither` in
`src/main/java`, no hits outside unrelated `withering`/`witherlord`-style
affix names) — so the fix is scoped to the two advancement JSON files plus
their `en_us.json` strings (both the per-criterion "Slay the X" lines and
`pinnacle.desc`'s "requires slaying the Wither" text, which also named the
boss explicitly; `summit.desc`'s vaguer "free the End" was reworded to name
the Wither directly, matching `pinnacle.desc`'s style, since it no longer
requires the dragon). Only one lang file exists (`en_us.json`), no other
locales to update.

Confirmed on a local `fabric 26.1/` boot test: server reaches `Done` clean;
`apotheosis:progression/summit` and `progression/pinnacle` don't appear in the
(pre-existing, unrelated) "Couldn't load advancements" list — that list is
entirely `endrem:main/*_eye` entries, a different mod's known gap (see
`docs/current-state.md`).

## 2026-09-15: `the_end/endersurge` fixed — `GemClass` now supports NeoForge's `"any"` wildcard

Last remaining gem failure. `endersurge`'s Sharpness bonus uses NeoForge's
`{"type": "neoforge:any"}` wildcard for its `gem_class.types` field — NeoForge's
own holder-set system adds a small family of any/all/and/or/not combinators on
top of vanilla's; `"any"` means "every `LootCategory`, don't make me list
them." Vanilla/Fabric's holder-set codec has no such wildcard concept, so
there was nothing to port the combinator itself to. But unlike the
`Registries.ENCHANTMENT`/tag-order issues elsewhere in this port,
`BuiltInRegs.LOOT_CATEGORY` is a registry this port owns outright (a real
`BuiltInRegistries`-style registry, not a datapack-driven dynamic one, per
point 4 in the earlier registry-load-tally section) — so "every value in it"
is just a concrete, finite, enumerable list, not something requiring
`RegistryOps`/`HolderLookup` context at all.

Fixed by special-casing the wildcard directly in `GemClass`'s `"types"` codec
(`GemClass.java`): try decoding `{"type": "neoforge:any"}` first (only that
exact string — any other value, i.e. one of NeoForge's other combinators,
fails with an explicit "Unsupported gem class wildcard type" error rather than
silently matching the wrong things, since none of those other combinators are
used by any ported content), and fall back to the existing explicit-list
codec otherwise. The wildcard expands to `BuiltInRegs.LOOT_CATEGORY.listElements()`
— every currently-registered `LootCategory` — wrapped in `HolderSet.direct(...)`.
Encode direction is intentionally asymmetric (always encodes back out as an
explicit list, never re-collapses to the wildcard shape) since nothing in this
port ever encodes a `GemClass` — verified by grepping for `GemClass`/`Gem`
`encodeStart`/`.encode(` calls, no hits.

Confirmed on a local `fabric 26.1/` boot test: `apotheosis:gems` now
**4/4** (up from 3/4) — `endersurge` loads with no error. `affixes` unchanged
at `69/70`. Only `armor/attribute/unbound` remains, and it's genuinely out of
scope (see the 2026-09-15 `aquatic`/`unbound` entry above) — every other
known gap in this port's datapack content is now resolved.

## 2026-09-15: `armor/attribute/unbound` fixed too — the creative-flight engine hook, implemented

The user asked for this specifically, after the earlier entry called it out as
needing new engine-level work rather than a datapack/codec fix. NeoForge's real
`neoforge:creative_flight` attribute works because NeoForge itself patches
player-ability-tick code to read it and toggle `Abilities#mayfly` — an actual
behavior hook, not a value Fabric could ever just resolve by registering the
right id.

Re-registered as `apotheosis:creative_flight` in `Apoth.CustomAttributes`
(0.0–1.0 range, boolean-style — `>= 0.5` means "grant flight"), same treatment
as the three Apothic-Attributes attributes documented just above it, added to
every `LivingEntity` via the existing `LivingEntityAttributesMixin` (same
mixin, no new one needed for registration). The actual behavior lives in a new
`ServerPlayerCreativeFlightMixin`, injecting at the `TAIL` of
`ServerPlayer#tick()` — deliberately modeled on vanilla's own
`ServerPlayer#updatePlayerAttributes()` (found by decompiling `ServerPlayer`
with Vineflower), which does the exact same "read an attribute every tick,
toggle a transient behavior" pattern already, just for
`Attributes.BLOCK_INTERACTION_RANGE`/creative reach instead. Two safety rules
baked in since this touches shared player state: (1) it never runs at all for
players already in creative or spectator mode — those already have real
flight through a completely different path, and must never be interfered
with; (2) it only ever *revokes* a grant it made itself, tracked via a
`@Unique` `apoth$grantedFlight` boolean on the mixin — so an admin's own fly
permission (e.g. the LuckPerms-driven `admin` group `fly`/`fly.flag` grant
documented in `docs/current-state.md`) is never touched by a player simply
un-equipping the affixed chestplate. `unbound.json`'s `"attribute"` field
remapped from `neoforge:creative_flight` to `apotheosis:creative_flight` to
match.

**Also found and fixed while testing this**: with `unbound` finally decoding
successfully for the first time, a previously-invisible second bug surfaced —
`"exclusive_set": ["apotheosis:armor/attribute/winged"]` referenced an affix
(`winged`) that was never ported and doesn't exist in this port at all,
logging `The affix apotheosis:armor/attribute/unbound contains the unknown
affix apotheosis:armor/attribute/winged in its exclusive set!` on every boot
(non-fatal — the affix still registers — but a real dangling reference,
invisible before now since `unbound` never successfully registered to reach
that validation check). Removed the phantom entry (`"exclusive_set": []`)
rather than porting `winged` itself, matching this port's existing precedent
of dropping unreachable content rather than faking it.

Confirmed on a local `fabric 26.1/` boot test: `apotheosis:affixes` now
**70/70** — every ported affix loads. `gems` unchanged at `4/4`. Zero
apotheosis-related errors of any kind on boot. Deployed to production and
restarted by the user; confirmed via production's own log: `affixes 70/70`,
`gems 4/4`, clean boot, zero apotheosis errors.

**In-game playtest done same day**: gave the user a test item live on the
server via
`/give <player> minecraft:netherite_chestplate 1` →
`/apoth set_rarity apotheosis:mythic` →
`/apoth affix apply apotheosis:armor/attribute/unbound 1` (each `/apoth`
command operates on whatever's in the main hand, so rarity/affix must be set
before equipping). Confirmed working: equipping the item granted flight
immediately. This closes out every documented content gap in the Apotheosis
Fabric port — the only remaining known gaps are purely cosmetic/asset ones
(Gem Case GUI texture, reforging/augmenting floating 3D props), not behavior
or content gaps.

## 2026-09-15: `random_enchant` global loot modifier — genuinely non-upstream, server-specific content

Server-side context (see the Server repo's own `docs/current-state.md`): the
Gameoverse server disables villagers entirely — no village structures
(`no-villages` datapack), `spawn-npcs=false`, the Better Mineshafts zombie
villager cage disabled, and the Woodland Mansion hostage-villager room
removed via a datapack override. That makes vanilla's Curse of Binding and
Curse of Vanishing genuinely unobtainable: their only vanilla source is
librarian trading, confirmed by grepping every vanilla loot table and every
installed mod's bundled loot data directly — zero other hits for either
enchantment anywhere.

Rather than special-case just those two, the user asked for the broadest
possible fix: any enchantment any currently- or future-installed mod adds
should be eligible too, not just a hardcoded pair. Added a new
`GlobalLootModifier` type, `apotheosis:random_enchant`
(`RandomEnchantLootModifier.java`, registered in
`GlobalLootModifierRegistry`): for each loot table matching a configured
pattern, every already-generated item that's still enchant-slot-empty
(`ItemStack#isEnchantable()`) gets an independent chance to roll a random
enchantment. Deliberately reuses vanilla's own
`EnchantRandomlyFunction.randomApplicableEnchantment(registries)` — the exact
helper vanilla's own loot tables use for "roll any enchantment appropriate
for this item" — instead of reimplementing compatibility/leveling logic.
That helper pulls from whatever's tagged `#minecraft:on_random_loot` in the
live registry, which vanilla already populates with `binding_curse`,
`vanishing_curse`, `frost_walker`, `mending`, plus `#minecraft:non_treasure`
(everything else) — checked directly in `data/minecraft/tags/enchantment/
on_random_loot.json`. Any mod's own custom enchantment automatically becomes
eligible too, the moment that mod tags it into `on_random_loot`, with zero
special-casing needed here — this is what makes the fix genuinely
integrate with the rest of the modpack's content rather than just patching
two specific IDs.

Configured via `data/apotheosis/apotheosis/loot_modifier/random_enchant.json`:
a 3% independent per-item chance, matching literally every loot table
(`"pattern": {"path_regex": ".*"}`) as a deliberately broad starting default —
easy to narrow to specific domains/patterns later if 3%-on-everything turns
out too generous or too sparse in practice. Runs at priority 900, just below
`affix_loot_injection`'s 1050, so it can also roll on a freshly-Apotheosis-affixed
item in the same loot generation (affixes are a separate data component from
vanilla enchantments, so `isEnchantable()` still reports true for those).

**Explicitly out of upstream Apotheosis's scope, and that's fine here**: the
user confirmed this repo's public existence is specifically so AutoModpack's
"unverified jar" warning can be checked against real source — not to stay a
byte-faithful port — so genuinely server-specific features like this one are
welcome additions, not something to keep out of the public repo.

Boot-tested locally: `apotheosis:loot_modifier` count went 6 → 7, registers
with zero errors, clean boot. First build attempt silently produced a jar
**missing** the new datapack JSON file (`Registered 6 apotheosis:loot_modifier`,
not 7) — a stale incremental-build cache issue in Gradle, not a code or path
bug; `./gradlew clean build` picked it up correctly. Worth remembering: after
adding a brand-new resource file (not editing an existing one), a plain
`build` can silently skip it — verify the file actually landed in the built
jar (`unzip -l ... | grep ...`) before trusting a "successful" build that
adds new resources, or just default to `clean build` for those cases.
**Not statistically verified in-game**: no connectable client in this dev
environment and local RCON is disabled, so the actual roll behavior wasn't
observed directly — `/loot give @s loot <table>` repeated against production
would be the real way to confirm it.

## 2026-09-15 (same day, remote follow-up): `random_enchant` scaled by tier; Haven/Frontier rarity generosity toned down

Two follow-up requests from the user after reviewing the loot modifier: (1)
was the 3% roll tied to World Tier at all, and (2) separately, Apotheosis's
own rarity generosity felt too high — they were already finding "really good
stuff" at Haven.

**(1)**: it wasn't — `RandomEnchantLootModifier.doApply` never read
`gCtx.tier()`/`gCtx.luck()` at all, just a flat `entry.chance()`. Changed
`TableEntry.chance` from a single `float` to `Map<WorldTier, Float>` (codec:
`WorldTier.mapCodec(Codec.floatRange(0, 1)).codec().fieldOf("chance")`,
matching this port's existing `TieredWeights` per-tier convention — a tier
missing from the map now rolls a 0% chance, not an error). `doApply` now
looks up `entry.chanceFor(gCtx.tier())` before rolling. `random_enchant.json`
now specifies haven 1% → pinnacle 5%, a straight linear ramp, in place of the
old flat 3%.

**(2)**: pulled the real numbers from `data/apotheosis/apotheosis/rarities/
{uncommon,rare}.json` to show the user exactly why — Haven's `rare` had
weight `40` + quality `5.0`, Frontier's had weight `100` + quality `5.0`,
both quite large relative to their tier's total pool (Haven: common 600 +
uncommon 360 + rare 40 = 1000, i.e. rare was already 4% *before* any luck
bonus, and quality 5.0 meant luck could push that meaningfully higher at the
very first tier). The user chose "moderate" cuts, applied to `uncommon`/
`rare` across **all five tiers**, not just Haven — roughly halved quality
(luck scaling) and trimmed weight 35–60%, heaviest at Haven/Frontier where
each rarity was most disproportionate to its tier:
- `uncommon`: haven `360→220`/quality `2.5→1.2`, frontier `600→420`, ascent
  `300→200`, summit `120→80`, pinnacle unchanged (`0`).
- `rare`: haven `40→15`/quality `5.0→2.0`, frontier `100→65`/quality
  `5.0→2.5`, ascent `500→320`/quality `2.5→1.5`, summit `200→200`(from `290`)
  /quality `2.5→1.5`, pinnacle unchanged (`100`, no quality).

`common`/`epic`/`mythic` weights untouched — the complaint was specifically
about rare/uncommon feeling too available too early, not the overall curve
shape.

Boot-tested locally for both changes together: `apotheosis:rarities` still
registers all 5, `loot_modifier` count unchanged at 7, zero errors, clean
boot. Committed as two separate commits (tier-scaling, then rarity tuning)
and pushed. Game-balance numbers, not a bug fix — worth revisiting again if
Haven/Frontier still feel off in practice; these are starting adjustments; not
a claim they're perfectly tuned.

## 0.2.0 (2026-09-25): upstream's generated recipes, block loot, gem smashing

Found by the Gameoverse Guide accuracy audit: the port shipped only the 8 recipe
JSONs that live in upstream's `src/main/resources`. Everything upstream builds with
datagen (`src/generated/resources`) had never been copied, so the Salvaging, Gem
Cutting, Reforging, Simple Reforging and Augmenting Tables, the Gem Case, Gem-Fused
Slate and four Sigils had no recipe, and nothing could be salvaged, cut or
reforged. On the Gameoverse server that made every Salvage Material unobtainable,
and our salvage-gate datapack needs one to enter the Frontier World Tier.

Ported from upstream `26.1` (`ae0ef78`):
- 11 root recipes (the five tables, gem case, gem-fused slate, sigils of
  enhancement/rebirth/socketing/withdrawal), and the whole `gem_cutting/` (5),
  `reforging/` (5) and `salvaging/` (37) folders. Skipped: `smithing/` and the three
  upgrade-template recipes (the templates aren't registered in this port),
  `widthdrawal.json` (duplicate of our `withdrawal.json`), and every recipe gated on
  a mod we don't port (spawners, gateways, infusion, Patchouli book, God-Fused
  Pearl, Raven Enchanting Table, PneumaticCraft armor).
- All 7 block loot tables (tables and gem cases dropped nothing when broken).
- `AffixItemIngredient` (`apotheosis:affix`, by rarity) and `GemIngredient`
  (`apotheosis:gem`, by purity) as Fabric `CustomIngredient`s, registered in
  `onInitialize`. JSON converted from `"neoforge:ingredient_type"` to
  `"fabric:type"`. `GemIngredient` reads only `purity`; upstream's optional `gems`
  filter is unused by every generated recipe.
- `mixin.AnvilGemSmashingMixin`: a landing anvil turns dropped gems into Gem Dust
  (upstream's NeoForge `AnvilLandEvent` handler). Without it Gem Dust had no source,
  and both the Salvaging and Gem Cutting Tables need it.

Boot-tested clean on the local server (no new errors). In-game verification
pending. Still out of scope and unchanged: the 24 affixes and several gems that need
`apothic_attributes`, and gem bonus types not yet ported (`all_stats`,
`bloody_arrow`, `drop_transform`, `leech_block`, `mageslayer`, `omnetic`,
`radial`), which is why only 4 of upstream's 21 gems ship.

## 0.4.0 (2026-09-29): mob features (invaders, elites, augmentations)

Ported upstream's `mobs` package (26.1 branch) and what it needs. Requires
`placebo-fabric` 0.1.2 (gear sets, and a dynamic-tag ordering fix, see below).

### What was ported

- `mobs/types`: `Invader`, `Elite`, `Augmentation`. `mobs/util`: `AffixData`,
  `BasicBossData`, `BossStats`, `EntityModifier` (`mob_effect`, `attribute`,
  `gear_set`, `random_affix_item`), `SpawnCondition` (`spawn_type`,
  `surface_type`, `has_tag`, `is_monster`, `nbt`, `and`, `or`, `not`, `xor`),
  `SupportingEntity`, `SurfaceType`. `mobs/registries`: `InvaderRegistry`,
  `EliteRegistry`, `AugmentRegistry`, plus a new `InvaderSpawnRulesRegistry`.
  `mobs/InvaderSpawnRules`, `mobs/ApothMobEvents`.
- Supporting pieces: `NameHelper` boss-name generation (upstream's default name
  pools), `BonusLootTables` attachment + drop hook, `BossSpawnPayload` and its
  client effects (invader sound, rarity-coloured beacon beam for 20 s),
  `AdventureConfig` boss options (`curseBossItems`, `bossAnnounceRange`,
  `bossSpawnCooldown`, `bossAutoAggro`, `bossGlowOnSpawn`, upstream defaults,
  plain constants like the rest of that class), upstream's small boss hooks
  from `AdventureEvents` (bosses immune to suffocation, golem-type invaders may
  despawn after 10 minutes, elites' effect-cloud passengers removed on death,
  `apoth.burns_in_sun` mobs burn in daylight), `EntityInvoker`.
- Commands: upstream's `/apoth spawn_boss <pos|entity> [invader] [rarity]
  [send_notification]`, a new `/apoth spawn_elite <pos|entity> [elite]`, and
  the `elites`/`invaders` subcommands of `/apoth debug weights`.
- Data, copied from upstream's `src/generated/resources` with upstream paths:
  27 `placebo:gear_sets` (`data/apotheosis/placebo/gear_sets/**`) and their 10
  tags (`data/apotheosis/tags/placebo/gear_sets/*.json`), 23
  `apothic_invaders`, 4 `apothic_elites`, 1 `apothic_augments`, the
  `entity/boss_drops` and `entity/rare_boss_drops` loot tables, and the invader
  spawn rules (converted, below). Not copied: the treasure goblin (Twilight
  Forest compat), Gateways gear use aside from shipping the two
  `gateway_only` gear sets (weight 0, harmless, kept to match upstream).
- Not ported: `BossSpawnerBlock`, the boss summoner item, rogue spawners and
  boss dungeons (worldgen, a later phase), Jade/WTHIT boss tooltips.

### NeoForge -> Fabric decisions

- **Spawn hook and cancellation.** NeoForge fires `FinalizeSpawnEvent` before
  `Mob#finalizeSpawn` and lets a handler cancel the whole spawn. Invaders only
  replace `NATURAL`/`CHUNK_GENERATION` spawns, and both come from
  `NaturalSpawner` (`spawnCategoryForPosition`, `spawnMobsForChunkGeneration`),
  so `NaturalSpawnerInvaderMixin` wraps exactly those two `finalizeSpawn` calls
  (MixinExtras `@WrapOperation`): if an invader spawns, the original mob's
  `finalizeSpawn` never runs (no chicken jockeys or other side effects left
  behind) and the following `addFreshEntityWithPassengers` for that mob is
  skipped, via a `@Share`d flag reset on every call. The mob never enters the
  world. The rest of the pipeline (monster tier augments, `apothic_augments`,
  elite selection) runs for every spawn reason at the head of
  `Mob#finalizeSpawn` (`MobFinalizeSpawnMixin`, replacing
  `MobSpawnAugmentMixin`). Every subclass reaches it through `super`, before its
  own equipment logic, like the NeoForge event.
- **Limit:** an elite with `"finalize": false` can't cancel a subclass's own
  `finalizeSpawn` from inside `Mob#finalizeSpawn`, so such elites keep vanilla
  randomisation in slots their gear set doesn't cover (seen: a Haven Undead
  Knight stray kept its bow, since its Haven gear set is armour only).
- **Tier source changed:** the old `MobSpawnAugmentMixin` used the highest World
  Tier among players within 128 blocks. Now the nearest player's tier is used
  for everything, as upstream. Kept from before: tier augments only apply to
  `Monster`s (upstream: every `Mob`).
- **Elite transformation** is queued on `ServerEntityEvents.ENTITY_LOAD` and run
  at the end of the server tick (upstream: directly in `EntityJoinLevelEvent`,
  which fires before the entity is added). The transformation adds supporting
  entities, and adding entities from inside the entity manager's tracking
  callback can modify the section being iterated. After transformation
  `apoth.miniboss` is boolean `true` and the player key is removed, so a reload
  never transforms twice.
- **Join-level fallback:** monsters that never went through `finalizeSpawn`
  (invaders, NBT/structure mobs, mobs saved before this version) get the
  nearest player's monster tier augments the first time they load with a player
  online (upstream `applyMissedTierAugments`, mob half).
- **Attachments:** `INVADER_COOLDOWN` (player, persistent, kept on death),
  `TIER_AUGMENTS_APPLIED` (mob, persistent) and `BONUS_LOOT_TABLES` (mob,
  persistent) are Fabric Data Attachment API types in `Apoth.Attachments`.
  `getPersistentData()` keys (`apoth.boss`, `apoth.boss.rarity`,
  `apoth.miniboss`, ...) live in the existing `PersistentDataComponent`. Entity
  NBT written for NeoForge (`"NeoForgeData": {...}`, used by the Undead
  Knight's horse) is merged into that component when invaders, elites and
  supporting entities are built, so upstream's data works unchanged.
- **Invader spawn rules:** upstream's NeoForge data map on `dimension_type` is a
  Placebo dynamic registry, `apotheosis:invader_spawn_rules`, one file per
  dimension type, keyed by the file's namespace and path:
  `data/minecraft/apotheosis/invader_spawn_rules/{overworld,the_nether,the_end}.json`
  and `data/twilightforest/apotheosis/invader_spawn_rules/twilight_forest_type.json`.
  Same body as one data-map value (`spawn_chances` per tier, optional
  `cooldown`, `surface_type`), upstream values. A dimension type without a
  file never spawns invaders; an entry for an absent mod's dimension is just
  never looked up (no `neoforge:mod_loaded` condition needed).
- **Chunk-generation threads:** `spawnMobsForChunkGeneration` runs on worldgen
  threads. The cooldown check-and-set is synchronized, the announcement is
  handed to the server thread, the generation context reads the biome from the
  generating region, and any exception in the invader roll is logged instead
  of breaking generation.
- **Client:** beams are submitted from Fabric's `LevelRenderEvents.COLLECT_SUBMITS`
  (same collector/pose stack/render state as NeoForge's
  `SubmitCustomGeometryEvent`), timer on `END_CLIENT_TICK`.
- Apothic Enchanting's max-level override isn't ported: invader item
  enchantment upgrades clamp to the enchantment's own max level.
- `/apoth spawn_boss`: upstream's entity branch read the `pos` argument when
  `send_notification` was given; fixed. Both spawn commands fall back to a
  Haven, zero-luck context when no player is within 64 blocks (upstream fails),
  so they work from the console/RCON.
- Debug logging: `APOTH_DEBUG_MOBS=on` in the environment logs every step at
  INFO (upstream: DEBUG).

### Data deviations from upstream

- **`random_affix_items` scales with World Tier.** `Augmentation`'s
  `application_chance` now accepts either upstream's float (all tiers) or a
  per-tier map, the same shape as `spawn_chances` and our `random_enchant`
  modifier; a tier left out rolls 0%. Our copy uses
  `{"haven": 0.02, "frontier": 0.04, "ascent": 0.07, "summit": 0.10, "pinnacle": 0.12}`
  (upstream: flat 0.12). The chance compares with `<`, so 0 never applies.
- Craig's name colour `rainbow` is `light_purple`: Placebo's animated gradient
  colour can't be registered as a named `TextColor` in this port (and a named
  colour the client doesn't know would break the component's network decode).
- Undead Knight's skeleton horse NBT used pre-1.21 attribute ids
  (`generic.movement_speed`, `generic.knockback_resistance`, an upstream data
  bug: the attributes were silently dropped); now `minecraft:` ids.
- `rare_boss_drops` has a weight-5 entry for `#apotheosis:boss_music_discs`;
  the discs aren't ported, so the tag ships empty and that entry drops nothing.

### Entity predicates

Registered as entity sub-predicates, used under `type_specific`:

```json
{ "type": "apotheosis:is_invader" }
{ "type": "apotheosis:is_invader", "min_rarity": "apotheosis:rare" }
{ "type": "apotheosis:is_invader", "id": "apotheosis:overworld/zombie" }
{ "type": "apotheosis:is_elite" }
{ "type": "apotheosis:is_elite", "min_rarity": "apotheosis:epic", "id": "apotheosis:overworld/craig" }
```

`min_rarity` passes at or above that rarity by `sort_index` (common < uncommon <
rare < epic < mythic). An invader's rarity is its boss rarity (`apoth.boss.rarity`);
an elite's is the rarity of its affixed item (`apoth.miniboss.rarity`, new), so an
elite without an affixed item fails any `min_rarity`. `id` is the definition id
(`apoth.boss.id` / `apoth.miniboss.id`, new; invaders spawned before 0.4.0 don't
have one). `is_elite` is new (upstream has only `is_invader`). Example criterion:

```json
"kill_rare_invader": {
  "trigger": "minecraft:player_killed_entity",
  "conditions": { "entity": [ { "condition": "minecraft:entity_properties", "entity": "this",
    "predicate": { "type_specific": { "type": "apotheosis:is_invader", "min_rarity": "apotheosis:rare" } } } ] }
}
```

The Ascent advancement still uses `apotheosis:is_monster` (see `WorldTier#isUnlocked`).

### Found while testing

- Fabric sorts independent reload listeners by id, so `apothic_invaders` applied
  before `rarities` and every invader failed ("Trying to access unbound value:
  apotheosis:uncommon"). `InvaderRegistry`/`EliteRegistry` now depend on the
  rarity registry (same fix as `AffixRegistry`).
- Every gear-set tag failed with "missing references": Placebo's tag manager
  ran before the registries. Fixed in placebo-fabric 0.1.2.
- `apotheosis.mixins.json` now sets `injectors.defaultRequire: 1`, so an
  injector that stops matching after a Minecraft update fails at boot instead
  of silently doing nothing (all existing injectors still apply).

### Tested (local server, no player online)

Boot: no new ERRORs (only the known moonlight x2 / tapir lines). Counts:
`Registered 27 placebo:gear_sets`, `Loaded 10 tags for placebo:gear_sets`,
`Registered 23 apotheosis:apothic_invaders`, `4 apothic_elites`,
`1 apothic_augments`, `4 invader_spawn_rules`; affixes 94, gems 26 unchanged.

- `/apoth spawn_boss 1600 120 1600 apotheosis:overworld/zombie apotheosis:epic`:
  generated name in the epic colour, Haven chain gear set, chestplate affixed
  (epic, 5 affixes, `from_boss`, "Forosto's ..." name, drop chance 2.0),
  boss_stats attribute modifiers (max health +95.8, speed, attack, armor,
  scale), fire resistance + glowing, `apoth.boss*` keys, bonus loot attachment.
- `/apoth spawn_elite ... apotheosis:overworld/undead_knight`: a stray named
  Undead Knight, leather set, leggings affixed (uncommon), elite keys set.
  Craig: goat named from the lang key; its `jeb_` sheep support spawned (and
  was killed by the wolf invader spawned next to it).
- Predicates via `execute if predicate {...}`: is_invader (plain, min epic,
  id) match the epic zombie; min mythic and a wrong id don't; is_elite doesn't
  match it. is_elite (plain, min uncommon, id) match the knight; min rare and
  is_invader don't.
- With `APOTH_DEBUG_MOBS=on`: `/summon zombie` hits the finalize hook, and
  generating new chunks runs chunk-generation spawns through the invader
  wrapper (logged per mob) and still adds them.
- Not tested (needs a player online): a natural invader replacing a spawn, the
  announcement/beam/sound on a client, random affix items and elites rolling
  on natural spawns, the cooldown.

## 0.4.1 (2026-09-29): rogue spawners and boss dungeons (worldgen)

Ported upstream's worldgen (26.1 branch): rogue spawners and both boss dungeon
variants. They share one setup upstream (`SuccessChanceFeatureConfig`, the
`apotheosis:blacklist` biome modifier, `AdventureConfig.canGenerateIn`), and the
boss dungeons only needed the Caged Invader block on top of the invaders ported
in 0.4.0, so both are in.

### What was ported

- `spawner/RogueSpawner`, `spawner/RogueSpawnerRegistry` (Placebo weighted
  dynamic registry `apotheosis:rogue_spawners`), `util/PresetSpawnerStats`.
- `gen/SuccessChanceFeatureConfig`, `gen/RogueSpawnerFeature`,
  `gen/BossDungeonFeature`, `gen/BossDungeonFeature2`; features registered as
  `apotheosis:rogue_spawner`, `boss_dungeon`, `boss_dungeon_2`.
- `mobs/BossSpawnerBlock` + `BossSpawnerTile` ("Caged Invader", no item, no
  drops, unbreakable, as upstream): every 40 ticks it looks for a non-creative
  player within 8 blocks, removes itself and spawns a random invader for that
  player's World Tier (or the saved `boss_item`), targeting the player.
- `AdventureConfig`: `DIM_WHITELIST` (overworld) and `spawnerValueChance`
  (0.11), upstream defaults as constants; `canGenerateIn`.
- `Apotheosis.DEBUG_WORLDGEN` (`APOTH_DEBUG_WORLDGEN=on`) + `debugLog`, as
  upstream (logs at INFO).
- Data, upstream values and paths: 9 `rogue_spawners` (brutal zombie/husk/
  pillager/rotating, swarm spider/cave spider/fast cave spider/silverfish/baby
  zombie), `loot_table/chests/{chest_valuable,spawner_brutal,spawner_swarm}`,
  3 configured and 6 placed features (`rogue_spawner` 65/chunk y 10..top and
  `rogue_spawner_deep` 85/chunk bottom+6..-1, both at 1% success;
  `boss_dungeon` and `boss_dungeon_2` 6/chunk y 0..top and 8/chunk deep, 100%
  success but strict room checks), `tags/block/rogue_spawner_covers`,
  `structure/boss_1.nbt` (variant 2's room), the boss spawner blockstate,
  model and textures (moved to `textures/block/`, `render_type` dropped:
  26.1 picks the layer from the texture).
- Not ported: the tower structures (`towers` structure set,
  `ItemFrameGemsProcessor`, `tome_tower` loot): a separate structure system,
  not part of this feature setup. Apothic Spawners compat (no Fabric port).

### NeoForge -> Fabric decisions

- **Biome modifiers.** Upstream's six `apotheosis:blacklist` biome modifiers
  add each placed feature to `UNDERGROUND_STRUCTURES` in every biome except
  the vanilla oceans and the deep dark. `gen/ApothWorldgen` does the same with
  Fabric's `BiomeModifications.addFeature`, the blacklist moved into the biome
  tag `#apotheosis:worldgen_blacklist` (same 10 biomes) so a datapack can still
  edit it. As upstream, the features go into every other biome of every
  dimension and `canGenerateIn` limits them to the overworld when they place.
- **Spawner fields.** `BaseSpawner`'s private stat fields and `spawnPotentials`
  are opened by the access widener; `setNextSpawnData` goes through a mixin
  invoker (`BaseSpawnerAccessor`, like upstream) so the block entity's
  override still runs.
- **Access widener was never loaded.** `fabric.mod.json` had no
  `accessWidener` entry, so none of the port's widenings applied at runtime
  (they only affected compilation). Found when the first rogue spawner threw
  `IllegalAccessError` on `BaseSpawner.maxSpawnDelay`. Now declared. This also
  makes the older entries real at runtime: `LivingEntity.attackStrengthTicker`
  (Cleaving), `AbstractArrow.baseDamage` (Spectral Shot, Bloody Arrow),
  `Mob.goalSelector` / `NearestAttackableTargetGoal.targetType` (invaders drop
  their villager-attack goal), and the client item-linking fields; each of
  those would have thrown `IllegalAccessError` when first reached before.
  (`placebo-fabric` has the same gap: `placebo.accesswidener` isn't declared
  in its `fabric.mod.json`; not fixed here.)
- **Feature exceptions.** That `IllegalAccessError` escaped the feature, the
  chunk's generation future never completed, the server thread waited on it
  in `forceload add` and the watchdog killed the server after 60 s.
  `RogueSpawnerFeature` now catches an exception from a preset's `place`, logs
  it and skips that spawner (port addition; rogue spawners are data-driven).
- `BossDungeonFeature2` reads the structure template manager from the
  generating level's server (upstream: NeoForge `ServerLifecycleHooks`) and
  skips the room with an error if `apotheosis:boss_1` is missing.
- `PresetSpawnerStats` only has upstream's vanilla path: min/max delay, spawn
  count, max nearby entities, required player range and spawn range. Other
  stats (`apothic_spawners:youthful`) are skipped, as upstream does without
  Apothic Spawners.

### Interaction with the mob features (kept as upstream)

- Every rogue spawner's spawn data carries extra NBT, so vanilla
  `BaseSpawner` doesn't call `finalizeSpawn` for its mobs. NeoForge still fires
  `FinalizeSpawnEvent` there (checked in NeoForge 21.1's
  `EventHooks.finalizeMobSpawnSpawner`: event always posted, only
  `finalizeSpawn` itself skipped), with spawn reason `SPAWNER`. Upstream's
  handler then does nothing for these mobs besides tier augments: invaders
  only replace `NATURAL`/`CHUNK_GENERATION`, `random_affix_items` excludes
  `spawner`/`trial_spawner`, and the three elites that could match exclude
  `spawner` (Craig only applies to goats). Here `MobFinalizeSpawnMixin` isn't
  reached for them, and the monster tier augments come from the existing
  join-level fallback (nearest player's tier, first load with a player
  online; a spawner only runs with a player in range). Same result, so no
  spawner hook was added.
- Caged Invader bosses come from `Invader#createBoss` (spawn reason `EVENT`),
  not a natural spawn, so no invader cooldown or announcement, as upstream.
  Supporting entities aren't spawned (upstream TODO, same as `/apoth
  spawn_boss`).

### Data deviations from upstream

- Rogue spawner mob NBT used pre-1.21.2 attribute ids
  (`minecraft:generic.movement_speed`, `generic.attack_damage`,
  `generic.max_health`), which 26.1 drops: the "brutal" mobs would have been
  plain mobs (and `Health: 30` clamped to 20). Now `minecraft:` ids.
- The brutal pillager's `HandItems` (pre-1.21.5) is now
  `equipment: {mainhand: crossbow}`; spawner mobs with custom NBT skip
  `finalizeSpawn`, so without it the pillager had no crossbow.
- `swarm/baby_zombie` adds `IsBaby: true`: upstream makes it a baby through
  Apothic Spawners' `youthful` stat, which doesn't exist here.
- `chest_valuable` loses its three Apothic Attributes potion entries
  (Knowledge, Strong Resistance, Extra Long Flying): our Apothic Attributes
  port has no potions and the unknown ids made the whole table fail to load.

### Tested (local server, no player online)

Boot with no new ERRORs (only moonlight x2 / tapir x2); `Registered 9
apotheosis:rogue_spawners`; other counts unchanged (affixes 94, gems 26,
invaders 23, elites 4). With `APOTH_DEBUG_WORLDGEN=on`, force-loading fresh
chunks at x/z 29700..29855 (region r.58.58, never generated before) placed
19 boss dungeons (15 variant 1, 4 variant 2) and 6 rogue spawners (brutal
zombie, husk, pillager; swarm baby zombie, spider, cave spider). A region scan
after `save-all flush`: 220 chunks at or past the features step in r.58.58,
holding 8 Caged Invaders and 6 rogue spawners (about 1 boss dungeon per 27
chunks, 1 rogue spawner per 37). No vanilla dungeon spawners in that sample.

- `data get block 29768 -30 29759`: `minecraft:mob_spawner`, brutal zombie
  spawn data with `minecraft:movement_speed`/`attack_damage`/`max_health`
  modifiers, `MinSpawnDelay 200`, `MaxSpawnDelay 400`, `SpawnCount 3`,
  `MaxNearbyEntities 6`, `RequiredPlayerRange 12`, `SpawnRange 5`; chest below
  with `LootTable: apotheosis:chests/spawner_brutal`; the block above is in
  `#apotheosis:rogue_spawner_covers`.
- `29779 -42 29808`: brutal pillager with the crossbow `equipment`, chest
  `spawner_brutal`. `29750 -53 29811`: swarm baby zombie (`IsBaby: 1b`, Speed
  II), chest `spawner_swarm`.
- Summoning a pillager and zombie with exactly that stored NBT: crossbow
  equipped, `projectile_damage` 1.3 (1.0 x1.3), max health modifiers applied
  (Dynamic Difficulty scales further), zombie is a baby.
- `29732 -18 29761`, `29836 -1 29775`, `29854 -11 29672`: `apotheosis:boss_spawner`
  (`boss_item: "empty:empty"`, so a random invader).
- Not tested (needs a player): a Caged Invader releasing its boss, a rogue
  spawner actually spawning, the block's look on a client.

## 0.4.2 (2026-09-29): chest under a rogue spawner opens

In-game test: Multiplayer Spawners makes spawners unbreakable (mining only disables them), and vanilla won't open a chest
with a solid block on top, so a rogue spawner's chest could never be opened; breaking the chest lost the loot (Slash
Loot keeps per-player contents). `ChestUnderSpawnerMixin` lets `ChestBlock.isBlockedChestByBlock` ignore a spawner above.
Also in 0.4.2: the custom sounds had no `sounds.json` or audio, so Invader arrivals, reforging and the Sigil of Malice
were silent (found in game). Upstream's invader/malice/reforge-placed sounds postdate the 2025 asset license split (All
Rights Reserved), so `sounds.json` maps them to vanilla events: raid horn for invaders (pitch 0.8/0.95/1.1/1.25 by rarity),
smithing table for placing, Wither ambient for malice; reforging plays upstream's pre-split MIT `reforge.ogg`.

## 0.4.3 (2026-09-30): projectile affixes, invader combat stats, spell weapons

### Projectile hooks (bug since the first port)

Nothing called `AffixHelper.copyToProjectile` or the affix/gem `onProjectileFired`/`onProjectileImpact` hooks
(upstream: `AdventureEvents#fireProjectile` on `EntityJoinLevelEvent`, `#impact` on `ProjectileImpactEvent`), so every
arrow affix was inert: the arrow mob effects (Ensnaring, Ivy-Laced, Grievous, ...), Spectral Shot, Magical, and gem
bonuses such as Bloody Arrow and on-impact mob effects. Now `affix/ProjectileHooks`:

- `ServerLevelProjectileMixin` (`ServerLevel.addFreshEntity` HEAD, freshly added projectiles only, priority 900 so it
  runs before Apothic Attributes' arrow-damage hook at the same point): for a projectile with a living owner and without
  `apoth.generated`, the weapon is `AbstractArrow#getWeaponItem()` when set (vanilla bows/crossbows, skeletons, modded
  bows that pass the weapon), else upstream's choice: the owner's item in use (Too Many Bows fires in `releaseUsing`, while
  the bow is still the use item), else the main hand if it's a ranged weapon (or a spell weapon, below), else the off hand.
  Runs the gem and affix fire hooks and copies the weapon onto the projectile.
- `ProjectileImpactMixin` (`Projectile.onHit` HEAD): vanilla reaches it only for non-deflected hits (Apothic's dodge
  cancels `hitTargetOrDeflectSelf` before it), Spell Engine's spell projectiles call it directly, and every vanilla
  override calls super. Runs the source weapon's gem and affix impact hooks.
- Magical: upstream adds `c:is_magic` and `bypasses_armor` to the damage source via NeoForge's `DamageSourceExtension`.
  The fire hook sets `apoth.magical` on the arrow and `DamageSourceMagicalMixin` answers those two tag checks for damage
  whose direct entity is such an arrow.

Tested without a player (arrows summoned with `Owner` and a `weapon` item, since mobs don't tick their AI with nobody
online; `pause-when-empty-seconds` set to 0 for the test and restored): the arrow's persistent data holds
`apoth.source_weapon` (and `apoth.magical` for a Magical bow); an Ensnaring + Ivy-Laced bow's arrow gave an iron golem
Slowness II and Poison III; against a zombie in full netherite a Magical arrow did 12.5 damage where the same arrow
without Magical did 1.7-2.3. Not tested in game: a player's bow/crossbow, Spectral Shot's extra arrow, Bloody Arrow, a
Too Many Bows bow, spell projectiles.

### Invader combat stats (Gameoverse data change, not upstream)

`tools/invader_combat_stats.py` adds Apothic Attributes combat stats to every invader's
`stats.<rarity>.attribute_modifiers` (all `add_value`; idempotent, `--check` to verify). Values are kept low because
players here start with 3 hearts:

| Rarity | crit_chance | crit_damage | life_steal | armor_pierce |
|---|---|---|---|---|
| uncommon | - | - | - | - |
| rare | +0.05 | - | - | - |
| epic | +0.10 | - | +0.05 | +2 |
| mythic | +0.15 | +0.25 | +0.10 | +4 |

Apothic's crit chance and life steal are fractions (0.05 = 5%), crit damage a multiplier over base 1.5, armor pierce
flat armor. Verified with `/apoth spawn_boss ... overworld/zombie mythic|epic` and `/attribute`. Elites unchanged.

### Spell weapons (Gameoverse addition)

Staves and wands had attack damage 2-4, so they were `melee_weapon` and rolled melee affixes.

- New loot category `apotheosis:spell_weapon` (main hand, priority 1900, before melee weapons at 2000):
  `util/SpellWeapons#isSpellWeapon`, not a sword or axe and its main hand `add_value` power in one Spell Power school
  (`spell_power:*` other than generic/critical_*/haste/resistance) is at least its attack damage. No compile or runtime
  dependency on Spell Power. On this server it matches the 31 RPG Series staves and wands (Wizards, Paladins, Arsenal),
  logged at server start as `Spell weapons (31): [...]`.
- Gems: `Gem` maps `spell_weapon` to the gem's `melee_weapon` bonus unless a bonus names `spell_weapon` itself (staves
  had the melee bonuses before). Covers datapack gems (Jewelry gems) without edits.
- Projectile hooks also read a spell weapon in the main hand, so projectile mob-effect affixes apply to spell projectiles.
- New affix type `apotheosis:optional_attribute`: an attribute affix whose attribute may be missing (it then never
  rolls instead of failing to load) and, with `require_item_attribute`, only rolls on items that already have the
  attribute.
- `tools/spell_affixes.py` writes `affixes/spell/**`, adds `spell_weapon` to Lucky and Experienced, and merges the lang
  entries (the lang file is hand-formatted, so it edits lines). Values follow Spell Power's scaling: school power has base
  1 plus the weapon's 3-8, and `add_multiplied_base` multiplies base plus flat adds; haste/crit use base 100 (so 0.05 is
  about 5 points). RPG Series robes give +20-35% school power and 2-5% haste/crit per piece; Jewelry gems 2.4-21.6%.

| Affix | Type | Attribute (`add_multiplied_base`) | common | uncommon | rare | epic | mythic |
|---|---|---|---|---|---|---|---|
| Runic / Blazing / Frigid / Hallowed / Stormcalled / Soulbound | stat, weight 60, item's own school only, one per item | `spell_power:arcane/fire/frost/healing/lightning/soul` | 8-12% | 10-15% | 12-20% | 16-25% | 20-30% |
| Quickened | stat | `spell_power:haste` | 2-4 | 3-5 | 4-6 | 5-8 | 6-10 |
| Focused | stat | `spell_power:critical_chance` | 2-4 | 3-5 | 4-6 | 5-8 | 6-10 |
| Archmage's | ability | `spell_power:generic` (all schools' flat power) | - | - | - | 8-12% | 12-18% |
| Devastating | ability | `spell_power:critical_damage` | - | - | - | 10-15 | 15-20 |
| Lucky, Experienced | stat | existing affixes, now also on spell weapons | | | | | |
| Binding / Hexing | basic effect | slowness / weakness on projectile hit (Ensnaring's numbers) | - | yes | yes | yes | yes |
| Necrotic | basic effect | wither on projectile hit (Blighted's numbers) | - | - | - | yes | yes |
| Flowing | basic effect | speed to the caster on projectile hit (Fleeting's numbers) | - | yes | yes | yes | yes |

Every rarity rule finds enough candidates (mythic: 4 stat, 2 basic effect, 1 ability). The effect affixes only fire
for spell projectiles (and thrown/shot projectiles), not beam, area or instant spells, and not melee hits with the staff.

Tested: boot clean, `Registered 108 apotheosis:affixes` (was 94), and `execute as <armor stand> run apoth reforge`
(the reforge and loot_category commands now accept any living entity and report back) on a fire staff, frost wand,
holy wand, Wizard Staff and Arsenal heal staff: category `spell_weapon`, only the item's school rolls (Wizard Staff gets
exactly one of its three), iron sword stays melee, bow stays bow. A mythic fire staff took an armor stand's Fire Spell
Power from 7 to 8.61 (+23%) and Spell Critical Chance from 105 to 113. Not tested in game: tooltips, casting with an
affixed staff, the effect affixes on a spell projectile, gems socketed into a staff.

## 0.4.4 (2026-09-30): wiring audit of every upstream integration point

We kept finding upstream hooks the port never connected (0.3.0: on-hit/when-hurt, protection, durability; 0.4.3:
projectiles). This pass lists every integration point of upstream's 26.1 branch (clone at `ae0ef78`, 2026-09-06, the
commit the port follows) that touches shipped content, with the port's wiring. Out of scope and not listed: Apothic
Enchanting, Gateways, Apothic Spawners and the other compat packages, datagen, and the tower structures.

A point counts as wired only if something reaches it at runtime (a mixin, Fabric API event or callback).
**Fixed** = connected in 0.4.4. New code: `AdventureEvents` (the port's counterpart of upstream's class of the same name,
static handlers), `mixin/LivingEntityAdventureMixin`, `EntityAdventureMixin`, `PlayerOmneticMixin`, `BlockTelepathicMixin`,
`AbstractSkeletonMixin`, `BossFinalizeSpawnMixin`, `WitherSkullBlockMixin`, additions to `ItemStackMixin` and
`ServerLevelProjectileMixin`, `net/RadialStatePayload`, `compat/TrinketsCompat`.

### Upstream `AdventureEvents` (30 handlers)

| Upstream handler (event, priority) | Port | Status |
|---|---|---|
| `cmds` (ApotheosisCommandEvent) | `Apotheosis#registerCommands` (CommandRegistrationCallback) | wired |
| `affixModifiers` (StackAttributeModifiersEvent) | `ItemStackAttributesMixin` (both `ItemStack.forEachModifier` overloads) | wired |
| `preventBossSuffocate` (EntityInvulnerabilityCheckEvent) | `ApothMobEvents#allowDamage` (ServerLivingEntityEvents.ALLOW_DAMAGE) | wired |
| `fireProjectile` (EntityJoinLevelEvent, HIGH) | `ServerLevelProjectileMixin` -> `ProjectileHooks` | wired (0.4.3) |
| `impact` (ProjectileImpactEvent) | `ProjectileImpactMixin` (`Projectile.onHit`) | wired (0.4.3) |
| `modifyIncomingDamageTags` (Magical) | `DamageSourceMagicalMixin` | wired (0.4.3) |
| `onDamage`: gem/affix `onHurt` (LivingIncomingDamageEvent, LOW) | `LivingEntityAdventureMixin#apoth_onHurt` | **fixed** |
| `shieldBlock`: gem/affix `onShieldBlock` (LivingShieldBlockEvent) | `LivingEntityAdventureMixin#apoth_onShieldBlock` | **fixed** |
| `blockBreak`: gem/affix `onBlockBreak` (BreakBlockEvent) | `AdventureEvents#blockBreak` (PlayerBlockBreakEvents.AFTER) | **fixed** |
| `drops`: affix `modifyEntityLoot` (LivingDropsEvent, LOW) | `LivingEntityAdventureMixin` drop collection -> `AdventureEvents#onEntityDrops` | **fixed** |
| `deathMark`: Festive marks equipment (LivingDeathEvent, LOW) | replaced: drops from outside the loot table count as marked (`LivingDrops`) | **fixed** |
| `dropsLowest` (LivingDropsEvent): Telepathic | `TelepathicAffix#drops(LivingDrops)` via `onEntityDrops` | **fixed** |
| `dropsLowest` (BlockDropsEvent): Telepathic | `BlockTelepathicMixin` (`Block.dropResources` with breaker) | **fixed** |
| `festive_removeMarker` | not needed (no per-stack marker) | n/a |
| `harvest` (HarvestCheck): Omnetic affix/gem | `PlayerOmneticMixin` (`Player.hasCorrectToolForDrops`) | **fixed** |
| `speed` (BreakSpeed, HIGHEST): Omnetic affix/gem | `PlayerOmneticMixin` (`Player.getDestroySpeed`) | **fixed** |
| `onBreak` (BreakBlockEvent, LOW): Radial affix/gem | `Apotheosis#registerRadialMiningHook` (PlayerBlockBreakEvents.AFTER, after `blockBreak`) | wired |
| `gemSmashing` (AnvilLandEvent) | `AnvilGemSmashingMixin` | wired |
| `enchLevels` (GetEnchantmentLevelEvent, HIGH) | `EnchantmentHelperMixin#getItemEnchantmentLevel` -> `applyBonus` | wired (no reentrancy lock; no loop seen) |
| `update`: `apoth.burns_in_sun` (EntityTickEvent.Post) | `MobBossMixin#aiStep` (mobs only; upstream any entity, only mobs get the tag) | wired |
| `despawn`: golem bosses (MobDespawnEvent) | `MobBossMixin#checkDespawn` | wired |
| `clone`: reforge seed (PlayerEvent.Clone) | `AdventureEvents` (ServerPlayerEvents.COPY_FROM) | **fixed** (dying rerolled the reforging table's choices) |
| `equip`: equipped_item trigger | `LivingEntityEquipmentTriggerMixin` | wired |
| `sendWorldTierDataOnJoin` | `WorldTierComponent` (Cardinal Components auto-sync) | wired |
| `applyMissedTierAugments`, mobs | `ApothMobEvents#onEntityLoad` (ENTITY_LOAD) | wired |
| `applyMissedTierAugments`, players | `AdventureEvents#applyMissedTierAugments` (JOIN, AFTER_RESPAWN) | **fixed** (tier luck/experience were lost on death until the tier changed) |
| `sync` (OnDatapackSyncEvent): recipes | Fabric `RecipeSynchronization` + `ClientRecipeSynchronizedEvent` | wired |
| `sync`: `ConfigPayload` | none: `AdventureConfig` is hard-coded defaults on both sides | n/a |
| `recordColdDamage` (LivingDamageEvent.Post) | none: only feeds `FrozenDropsBonus` (Twilight Forest gem, not shipped) | skipped |
| `stackedOnOther`: right-click socketing (ItemStackedOnOtherEvent) | `ItemStackMixin` (`ItemStack.overrideOtherStackedOnMe`) -> `AdventureEvents#stackedOnOther` | **fixed** |
| `removeCloudsOnDeath` | `ApothMobEvents#afterDeath` | wired |
| `syncRadialState` | `RadialMiningComponent` (auto-sync) | wired |

### Other upstream listeners

| Upstream | Port | Status |
|---|---|---|
| `ApothMobEvents#finalizeMobSpawns` (FinalizeSpawnEvent) | `MobFinalizeSpawnMixin`, `NaturalSpawnerInvaderMixin` | wired (0.4.0) |
| `ApothMobEvents#delayedEliteMobs` (EntityJoinLevelEvent) | ENTITY_LOAD + END_SERVER_TICK | wired (0.4.0) |
| `Apotheosis#setup`, payload and registry registration | `Apotheosis#onInitialize` | wired |
| `Apotheosis#caps`: automation handlers for the salvaging/reforging/augmenting tables and gem cases | none | skipped: hoppers/pipes can't use the tables or gem cases. Placebo-fabric's `InternalItemHandler` (their backing store) doesn't roll back aborted transactions, so exposing it to the Transfer API would duplicate or delete items; needs a transactional wrapper first |
| Config file and reloader | none (hard-coded defaults, see `AdventureConfig`) | n/a |

### Upstream mixins (`apotheosis.mixins.json`)

| Mixin | Port | Status |
|---|---|---|
| `AbstractSkeletonMixin`: skeletons use crossbows | `AbstractSkeletonMixin` | **fixed** (skeleton invaders with a ranged gear set could get a crossbow and melee with it) |
| `AnyHolderSetMixin` | `GemClass` handles `neoforge:any` | n/a |
| `BaseSpawnerAccessor`, `EntityInvoker`, `LivingEntityInvoker`, `SmithingMenuMixin` | same names | wired |
| `DamageSourceMixin` (extra damage tags) | `DamageSourceMagicalMixin` (Magical) and static damage type tags (Thunderstruck, `data/minecraft/tags/damage_type`) | wired |
| `EnchantmentHelperMixin` (protection, damage bonus, post attack/hurt, durability) | `EnchantmentHelperMixin` | wired (0.3.0) |
| `EnderDragonFightMixin`, `WitherSkullBlockMixin`: finalize spawn | `BossFinalizeSpawnMixin`, `WitherSkullBlockMixin` | **fixed** (only Apotheosis's own finalize-spawn handling, not vanilla `Mob#finalizeSpawn`) |
| `EntityMixin`: glow colour from the name colour | `EntityAdventureMixin#getTeamColor` | **fixed** (invaders glow in their rarity colour) |
| `GLMProviderMixin` | datagen only | n/a |
| `GoldToolsHaveFortuneModuleMixin` | Quark, not on this server | n/a |
| `ItemStackMixin`: affix name | `ItemStackMixin#getHoverName` | wired |
| `ItemStackMixin`: rarity colour on the name | `ItemStackMixin#getHoverName` | **fixed** (renamed/unnamed items with a rarity kept a white name) |
| `ItemStackMixin`: `useOn` (onItemUse) | `ItemStackMixin#useOn` | wired (0.3.0) |
| `ItemStackMixin`: festive marker | `LivingDrops` | n/a |
| `ItemStackMixin`: malice marker in `inventoryTick` | `MaliceRecipe#onCraft` via `SmithingMenuMixin` (applies on take) | wired differently |
| `LivingEntityMixin`: mobs keep health percentage when gear changes max health | `LivingEntityAdventureMixin` (only when max health changed) | **fixed** |
| `MHFMixinLivingEntity` (MC-17876) | MaxHealthFix mod on the server does the same | skipped |
| `MobMixin`: bonus loot tables | `MobBossMixin#dropFromLootTable` | wired |
| `WandererSpawnerMixin` | wandering trader affix trades aren't shipped (and the server disables trader spawns) | skipped |

### Affix and gem bonus hooks (`Affix`, `GemBonus`, through `AffixInstance`/`SocketedGems`)

| Hook | Called from | Status |
|---|---|---|
| `addModifiers` | `ItemStackAttributesMixin` | wired |
| `getDamageProtection`, `getDamageBonus`, `doPostAttack`, `doPostHurt`, `getDurabilityBonusPercentage` | `EnchantmentHelperMixin` | wired (0.3.0) |
| `onProjectileFired`, `onProjectileImpact` | `ProjectileHooks` | wired (0.4.3) |
| `onItemUse` | `ItemStackMixin#useOn` | wired (0.3.0) |
| `getEnchantmentLevels` (affix and gem) | `EnchantmentAffix`/`EnchantmentBonus#applyBonus` | wired |
| `modifyLoot` (Stoneforming, Drop Transform gems) | `AffixHookLootModifier` (`affix_hook` loot modifier) | wired |
| `onHurt` (damage reduction affixes: Blast-Forged, Runed, ...; Damage Reduction and Mageslayer gems) | `AdventureEvents#onHurt` | **fixed** |
| `onShieldBlock` (Catalyzing, Psychic, Retreating, shield mob effect affixes, Leech Block and mob effect gems) | `AdventureEvents#onShieldBlock` | **fixed** |
| `onBlockBreak` (breaker mob effect affixes: Spelunker's, Swift, ...; mob effect gems) | `AdventureEvents#blockBreak` | **fixed** |
| `enablesTelepathy` (Telepathic) | `TelepathicAffix#drops`, `#blockDropTarget` | **fixed** |
| `modifyEntityLoot` (Festive) | `AdventureEvents#onEntityDrops` | **fixed** (hook restored on `Affix`) |
| `skipModifierIds` | gems: modifiers built with `Display.hidden()`; affixes: only during the World Tier tutorial upstream | partial (see client) |
| Omnetic (static `harvest`/`speed`) | `PlayerOmneticMixin` | **fixed** |
| Radial (static `onBreak`) | `registerRadialMiningHook` | wired |

### Client (`AdventureModuleClient`, `AdventureKeys`, client mixins)

| Upstream | Port | Status |
|---|---|---|
| Screens, gem case renderer, per-gem models, recipe caches, REQUEST_STATS on login, boss beams | `ApotheosisClient`, `BossSpawnEffects`, `GemSelectProperty` | wired |
| `affixTooltips`: affix lines | `AdventureTooltips` | wired |
| `affixTooltips`: durability bonus, malice marker, Touched by Malice | `AdventureTooltips` | **fixed** |
| `showBlacklistedPotions` | `AdventureTooltips` | **fixed** |
| `renderCanSocketTooltip` ("Right-click to socket ...") | `AdventureTooltips` (ScreenEvents.afterExtract) | **fixed** |
| `tooltips`/`comps`: socket row with gem icons (`SocketComponent`) | text lines, one per socket (0.3.0) | wired differently |
| Stoneforming tooltip component | none | skipped (cosmetic) |
| World Tier tutorial gating of affix tooltips, over-max star search | none | skipped: the attribute lines can't be hidden here, so gating only the affix lines would be inconsistent |
| Keys: link item to chat, open World Tier select | `AdventureKeys` | wired |
| Key: toggle radial mining (Ctrl+O) | `AdventureKeys` + `RadialStatePayload` | **fixed** (the mode was stuck on "Disabled While Sneaking") |
| Key: compare equipment | none | skipped (cosmetic) |
| `AffixItemEffectRenderer` (loot beams, shadows, particles on dropped rarity items), `EntityRendererMixin` | none | skipped (cosmetic; own beam/shadow renderer needed) |
| `RadialProgressTracker`, `MultiPlayerGameModeMixin` (client-side radial break prediction and outlines) | none | skipped (the server breaks and syncs the blocks) |
| Reforging/augmenting table renderers, ghost-item pipelines (`GuiGraphicsExtractorMixin`, `GuiItemAtlasMixin`, `ItemStackRenderStateMixin`), `AbstractSkeletonRendererMixin`, `GuiMixin` (8 s action bar) | none | skipped (cosmetic) |
| `TierAugmentModifierSource` (Dynamic Tooltips source name) | none | skipped (cosmetic) |

### Counts

84 rows above (a few rows group several related points, e.g. the five `EnchantmentHelperMixin` hooks): 38 already
wired (3 of them wired differently from upstream), 27 fixed in 0.4.4, 1 partial (`skipModifierIds`), 7 not applicable on
Fabric or this server, 11 skipped: 7 cosmetic client features, plus table/gem case automation (Placebo transaction issue),
cold damage tracking (unshipped gem), wandering trader trades (unshipped) and the MC-17876 fix (MaxHealthFix does it).

NeoForge item extensions: `GemItem#canBeHurtBy` (falling anvils) isn't needed, anvils only damage living entities in 26.1;
`getCreatorModId` is cosmetic; `PotionCharmItem#supportsEnchantment`/`isPrimaryItemFor` = false is the default for an
item without an enchantable component.

### Behaviour notes

- **onHurt order**: injected at the same point as Apothic Attributes' incoming damage hook (`LivingEntity.hurtServer`, at
  the `isSleeping()` call, before blocking and armor). The exported class (`-Dmixin.debug.export`) shows the order Apothic
  (projectile damage, crits, dodge) -> Fabric ALLOW_DAMAGE -> Spell Engine evasion -> Apotheosis `onHurt`, matching
  upstream (Apothic HIGHEST/HIGH, Apotheosis LOW). A dodged hit never reaches `onHurt`.
- **Shield block**: the blocked amount from `BlocksAttacks.resolveBlockedDamage` goes through the gem and affix hooks
  before the shield takes durability damage, as NeoForge's event does.
- **Death drops**: other mods capture `spawnAtLocation` during `dropAllDeathLoot` and add the drops themselves at the end
  (Puzzles Lib's drops event does), so a `spawnAtLocation` return hook saw nothing. Drops are collected in
  `ServerLevel.addFreshEntity` while the entity drops its loot (`DeathDropsCollector`); loot table stacks are recorded by
  identity in `spawnAtLocation`. Only non-player deaths.
- **Festive** copies only loot table drops (upstream: everything not in the mob's equipment or item capability). Mob
  equipment, inventories (modded ones too) and custom death loot are never copied, so no duplication of what a mob carried.
  Side effect: a Wither's Nether Star (custom death loot) isn't copied; upstream would.
- **Omnetic** runs on both sides (the client predicts break speed), as upstream.
- **Dragon/Wither**: now go through Apotheosis's spawn finalization with reason EVENT: World Tier monster augments (for
  the Wither, a Monster) and augmentations can apply, as upstream. Needs a player nearby, like every spawn.
- **Right-click socketing** needs a free socket that accepts the gem (`SocketHelper.canSocketGemInItem`), same as the
  smithing recipe; the hint tooltip shows when it applies.

### Potion Charms in a Trinkets slot (Gameoverse addition)

Upstream lets the charm go in a Curios `charm` slot (`compat/curios/CuriosCompat`, `player_charm.json`). Here:
`compat/TrinketsCompat` (Trinkets Updated as an optional `compileOnly` dependency, only loaded when `trinkets` is present;
`suggests` in `fabric.mod.json`) registers a `TrinketCallback` whose `tick` runs the inventory logic
(`PotionCharmItem#tickCharm`, no equipment slot, like Curios upstream). The slot is `charm/charm`, the one Friends & Foes
already defines for totems; the port ships the same slot file, adds it to players (`data/trinkets/entities/apotheosis_charm.json`)
and tags the charm (`trinkets:charm/charm`), so it works without Friends & Foes too. One Charm slot, shared with totems.
`charmsInCuriosOnly` stays false (the charm also works from the inventory).

### Verified on the local server (no player online)

- Boot clean (only the known moonlight/tapir errors), no mixin errors after the tests loaded the target classes (skeleton,
  wither skull block, player, block, living entity). The dragon fight mixin wasn't exercised (it fails loudly if its target
  is wrong, `defaultRequire` 1).
- `onHurt`: two zombies in iron chestplates, one mythic Blast-Forged at level 1.0 (35%); `/damage ... 10 minecraft:explosion`
  took 6.40 health vs 9.84 without the affix.
- Order of the damage hooks: exported `LivingEntity.class` (see above).
- Telepathic mob drops: a vindicator with a rare Telepathic iron axe killed a cow (`/damage ... by`); the leather and beef
  were moved to the vindicator, recorded as loot table drops (logged during the test).
- Mob health percentage: a zombie at 18/36 put on a chestplate with +36 max health went to 48.4/100.8 (50%, minus sunlight
  burning).
- Wither built by a dispenser: `APOTH_DEBUG_MOBS=on` logged "Finalizing spawn for: Wither".
- Trinkets: the Potion Charm matches `#trinkets:charm/charm`.
- Not testable without a player (mob AI doesn't run with nobody online, `/damage` can't give a player source): Festive,
  shield block, block break effects, Omnetic, block Telepathic, right-click socketing, skeleton crossbows, player tier
  augments after death, reforge seed after death, radial toggle key, tooltips, the charm in its slot. In-game steps are in
  the handoff to the user.

## 0.4.5 (2026-09-30): Potion Charms can't be combined

Upstream's `setNoCombineRepair()` (NeoForge-only) had been dropped, so any two charms merged in an anvil or grindstone,
keeping the left one's potion. `CharmNoCombineAnvilMixin` (AnvilMenu.createResult) and `CharmNoCombineGrindstoneMixin`
(GrindstoneMenu.computeResult) block charm + charm. On Gameoverse charms are also on Simple Smithing Overhaul's
destruction allow list, so they break for good (the user's call: charms are used up, not repaired).
