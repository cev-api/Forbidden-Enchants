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

final class LibrarianTradeCommandHandler {
    private static final List<String> SUBCOMMANDS = List.of("help", "status", "list", "enable", "disable", "gui", "add", "remove", "clear");
    private static final List<String> CHANCE_SUGGESTIONS = List.of("5", "10", "15", "25", "33.3", "50", "75", "100");
    private static final List<String> COST_SUGGESTIONS = List.of("1", "5", "8", "12", "16", "24", "32", "48", "64");

    private final ForbiddenEnchantsPlugin plugin;

    LibrarianTradeCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
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
                plugin.setLibrarianTradesEnabled(true);
                plugin.saveLibrarianTradeSettings();
                plugin.sendFeSuccess(sender, "Librarian trade blend enabled.");
                yield true;
            }
            case "disable" -> {
                plugin.setLibrarianTradesEnabled(false);
                plugin.saveLibrarianTradeSettings();
                plugin.sendFeSuccess(sender, "Librarian trade blend disabled.");
                yield true;
            }
            case "gui", "menu" -> {
                if (args.length > 3) {
                    plugin.sendFeError(sender, "Usage: /fe librarian gui [player]");
                    yield true;
                }
                Player target = resolveTarget(sender, args.length == 3 ? args[2] : null);
                if (target == null) {
                    yield true;
                }
                plugin.openLibrarianTradeMenu(target, 0);
                if (!sender.equals(target)) {
                    plugin.sendFeSuccess(sender, "Opened librarian trade editor for " + target.getName() + ".");
                }
                yield true;
            }
            case "add" -> {
                if (args.length < 6 || args.length > 7) {
                    plugin.sendFeError(sender, "Usage: /fe librarian add <enchant> <level> <chance> <emeralds> [books]");
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
                int emeralds = parseCost(sender, args[5], "Emerald cost");
                if (emeralds < 1) {
                    yield true;
                }
                int books = args.length == 7 ? parseBookCost(sender, args[6]) : 1;
                if (books < 0) {
                    yield true;
                }

                upsertTrade(new LibrarianTradeEntry(type, level, chance, emeralds, books));
                plugin.saveLibrarianTradeSettings();
                plugin.sendFeSuccess(sender,
                        "Configured librarian trade: "
                                + type.displayName + " " + RomanNumeralUtil.toRoman(level)
                                + " @ " + StructureInjectorUtil.formatPercent(chance)
                                + " for " + emeralds + " emerald(s)"
                                + (books > 0 ? " + " + books + " book(s)." : "."));
                yield true;
            }
            case "remove", "delete" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, "Usage: /fe librarian remove <index>");
                    yield true;
                }
                int index = parseIndex(sender, args[2]);
                List<LibrarianTradeEntry> sorted = sortedConfiguredTrades();
                if (index < 1 || index > sorted.size()) {
                    plugin.sendFeError(sender, "Index out of range. Use /fe librarian list.");
                    yield true;
                }
                LibrarianTradeEntry removed = sorted.get(index - 1);
                plugin.librarianTrades().removeIf(entry -> entry.type() == removed.type() && entry.level() == removed.level());
                plugin.saveLibrarianTradeSettings();
                plugin.sendFeSuccess(sender,
                        "Removed trade #" + index + ": "
                                + removed.type().displayName + " " + RomanNumeralUtil.toRoman(removed.level()) + ".");
                yield true;
            }
            case "clear" -> {
                plugin.librarianTrades().clear();
                plugin.saveLibrarianTradeSettings();
                plugin.sendFeSuccess(sender, "Cleared all configured librarian trades.");
                yield true;
            }
            default -> {
                plugin.sendFeError(sender, "Unknown librarian subcommand. Use /fe librarian help");
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
                for (int i = 1; i <= plugin.librarianTrades().size(); i++) {
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
            if (type == null || plugin.isRetiredEnchant(type)) {
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
        if (args.length == 6 && args[1].equalsIgnoreCase("add")) {
            return StringUtil.copyPartialMatches(args[5], COST_SUGGESTIONS, new ArrayList<>());
        }
        if (args.length == 7 && args[1].equalsIgnoreCase("add")) {
            return StringUtil.copyPartialMatches(args[6], List.of("0", "1", "2", "3", "4", "5", "8", "16"), new ArrayList<>());
        }
        return List.of();
    }

    private void sendHelp(@NotNull CommandSender sender) {
        plugin.sendFeSuccess(sender, "Librarian Trade Commands:");
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian status | list", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian enable | disable", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian gui [player]", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian add <enchant> <level> <chance> <emeralds> [books]", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian remove <index>", net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(" - /fe librarian clear", net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    private void sendStatus(@NotNull CommandSender sender) {
        List<LibrarianTradeEntry> entries = sortedConfiguredTrades();
        plugin.sendFeSuccess(sender,
                "Librarian blend is " + (plugin.isLibrarianTradesEnabled() ? "enabled" : "disabled")
                        + " with " + entries.size() + " configured trade(s).");
        for (int i = 0; i < entries.size(); i++) {
            LibrarianTradeEntry entry = entries.get(i);
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    String.format(Locale.ROOT,
                            " #%d %s %s @ %s -> %d emerald(s)%s",
                            i + 1,
                            entry.type().arg,
                            RomanNumeralUtil.toRoman(entry.level()),
                            StructureInjectorUtil.formatPercent(entry.chancePercent()),
                            entry.emeraldCost(),
                            entry.bookCost() > 0 ? " + " + entry.bookCost() + " book(s)" : ""),
                    net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
    }

    private @NotNull List<LibrarianTradeEntry> sortedConfiguredTrades() {
        List<LibrarianTradeEntry> entries = new ArrayList<>(plugin.librarianTrades());
        entries.sort(Comparator
                .comparingInt((LibrarianTradeEntry entry) -> entry.type().modelTypeIndex())
                .thenComparingInt(LibrarianTradeEntry::level));
        return entries;
    }

    private void upsertTrade(@NotNull LibrarianTradeEntry updated) {
        plugin.librarianTrades().removeIf(entry -> entry.type() == updated.type() && entry.level() == updated.level());
        if (updated.chancePercent() > 0.0D) {
            plugin.librarianTrades().add(updated);
        }
    }

    private @Nullable EnchantType parseEnchantType(@NotNull CommandSender sender, @NotNull String arg) {
        EnchantType type = EnchantType.fromArg(arg);
        if (type == null || plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
            plugin.sendFeError(sender, "Unknown enchant: " + arg);
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
        plugin.sendFeError(sender, "Level for " + type.arg + " must be 1-" + type.maxLevel + ".");
        return -1;
    }

    private @Nullable Double parseChance(@NotNull CommandSender sender, @NotNull String arg) {
        Double chance = StructureInjectorUtil.parseChancePercent(arg);
        if (chance == null) {
            plugin.sendFeError(sender, "Chance must be 0-100.");
            return null;
        }
        return chance;
    }

    private int parseCost(@NotNull CommandSender sender, @NotNull String arg, @NotNull String fieldName) {
        try {
            int cost = Integer.parseInt(arg);
            if (cost >= 1 && cost <= 64) {
                return cost;
            }
        } catch (NumberFormatException ignored) {
        }
        plugin.sendFeError(sender, fieldName + " must be 1-64.");
        return -1;
    }

    private int parseBookCost(@NotNull CommandSender sender, @NotNull String arg) {
        try {
            int cost = Integer.parseInt(arg);
            if (cost >= 0 && cost <= 64) {
                return cost;
            }
        } catch (NumberFormatException ignored) {
        }
        plugin.sendFeError(sender, "Book cost must be 0-64.");
        return -1;
    }

    private int parseIndex(@NotNull CommandSender sender, @NotNull String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
            plugin.sendFeError(sender, "Index must be a number.");
            return -1;
        }
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
}
