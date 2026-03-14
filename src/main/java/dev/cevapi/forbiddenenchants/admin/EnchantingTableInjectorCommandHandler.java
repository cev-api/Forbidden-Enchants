package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EnchantingTableInjectorCommandHandler {
    private static final List<String> SUBCOMMANDS = List.of("help", "status", "list", "enable", "disable", "gui", "add", "remove", "clear", "xp");
    private static final List<String> CHANCE_SUGGESTIONS = List.of("0.5", "1", "2.5", "5", "10", "15", "25", "33.3", "50", "75", "100");

    private final ForbiddenEnchantsPlugin plugin;

    EnchantingTableInjectorCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    boolean handleCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status", "list" -> {
                sendStatus(sender);
                yield true;
            }
            case "enable" -> {
                plugin.setEnchantingTableInjectorEnabled(true);
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender, "Enchanting-table injector enabled.");
                yield true;
            }
            case "disable" -> {
                plugin.setEnchantingTableInjectorEnabled(false);
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender, "Enchanting-table injector disabled.");
                yield true;
            }
            case "gui", "menu" -> {
                if (args.length > 3) {
                    plugin.sendFeError(sender, msg("usage_gui", "Usage: /fe enchanting gui [player]"));
                    yield true;
                }
                Player target = resolveTarget(sender, args.length == 3 ? args[2] : null);
                if (target == null) {
                    yield true;
                }
                plugin.openEnchantingTableInjectorMenu(target, 0);
                if (!sender.equals(target)) {
                    plugin.sendFeSuccess(sender, "Opened enchanting-table injector editor for " + target.getName() + ".");
                }
                yield true;
            }
            case "add" -> {
                if (args.length != 5) {
                    plugin.sendFeError(sender, msg("usage_add", "Usage: /fe enchanting add <enchant> <level> <chance>"));
                    yield true;
                }
                EnchantType type = parseEnchantType(sender, args[2]);
                if (type == null) {
                    yield true;
                }
                int level = parseLevel(sender, type, args[3]);
                if (level < 1) {
                    yield true;
                }
                Double chance = parseChance(sender, args[4]);
                if (chance == null) {
                    yield true;
                }
                upsertEntry(new EnchantingTableBookEntry(type, level, chance));
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender,
                        "Configured enchanting-table book: "
                                + type.displayName + " " + RomanNumeralUtil.toRoman(level)
                                + " @ " + StructureInjectorUtil.formatPercent(chance) + ".");
                yield true;
            }
            case "remove", "delete" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, msg("usage_remove", "Usage: /fe enchanting remove <index>"));
                    yield true;
                }
                int index = parseIndex(sender, args[2]);
                List<EnchantingTableBookEntry> sorted = sortedEntries();
                if (index < 1 || index > sorted.size()) {
                    plugin.sendFeError(sender, msg("index_out_of_range", "Index out of range. Use /fe enchanting list."));
                    yield true;
                }
                EnchantingTableBookEntry removed = sorted.get(index - 1);
                plugin.enchantingTableInjectorBooks().removeIf(entry -> entry.type() == removed.type() && entry.level() == removed.level());
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender,
                        "Removed enchanting-table book #" + index + ": "
                                + removed.type().displayName + " " + RomanNumeralUtil.toRoman(removed.level()) + ".");
                yield true;
            }
            case "clear" -> {
                plugin.enchantingTableInjectorBooks().clear();
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender, "Cleared all configured enchanting-table books.");
                yield true;
            }
            case "xp", "cost" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, msg("usage_xp", "Usage: /fe enchanting xp <1-60>"));
                    yield true;
                }
                int xp = parseXpCost(sender, args[2]);
                if (xp < 1) {
                    yield true;
                }
                plugin.setEnchantingTableInjectorXpCost(xp);
                plugin.saveEnchantingTableInjectorSettings();
                plugin.sendFeSuccess(sender, "Set enchanting injector XP cost to " + xp + ".");
                yield true;
            }
            default -> {
                plugin.sendFeError(sender, msg("unknown_subcommand", "Unknown enchanting subcommand. Use /fe enchanting help"));
                yield true;
            }
        };
    }

    @NotNull List<String> tabComplete(@NotNull String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], SUBCOMMANDS, new ArrayList<>());
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("add")) {
                List<String> enchants = new ArrayList<>();
                for (EnchantType type : plugin.activeEnchantTypes()) {
                    if (!plugin.isRetiredEnchant(type) && !type.isAnvilOnlyUtilityBook()) {
                        enchants.add(type.arg);
                    }
                }
                return StringUtil.copyPartialMatches(args[2], enchants, new ArrayList<>());
            }
            if (sub.equals("remove") || sub.equals("delete")) {
                List<String> indexes = new ArrayList<>();
                for (int i = 1; i <= plugin.enchantingTableInjectorBooks().size(); i++) {
                    indexes.add(String.valueOf(i));
                }
                return StringUtil.copyPartialMatches(args[2], indexes, new ArrayList<>());
            }
            if (sub.equals("gui") || sub.equals("menu")) {
                List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            EnchantType type = EnchantType.fromArg(args[2]);
            if (type == null || plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
                return List.of();
            }
            List<String> levels = new ArrayList<>();
            for (int i = 1; i <= type.maxLevel; i++) {
                levels.add(String.valueOf(i));
            }
            return StringUtil.copyPartialMatches(args[3], levels, new ArrayList<>());
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            return StringUtil.copyPartialMatches(args[4], CHANCE_SUGGESTIONS, new ArrayList<>());
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("xp") || args[1].equalsIgnoreCase("cost"))) {
            return StringUtil.copyPartialMatches(args[2], List.of("10", "15", "20", "25", "30", "35", "40", "50", "60"), new ArrayList<>());
        }
        return List.of();
    }

    private void sendHelp(@NotNull CommandSender sender) {
        plugin.sendFeSuccess(sender, msg("help_header", "Enchanting Table Injector Commands:"));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_1", " - /fe enchanting status | list"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_2", " - /fe enchanting enable | disable"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_3", " - /fe enchanting gui [player]"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_4", " - /fe enchanting add <enchant> <level> <chance>"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_5", " - /fe enchanting remove <index>"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_6", " - /fe enchanting clear"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_7", " - /fe enchanting xp <1-60>"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    private void sendStatus(@NotNull CommandSender sender) {
        List<EnchantingTableBookEntry> entries = sortedEntries();
        plugin.sendFeSuccess(sender,
                "Enchanting-table injector is " + (plugin.isEnchantingTableInjectorEnabled() ? "enabled" : "disabled")
                        + " with " + entries.size() + " configured book(s). XP cost: " + plugin.getEnchantingTableInjectorXpCost() + ".");
        sender.sendMessage(net.kyori.adventure.text.Component.text(
                " Injection requires selected offer cost >= " + plugin.getEnchantingTableInjectorXpCost() + ".",
                net.kyori.adventure.text.format.NamedTextColor.GRAY
        ));
        for (int i = 0; i < entries.size(); i++) {
            EnchantingTableBookEntry entry = entries.get(i);
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    String.format(Locale.ROOT,
                            " #%d %s %s @ %s",
                            i + 1,
                            entry.type().arg,
                            RomanNumeralUtil.toRoman(entry.level()),
                            StructureInjectorUtil.formatPercent(entry.chancePercent())),
                    net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
    }

    private @NotNull List<EnchantingTableBookEntry> sortedEntries() {
        List<EnchantingTableBookEntry> entries = new ArrayList<>(plugin.enchantingTableInjectorBooks());
        entries.sort(Comparator
                .comparingInt((EnchantingTableBookEntry entry) -> entry.type().modelTypeIndex())
                .thenComparingInt(EnchantingTableBookEntry::level));
        return entries;
    }

    private void upsertEntry(@NotNull EnchantingTableBookEntry updated) {
        plugin.enchantingTableInjectorBooks().removeIf(entry -> entry.type() == updated.type() && entry.level() == updated.level());
        if (updated.chancePercent() > 0.0D) {
            plugin.enchantingTableInjectorBooks().add(updated);
        }
    }

    private @Nullable EnchantType parseEnchantType(@NotNull CommandSender sender, @NotNull String arg) {
        EnchantType type = EnchantType.fromArg(arg);
        if (type == null || plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
            plugin.sendFeError(sender, msg("unknown_enchant", "Unknown enchant: {enchant}", Map.of("enchant", arg)));
            return null;
        }
        return type;
    }

    private int parseLevel(@NotNull CommandSender sender, @NotNull EnchantType type, @NotNull String arg) {
        try {
            int level = Integer.parseInt(arg);
            if (level >= 1 && level <= type.maxLevel) {
                return level;
            }
        } catch (NumberFormatException ignored) {
        }
        plugin.sendFeError(sender,
                msg("level_out_of_range", "Level for {enchant} must be 1-{max}.",
                        Map.of("enchant", type.arg, "max", String.valueOf(type.maxLevel))));
        return -1;
    }

    private @Nullable Double parseChance(@NotNull CommandSender sender, @NotNull String arg) {
        Double chance = StructureInjectorUtil.parseChancePercent(arg);
        if (chance == null) {
            plugin.sendFeError(sender, msg("chance_range", "Chance must be 0-100."));
            return null;
        }
        return chance;
    }

    private int parseIndex(@NotNull CommandSender sender, @NotNull String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
            plugin.sendFeError(sender, msg("index_not_number", "Index must be a number."));
            return -1;
        }
    }

    private int parseXpCost(@NotNull CommandSender sender, @NotNull String arg) {
        try {
            int value = Integer.parseInt(arg);
            if (value >= 1 && value <= 60) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        plugin.sendFeError(sender, msg("xp_cost_range", "XP cost must be 1-60."));
        return -1;
    }

    private @Nullable Player resolveTarget(@NotNull CommandSender sender, @Nullable String arg) {
        if (arg != null && !arg.isBlank()) {
            Player target = Bukkit.getPlayer(arg);
            if (target == null) {
                plugin.sendFeError(sender, msg("player_not_found", "Player not found: {player}", Map.of("player", arg)));
                return null;
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        plugin.sendFeError(sender, msg("console_needs_target", "Console must provide a target player."));
        return null;
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback) {
        return plugin.message("enchanting.command." + key, fallback);
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return plugin.message("enchanting.command." + key, fallback, placeholders);
    }
}
