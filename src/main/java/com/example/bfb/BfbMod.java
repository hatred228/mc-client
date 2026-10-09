package com.example.bfb;

import com.example.bfb.gui.ClickGuiScreen;
import com.example.bfb.modules.FriendsModule;
import com.example.bfb.modules.NukerModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class BfbMod implements ClientModInitializer {
    public static final String MOD_ID = "undetected";

    private static final Set<Integer> heldKeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        BfbRenderLayers.init();
        BfbRenderPipelines.init();
        ModuleManager.init();
        RenderEventHandler.register();
        HudRenderer.register();
        ScriptManager.load();

        ClientTickEvents.START_CLIENT_TICK.register(client -> ModuleManager.onStartTick());
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();

        // 1. Rotation manager — ДО модулей, чтобы holdTicks/release работали
        RotationManager.tick(client.player);

        // 2. Паника — работает всегда
        PanicManager.tick(client);

        // 3. Если паника активна — блокируем всё
        if (PanicManager.isPanicked()) {
            heldKeys.clear();
            RotationManager.clear();
            EntityCache.refresh(client, client.world != null ? client.world.getTime() : 0);
            ModuleManager.onTick();
            return;
        }

        // 4. GUI — Right Shift
        if (isPressedOnce(handle, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            else if (client.currentScreen instanceof ClickGuiScreen) client.setScreen(null);
        }

        // 5. Nuker positions
        if (isPressedOnce(handle, GLFW.GLFW_KEY_N)) mark(client, true);
        if (isPressedOnce(handle, GLFW.GLFW_KEY_M)) mark(client, false);

        // 6. Бинды модулей
        for (CheatModule module : ModuleManager.getModules()) {
            int key = module.getKeybind();
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;
            if (client.currentScreen instanceof ClickGuiScreen) continue;
            if (isPressedOnce(handle, key)) {
                if (module instanceof FriendsModule) {
                    FriendsModule.toggleUnderCrosshair();
                    continue;
                }
                module.toggle();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[" + module.getName() + "] "
                            + (module.isEnabled() ? "§aON" : "§cOFF")), true);
                }
            }
        }

        EntityCache.refresh(client, client.world != null ? client.world.getTime() : 0);
        ModuleManager.onTick();
    }

    private static boolean isPressedOnce(long handle, int key) {
        boolean down;
        if (key >= 0 && key <= 7) {
            down = GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS;
        } else {
            down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        }
        if (down && !heldKeys.contains(key)) {
            heldKeys.add(key);
            return true;
        }
        if (!down) heldKeys.remove(key);
        return false;
    }

    private static void mark(MinecraftClient client, boolean first) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        if (first) NukerModule.pos1 = hit.getBlockPos();
        else NukerModule.pos2 = hit.getBlockPos();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§aNuker pos" + (first ? "1" : "2") + ": §f"
                    + hit.getBlockPos().toShortString()), true);
        }
    }

    public static boolean bindsReady() { return true; }
    public static void registerBind(CheatModule module) {}
    public static void forget(CheatModule module) {}
    public static void updateBind(CheatModule module) {}
    public static MinecraftClient getClient() { return MinecraftClient.getInstance(); }
}