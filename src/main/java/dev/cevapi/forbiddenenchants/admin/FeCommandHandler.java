package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FeCommandHandler implements CommandExecutor, TabCompleter {
    private final ForbiddenEnchantsPlugin plugin;
    private final InjectorCommandHandler injectorCommandHandler;
    private final LibrarianTradeCommandHandler librarianTradeCommandHandler;
    private final BundleDropCommandHandler bundleDropCommandHandler;

    FeCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.injectorCommandHandler = new InjectorCommandHandler(plugin);
        this.librarianTradeCommandHandler = new LibrarianTradeCommandHandler(plugin);
        this.bundleDropCommandHandler = new BundleDropCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("forbiddenenchants.admin")) {
            plugin.sendFeError(sender, msg("no_permission", "You do not have permission (forbiddenenchants.admin)."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.sendFeHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                plugin.sendEnchantList(sender);
                yield true;
            }
            case "gui", "menu" -> handleOpenGui(sender, args);
            case "toggles", "settings", "enchanttoggles" -> handleToggleGui(sender, args);
            case "give", "givebook" -> handleGiveBook(sender, args);
            case "giveitem" -> handleGiveItem(sender, args);
            case "mysterybook" -> handleGiveMysteryBook(sender, args);
            case "mysteryitem" -> handleGiveMysteryItem(sender, args);
            case "reload" -> handleReload(sender);
            case "injector", "structureinjector" -> injectorCommandHandler.handleCommand(sender, args);
            case "librarian", "librariantrades", "libtrades" -> librarianTradeCommandHandler.handleCommand(sender, args);
            case "bundle", "bundledrop", "mobbundle" -> bundleDropCommandHandler.handleCommand(sender, args);
            default -> {
                plugin.sendFeError(
                        sender,
                        msg("unknown_subcommand", "Unknown subcommand. Use /{label} help", Map.of("label", label))
                );
                yield true;
            }
        };
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("help", "list", "gui", "menu", "toggles", "settings", "enchanttoggles", "give", "givebook", "giveitem", "mysterybook", "mysteryitem", "reload", "injector", "librarian", "bundle", "bundledrop", "mobbundle"), new ArrayList<>());
        }

        boolean isGiveBook = args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("givebook");
        boolean isGiveItem = args[0].equalsIgnoreCase("giveitem");
        boolean isMysteryBook = args[0].equalsIgnoreCase("mysterybook");
        boolean isMysteryItem = args[0].equalsIgnoreCase("mysteryitem");
        boolean isGui = args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu");
        boolean isToggleGui = args[0].equalsIgnoreCase("toggles")
                || args[0].equalsIgnoreCase("settings")
                || args[0].equalsIgnoreCase("enchanttoggles");
        boolean isInjector = args[0].equalsIgnoreCase("injector") || args[0].equalsIgnoreCase("structureinjector");
        boolean isLibrarian = args[0].equalsIgnoreCase("librarian")
                || args[0].equalsIgnoreCase("librariantrades")
                || args[0].equalsIgnoreCase("libtrades");
        boolean isBundle = args[0].equalsIgnoreCase("bundle")
                || args[0].equalsIgnoreCase("bundledrop")
                || args[0].equalsIgnoreCase("mobbundle");

        if (isInjector && args.length == 2) {
            return injectorCommandHandler.tabComplete(args);
        }
        if (isInjector && (args.length == 3 || args.length == 4)) {
            return injectorCommandHandler.tabComplete(args);
        }
        if (isLibrarian) {
            return librarianTradeCommandHandler.tabComplete(args);
        }
        if (isBundle) {
            return bundleDropCommandHandler.tabComplete(args);
        }

        if (args.length == 2 && (isGiveBook || isGiveItem)) {
            List<String> options = new ArrayList<>();
            for (EnchantType type : plugin.activeEnchantTypes()) {
                if (type.isAnvilOnlyUtilityBook()) {
                    continue;
                }
                options.add(type.arg);
            }
            return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
        }

        if (args.length == 3 && (isGiveBook || isGiveItem)) {
            EnchantType type = EnchantType.fromArg(args[1]);
            if (type == null || plugin.isRetiredEnchant(type)) {
                return Collections.emptyList();
            }
            List<String> levels = new ArrayList<>();
            for (int i = 1; i <= type.maxLevel; i++) {
                levels.add(String.valueOf(i));
            }
            return StringUtil.copyPartialMatches(args[2], levels, new ArrayList<>());
        }

        if (args.length == 4 && isGiveBook) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[3], players, new ArrayList<>());
        }

        if (args.length == 4 && isGiveItem) {
            EnchantType type = EnchantType.fromArg(args[1]);
            if (type == null || plugin.isRetiredEnchant(type)) {
                return Collections.emptyList();
            }
            return StringUtil.copyPartialMatches(args[3], EnchantMaterialCatalog.materialSuggestions(type), new ArrayList<>());
        }

        if (args.length == 5 && isGiveItem) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[4], players, new ArrayList<>());
        }

        if (args.length == 2 && isGui) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[1], players, new ArrayList<>());
        }
        if (args.length == 2 && isToggleGui) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[1], players, new ArrayList<>());
        }

        if (args.length == 2 && isMysteryBook) {
            return StringUtil.copyPartialMatches(args[1], List.of("helmet", "chestplate", "elytra", "leggings", "boots", "armor", "compass", "sword", "ranged", "trident", "spear", "hoe", "axe", "mace", "nametag", "lead", "shield", "totem"), new ArrayList<>());
        }

        if (args.length == 3 && isMysteryBook) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
        }

        if (args.length == 2 && isMysteryItem) {
            return StringUtil.copyPartialMatches(args[1], EnchantMaterialCatalog.allEnchantableMaterialSuggestions(), new ArrayList<>());
        }

        if (args.length == 3 && isMysteryItem) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return StringUtil.copyPartialMatches(args[2], players, new ArrayList<>());
        }

        return Collections.emptyList();
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

    private @Nullable EnchantType parseEnchantType(@NotNull CommandSender sender, @NotNull String arg) {
        EnchantType type = EnchantType.fromArg(arg);
        if (type == null || plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
            plugin.sendFeError(sender, msg("unknown_enchant", "Unknown enchant: {enchant}", Map.of("enchant", arg)));
            return null;
        }
        return type;
    }

    private int parseEnchantLevel(@NotNull CommandSender sender, @NotNull EnchantType type, @NotNull String arg) {
        final int level;
        try {
            level = Integer.parseInt(arg);
        } catch (NumberFormatException ex) {
            plugin.sendFeError(sender, msg("level_not_number", "Level must be a number."));
            return -1;
        }
        if (level < 1 || level > type.maxLevel) {
            plugin.sendFeError(
                    sender,
                    msg("level_out_of_range", "Level for {enchant} must be 1-{max}.",
                            Map.of("enchant", type.arg, "max", String.valueOf(type.maxLevel)))
            );
            return -1;
        }
        return level;
    }

    private boolean handleGiveBook(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3 || args.length > 4) {
            plugin.sendFeError(sender, msg("usage_give", "Usage: /fe give <enchant> <level> [player]"));
            return true;
        }
        EnchantType type = parseEnchantType(sender, args[1]);
        if (type == null) {
            return true;
        }
        int level = parseEnchantLevel(sender, type, args[2]);
        if (level < 1) {
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 4 ? args[3] : null);
        if (target == null) {
            return true;
        }
        plugin.giveOrDrop(target, plugin.createBook(type, level));
        plugin.sendFeSuccess(
                sender,
                msg(
                        "gave_book",
                        "Gave {enchant} {level} book to {player}.",
                        Map.of(
                                "enchant", type.displayName,
                                "level", RomanNumeralUtil.toRoman(level),
                                "player", target.getName()
                        )
                )
        );
        if (!sender.equals(target)) {
            target.sendMessage(net.kyori.adventure.text.Component.text(plugin.message("fe.prefix", "[Forbidden Enchants] "), net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
                    .append(net.kyori.adventure.text.Component.text(
                            msg(
                                    "you_received_book",
                                    "You received {enchant} {level} book.",
                                    Map.of("enchant", type.displayName, "level", RomanNumeralUtil.toRoman(level))
                            ),
                            type.color
                    )));
        }
        return true;
    }

    private boolean handleGiveItem(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4 || args.length > 5) {
            plugin.sendFeError(sender, msg("usage_giveitem", "Usage: /fe giveitem <enchant> <level> <material> [player]"));
            return true;
        }
        EnchantType type = parseEnchantType(sender, args[1]);
        if (type == null) {
            return true;
        }
        int level = parseEnchantLevel(sender, type, args[2]);
        if (level < 1) {
            return true;
        }
        var material = SlotParsingUtil.parseMaterial(args[3]);
        if (material == null || material == org.bukkit.Material.AIR) {
            plugin.sendFeError(sender, msg("unknown_material", "Unknown material: {material}", Map.of("material", args[3])));
            return true;
        }
        if (!plugin.isMaterialValidForEnchant(material, type)) {
            plugin.sendFeError(
                    sender,
                    msg(
                            "invalid_material_for_enchant",
                            "{enchant} requires {category}.",
                            Map.of("enchant", type.displayName, "category", EnchantMaterialCatalog.requiredMaterialCategory(type))
                    )
            );
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 5 ? args[4] : null);
        if (target == null) {
            return true;
        }
        var enchanted = plugin.createEnchantedItem(type, level, material);
        if (enchanted == null) {
            plugin.sendFeError(sender, msg("could_not_build_item", "Could not build enchanted item."));
            return true;
        }
        plugin.giveOrDrop(target, enchanted);
        plugin.sendFeSuccess(
                sender,
                msg(
                        "gave_item",
                        "Gave {item} with {enchant} {level} to {player}.",
                        Map.of(
                                "item", DisplayNameUtil.toDisplayName(material),
                                "enchant", type.displayName,
                                "level", RomanNumeralUtil.toRoman(level),
                                "player", target.getName()
                        )
                )
        );
        return true;
    }

    private boolean handleGiveMysteryBook(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) {
            plugin.sendFeError(sender, msg("usage_mysterybook", "Usage: /fe mysterybook <slot> [player]"));
            return true;
        }
        ArmorSlot slot = SlotParsingUtil.parseSlotArg(args[1]);
        if (slot == null) {
            plugin.sendFeError(sender, msg("unknown_slot", "Unknown slot: {slot}", Map.of("slot", args[1])));
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            return true;
        }
        plugin.giveOrDrop(target, plugin.createMysteryBook(slot));
        plugin.sendFeSuccess(
                sender,
                msg(
                        "gave_mystery_book",
                        "Gave mystery {slot} enchant book to {player}.",
                        Map.of("slot", SlotParsingUtil.slotName(slot), "player", target.getName())
                )
        );
        return true;
    }

    private boolean handleGiveMysteryItem(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) {
            plugin.sendFeError(sender, msg("usage_mysteryitem", "Usage: /fe mysteryitem <material> [player]"));
            return true;
        }
        var material = SlotParsingUtil.parseMaterial(args[1]);
        if (material == null || material == org.bukkit.Material.AIR) {
            plugin.sendFeError(sender, msg("unknown_material", "Unknown material: {material}", Map.of("material", args[1])));
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            return true;
        }
        var mystery = plugin.createMysteryItem(material);
        if (mystery == null) {
            plugin.sendFeError(
                    sender,
                    msg(
                            "no_enchant_for_material",
                            "No forbidden enchant exists for {material}.",
                            Map.of("material", DisplayNameUtil.toDisplayName(material))
                    )
            );
            return true;
        }
        plugin.giveOrDrop(target, mystery);
        plugin.sendFeSuccess(
                sender,
                msg(
                        "gave_mystery_item",
                        "Gave mystery {material} to {player}.",
                        Map.of("material", DisplayNameUtil.toDisplayName(material), "player", target.getName())
                )
        );
        return true;
    }

    private boolean handleOpenGui(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length > 2) {
            plugin.sendFeError(sender, msg("usage_gui", "Usage: /fe gui [player]"));
            return true;
        }
        Player target = resolveTarget(sender, args.length == 2 ? args[1] : null);
        if (target == null) {
            return true;
        }
        int startPage = plugin.getLastMenuPage(target.getUniqueId());
        plugin.openFeMenu(target, startPage);
        if (!sender.equals(target)) {
            plugin.sendFeSuccess(
                    sender,
                    msg("opened_gui_for_player", "Opened Forbidden Enchants menu for {player}.", Map.of("player", target.getName()))
            );
        }
        return true;
    }

    private boolean handleToggleGui(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length > 2) {
            plugin.sendFeError(sender, msg("usage_toggles", "Usage: /fe toggles [player]"));
            return true;
        }
        Player target = resolveTarget(sender, args.length == 2 ? args[1] : null);
        if (target == null) {
            return true;
        }
        plugin.openEnchantToggleMenu(target, 0);
        if (!sender.equals(target)) {
            plugin.sendFeSuccess(
                    sender,
                    msg("opened_toggles_for_player", "Opened enchant toggle GUI for {player}.", Map.of("player", target.getName()))
            );
        }
        return true;
    }

    private boolean handleReload(@NotNull CommandSender sender) {
        plugin.reloadRuntimeConfiguration();
        plugin.sendFeSuccess(sender, msg("reloaded", "Forbidden Enchants config/messages reloaded."));
        return true;
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback) {
        return plugin.message("fe.command." + key, fallback);
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback, @NotNull Map<String, String> placeholders) {
        return plugin.message("fe.command." + key, fallback, placeholders);
    }
}

