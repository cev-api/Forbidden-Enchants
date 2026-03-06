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

final class FeCommandHandler implements CommandExecutor, TabCompleter {
    private final ForbiddenEnchantsPlugin plugin;
    private final InjectorCommandHandler injectorCommandHandler;
    private final LibrarianTradeCommandHandler librarianTradeCommandHandler;

    FeCommandHandler(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.injectorCommandHandler = new InjectorCommandHandler(plugin);
        this.librarianTradeCommandHandler = new LibrarianTradeCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("forbiddenenchants.admin")) {
            plugin.sendFeError(sender, "You do not have permission (forbiddenenchants.admin).");
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
            case "injector", "structureinjector" -> injectorCommandHandler.handleCommand(sender, args);
            case "librarian", "librariantrades", "libtrades" -> librarianTradeCommandHandler.handleCommand(sender, args);
            default -> {
                plugin.sendFeError(sender, "Unknown subcommand. Use /" + label + " help");
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
            return StringUtil.copyPartialMatches(args[0], List.of("help", "list", "gui", "menu", "toggles", "settings", "enchanttoggles", "give", "givebook", "giveitem", "mysterybook", "mysteryitem", "injector", "librarian"), new ArrayList<>());
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

        if (isInjector && args.length == 2) {
            return injectorCommandHandler.tabComplete(args);
        }
        if (isInjector && (args.length == 3 || args.length == 4)) {
            return injectorCommandHandler.tabComplete(args);
        }
        if (isLibrarian) {
            return librarianTradeCommandHandler.tabComplete(args);
        }

        if (args.length == 2 && (isGiveBook || isGiveItem)) {
            List<String> options = new ArrayList<>();
            for (EnchantType type : plugin.activeEnchantTypes()) {
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

    private @Nullable EnchantType parseEnchantType(@NotNull CommandSender sender, @NotNull String arg) {
        EnchantType type = EnchantType.fromArg(arg);
        if (type == null || plugin.isRetiredEnchant(type)) {
            plugin.sendFeError(sender, "Unknown enchant: " + arg);
            return null;
        }
        return type;
    }

    private int parseEnchantLevel(@NotNull CommandSender sender, @NotNull EnchantType type, @NotNull String arg) {
        final int level;
        try {
            level = Integer.parseInt(arg);
        } catch (NumberFormatException ex) {
            plugin.sendFeError(sender, "Level must be a number.");
            return -1;
        }
        if (level < 1 || level > type.maxLevel) {
            plugin.sendFeError(sender, "Level for " + type.arg + " must be 1-" + type.maxLevel + ".");
            return -1;
        }
        return level;
    }

    private boolean handleGiveBook(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3 || args.length > 4) {
            plugin.sendFeError(sender, "Usage: /fe give <enchant> <level> [player]");
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
        plugin.sendFeSuccess(sender, "Gave " + type.displayName + " " + RomanNumeralUtil.toRoman(level) + " book to " + target.getName() + ".");
        if (!sender.equals(target)) {
            target.sendMessage(net.kyori.adventure.text.Component.text("[Forbidden Enchants] ", net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
                    .append(net.kyori.adventure.text.Component.text("You received " + type.displayName + " " + RomanNumeralUtil.toRoman(level) + " book.", type.color)));
        }
        return true;
    }

    private boolean handleGiveItem(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4 || args.length > 5) {
            plugin.sendFeError(sender, "Usage: /fe giveitem <enchant> <level> <material> [player]");
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
            plugin.sendFeError(sender, "Unknown material: " + args[3]);
            return true;
        }
        if (!plugin.isMaterialValidForEnchant(material, type)) {
            plugin.sendFeError(sender, type.displayName + " requires " + EnchantMaterialCatalog.requiredMaterialCategory(type) + ".");
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 5 ? args[4] : null);
        if (target == null) {
            return true;
        }
        var enchanted = plugin.createEnchantedItem(type, level, material);
        if (enchanted == null) {
            plugin.sendFeError(sender, "Could not build enchanted item.");
            return true;
        }
        plugin.giveOrDrop(target, enchanted);
        plugin.sendFeSuccess(sender, "Gave " + DisplayNameUtil.toDisplayName(material) + " with " + type.displayName + " " + RomanNumeralUtil.toRoman(level) + " to " + target.getName() + ".");
        return true;
    }

    private boolean handleGiveMysteryBook(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) {
            plugin.sendFeError(sender, "Usage: /fe mysterybook <slot> [player]");
            return true;
        }
        ArmorSlot slot = SlotParsingUtil.parseSlotArg(args[1]);
        if (slot == null) {
            plugin.sendFeError(sender, "Unknown slot: " + args[1]);
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            return true;
        }
        plugin.giveOrDrop(target, plugin.createMysteryBook(slot));
        plugin.sendFeSuccess(sender, "Gave mystery " + SlotParsingUtil.slotName(slot) + " enchant book to " + target.getName() + ".");
        return true;
    }

    private boolean handleGiveMysteryItem(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) {
            plugin.sendFeError(sender, "Usage: /fe mysteryitem <material> [player]");
            return true;
        }
        var material = SlotParsingUtil.parseMaterial(args[1]);
        if (material == null || material == org.bukkit.Material.AIR) {
            plugin.sendFeError(sender, "Unknown material: " + args[1]);
            return true;
        }
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null);
        if (target == null) {
            return true;
        }
        var mystery = plugin.createMysteryItem(material);
        if (mystery == null) {
            plugin.sendFeError(sender, "No forbidden enchant exists for " + DisplayNameUtil.toDisplayName(material) + ".");
            return true;
        }
        plugin.giveOrDrop(target, mystery);
        plugin.sendFeSuccess(sender, "Gave mystery " + DisplayNameUtil.toDisplayName(material) + " to " + target.getName() + ".");
        return true;
    }

    private boolean handleOpenGui(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length > 2) {
            plugin.sendFeError(sender, "Usage: /fe gui [player]");
            return true;
        }
        Player target = resolveTarget(sender, args.length == 2 ? args[1] : null);
        if (target == null) {
            return true;
        }
        int startPage = plugin.getLastMenuPage(target.getUniqueId());
        plugin.openFeMenu(target, startPage);
        if (!sender.equals(target)) {
            plugin.sendFeSuccess(sender, "Opened Forbidden Enchants menu for " + target.getName() + ".");
        }
        return true;
    }

    private boolean handleToggleGui(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length > 2) {
            plugin.sendFeError(sender, "Usage: /fe toggles [player]");
            return true;
        }
        Player target = resolveTarget(sender, args.length == 2 ? args[1] : null);
        if (target == null) {
            return true;
        }
        plugin.openEnchantToggleMenu(target, 0);
        if (!sender.equals(target)) {
            plugin.sendFeSuccess(sender, "Opened enchant toggle GUI for " + target.getName() + ".");
        }
        return true;
    }
}

