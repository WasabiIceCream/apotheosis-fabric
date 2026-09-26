package dev.shadowsoffire.apotheosis.client;

import net.minecraft.network.chat.MutableComponent;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.joml.Matrix3x2fStack;

import dev.shadowsoffire.apotheosis.AdventureConfig;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.net.WorldTierPayload;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;

/**
 * Port of NeoForge's {@code WorldTierSelectScreen} — lets a player view and switch between
 * unlocked {@link WorldTier}s.
 * <p>
 * Port note (NeoForge -> Fabric): {@link WorldTierDetailScreen} and {@link WorldTierTutorialScreen}
 * are opened via plain {@link Minecraft#setScreen} swaps rather than upstream's GUI-layer stacking
 * ({@code Minecraft#pushGuiLayer}/{@code #popGuiLayer}) — that API doesn't exist at all in this
 * vanilla version's mappings (confirmed via javap against {@code Minecraft.class}). See those two
 * screens' own javadocs for the same finding.
 */
public class WorldTierSelectScreen extends Screen {

    public static final Identifier TEXTURE = Apotheosis.loc("textures/gui/mountain.png");
    public static final Identifier SEPARATOR_LINE = Apotheosis.loc("textures/gui/separator_line.png");
    public static final Identifier SWORD_EMPTY = Apotheosis.loc("textures/gui/sword_empty.png");
    public static final Identifier SWORD_FULL = Apotheosis.loc("textures/gui/sword_full.png");

    public static final AnimationData HAVEN_ANIMATION = new AnimationData(138, 156, 21, 320, 10, Apotheosis.loc("textures/gui/animations/haven.png"));
    public static final AnimationData FRONTIER_ANIMATION = new AnimationData(210, 236, 45, 588, 21, Apotheosis.loc("textures/gui/animations/frontier.png"));
    public static final AnimationData ASCENT_ANIMATION = new AnimationData(251, 106, 42, 1380, 30, Apotheosis.loc("textures/gui/animations/ascent.png"));
    public static final AnimationData SUMMIT_ANIMATION = new AnimationData(349, 41, 5, 640, 20, Apotheosis.loc("textures/gui/animations/summit.png"));
    public static final AnimationData PINNACLE_ANIMATION = new AnimationData(356, -2, 47, 960, 12, Apotheosis.loc("textures/gui/animations/pinnacle.png"));

    public static final int GUI_WIDTH = 480;
    public static final int GUI_HEIGHT = 270;
    public static final int IMAGE_WIDTH = 498;
    public static final int IMAGE_HEIGHT = 286;

    protected SimpleTexButton activateButton, detailButton, tutorialButton;
    protected WorldTier displayedTier = WorldTier.getTier(Minecraft.getInstance().player);
    protected int leftPos, topPos;
    protected Map<WorldTier, SimpleTexButton> tierButtons = new EnumMap<>(WorldTier.class);
    protected int animTicks = 0;
    /**
     * Port note: guards against the tutorial reopening/re-firing its completion request on
     * close. {@link WorldTierTutorialScreen#closeTutorial()} swaps back to this screen via
     * {@link Minecraft#setScreen}, which re-runs {@link #init()} — including the auto-open
     * check below — using client-side tutorial-active state that hasn't resynced from the
     * server yet (the completion payload is still in flight). Without this flag, that stale
     * read reopens the tutorial a second time, and the freshly-recreated {@link #activateButton}
     * (reset to active by the same re-init) then fires a second completion payload on the
     * second close — see {@code WorldTierPayload#handleServer} for why a genuine duplicate
     * there is otherwise treated as fraudulent. Once true, this screen never auto-opens the
     * tutorial again; the "?" button remains available to reopen it manually.
     */
    private boolean tutorialClosed = false;

    public WorldTierSelectScreen() {
        super(Apotheosis.lang("title", "select_world_tier"));
    }

    @Override
    protected void init() {
        this.leftPos = Math.max(0, (this.width - GUI_WIDTH) / 2);
        this.topPos = Math.max(0, (this.height - GUI_HEIGHT) / 2);

        this.addTierButton(WorldTier.HAVEN, b -> b.pos(this.leftPos + 100, this.topPos + 215));
        this.addTierButton(WorldTier.FRONTIER, b -> b.pos(this.leftPos + 210, this.topPos + 205));
        this.addTierButton(WorldTier.ASCENT, b -> b.pos(this.leftPos + 230, this.topPos + 115));
        this.addTierButton(WorldTier.SUMMIT, b -> b.pos(this.leftPos + 315, this.topPos + 60));
        this.addTierButton(WorldTier.PINNACLE, b -> b.pos(this.leftPos + 395, this.topPos));

        this.activateButton = this.addRenderableWidget(
            SimpleTexButton.builder()
                .size(60, 24)
                .pos(this.leftPos + 198, this.topPos + 15)
                .texture(SimpleTexButton.APOTH_SPRITES)
                .action(this.activateSelectedTier())
                .buttonText(Apotheosis.lang("button", "activate_tier"))
                .build());

        this.detailButton = this.addRenderableWidget(
            SimpleTexButton.builder()
                .size(80, 20)
                .pos(this.leftPos + 178, this.topPos + 75)
                .texture(SimpleTexButton.APOTH_SPRITES)
                .action(this.openDetailedInfoScreen())
                .buttonText(Apotheosis.lang("button", "show_detailed_info"))
                .message(Apotheosis.lang("button", "show_detailed_info.desc"))
                .build());

        this.tutorialButton = this.addRenderableWidget(
            SimpleTexButton.builder()
                .size(12, 15)
                .pos(this.leftPos + GUI_WIDTH - 14, this.topPos + GUI_HEIGHT - 17)
                .texture(SimpleTexButton.APOTH_SPRITES)
                .action(btn -> {
                    Minecraft.getInstance().setScreen(new WorldTierTutorialScreen(this, Apotheosis.lang("title", "world_tier_tutorial")));
                })
                .buttonText(Component.literal("?"))
                .message(Apotheosis.lang("button", "open_world_tier_tutorial"))
                .build());

        this.updateButtonStatus();

        if (this.minecraft.screen == this && !this.tutorialClosed && WorldTier.isTutorialActive(this.minecraft.player) && WorldTier.isUnlocked(this.minecraft.player, WorldTier.HAVEN)) {
            Minecraft.getInstance().setScreen(new WorldTierTutorialScreen(this, Apotheosis.lang("title", "world_tier_tutorial")));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);

        int imgLeft = (this.width - IMAGE_WIDTH) / 2;
        int imgTop = (this.height - IMAGE_HEIGHT) / 2;

        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, imgLeft, imgTop, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        gfx.blit(RenderPipelines.GUI_TEXTURED, SEPARATOR_LINE, this.leftPos, this.topPos + 50, 0, 0, 275, 30, 275, 30);

        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        float scale = 0.5F;
        pose.scale(scale, scale);
        Component diffText = Apotheosis.lang("text", "world_tier.difficulty").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
        for (int i = 0; i < 5; i++) {
            Identifier tex = this.displayedTier.ordinal() >= i ? SWORD_FULL : SWORD_EMPTY;
            int swordLeft = this.leftPos + this.font.width(diffText) + 20 + i * (int) (30 * scale);
            gfx.blit(RenderPipelines.GUI_TEXTURED, tex, (int) (swordLeft / scale), (int) ((this.topPos + 77) / scale), 0, 0, 30, 30, 30, 30);
        }
        pose.popMatrix();

        AnimationData anim = switch (this.displayedTier) {
            case HAVEN -> HAVEN_ANIMATION;
            case FRONTIER -> FRONTIER_ANIMATION;
            case ASCENT -> ASCENT_ANIMATION;
            case SUMMIT -> SUMMIT_ANIMATION;
            case PINNACLE -> PINNACLE_ANIMATION;
        };

        anim.render(gfx, this.leftPos, this.topPos, this.animTicks, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();

        float scale = 3;
        pose.scale(scale, scale);
        Component title = Apotheosis.lang("text", "world_tier." + this.displayedTier.getSerializedName());
        gfx.text(this.font, title.getVisualOrderText(), (int) ((this.leftPos + 15) / scale), (int) ((this.topPos + 15) / scale), 0xFFFFFFFF, true);
        pose.popMatrix();

        Component desc = Apotheosis.lang("text", "world_tier." + this.displayedTier.getSerializedName() + ".desc");
        gfx.text(this.font, desc, this.leftPos + 15, this.topPos + 45, 0xFFC8C86E);

        Component diffText = Apotheosis.lang("text", "world_tier.difficulty").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
        gfx.text(this.font, diffText.getVisualOrderText(), this.leftPos + 15, this.topPos + 80, 0xFFFFFFFF, true);
    }

    @Override
    public void tick() {
        this.animTicks++;
    }

    protected OnPress displayTier(WorldTier tier) {
        // Switches the main screen display to the selected world tier.
        // There's a separate button for actually locking in that world tier.
        return btn -> {
            this.displayedTier = tier;
            this.updateButtonStatus();
            this.animTicks = 0;
        };
    }

    private OnPress activateSelectedTier() {
        return btn -> {
            WorldTier tier = this.displayedTier;
            if (WorldTier.getTier(Minecraft.getInstance().player) != tier || WorldTier.isTutorialActive(Minecraft.getInstance().player)) {
                ClientPlayNetworking.send(new WorldTierPayload(tier));
                this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            btn.active = false;
            this.activateButton.setButtonText(Apotheosis.lang("button", "activated").withColor(0x9A669C));
            this.activateButton.setMessage(Apotheosis.lang("button", "already_activated").withStyle(ChatFormatting.RED));
        };
    }

    protected OnPress openDetailedInfoScreen() {
        return btn -> {
            Minecraft.getInstance().setScreen(new WorldTierDetailScreen(this, this.displayedTier));
        };
    }

    void closeTutorial() {
        if (this.tutorialClosed) {
            return;
        }
        this.tutorialClosed = true;
        if (this.activateButton.isActive() || WorldTier.isTutorialActive(Minecraft.getInstance().player)) {
            this.activateButton.onPress(new net.minecraft.client.input.MouseButtonEvent(
                this.activateButton.getX() + this.activateButton.getWidth() / 2.0,
                this.activateButton.getY() + this.activateButton.getHeight() / 2.0,
                new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)));
        }
    }

    protected void updateButtonStatus() {
        LocalPlayer player = Minecraft.getInstance().player;

        for (WorldTier tier : WorldTier.values()) {
            SimpleTexButton button = this.tierButtons.get(tier);
            if (WorldTier.isUnlocked(player, tier)) {
                button.active = true;
                button.setMessage(Apotheosis.lang("button", tier.getSerializedName()));
            }
            else {
                button.active = false;
                button.setMessage(Apotheosis.lang("button", "tier_locked", Apotheosis.lang("button", tier.getSerializedName())).withStyle(ChatFormatting.RED));
            }
            button.forceHovered = this.displayedTier == tier;
        }

        this.activateButton.active = WorldTier.getTier(player) != this.displayedTier;
        if (WorldTier.isTutorialActive(player) && this.displayedTier == WorldTier.HAVEN) {
            this.activateButton.active = WorldTier.isUnlocked(player, this.displayedTier);
        }

        if (this.activateButton.active) {
            this.activateButton.setButtonText(Apotheosis.lang("button", "activate").withColor(0xFAA8FF));
            Component tierName = Apotheosis.lang("text", "world_tier." + this.displayedTier.getSerializedName()).withStyle(ChatFormatting.GOLD);
            this.activateButton.setMessage(Apotheosis.lang("button", "activate_tier", tierName));

            if (!AdventureConfig.enableManualWorldTierChanges) {
                this.activateButton.active = false;
                this.activateButton.setButtonText(Apotheosis.lang("button", "disabled").withStyle(ChatFormatting.RED));
                this.activateButton.setMessage(Apotheosis.lang("button", "tier_changes_disabled").withStyle(ChatFormatting.RED));
            }
        }
        else if (WorldTier.isTutorialActive(player) && !WorldTier.isUnlocked(player, this.displayedTier)) {
            this.activateButton.setButtonText(Apotheosis.lang("button", "inactive").withStyle(ChatFormatting.RED));
            this.activateButton.setMessage(Apotheosis.lang("button", "locked").withStyle(ChatFormatting.RED));
        }
        else {
            this.activateButton.setButtonText(Apotheosis.lang("button", "activated").withColor(0x9A669C));
            this.activateButton.setMessage(Apotheosis.lang("button", "already_activated").withStyle(ChatFormatting.GOLD));
        }
    }

    private void addTierButton(WorldTier tier, UnaryOperator<SimpleTexButton.Builder> config) {
        SimpleTexButton button = config.apply(
            SimpleTexButton.builder()
                .size(30, 30)
                .texture(Apotheosis.loc("textures/gui/buttons/" + tier.getSerializedName() + ".png"))
                .texSize(30, 90)
                .action(this.displayTier(tier))
                .message(Apotheosis.lang("button", tier.getSerializedName()))
                .inactiveMessage(tierLocked(tier)))
            .build();
        this.tierButtons.put(tier, button);
        this.addRenderableWidget(button);
    }

    /**
     * Tooltip for a locked tier: its unlock advancement and a checklist of that advancement's
     * criteria. Port note (bugfix, 2026-09-25): this was dropped in the initial port, so locked
     * tiers showed only "(Locked)" with no way to see what unlocks them. Progress is read through
     * {@link dev.shadowsoffire.apotheosis.mixin.ClientAdvancementsAccessor}, since
     * {@code ClientAdvancements.progress} is private here.
     */
    private static List<Component> tierLocked(WorldTier tier) {
        var connection = Minecraft.getInstance().getConnection();
        List<Component> list = new java.util.ArrayList<>();
        MutableComponent advName = Apotheosis.lang("advancements", "progression." + tier.getSerializedName() + ".title").withStyle(ChatFormatting.GOLD);
        var advancements = connection == null ? null : connection.getAdvancements();
        var advancement = advancements == null ? null : advancements.get(Apotheosis.loc("progression/" + tier.getSerializedName()));
        var progress = advancement == null ? null
            : ((dev.shadowsoffire.apotheosis.mixin.ClientAdvancementsAccessor) advancements).apoth$getProgress().get(advancement);

        if (progress == null) {
            list.add(Apotheosis.lang("button", "tier_advancement", advName.withStyle(ChatFormatting.OBFUSCATED)).withStyle(ChatFormatting.RED));
            return list;
        }

        list.add(Apotheosis.lang("button", "tier_advancement", advName).withStyle(ChatFormatting.RED));
        list.add(net.minecraft.network.chat.CommonComponents.SPACE);
        // The client never receives an advancement's criteria (advancement.value().criteria() is
        // empty here), only the player's progress, so the names come from the progress object.
        List<String> criteria = new java.util.ArrayList<>();
        progress.getCompletedCriteria().forEach(criteria::add);
        progress.getRemainingCriteria().forEach(criteria::add);
        java.util.Collections.sort(criteria);
        for (String criterion : criteria) {
            var critProg = progress.getCriterion(criterion);
            boolean done = critProg != null && critProg.isDone();
            Component desc = Apotheosis.lang("advancements", "progression." + tier.getSerializedName() + ".criteria." + criterion)
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            list.add(Apotheosis.lang("info", done ? "criteria_done" : "criteria_unfinished", desc));
        }
        return list;
    }

    private static record AnimationData(int x, int y, int width, int height, int frames, Identifier texture) {

        private void render(GuiGraphicsExtractor gfx, int left, int top, int time, float partialTick) {
            int frameHeight = this.height / this.frames;
            int frame = (int) ((time + partialTick) / 2F);
            if (frame >= this.frames) {
                return;
            }
            gfx.blit(RenderPipelines.GUI_TEXTURED, this.texture, left + this.x, top + this.y, 0, (frame + 1F) * frameHeight, this.width, frameHeight, this.width, this.height);
        }

    }

}
