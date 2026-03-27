package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class FePresentationService {
    private final ForbiddenEnchantsPlugin plugin;

    FePresentationService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull ItemStack createMenuNavPane(boolean enabled, boolean previous) {
        ItemStack pane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            String arrow = previous
                    ? plugin.message("menu.navigation.previous_arrow", "<<")
                    : plugin.message("menu.navigation.next_arrow", ">>");
            String label = previous
                    ? plugin.message("menu.navigation.previous_label", "Previous Page")
                    : plugin.message("menu.navigation.next_label", "Next Page");
            NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY;
            meta.displayName(Component.text(arrow + " " + label, color));
            meta.lore(List.of(
                    Component.text(enabled
                            ? plugin.message("menu.navigation.left_click", "Left-click: 1 page")
                            : plugin.message("menu.navigation.no_page", "No page available."), NamedTextColor.GRAY),
                    Component.text(enabled
                            ? plugin.message("menu.navigation.right_click", "Right-click: 5 pages")
                            : "", NamedTextColor.DARK_GRAY)
            ));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    @NotNull ItemStack toMenuDisplayItem(@NotNull ItemStack source) {
        ItemStack display = source.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text(plugin.message("menu.click_to_claim", "Click to claim"), NamedTextColor.GREEN));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    @NotNull String describeItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return DisplayNameUtil.toDisplayName(item.getType());
    }

    void sendFeHelp(@NotNull CommandSender sender, @NotNull String label) {
        sender.sendMessage(Component.text(plugin.message("fe.help.header", "Forbidden Enchants Admin"), NamedTextColor.GOLD));
        sendHelpLine(sender, "/" + label + " help", plugin.message("fe.help.help_desc", "Show command help."));
        sendHelpLine(sender, "/" + label + " list", plugin.message("fe.help.list_desc", "List enchants + levels."));
        sendHelpLine(sender, "/" + label + " gui [player]", plugin.message("fe.help.gui_desc", "Open item picker menu."));
        sendHelpLine(sender, "/" + label + " give <enchant> <level> [player]", plugin.message("fe.help.give_desc", "Give enchant book."));
        sendHelpLine(sender, "/" + label + " givebook <enchant> <level> [player]", plugin.message("fe.help.givebook_desc", "Give enchant book (explicit)."));
        sendHelpLine(sender, "/" + label + " giveitem <enchant> <level> <material> [player]", plugin.message("fe.help.giveitem_desc", "Give pre-enchanted gear."));
        sendHelpLine(sender, "/" + label + " mysterybook <slot> [player]", plugin.message("fe.help.mysterybook_desc", "Give obfuscated random enchant book for a slot."));
        sendHelpLine(sender, "/" + label + " mysteryitem <material> [player]", plugin.message("fe.help.mysteryitem_desc", "Give obfuscated random pre-enchanted item."));
        sendHelpLine(sender, "/" + label + " reload", plugin.message("fe.help.reload_desc", "Reload config.yml and messages.yml."));
        sendHelpLine(sender, "/" + label + " injector <...>", plugin.message("fe.help.injector_desc", "Structure-based loot injection controls."));
        sendHelpLine(sender, "/" + label + " librarian <...>", plugin.message("fe.help.librarian_desc", "Librarian forbidden-book trade editor and controls."));
        sendHelpLine(sender, "/" + label + " enchanting <...>", plugin.message("fe.help.enchanting_desc", "Enchanting-table forbidden-book injector editor and controls."));
        sendHelpLine(sender, "/" + label + " bundle <...>", plugin.message("fe.help.bundle_desc", "Mob bundle-drop editor and controls."));
        sendHelpLine(sender, "/" + label + " toggles [player]", plugin.message("fe.help.toggles_desc", "Toggle enchant use and chest/vault spawning."));
    }

    private void sendHelpLine(@NotNull CommandSender sender, @NotNull String command, @NotNull String description) {
        sender.sendMessage(Component.text(" - ", NamedTextColor.YELLOW)
                .append(Component.text(command, NamedTextColor.AQUA))
                .append(Component.text(" -> ", NamedTextColor.YELLOW))
                .append(Component.text(description, NamedTextColor.WHITE)));
    }

    void sendEnchantList(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text(plugin.message("fe.list.header", "Forbidden Enchants"), NamedTextColor.LIGHT_PURPLE));
        for (EnchantType type : plugin.activeEnchantTypes()) {
            if (type.isAnvilOnlyUtilityBook()) {
                continue;
            }
            String range = type.maxLevel == 1 ? "I" : "I-" + RomanNumeralUtil.toRoman(type.maxLevel);
            sender.sendMessage(Component.text("- " + type.arg + " (" + range + ")", type.color)
                    .append(Component.text(" | " + type.slotDescription(), NamedTextColor.DARK_GRAY)));
        }
    }

    void sendFeError(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Component.text(plugin.message("fe.prefix", "[Forbidden Enchants] "), NamedTextColor.DARK_PURPLE)
                .append(Component.text(message, NamedTextColor.RED)));
    }

    void sendFeSuccess(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Component.text(plugin.message("fe.prefix", "[Forbidden Enchants] "), NamedTextColor.DARK_PURPLE)
                .append(Component.text(message, NamedTextColor.GREEN)));
    }
}

