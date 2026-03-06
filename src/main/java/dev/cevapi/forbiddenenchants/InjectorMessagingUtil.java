package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InjectorMessagingUtil {
    private InjectorMessagingUtil() {
    }

    public static void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("Structure Injector", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Commands:", NamedTextColor.YELLOW));
        sendHelpLine(sender, "/fe injector status", "Show enabled state, default chance and structure list.");
        sendHelpLine(sender, "/fe injector vault <status|enable|disable|normal|ominous|both> [chance]", "Configure trial vault/ominous vault injection.");
        sendHelpLine(sender, "/fe injector enable|disable", "Toggle structure-based loot injection.");
        sendHelpLine(sender, "/fe injector gui [player]", "Open visual structure/chance editor.");
        sendHelpLine(sender, "/fe injector add <s1,s2,...> [chance]", "Add one or many structures using comma list.");
        sendHelpLine(sender, "/fe injector set <structure> <chance>", "Set exact % chance for a structure.");
        sendHelpLine(sender, "/fe injector mode <structure> <mode|cycletype|cyclecurse|cyclemystery|status>", "Set/query loot mode for a structure.");
        sendHelpLine(sender, "/fe injector mode <structure> <books|items|all> <all|cursed|uncursed> [all|mystery|non_mystery]", "Set loot type + curse + mystery states directly.");
        sendHelpLine(sender, "/fe injector mystery <structure> <on|off|toggle|status>", "Legacy alias for mystery-state toggling.");
        sendHelpLine(sender, "/fe injector notify <on|off|toggle|status>", "Set/query chest add action-bar messages.");
        sendHelpLine(sender, "/fe injector remove <s1,s2,...>", "Remove one or many structures.");
        sendHelpLine(sender, "/fe injector defaultchance <chance>", "Default % used by add/gui operations.");
        sendHelpLine(sender, "/fe injector clear", "Remove all configured structures.");
    }

    private static void sendHelpLine(@NotNull CommandSender sender, @NotNull String command, @NotNull String description) {
        sender.sendMessage(Component.text(" - ", NamedTextColor.YELLOW)
                .append(Component.text(command, NamedTextColor.AQUA))
                .append(Component.text(" -> ", NamedTextColor.YELLOW))
                .append(Component.text(description, NamedTextColor.WHITE)));
    }

    public static void sendStatus(@NotNull CommandSender sender,
                                  boolean structureInjectorEnabled,
                                  double structureInjectDefaultChance,
                                  boolean structureInjectNotifyOnAdd,
                                  boolean trialVaultInjectorEnabled,
                                  double trialVaultNormalChance,
                                  double trialVaultOminousChance,
                                  @NotNull InjectorLootMode trialVaultNormalLootMode,
                                  @NotNull InjectorLootMode trialVaultOminousLootMode,
                                  @NotNull InjectorMysteryState trialVaultNormalMysteryState,
                                  @NotNull InjectorMysteryState trialVaultOminousMysteryState,
                                  @NotNull Map<NamespacedKey, Double> structureInjectChances,
                                  @NotNull Map<NamespacedKey, InjectorLootMode> structureInjectLootModes,
                                  @NotNull Map<NamespacedKey, InjectorMysteryState> structureInjectMysteryStates) {
        sender.sendMessage(Component.text("Structure Injector: ", NamedTextColor.GOLD)
                .append(Component.text(structureInjectorEnabled ? "ENABLED" : "DISABLED",
                        structureInjectorEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("Default chance: ", NamedTextColor.YELLOW)
                .append(Component.text(StructureInjectorUtil.formatPercent(structureInjectDefaultChance), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Chest add message: ", NamedTextColor.YELLOW)
                .append(Component.text(structureInjectNotifyOnAdd ? "ENABLED" : "DISABLED",
                        structureInjectNotifyOnAdd ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("Vault injector: ", NamedTextColor.YELLOW)
                .append(Component.text(trialVaultInjectorEnabled ? "ENABLED" : "DISABLED",
                        trialVaultInjectorEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.YELLOW))
                .append(Component.text("normal=", NamedTextColor.YELLOW))
                .append(Component.text(StructureInjectorUtil.formatPercent(trialVaultNormalChance), NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.YELLOW))
                .append(Component.text("ominous=", NamedTextColor.YELLOW))
                .append(Component.text(StructureInjectorUtil.formatPercent(trialVaultOminousChance), NamedTextColor.WHITE))
                .append(Component.text(" | n_mode=", NamedTextColor.YELLOW))
                .append(Component.text(trialVaultNormalLootMode.id() + "+" + trialVaultNormalMysteryState.id(), NamedTextColor.WHITE))
                .append(Component.text(" | o_mode=", NamedTextColor.YELLOW))
                .append(Component.text(trialVaultOminousLootMode.id() + "+" + trialVaultOminousMysteryState.id(), NamedTextColor.WHITE)));

        if (structureInjectChances.isEmpty()) {
            sender.sendMessage(Component.text("No structures configured.", NamedTextColor.YELLOW));
            return;
        }
        List<Map.Entry<NamespacedKey, Double>> entries = new ArrayList<>(structureInjectChances.entrySet());
        entries.sort((left, right) -> left.getKey().toString().compareToIgnoreCase(right.getKey().toString()));
        for (Map.Entry<NamespacedKey, Double> entry : entries) {
            InjectorLootMode mode = structureInjectLootModes.getOrDefault(entry.getKey(), InjectorLootMode.ALL);
            InjectorMysteryState mysteryState = structureInjectMysteryStates.getOrDefault(entry.getKey(), InjectorMysteryState.ALL);
            sender.sendMessage(Component.text("- ", NamedTextColor.YELLOW)
                    .append(Component.text(entry.getKey().toString(), NamedTextColor.AQUA))
                    .append(Component.text(" -> ", NamedTextColor.YELLOW))
                    .append(Component.text(StructureInjectorUtil.formatPercent(entry.getValue()), NamedTextColor.WHITE))
                    .append(mode != InjectorLootMode.ALL || mysteryState != InjectorMysteryState.ALL
                            ? Component.text(" | " + mode.id() + "+" + mysteryState.id(), NamedTextColor.LIGHT_PURPLE)
                            : Component.empty()));
        }
    }
}

