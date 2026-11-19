package com.phantomclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhantomClient implements ClientModInitializer {
    public static final String MOD_ID = "phantomclient";
    private static final int ZOOM_FOV = 30;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    public static final Module FLY        = new Module("Fly", "movimento", Items.FEATHER);
    public static final Module SPEED      = new Module("Speed", "movimento", Items.SUGAR);
    public static final Module STEP       = new Module("Step", "movimento", Items.OAK_STAIRS);
    public static final Module SPRINT     = new Module("Auto sprint", "movimento", Items.LEATHER_BOOTS);
    public static final Module FULLBRIGHT = new Module("Fullbright", "visual", Items.GLOWSTONE);
    public static final Module ZOOM       = new Module("Zoom", "visual", Items.SPYGLASS);
    public static final Module HUD        = new Module("HUD", "hud", Items.COMPASS);
    public static final Module AURA       = new Module("MobAura", "combate", Items.IRON_SWORD);

    public static final List<Module> MODULES =
            List.of(FLY, SPEED, STEP, SPRINT, FULLBRIGHT, ZOOM, HUD, AURA);

    private static final Map<KeyBinding, Module> TOGGLES = new LinkedHashMap<>();
    private static KeyBinding zoomKey, menuKey;

    private static boolean zooming;
    private static int savedFov = 70;
    private static int auraCooldown;

    private static KeyBinding bind(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.phantomclient." + name, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    @Override
    public void onInitializeClient() {
        ZOOM.enabled = true;
        HUD.enabled = true;

        FLY.onDisable = () -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p != null) disableFly(p);
        };
        FULLBRIGHT.onDisable = () -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p != null) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        };
        STEP.onDisable = () -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p != null) setStep(p, 0.6);
        };
        ZOOM.onDisable = () -> stopZoom(MinecraftClient.getInstance());

        TOGGLES.put(bind("fly", GLFW.GLFW_KEY_B), FLY);
        TOGGLES.put(bind("speed", GLFW.GLFW_KEY_N), SPEED);
        TOGGLES.put(bind("step", GLFW.GLFW_KEY_M), STEP);
        TOGGLES.put(bind("sprint", GLFW.GLFW_KEY_V), SPRINT);
        TOGGLES.put(bind("fullbright", GLFW.GLFW_KEY_G), FULLBRIGHT);
        TOGGLES.put(bind("aura", GLFW.GLFW_KEY_K), AURA);
        TOGGLES.put(bind("hud", GLFW.GLFW_KEY_H), HUD);
        zoomKey = bind("zoom", GLFW.GLFW_KEY_C);
        menuKey = bind("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            stopZoom(mc);
            return;
        }

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new PhantomMenu());
        }

        for (Map.Entry<KeyBinding, Module> e : TOGGLES.entrySet()) {
            while (e.getKey().wasPressed()) {
                Module m = e.getValue();
                m.toggle();
                msg(p, m.name, m.enabled);
            }
        }

        if (ZOOM.enabled && zoomKey.isPressed() && mc.currentScreen == null) {
            if (!zooming) { savedFov = mc.options.getFov().getValue(); zooming = true; }
            mc.options.getFov().setValue(ZOOM_FOV);
        } else {
            stopZoom(mc);
        }

        if (SPRINT.enabled && mc.options.forwardKey.isPressed() && !p.isUsingItem()
                && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }

        if (FLY.enabled) {
            p.getAbilities().allowFlying = true;
            p.getAbilities().setFlySpeed(0.08f);
        }

        if (SPEED.enabled && p.isOnGround()
                && (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed())) {
            Vec3d v = p.getVelocity();
            double h = Math.hypot(v.x, v.z);
            if (h > 0 && h < 0.45) {
                p.setVelocity(v.x * 1.25, v.y, v.z * 1.25);
            }
        }

        if (FULLBRIGHT.enabled && !p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            p.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
        }

        if (STEP.enabled) setStep(p, 1.0);

        if (AURA.enabled) {
            if (auraCooldown > 0) {
                auraCooldown--;
            } else {
                List<HostileEntity> mobs = new ArrayList<>(mc.world.getEntitiesByClass(
                        HostileEntity.class, p.getBoundingBox().expand(4.0),
                        e -> e.isAlive() && e.distanceTo(p) <= 4.0f));
                mobs.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)))
                        .ifPresent(t -> {
                            mc.interactionManager.attackEntity(p, t);
                            p.swingHand(Hand.MAIN_HAND);
                        });
                auraCooldown = 10;
            }
        }
    }

    private static void disableFly(ClientPlayerEntity p) {
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
        }
        p.getAbilities().setFlySpeed(0.05f);
    }

    private static void setStep(ClientPlayerEntity p, double value) {
        EntityAttributeInstance a = p.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (a != null) a.setBaseValue(value);
    }

    private static void stopZoom(MinecraftClient mc) {
        if (zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private static void msg(ClientPlayerEntity p, String name, boolean on) {
        p.sendMessage(Text.literal(name + ": " + (on ? "\u00a7aLIGADO" : "\u00a7cDESLIGADO")), true);
    }

    private void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!HUD.enabled || mc.player == null || mc.options.hudHidden) return;

        var tr = mc.textRenderer;
        int x = 4, y = 4, s = 11;

        ctx.drawTextWithShadow(tr, "\u00a75Phantom \u00a7dClient \u00a77v1.2", x, y, 0xFFFFFFFF); y += s;
        ctx.drawTextWithShadow(tr, "FPS: " + mc.getCurrentFps(), x, y, 0xFFFFFFFF); y += s;
        ctx.drawTextWithShadow(tr, String.format("XYZ: %.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y, 0xFFFFFFFF); y += s;
        ctx.drawTextWithShadow(tr, "Direcao: " + mc.player.getHorizontalFacing().asString(),
                x, y, 0xFFFFFFFF); y += s + 2;

        for (Module m : MODULES) {
            if (m == HUD || m == ZOOM || !m.enabled) continue;
            ctx.drawTextWithShadow(tr, "\u00a7a+ " + m.name, x, y, 0xFFFFFFFF);
            y += s;
        }
    }
}
