package com.example.bfb.modules;

import com.example.bfb.BfbMod;
import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import com.example.bfb.setting.BooleanSetting;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * Jesus — ходьба по воде и лаве.
 *
 * РАНЬШЕ: отправлял PositionAndOnGround(y+0.01, onGround=true) + setVelocity(v.x, 0.04, v.z).
 * Двойной флаг в одном тике — Intave/Vulcan палят моментально.
 *
 * ТЕПЕРЬ: только addVelocity. Никаких packets.
 *
 * Логика: если игрок над водой/лавой и velocity.y < 0 (падает в воду),
 * добавляем микро-импульс вверх. Не превышаем 0.02. Это медленно, но
 * безопасно. Скорость подъёма ~1 блок в 2-3 секунды.
 *
 * НЕ РАБОТАЕТ как "стою на воде". Только плавное скольжение.
 * Настоящий Jesus требует position spoof — а это flag на любом 1.21 AC.
 */
public class JesusModule extends CheatModule {
    public final BooleanSetting water = add(new BooleanSetting("Water", true));
    public final BooleanSetting lava = add(new BooleanSetting("Lava", false));

    public JesusModule() {
        super("Jesus", "Walk on water and lava (velocity-based)", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        var client = BfbMod.getClient();
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        // шифт = не Jesus, дай игроку нормально нырнуть
        if (player.isSneaking()) return;

        // проверяем: над водой/лавой или в ней
        boolean overWater = water.get() && isOverFluid(player, world, FluidTags.WATER);
        boolean overLava = lava.get() && isOverFluid(player, world, FluidTags.LAVA);

        if (!overWater && !overLava) return;

        // приземлились на поверхность — не трогаем
        if (player.isOnGround()) return;

        // velocity.y сейчас
        double yVel = player.getVelocity().y;

        // падаем — не помогаем, пусть упадёт в воду
        if (yVel < -0.1) return;

        // уже поднимаемся быстро — не помогаем (палимся)
        if (yVel > 0.05) return;

        // очень маленький импульс вверх
        player.addVelocity(0, 0.02, 0);

        // сбрасываем fall — иначе при скольжении на воде накапливается
        player.fallDistance = 0f;
    }

    /**
     * Проверяет: ноги или ниже находится в fluid (вода/лава).
     */
    private static boolean isOverFluid(net.minecraft.client.network.ClientPlayerEntity player,
                                       net.minecraft.world.World world,
                                       net.minecraft.registry.tag.TagKey<net.minecraft.fluid.Fluid> fluidTag) {
        // три точки: под ногами, на уровне ног, чуть ниже
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        BlockPos[] checks = {
                BlockPos.ofFloored(x, y - 0.1, z),
                BlockPos.ofFloored(x, y - 0.5, z),
                BlockPos.ofFloored(x, y, z)
        };

        for (BlockPos pos : checks) {
            if (world.getFluidState(pos).isIn(fluidTag)) return true;
        }
        return false;
    }
}