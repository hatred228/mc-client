package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.hud.Notifications;
import com.example.bfb.setting.BooleanSetting;
import com.example.bfb.setting.NumberSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerWatcherModule extends CheatModule {

    public final BooleanSetting watchSelf = add(new BooleanSetting("Watch self", false));
    public final BooleanSetting totemPop = add(new BooleanSetting("Totem pop", true));
    public final BooleanSetting shieldBreak = add(new BooleanSetting("Shield break", true));
    public final BooleanSetting helmetBreak = add(new BooleanSetting("Helmet break", true));
    public final BooleanSetting totemSwap = add(new BooleanSetting("Totem swap", false));
    public final BooleanSetting potionDrink = add(new BooleanSetting("Potion drink", true));
    public final BooleanSetting criticalHp = add(new BooleanSetting("Critical HP", true));
    public final NumberSetting criticalThreshold = add(new NumberSetting("Critical HP %", 30, 5, 60, 5));
    public final BooleanSetting showName = add(new BooleanSetting("Show name", true));

    private final Map<UUID, WatchState> states = new HashMap<>();

    public PlayerWatcherModule() {
        super("PlayerWatcher", "Notifications when players pop totems, break shields, etc.",
                Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        states.clear();
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        if (client.world == null || client.player == null) return;

        long now = System.currentTimeMillis();

        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player && !watchSelf.get()) continue;
            if (!p.isAlive()) continue;

            WatchState st = states.computeIfAbsent(p.getUuid(), k -> new WatchState());
            updatePlayer(p, st, now);
        }

        if (states.size() > 32) {
            states.entrySet().removeIf(e -> client.world.getPlayerByUuid(e.getKey()) == null);
        }
    }

    private int colorOf(String key, int fallback) {
        try {
            VisualsModule vm = VisualsModule.get();
            if (vm == null) return fallback;
            return 0xFF000000 | (vm.notifications.colorRgb(key) & 0xFFFFFF);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private void updatePlayer(PlayerEntity p, WatchState st, long now) {
        String name = p.getGameProfile().name();
        if (name == null) name = "?";

        int totemCount = countItem(p, Items.TOTEM_OF_UNDYING);
        float hp = p.getHealth();
        float absorption = p.getAbsorptionAmount();

        boolean absorbed = absorption > st.lastAbsorption + 4f;
        boolean tookDamage = hp < st.lastHealth - 1f;

        if (totemPop.get() && totemCount < st.lastTotemCount && (absorbed || tookDamage)) {
            push(name + " popped totem", colorOf("Totem pop color", 0xFFFFC85F));
        } else if (totemSwap.get() && totemCount < st.lastTotemCount && !absorbed && !tookDamage) {
            push(name + " moved totem", colorOf("Totem swap color", 0xFF9AA0B4));
        }

        if (shieldBreak.get()) {
            ItemStack offHand = p.getOffHandStack();
            ItemStack mainHand = p.getMainHandStack();
            boolean hasShield = offHand.getItem() instanceof ShieldItem
                    || mainHand.getItem() instanceof ShieldItem;
            if (hasShield) {
                ItemStack shield = offHand.getItem() instanceof ShieldItem ? offHand : mainHand;
                boolean onCooldown = p.getItemCooldownManager().isCoolingDown(shield);
                if (onCooldown && !st.lastShieldCooldown) {
                    push(name + " shield broke", colorOf("Shield break color", 0xFFFFFFFF));
                }
                st.lastShieldCooldown = onCooldown;
            } else {
                st.lastShieldCooldown = false;
            }
        }

        if (helmetBreak.get()) {
            ItemStack helmet = p.getEquippedStack(EquipmentSlot.HEAD);
            if (st.lastHelmet != null && !st.lastHelmet.isEmpty() && helmet.isEmpty()
                    && tookDamage && st.lastHelmet.isDamageable()) {
                push(name + " lost helmet", colorOf("Helmet break color", 0xFFB0B5C0));
            }
            st.lastHelmet = helmet.copy();
        }

        if (potionDrink.get()) {
            boolean using = p.isUsingItem();
            ItemStack active = p.getActiveItem();
            boolean activeIsPotion = active.isOf(Items.POTION)
                    || active.isOf(Items.SPLASH_POTION)
                    || active.isOf(Items.LINGERING_POTION);
            if (st.lastUsingPotion && !using && st.lastPotionWasActive) {
                push(name + " drank potion", colorOf("Potion drink color", 0xFFB14DFF));
            }
            st.lastUsingPotion = using;
            st.lastPotionWasActive = using && activeIsPotion;
        }

        if (criticalHp.get()) {
            float maxHp = Math.max(1f, p.getMaxHealth());
            float ratio = hp / maxHp;
            boolean isCrit = ratio < criticalThreshold.getFloat() / 100f && hp > 0;
            if (isCrit && !st.lastCritical) {
                push(name + " critical HP " + Math.round(hp) + "hp",
                        colorOf("Critical HP color", 0xFFFF5050));
            }
            st.lastCritical = isCrit;
        }

        st.lastTotemCount = totemCount;
        st.lastHealth = hp;
        st.lastAbsorption = absorption;
    }

    private void push(String text, int color) {
        Notifications.push(text, color);
    }

    private int countItem(PlayerEntity p, net.minecraft.item.Item item) {
        int count = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && s.isOf(item)) count += s.getCount();
        }
        return count;
    }

    private static final class WatchState {
        int lastTotemCount = 0;
        float lastHealth = 20f;
        float lastAbsorption = 0f;
        boolean lastShieldCooldown = false;
        ItemStack lastHelmet = ItemStack.EMPTY;
        boolean lastUsingPotion = false;
        boolean lastPotionWasActive = false;
        boolean lastCritical = false;
    }
}