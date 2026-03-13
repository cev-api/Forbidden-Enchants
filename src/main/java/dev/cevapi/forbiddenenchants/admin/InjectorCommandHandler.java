package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class InjectorCommandHandler {
    private static final List<String> INJECTOR_SUBCOMMANDS = List.of(
            "help", "status", "list", "enable", "disable", "gui", "add", "set", "chance", "mode", "mystery", "notify", "remove", "clear", "defaultchance", "vault"
    );
    private static final List<String> VAULT_TARGETS = List.of("status", "enable", "disable", "normal", "ominous", "both");
    private static final List<String> CHANCE_SUGGESTIONS = List.of("1", "2.5", "5", "10", "15", "20", "25", "33.3", "50", "75", "100");
    private static final List<String> DEFAULT_CHANCE_SUGGESTIONS = List.of("1", "2.5", "5", "10", "20", "33.3", "50");
    private static final List<String> MODE_SUGGESTIONS = List.of(
            "book_only",
            "uncursed_book_only",
            "cursed_book_only",
            "mystery_book_only",
            "cursed_item_only",
            "item_only",
            "uncursed_item_only",
            "mystery_item_only",
            "cursed_only",
            "uncursed_only",
            "mystery_only",
            "all",
            "cycle",
            "cycletype",
            "cyclecurse",
            "cyclemystery",
            "status"
    );
    private static final List<String> LOOT_TYPE_SUGGESTIONS = List.of("books", "items", "all");
    private static final List<String> CURSE_STATE_SUGGESTIONS = List.of("all", "cursed", "uncursed");
    private static final List<String> MYSTERY_STATE_SUGGESTIONS = List.of("all", "mystery", "non_mystery");

    private final ForbiddenEnchantsPlugin plugin;

    InjectorCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
            InjectorMessagingUtil.sendHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status", "list" -> {
                InjectorMessagingUtil.sendStatus(
                        sender,
                        plugin.isStructureInjectorEnabled(),
                        plugin.getStructureInjectDefaultChance(),
                        plugin.isStructureInjectNotifyOnAdd(),
                        plugin.isTrialVaultInjectorEnabled(),
                        plugin.getTrialVaultNormalChance(),
                        plugin.getTrialVaultOminousChance(),
                        plugin.getTrialVaultNormalLootMode(),
                        plugin.getTrialVaultOminousLootMode(),
                        plugin.getTrialVaultNormalMysteryState(),
                        plugin.getTrialVaultOminousMysteryState(),
                        plugin.structureInjectChances(),
                        plugin.structureInjectLootModes(),
                        plugin.structureInjectMysteryStates()
                );
                return true;
            }
            case "vault" -> {
                if (args.length < 3 || args.length > 4) {
                    plugin.sendFeError(sender, "Usage: /fe injector vault <status|enable|disable|normal|ominous|both> [chance]");
                    return true;
                }
                String target = args[2].toLowerCase(Locale.ROOT);
                if (target.equals("status")) {
                    plugin.sendFeSuccess(sender,
                            "Vault injector: "
                                    + (plugin.isTrialVaultInjectorEnabled() ? "enabled" : "disabled")
                                    + " | normal=" + StructureInjectorUtil.formatPercent(plugin.getTrialVaultNormalChance())
                                    + " [" + plugin.getTrialVaultNormalLootMode().id() + "+" + plugin.getTrialVaultNormalMysteryState().id() + "]"
                                    + " | ominous=" + StructureInjectorUtil.formatPercent(plugin.getTrialVaultOminousChance())
                                    + " [" + plugin.getTrialVaultOminousLootMode().id() + "+" + plugin.getTrialVaultOminousMysteryState().id() + "]");
                    return true;
                }
                if (target.equals("enable")) {
                    plugin.setTrialVaultInjectorEnabled(true);
                    plugin.saveStructureInjectorSettings();
                    plugin.sendFeSuccess(sender, "Vault injector enabled.");
                    return true;
                }
                if (target.equals("disable")) {
                    plugin.setTrialVaultInjectorEnabled(false);
                    plugin.saveStructureInjectorSettings();
                    plugin.sendFeSuccess(sender, "Vault injector disabled.");
                    return true;
                }
                if (args.length != 4) {
                    plugin.sendFeError(sender, "Usage: /fe injector vault <normal|ominous|both> <0-100>");
                    return true;
                }
                Double chance = StructureInjectorUtil.parseChancePercent(args[3]);
                if (chance == null) {
                    plugin.sendFeError(sender, "Invalid chance: " + args[3]);
                    return true;
                }
                switch (target) {
                    case "normal" -> {
                        plugin.setTrialVaultNormalChance(chance);
                        plugin.setTrialVaultInjectorEnabled(true);
                    }
                    case "ominous" -> {
                        plugin.setTrialVaultOminousChance(chance);
                        plugin.setTrialVaultInjectorEnabled(true);
                    }
                    case "both" -> {
                        plugin.setTrialVaultNormalChance(chance);
                        plugin.setTrialVaultOminousChance(chance);
                        plugin.setTrialVaultInjectorEnabled(true);
                    }
                    default -> {
                        plugin.sendFeError(sender, "Unknown vault target: " + args[2]);
                        return true;
                    }
                }
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender,
                        "Vault injector updated: normal="
                                + StructureInjectorUtil.formatPercent(plugin.getTrialVaultNormalChance())
                                + ", ominous="
                                + StructureInjectorUtil.formatPercent(plugin.getTrialVaultOminousChance())
                                + " (enabled).");
                return true;
            }
            case "enable" -> {
                plugin.setStructureInjectorEnabled(true);
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Structure injector enabled.");
                return true;
            }
            case "disable" -> {
                plugin.setStructureInjectorEnabled(false);
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Structure injector disabled.");
                return true;
            }
            case "gui", "menu" -> {
                if (args.length > 3) {
                    plugin.sendFeError(sender, "Usage: /fe injector gui [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args.length == 3 ? args[2] : null);
                if (target == null) {
                    return true;
                }
                plugin.openInjectorConfigMenu(target);
                if (!sender.equals(target)) {
                    plugin.sendFeSuccess(sender, "Opened injector editor for " + target.getName() + ".");
                }
                return true;
            }
            case "defaultchance", "default" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, "Usage: /fe injector defaultchance <0-100>");
                    return true;
                }
                Double chance = StructureInjectorUtil.parseChancePercent(args[2]);
                if (chance == null) {
                    plugin.sendFeError(sender, "Invalid chance: " + args[2]);
                    return true;
                }
                plugin.setStructureInjectDefaultChance(chance);
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Default injector chance set to " + StructureInjectorUtil.formatPercent(chance) + ".");
                return true;
            }
            case "clear" -> {
                plugin.structureInjectChances().clear();
                plugin.structureInjectLootModes().clear();
                plugin.structureInjectMysteryStates().clear();
                plugin.setTrialVaultNormalLootMode(InjectorLootMode.ALL);
                plugin.setTrialVaultOminousLootMode(InjectorLootMode.ALL);
                plugin.setTrialVaultNormalMysteryState(InjectorMysteryState.ALL);
                plugin.setTrialVaultOminousMysteryState(InjectorMysteryState.ALL);
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Cleared all configured structures.");
                return true;
            }
            case "add" -> {
                if (args.length < 3 || args.length > 4) {
                    plugin.sendFeError(sender, "Usage: /fe injector add <structure[,structure2...]> [chance]");
                    return true;
                }
                Double chance = args.length == 4 ? StructureInjectorUtil.parseChancePercent(args[3]) : plugin.getStructureInjectDefaultChance();
                if (chance == null) {
                    plugin.sendFeError(sender, "Invalid chance: " + args[3]);
                    return true;
                }
                List<Structure> structures = StructureInjectorUtil.parseStructureList(args[2]);
                if (structures.isEmpty()) {
                    plugin.sendFeError(sender, "No valid structures in: " + args[2]);
                    return true;
                }
                for (Structure structure : structures) {
                    plugin.structureInjectChances().put(structure.getKey(), chance);
                }
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Added/updated " + structures.size() + " structure(s) at " + StructureInjectorUtil.formatPercent(chance) + ".");
                return true;
            }
            case "set", "chance" -> {
                if (args.length != 4) {
                    plugin.sendFeError(sender, "Usage: /fe injector set <structure> <0-100>");
                    return true;
                }
                Structure structure = StructureInjectorUtil.parseStructure(args[2]);
                if (structure == null) {
                    plugin.sendFeError(sender, "Unknown structure: " + args[2]);
                    return true;
                }
                Double chance = StructureInjectorUtil.parseChancePercent(args[3]);
                if (chance == null) {
                    plugin.sendFeError(sender, "Invalid chance: " + args[3]);
                    return true;
                }
                plugin.structureInjectChances().put(structure.getKey(), chance);
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Set " + structure.getKey() + " to " + StructureInjectorUtil.formatPercent(chance) + ".");
                return true;
            }
            case "mode", "mystery" -> {
                if (args.length < 4 || args.length > 6) {
                    if (sub.equals("mystery")) {
                        plugin.sendFeError(sender, "Usage: /fe injector mystery <structure> <on|off|toggle|status>");
                    } else {
                        plugin.sendFeError(sender, "Usage: /fe injector mode <structure> <mode|cycletype|cyclecurse|cyclemystery|status>");
                    }
                    return true;
                }
                Structure structure = StructureInjectorUtil.parseStructure(args[2]);
                if (structure == null) {
                    plugin.sendFeError(sender, "Unknown structure: " + args[2]);
                    return true;
                }
                NamespacedKey key = structure.getKey();
                InjectorLootMode currentMode = plugin.structureInjectLootMode(key);
                InjectorMysteryState currentMysteryState = plugin.structureInjectMysteryState(key);

                if (sub.equals("mode") && args.length == 5) {
                    InjectorLootMode.LootType type = InjectorLootMode.LootType.fromString(args[3]);
                    InjectorLootMode.CurseState curseState = InjectorLootMode.CurseState.fromString(args[4]);
                    if (type != null && curseState != null) {
                        InjectorLootMode parsed = InjectorLootMode.fromParts(type, curseState);
                        plugin.setStructureInjectLootMode(key, parsed);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + parsed.id() + " + " + currentMysteryState.id() + ".");
                        return true;
                    }
                }

                if (sub.equals("mode") && args.length == 6) {
                    InjectorLootMode.LootType type = InjectorLootMode.LootType.fromString(args[3]);
                    InjectorLootMode.CurseState curseState = InjectorLootMode.CurseState.fromString(args[4]);
                    InjectorMysteryState mysteryState = InjectorMysteryState.fromString(args[5]);
                    if (type == null || curseState == null || mysteryState == null) {
                        plugin.sendFeError(sender, "Usage: /fe injector mode <structure> <books|items|all> <all|cursed|uncursed> <all|mystery|non_mystery>");
                        return true;
                    }
                    InjectorLootMode parsed = InjectorLootMode.fromParts(type, curseState);
                    plugin.setStructureInjectLootMode(key, parsed);
                    plugin.setStructureInjectMysteryState(key, mysteryState);
                    plugin.saveStructureInjectorSettings();
                    plugin.sendFeSuccess(sender, "Set " + key + " to " + parsed.id() + " + " + mysteryState.id() + ".");
                    return true;
                }

                if (args.length != 4) {
                    plugin.sendFeError(sender, "Usage: /fe injector mode <structure> <mode|cycletype|cyclecurse|cyclemystery|status>");
                    return true;
                }

                String rawMode = args[3].toLowerCase(Locale.ROOT).trim()
                        .replace('-', '_')
                        .replace(' ', '_')
                        .replace("+", "");
                if (sub.equals("mystery")) {
                    switch (rawMode) {
                        case "status" -> plugin.sendFeSuccess(sender, key + " mystery state is " + currentMysteryState.id() + ".");
                        case "on" -> {
                            plugin.setStructureInjectMysteryState(key, InjectorMysteryState.MYSTERY_ONLY);
                            plugin.saveStructureInjectorSettings();
                            plugin.sendFeSuccess(sender, "Set " + key + " mystery state to mystery_only.");
                        }
                        case "off" -> {
                            plugin.setStructureInjectMysteryState(key, InjectorMysteryState.ALL);
                            plugin.saveStructureInjectorSettings();
                            plugin.sendFeSuccess(sender, "Set " + key + " mystery state to all.");
                        }
                        case "toggle" -> {
                            InjectorMysteryState next = currentMysteryState == InjectorMysteryState.MYSTERY_ONLY
                                    ? InjectorMysteryState.ALL
                                    : InjectorMysteryState.MYSTERY_ONLY;
                            plugin.setStructureInjectMysteryState(key, next);
                            plugin.saveStructureInjectorSettings();
                            plugin.sendFeSuccess(sender, "Set " + key + " mystery state to " + next.id() + ".");
                        }
                        default -> plugin.sendFeError(sender, "Unknown mode: " + args[3] + " (use on|off|toggle|status)");
                    }
                    return true;
                }

                switch (rawMode) {
                    case "status" -> plugin.sendFeSuccess(sender, key + " mode is " + currentMode.id() + " + " + currentMysteryState.id() + ".");
                    case "cycle", "cycletype", "type", "q" -> {
                        InjectorLootMode next = currentMode.nextType();
                        plugin.setStructureInjectLootMode(key, next);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + next.id() + " + " + currentMysteryState.id() + ".");
                    }
                    case "cyclestate", "cyclecurse", "state", "ctrlq", "controlq" -> {
                        InjectorLootMode next = currentMode.nextCurse();
                        plugin.setStructureInjectLootMode(key, next);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + next.id() + " + " + currentMysteryState.id() + ".");
                    }
                    case "cyclemystery", "shiftq" -> {
                        InjectorMysteryState next = currentMysteryState.next();
                        plugin.setStructureInjectMysteryState(key, next);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + currentMode.id() + " + " + next.id() + ".");
                    }
                    case "on" -> {
                        plugin.setStructureInjectMysteryState(key, InjectorMysteryState.MYSTERY_ONLY);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + currentMode.id() + " + mystery_only.");
                    }
                    case "off" -> {
                        plugin.setStructureInjectMysteryState(key, InjectorMysteryState.ALL);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + currentMode.id() + " + all.");
                    }
                    case "toggle" -> {
                        InjectorMysteryState next = currentMysteryState == InjectorMysteryState.MYSTERY_ONLY
                                ? InjectorMysteryState.ALL
                                : InjectorMysteryState.MYSTERY_ONLY;
                        plugin.setStructureInjectMysteryState(key, next);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + currentMode.id() + " + " + next.id() + ".");
                    }
                    default -> {
                        InjectorLootMode parsed = InjectorLootMode.fromString(rawMode);
                        if (parsed == null) {
                            plugin.sendFeError(sender, "Unknown mode: " + args[3]);
                            return true;
                        }
                        InjectorMysteryState mysteryAlias = InjectorMysteryState.fromLegacyModeAlias(rawMode);
                        plugin.setStructureInjectLootMode(key, parsed);
                        if (mysteryAlias != null) {
                            plugin.setStructureInjectMysteryState(key, mysteryAlias);
                            currentMysteryState = mysteryAlias;
                        }
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Set " + key + " to " + parsed.id() + " + " + currentMysteryState.id() + ".");
                    }
                }
                return true;
            }
            case "notify" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, "Usage: /fe injector notify <on|off|toggle|status>");
                    return true;
                }
                String mode = args[2].toLowerCase(Locale.ROOT);
                boolean current = plugin.isStructureInjectNotifyOnAdd();
                switch (mode) {
                    case "status" -> plugin.sendFeSuccess(sender, "Chest add message is " + (current ? "ON" : "OFF") + ".");
                    case "on" -> {
                        plugin.setStructureInjectNotifyOnAdd(true);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Chest add message enabled.");
                    }
                    case "off" -> {
                        plugin.setStructureInjectNotifyOnAdd(false);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Chest add message disabled.");
                    }
                    case "toggle" -> {
                        plugin.setStructureInjectNotifyOnAdd(!current);
                        plugin.saveStructureInjectorSettings();
                        plugin.sendFeSuccess(sender, "Chest add message " + (!current ? "enabled" : "disabled") + ".");
                    }
                    default -> plugin.sendFeError(sender, "Unknown mode: " + args[2] + " (use on|off|toggle|status)");
                }
                return true;
            }
            case "remove", "del", "delete" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, "Usage: /fe injector remove <structure[,structure2...]>");
                    return true;
                }
                List<Structure> structures = StructureInjectorUtil.parseStructureList(args[2]);
                if (structures.isEmpty()) {
                    plugin.sendFeError(sender, "No valid structures in: " + args[2]);
                    return true;
                }
                int removed = 0;
                for (Structure structure : structures) {
                    if (plugin.structureInjectChances().remove(structure.getKey()) != null) {
                        removed++;
                    }
                    plugin.setStructureInjectLootMode(structure.getKey(), InjectorLootMode.ALL);
                    plugin.setStructureInjectMysteryState(structure.getKey(), InjectorMysteryState.ALL);
                }
                plugin.saveStructureInjectorSettings();
                plugin.sendFeSuccess(sender, "Removed " + removed + " structure(s) from injector settings.");
                return true;
            }
            default -> {
                plugin.sendFeError(sender, "Unknown injector subcommand. Use /fe injector help");
                return true;
            }
        }
    }

    @NotNull List<String> tabComplete(@NotNull String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], INJECTOR_SUBCOMMANDS, new ArrayList<>());
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("vault")) {
                return StringUtil.copyPartialMatches(args[2], VAULT_TARGETS, new ArrayList<>());
            }
            if (sub.equals("gui")) {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
            }
            if (sub.equals("add") || sub.equals("remove")) {
                return structureCsvSuggestions(args[2]);
            }
            if (sub.equals("set") || sub.equals("chance")) {
                return StringUtil.copyPartialMatches(args[2], structureNameSuggestions(), new ArrayList<>());
            }
            if (sub.equals("mystery") || sub.equals("mode")) {
                return StringUtil.copyPartialMatches(args[2], structureNameSuggestions(), new ArrayList<>());
            }
            if (sub.equals("defaultchance")) {
                return StringUtil.copyPartialMatches(args[2], DEFAULT_CHANCE_SUGGESTIONS, new ArrayList<>());
            }
            if (sub.equals("notify")) {
                return StringUtil.copyPartialMatches(args[2], List.of("on", "off", "toggle", "status"), new ArrayList<>());
            }
        }
        if (args.length == 4) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("vault")
                    && (args[2].equalsIgnoreCase("normal")
                    || args[2].equalsIgnoreCase("ominous")
                    || args[2].equalsIgnoreCase("both"))) {
                return StringUtil.copyPartialMatches(args[3], CHANCE_SUGGESTIONS, new ArrayList<>());
            }
            if (sub.equals("add") || sub.equals("set") || sub.equals("chance")) {
                return StringUtil.copyPartialMatches(args[3], CHANCE_SUGGESTIONS, new ArrayList<>());
            }
            if (sub.equals("mystery")) {
                return StringUtil.copyPartialMatches(args[3], List.of("on", "off", "toggle", "status"), new ArrayList<>());
            }
            if (sub.equals("mode")) {
                List<String> suggestions = new ArrayList<>(MODE_SUGGESTIONS);
                suggestions.addAll(LOOT_TYPE_SUGGESTIONS);
                return StringUtil.copyPartialMatches(args[3], suggestions, new ArrayList<>());
            }
        }
        if (args.length == 5) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("mode")
                    && InjectorLootMode.LootType.fromString(args[3]) != null) {
                return StringUtil.copyPartialMatches(args[4], CURSE_STATE_SUGGESTIONS, new ArrayList<>());
            }
        }
        if (args.length == 6) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("mode")
                    && InjectorLootMode.LootType.fromString(args[3]) != null
                    && InjectorLootMode.CurseState.fromString(args[4]) != null) {
                return StringUtil.copyPartialMatches(args[5], MYSTERY_STATE_SUGGESTIONS, new ArrayList<>());
            }
        }
        return List.of();
    }

    private @Nullable Player resolveTarget(@NotNull CommandSender sender, @Nullable String arg) {
        if (arg != null && !arg.isBlank()) {
            Player target = Bukkit.getPlayer(arg);
            if (target == null) {
                plugin.sendFeError(sender, "Player not found: " + arg);
                return null;
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        plugin.sendFeError(sender, "Console must provide a target player.");
        return null;
    }

    private @NotNull List<String> structureNameSuggestions() {
        List<Structure> structures = plugin.allStructures();
        List<String> names = new ArrayList<>(structures.size() * 2);
        for (Structure structure : structures) {
            names.add(structure.getKey().toString());
            names.add(structure.getKey().getKey());
        }
        return names;
    }

    private @NotNull List<String> structureCsvSuggestions(@NotNull String input) {
        String[] parts = input.split(",", -1);
        String prefix = "";
        if (parts.length > 1) {
            prefix = String.join(",", List.of(parts).subList(0, parts.length - 1)) + ",";
        }
        String tail = parts[parts.length - 1];
        List<String> completions = StringUtil.copyPartialMatches(tail, structureNameSuggestions(), new ArrayList<>());
        List<String> result = new ArrayList<>(completions.size());
        for (String completion : completions) {
            result.add(prefix + completion);
        }
        return result;
    }
}

