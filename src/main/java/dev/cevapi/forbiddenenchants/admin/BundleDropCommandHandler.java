package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BundleDropCommandHandler {
    private static final List<String> SUBCOMMANDS = List.of("help", "status", "enable", "disable", "chance", "gui", "clear");

    private final ForbiddenEnchantsPlugin plugin;

    BundleDropCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
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
                plugin.sendFeSuccess(sender,
                        "Bundle drops are " + (plugin.isBundleDropEnabled() ? "enabled" : "disabled")
                                + " with default mob chance " + StructureInjectorUtil.formatPercent(plugin.getBundleDropChancePercent())
                                + ", " + plugin.bundleDropMobChances().size() + " mob chance entry(ies), "
                                + plugin.bundleDropRewards().size() + " bundle reward entry(ies), and "
                                + plugin.bundleDropExtraDrops().size() + " extra non-stackable drop(s).");
                yield true;
            }
            case "enable" -> {
                plugin.setBundleDropEnabled(true);
                plugin.saveBundleDropSettings();
                plugin.sendFeSuccess(sender, "Bundle drops enabled.");
                yield true;
            }
            case "disable" -> {
                plugin.setBundleDropEnabled(false);
                plugin.saveBundleDropSettings();
                plugin.sendFeSuccess(sender, "Bundle drops disabled.");
                yield true;
            }
            case "chance" -> {
                if (args.length != 3) {
                    plugin.sendFeError(sender, msg("usage_chance", "Usage: /fe bundle chance <0-100>"));
                    yield true;
                }
                Double chance = StructureInjectorUtil.parseChancePercent(args[2]);
                if (chance == null) {
                    plugin.sendFeError(sender, msg("chance_range", "Chance must be 0-100."));
                    yield true;
                }
                plugin.setBundleDropChancePercent(chance);
                plugin.saveBundleDropSettings();
                plugin.sendFeSuccess(sender, "Bundle drop chance set to " + StructureInjectorUtil.formatPercent(chance) + ".");
                yield true;
            }
            case "gui", "menu" -> {
                if (args.length > 3) {
                    plugin.sendFeError(sender, msg("usage_gui", "Usage: /fe bundle gui [player]"));
                    yield true;
                }
                Player target = resolveTarget(sender, args.length == 3 ? args[2] : null);
                if (target == null) {
                    yield true;
                }
                plugin.openBundleDropMenu(target);
                if (!sender.equals(target)) {
                    plugin.sendFeSuccess(sender, "Opened bundle drop editor for " + target.getName() + ".");
                }
                yield true;
            }
            case "clear" -> {
                plugin.bundleDropRewards().clear();
                plugin.bundleDropMobChances().clear();
                plugin.bundleDropExtraDrops().clear();
                plugin.saveBundleDropSettings();
                plugin.sendFeSuccess(sender, "Cleared bundle drop rewards and mob targets.");
                yield true;
            }
            default -> {
                plugin.sendFeError(sender, msg("unknown_subcommand", "Unknown bundle subcommand. Use /fe bundle help"));
                yield true;
            }
        };
    }

    @NotNull List<String> tabComplete(@NotNull String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], SUBCOMMANDS, new ArrayList<>());
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("gui") || args[1].equalsIgnoreCase("menu"))) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("chance")) {
            return StringUtil.copyPartialMatches(args[2], List.of("1", "5", "10", "25", "50"), new ArrayList<>());
        }
        return List.of();
    }

    private void sendHelp(@NotNull CommandSender sender) {
        plugin.sendFeSuccess(sender, msg("help_header", "Bundle Drop Commands:"));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_1", " - /fe bundle status"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_2", " - /fe bundle enable | disable"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_3", " - /fe bundle chance <0-100>"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_4", " - /fe bundle gui [player]"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
        sender.sendMessage(net.kyori.adventure.text.Component.text(msg("help_line_5", " - /fe bundle clear"), net.kyori.adventure.text.format.NamedTextColor.GRAY));
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
        return plugin.message("bundle.command." + key, fallback);
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return plugin.message("bundle.command." + key, fallback, placeholders);
    }
}
