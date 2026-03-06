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
            String arrow = previous ? "<<" : ">>";
            String label = previous ? "Previous Page" : "Next Page";
            NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY;
            meta.displayName(Component.text(arrow + " " + label, color));
            meta.lore(List.of(
                    Component.text(enabled ? "Left-click: 1 page" : "No page available.", NamedTextColor.GRAY),
                    Component.text(enabled ? "Right-click: 5 pages" : "", NamedTextColor.DARK_GRAY)
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
        lore.add(Component.text("Click to claim", NamedTextColor.GREEN));
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
        sender.sendMessage(Component.text("Forbidden Enchants Admin", NamedTextColor.GOLD));
        sendHelpLine(sender, "/" + label + " help", "Show command help.");
        sendHelpLine(sender, "/" + label + " list", "List enchants + levels.");
        sendHelpLine(sender, "/" + label + " gui [player]", "Open item picker menu.");
        sendHelpLine(sender, "/" + label + " give <enchant> <level> [player]", "Give enchant book.");
        sendHelpLine(sender, "/" + label + " givebook <enchant> <level> [player]", "Give enchant book (explicit).");
        sendHelpLine(sender, "/" + label + " giveitem <enchant> <level> <material> [player]", "Give pre-enchanted gear.");
        sendHelpLine(sender, "/" + label + " mysterybook <slot> [player]", "Give obfuscated random enchant book for a slot.");
        sendHelpLine(sender, "/" + label + " mysteryitem <material> [player]", "Give obfuscated random pre-enchanted item.");
        sendHelpLine(sender, "/" + label + " injector <...>", "Structure-based loot injection controls.");
        sendHelpLine(sender, "/" + label + " toggles [player]", "Toggle enchant use and chest/vault spawning.");
    }

    private void sendHelpLine(@NotNull CommandSender sender, @NotNull String command, @NotNull String description) {
        sender.sendMessage(Component.text(" - ", NamedTextColor.YELLOW)
                .append(Component.text(command, NamedTextColor.AQUA))
                .append(Component.text(" -> ", NamedTextColor.YELLOW))
                .append(Component.text(description, NamedTextColor.WHITE)));
    }

    void sendEnchantList(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("Forbidden Enchants", NamedTextColor.LIGHT_PURPLE));
        for (EnchantType type : plugin.activeEnchantTypes()) {
            String range = type.maxLevel == 1 ? "I" : "I-" + RomanNumeralUtil.toRoman(type.maxLevel);
            sender.sendMessage(Component.text("- " + type.arg + " (" + range + ")", type.color)
                    .append(Component.text(" | " + type.slotDescription(), NamedTextColor.DARK_GRAY)));
        }
    }

    void sendFeError(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Component.text("[Forbidden Enchants] ", NamedTextColor.DARK_PURPLE)
                .append(Component.text(message, NamedTextColor.RED)));
    }

    void sendFeSuccess(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Component.text("[Forbidden Enchants] ", NamedTextColor.DARK_PURPLE)
                .append(Component.text(message, NamedTextColor.GREEN)));
    }
}

