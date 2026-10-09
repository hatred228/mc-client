package com.example.bfb.modules;

import com.example.bfb.Category;
import com.example.bfb.CheatModule;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class CoordsModule extends CheatModule {
    public CoordsModule() {
        super("Coords", "Coordinates, facing, nether conversion", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static List<String> lines(ClientPlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String face = switch (player.getHorizontalFacing()) {
            case SOUTH -> "South";
            case WEST -> "West";
            case EAST -> "East";
            default -> "North";
        };
        var key = player.getEntityWorld().getRegistryKey();
        String other;
        if (key == World.NETHER) other = String.format(Locale.US, "Overworld  %.0f  %.0f", x * 8, z * 8);
        else if (key == World.OVERWORLD) other = String.format(Locale.US, "Nether  %.0f  %.0f", x / 8.0, z / 8.0);
        else other = "End";
        return List.of(
                String.format(Locale.US, "XYZ  %.1f   %.1f   %.1f", x, y, z),
                face + "   " + other);
    }
}
