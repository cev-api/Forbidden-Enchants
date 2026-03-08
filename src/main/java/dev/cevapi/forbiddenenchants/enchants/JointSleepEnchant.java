package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class JointSleepEnchant extends BaseForbiddenEnchant {
    public JointSleepEnchant() {
        super("joint_sleep",
                "joint_sleep_level",
                "Joint Sleep",
                ArmorSlot.POTION,
                1,
                NamedTextColor.BLUE,
                List.of("joint_sleep", "jointsleep", "sleep"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to teleport to the nearest other-player bed. If none exist, place 2 beds ahead or give beds.";
    }
}
