package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class BundleDropMenuService {
    private static final int MENU_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 46;
    private static final int PAGE_COUNTER_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int PAGE_IMPORT_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private static final int ROOT_ENABLED_SLOT = 10;
    private static final int ROOT_DEFAULT_CHANCE_SLOT = 12;
    private static final int ROOT_MOBS_SLOT = 14;
    private static final int ROOT_COUNTER_SLOT = 16;
    private static final int ROOT_ENCHANTS_SLOT = 29;
    private static final int ROOT_POTIONS_SLOT = 30;
    private static final int ROOT_LOOT_SLOT = 31;
    private static final int ROOT_REWARDS_SLOT = 32;
    private static final int ROOT_EXTRA_SLOT = 33;
    private static final int ROOT_HELP_SLOT = 45;
    private static final int ROOT_CLEAR_SLOT = 49;
    private static final int ROOT_CLOSE_SLOT = 53;

    private static final List<Material> BASIC_LOOT = List.of(
            Material.DIAMOND,
            Material.EMERALD,
            Material.NETHERITE_INGOT,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.ANCIENT_DEBRIS,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.TOTEM_OF_UNDYING
    );

    private final ForbiddenEnchantsPlugin plugin;
    private final BundleDropRuntimeService runtimeService;

    BundleDropMenuService(@NotNull ForbiddenEnchantsPlugin plugin, @NotNull BundleDropRuntimeService runtimeService) {
        this.plugin = plugin;
        this.runtimeService = runtimeService;
    }

    void openRoot(@NotNull Player player) {
        BundleDropMenuHolder holder = new BundleDropMenuHolder(BundleDropMenuPage.ROOT, 0);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(plugin.message("menu.bundle.root_title", "Bundle Drop Editor"), NamedTextColor.GOLD)
        );
        holder.attach(inventory);
        decorateRoot(inventory);

        inventory.setItem(ROOT_ENABLED_SLOT, enabledItem());
        inventory.setItem(ROOT_DEFAULT_CHANCE_SLOT, defaultChanceItem());
        inventory.setItem(ROOT_MOBS_SLOT, mobsItem());
        inventory.setItem(ROOT_COUNTER_SLOT, counterItem());
        inventory.setItem(ROOT_ENCHANTS_SLOT, openEditorItem(Material.ENCHANTED_BOOK, plugin.message("menu.bundle.root_enchant_rewards", "Enchant Rewards")));
        inventory.setItem(ROOT_POTIONS_SLOT, openEditorItem(Material.POTION, plugin.message("menu.bundle.root_potion_rewards", "Potion Rewards")));
        inventory.setItem(ROOT_LOOT_SLOT, openEditorItem(Material.DIAMOND, plugin.message("menu.bundle.root_loot_rewards", "Loot Rewards")));
        inventory.setItem(ROOT_REWARDS_SLOT, openEditorItem(Material.BUNDLE, plugin.message("menu.bundle.root_bundle_contents", "Bundle Contents")));
        inventory.setItem(ROOT_EXTRA_SLOT, openEditorItem(Material.NETHERITE_SWORD, plugin.message("menu.bundle.root_extra_drops", "Extra Drops")));
        inventory.setItem(ROOT_HELP_SLOT, rootHelpItem());
        inventory.setItem(ROOT_CLEAR_SLOT, clearItem());
        inventory.setItem(ROOT_CLOSE_SLOT, closeItem());

        player.openInventory(inventory);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BundleDropMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        // Allow adding inventory items into LOOT/REWARDS/EXTRA editors.
        if (!clicked.equals(top)) {
            if (holder.pageType() == BundleDropMenuPage.LOOT
                    || holder.pageType() == BundleDropMenuPage.REWARDS
                    || holder.pageType() == BundleDropMenuPage.EXTRA) {
                handleBottomInventoryImport(player, holder, event);
                return;
            }
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        if (holder.pageType() == BundleDropMenuPage.ROOT) {
            handleRootClick(player, event.getRawSlot(), event);
            return;
        }
        if ((holder.pageType() == BundleDropMenuPage.LOOT
                || holder.pageType() == BundleDropMenuPage.REWARDS
                || holder.pageType() == BundleDropMenuPage.EXTRA)
                && tryImportFromCursor(player, holder, event)) {
            return;
        }
        handlePagedClick(player, holder, event.getRawSlot(), event);
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BundleDropMenuHolder holder)) {
            return;
        }

        int topSize = top.getSize();
        boolean touchesTop = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                touchesTop = true;
                break;
            }
        }

        if (!touchesTop) {
            return;
        }

        if (holder.pageType() == BundleDropMenuPage.LOOT
                || holder.pageType() == BundleDropMenuPage.REWARDS
                || holder.pageType() == BundleDropMenuPage.EXTRA) {
            for (ItemStack stack : event.getNewItems().values()) {
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                if (holder.pageType() == BundleDropMenuPage.EXTRA) {
                    addExtraFromInventory(stack);
                } else {
                    addMaterialFromInventory(stack);
                }
            }
            plugin.saveBundleDropSettings();
            if (event.getWhoClicked() instanceof Player player) {
                openPaged(player, holder.pageType(), holder.page());
            }
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    private void handleBottomInventoryImport(@NotNull Player player,
                                             @NotNull BundleDropMenuHolder holder,
                                             @NotNull InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }
        if (!(event.isShiftClick() || event.getClick().isLeftClick() || event.getClick().isRightClick() || event.getClick() == ClickType.MIDDLE)) {
            return;
        }
        if (holder.pageType() == BundleDropMenuPage.EXTRA) {
            addExtraFromInventory(current);
        } else {
            addMaterialFromInventory(current);
        }
        plugin.saveBundleDropSettings();
        openPaged(player, holder.pageType(), holder.page());
        event.setCancelled(true);
    }

    private void handleRootClick(@NotNull Player player, int rawSlot, @NotNull InventoryClickEvent event) {
        if (rawSlot == ROOT_ENABLED_SLOT) {
            plugin.setBundleDropEnabled(!plugin.isBundleDropEnabled());
            plugin.saveBundleDropSettings();
            openRoot(player);
            return;
        }
        if (rawSlot == ROOT_DEFAULT_CHANCE_SLOT) {
            double chance = plugin.getBundleDropChancePercent();
            double step = event.isShiftClick() ? 5.0D : 1.0D;
            if (event.getClick().isLeftClick()) {
                plugin.setBundleDropChancePercent(chance + step);
            } else if (event.getClick().isRightClick()) {
                plugin.setBundleDropChancePercent(chance - step);
            } else if (event.getClick() == ClickType.MIDDLE) {
                plugin.setBundleDropChancePercent(5.0D);
            } else {
                return;
            }
            plugin.saveBundleDropSettings();
            openRoot(player);
            return;
        }
        if (rawSlot == ROOT_MOBS_SLOT) {
            openPaged(player, BundleDropMenuPage.MOBS, 0);
            return;
        }
        if (rawSlot == ROOT_ENCHANTS_SLOT) {
            openPaged(player, BundleDropMenuPage.ENCHANTS, 0);
            return;
        }
        if (rawSlot == ROOT_POTIONS_SLOT) {
            openPaged(player, BundleDropMenuPage.POTIONS, 0);
            return;
        }
        if (rawSlot == ROOT_LOOT_SLOT) {
            openPaged(player, BundleDropMenuPage.LOOT, 0);
            return;
        }
        if (rawSlot == ROOT_REWARDS_SLOT) {
            openPaged(player, BundleDropMenuPage.REWARDS, 0);
            return;
        }
        if (rawSlot == ROOT_EXTRA_SLOT) {
            openPaged(player, BundleDropMenuPage.EXTRA, 0);
            return;
        }
        if (rawSlot == ROOT_CLEAR_SLOT) {
            plugin.bundleDropRewards().clear();
            plugin.bundleDropExtraDrops().clear();
            plugin.bundleDropMobChances().clear();
            plugin.saveBundleDropSettings();
            openRoot(player);
            return;
        }
        if (rawSlot == ROOT_CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void handlePagedClick(@NotNull Player player,
                                  @NotNull BundleDropMenuHolder holder,
                                  int rawSlot,
                                  @NotNull InventoryClickEvent event) {
        List<?> entries = entriesFor(holder.pageType());
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = holder.page();

        if (rawSlot == PREV_SLOT) {
            if (page > 0) {
                openPaged(player, holder.pageType(), page - 1);
            }
            return;
        }
        if (rawSlot == NEXT_SLOT) {
            if (page + 1 < totalPages) {
                openPaged(player, holder.pageType(), page + 1);
            }
            return;
        }
        if (rawSlot == BACK_SLOT) {
            openRoot(player);
            return;
        }
        if (rawSlot < 0 || rawSlot >= PAGE_SIZE) {
            return;
        }

        int absolute = page * PAGE_SIZE + rawSlot;
        if (absolute < 0 || absolute >= entries.size()) {
            return;
        }

        switch (holder.pageType()) {
            case MOBS -> {
                EntityType type = (EntityType) entries.get(absolute);
                double currentChance = plugin.bundleDropMobChances().getOrDefault(type, 0.0D);
                double step = event.isShiftClick() ? 5.0D : 1.0D;
                if (event.getClick().isLeftClick()) {
                    double next = Math.min(100.0D, Math.max(0.0D, currentChance + step));
                    plugin.bundleDropMobChances().put(type, next);
                } else if (event.getClick().isRightClick()) {
                    double next = Math.min(100.0D, Math.max(0.0D, currentChance - step));
                    if (next <= 0.0D) {
                        plugin.bundleDropMobChances().remove(type);
                    } else {
                        plugin.bundleDropMobChances().put(type, next);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    if (currentChance > 0.0D) {
                        plugin.bundleDropMobChances().remove(type);
                    } else {
                        plugin.bundleDropMobChances().put(type, plugin.getBundleDropChancePercent());
                    }
                } else {
                    return;
                }
                plugin.saveBundleDropSettings();
                openPaged(player, holder.pageType(), page);
            }
            case ENCHANTS, POTIONS -> {
                BundleDropReward reward = (BundleDropReward) entries.get(absolute);
                plugin.bundleDropRewards().add(reward);
                plugin.saveBundleDropSettings();
                openPaged(player, holder.pageType(), page);
            }
            case LOOT -> {
                BundleDropReward reward = (BundleDropReward) entries.get(absolute);
                int step = event.isShiftClick() ? 8 : 1;
                upsertMaterialReward(reward.key(), step);
                plugin.saveBundleDropSettings();
                openPaged(player, holder.pageType(), page);
            }
            case REWARDS -> {
                BundleDropReward reward = plugin.bundleDropRewards().get(absolute);
                if (event.getClick() == ClickType.MIDDLE) {
                    plugin.bundleDropRewards().remove(absolute);
                } else if (reward.type() == BundleDropRewardType.MATERIAL) {
                    int delta = event.isShiftClick() ? 8 : 1;
                    int next = reward.amount();
                    if (event.getClick().isLeftClick()) {
                        next = Math.min(64, next + delta);
                    } else if (event.getClick().isRightClick()) {
                        next = Math.max(1, next - delta);
                    } else {
                        return;
                    }
                    plugin.bundleDropRewards().set(absolute, new BundleDropReward(reward.type(), reward.key(), reward.level(), next));
                } else if (event.getClick().isLeftClick()) {
                    plugin.bundleDropRewards().add(reward);
                } else {
                    return;
                }
                plugin.saveBundleDropSettings();
                int maxPage = Math.max(0, (plugin.bundleDropRewards().size() - 1) / PAGE_SIZE);
                openPaged(player, holder.pageType(), Math.min(page, maxPage));
            }
            case EXTRA -> {
                if (event.getClick().isLeftClick()) {
                    ItemStack copy = plugin.bundleDropExtraDrops().get(absolute).clone();
                    copy.setAmount(1);
                    plugin.bundleDropExtraDrops().add(copy);
                } else if (event.getClick().isRightClick() || event.getClick() == ClickType.MIDDLE) {
                    plugin.bundleDropExtraDrops().remove(absolute);
                } else {
                    return;
                }
                plugin.saveBundleDropSettings();
                int maxPage = Math.max(0, (plugin.bundleDropExtraDrops().size() - 1) / PAGE_SIZE);
                openPaged(player, holder.pageType(), Math.min(page, maxPage));
            }
            default -> {
            }
        }
    }

    private void addMaterialFromInventory(@NotNull ItemStack stack) {
        Material material = stack.getType();
        if (material.isAir()) {
            return;
        }
        upsertMaterialReward(material.name(), Math.max(1, stack.getAmount()));
    }

    private void addExtraFromInventory(@NotNull ItemStack stack) {
        if (stack.getType().isAir() || stack.getMaxStackSize() > 1) {
            return;
        }
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        plugin.bundleDropExtraDrops().add(copy);
    }

    private void upsertMaterialReward(@NotNull String materialKey, int amountToAdd) {
        for (int i = 0; i < plugin.bundleDropRewards().size(); i++) {
            BundleDropReward existing = plugin.bundleDropRewards().get(i);
            if (existing.type() == BundleDropRewardType.MATERIAL && existing.key().equalsIgnoreCase(materialKey)) {
                int merged = Math.max(1, Math.min(64, existing.amount() + amountToAdd));
                plugin.bundleDropRewards().set(i, new BundleDropReward(BundleDropRewardType.MATERIAL, materialKey, 1, merged));
                return;
            }
        }
        plugin.bundleDropRewards().add(new BundleDropReward(BundleDropRewardType.MATERIAL, materialKey, 1, Math.max(1, Math.min(64, amountToAdd))));
    }

    private void openPaged(@NotNull Player player, @NotNull BundleDropMenuPage pageType, int page) {
        List<?> entries = entriesFor(pageType);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        BundleDropMenuHolder holder = new BundleDropMenuHolder(pageType, safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(titleFor(pageType), NamedTextColor.GOLD)
                        .append(Component.text(" [" + (safePage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, displayItem(pageType, entries.get(i), i));
        }

        inventory.setItem(PREV_SLOT, plugin.createMenuNavPane(safePage > 0, true));
        inventory.setItem(NEXT_SLOT, plugin.createMenuNavPane(safePage + 1 < totalPages, false));
        inventory.setItem(BACK_SLOT, backItem());
        inventory.setItem(PAGE_INFO_SLOT, pageInfoItem(pageType));
        if (pageType == BundleDropMenuPage.REWARDS || pageType == BundleDropMenuPage.LOOT) {
            inventory.setItem(PAGE_COUNTER_SLOT, counterItem());
        }
        if (pageType == BundleDropMenuPage.LOOT || pageType == BundleDropMenuPage.REWARDS || pageType == BundleDropMenuPage.EXTRA) {
            inventory.setItem(PAGE_IMPORT_SLOT, importHintItem(pageType));
        }
        player.openInventory(inventory);
    }

    private @NotNull String titleFor(@NotNull BundleDropMenuPage pageType) {
        return switch (pageType) {
            case ROOT -> plugin.message("menu.bundle.root_title", "Bundle Drop Editor");
            case MOBS -> plugin.message("menu.bundle.mobs_title", "Bundle Drop Mobs (Per-Mob Chance)");
            case ENCHANTS -> plugin.message("menu.bundle.enchants_title", "Bundle Reward Enchants");
            case POTIONS -> plugin.message("menu.bundle.potions_title", "Bundle Reward Potions");
            case LOOT -> plugin.message("menu.bundle.loot_title", "Bundle Reward Loot");
            case REWARDS -> plugin.message("menu.bundle.rewards_title", "Current Bundle Rewards");
            case EXTRA -> plugin.message("menu.bundle.extra_title", "Extra Non-Stackable Drops");
        };
    }

    private @NotNull ItemStack displayItem(@NotNull BundleDropMenuPage pageType, @NotNull Object raw, int absoluteIndex) {
        return switch (pageType) {
            case MOBS -> mobChanceItem((EntityType) raw);
            case ENCHANTS, POTIONS, LOOT -> rewardPreview((BundleDropReward) raw);
            case REWARDS -> configuredRewardItem((BundleDropReward) raw, absoluteIndex);
            case EXTRA -> extraDropItem((ItemStack) raw, absoluteIndex);
            default -> new ItemStack(Material.BARRIER);
        };
    }

    private @NotNull ItemStack configuredRewardItem(@NotNull BundleDropReward reward, int index) {
        ItemStack item = rewardPreview(reward);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Index: " + (index + 1), NamedTextColor.DARK_GRAY));
        if (reward.type() == BundleDropRewardType.MATERIAL) {
            lore.add(Component.text("Left/Right: +/- count", NamedTextColor.GRAY));
            lore.add(Component.text("Shift Left/Right: +/-8", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Left: duplicate this reward", NamedTextColor.GRAY));
        }
        lore.add(Component.text("Middle: remove", NamedTextColor.RED));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack extraDropItem(@NotNull ItemStack raw, int index) {
        ItemStack item = raw.clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Index: " + (index + 1), NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Left: duplicate", NamedTextColor.GRAY));
        lore.add(Component.text("Right/Middle: remove", NamedTextColor.RED));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack rewardPreview(@NotNull BundleDropReward reward) {
        return switch (reward.type()) {
            case ENCHANT -> {
                EnchantType type = EnchantType.fromArg(reward.key());
                if (type == null) {
                    yield new ItemStack(Material.BARRIER);
                }
                yield plugin.createBook(type, Math.max(1, Math.min(reward.level(), type.maxLevel)));
            }
            case POTION -> {
                EnchantType potion = EnchantType.fromArg(reward.key());
                ItemStack item = potion == null ? new ItemStack(Material.POTION) : plugin.createEnchantedItem(potion, reward.level(), Material.POTION);
                yield item == null ? new ItemStack(Material.POTION) : item;
            }
            case MATERIAL -> {
                Material material = SlotParsingUtil.parseMaterial(reward.key());
                yield new ItemStack(material == null ? Material.STONE : material, Math.max(1, reward.amount()));
            }
        };
    }

    private @NotNull ItemStack mobChanceItem(@NotNull EntityType type) {
        double chance = plugin.bundleDropMobChances().getOrDefault(type, 0.0D);
        boolean enabled = chance > 0.0D;
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(type.name().toLowerCase(Locale.ROOT), enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            meta.lore(List.of(
                    Component.text("Chance: " + StructureInjectorUtil.formatPercent(chance), enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text("Left/Right: +/-1% (Shift +/-5%)", NamedTextColor.GRAY),
                    Component.text("Middle: toggle on/off (uses default chance)", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull List<?> entriesFor(@NotNull BundleDropMenuPage pageType) {
        return switch (pageType) {
            case MOBS -> mobEntries();
            case ENCHANTS -> enchantRewardEntries();
            case POTIONS -> potionRewardEntries();
            case LOOT -> materialRewardEntries();
            case REWARDS -> new ArrayList<>(plugin.bundleDropRewards());
            case EXTRA -> new ArrayList<>(plugin.bundleDropExtraDrops());
            default -> List.of();
        };
    }

    private @NotNull List<EntityType> mobEntries() {
        List<EntityType> entries = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive() || !type.isSpawnable()) {
                continue;
            }
            Class<?> clazz = type.getEntityClass();
            if (clazz == null || !LivingEntity.class.isAssignableFrom(clazz) || type == EntityType.PLAYER) {
                continue;
            }
            entries.add(type);
        }
        entries.sort(Comparator.comparing(Enum::name));
        return entries;
    }

    private @NotNull List<BundleDropReward> enchantRewardEntries() {
        List<BundleDropReward> rewards = new ArrayList<>();
        for (EnchantType type : plugin.activeEnchantTypes()) {
            if (plugin.isRetiredEnchant(type) || type.slot == ArmorSlot.POTION) {
                continue;
            }
            for (int level = 1; level <= type.maxLevel; level++) {
                rewards.add(new BundleDropReward(BundleDropRewardType.ENCHANT, type.arg, level, 1));
            }
        }
        rewards.sort(Comparator.comparing((BundleDropReward reward) -> reward.key()).thenComparingInt(BundleDropReward::level));
        return rewards;
    }

    private @NotNull List<BundleDropReward> potionRewardEntries() {
        List<BundleDropReward> rewards = new ArrayList<>();
        for (EnchantType type : plugin.activeEnchantTypes()) {
            if (plugin.isRetiredEnchant(type) || type.slot != ArmorSlot.POTION) {
                continue;
            }
            for (int level = 1; level <= type.maxLevel; level++) {
                rewards.add(new BundleDropReward(BundleDropRewardType.POTION, type.arg, level, 1));
            }
        }
        rewards.sort(Comparator.comparing((BundleDropReward reward) -> reward.key()).thenComparingInt(BundleDropReward::level));
        return rewards;
    }

    private @NotNull List<BundleDropReward> materialRewardEntries() {
        List<BundleDropReward> rewards = new ArrayList<>();
        for (Material material : BASIC_LOOT) {
            rewards.add(new BundleDropReward(BundleDropRewardType.MATERIAL, material.name(), 1, 1));
        }
        return rewards;
    }

    private @NotNull ItemStack enabledItem() {
        boolean enabled = plugin.isBundleDropEnabled();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Bundle Drops: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            meta.lore(List.of(Component.text("Click to toggle runtime drop system.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack defaultChanceItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Default Mob Chance: " + StructureInjectorUtil.formatPercent(plugin.getBundleDropChancePercent()), NamedTextColor.YELLOW));
            meta.lore(List.of(
                    Component.text("Used when toggling mobs on with middle-click.", NamedTextColor.GRAY),
                    Component.text("Left/Right: +/-1% (Shift +/-5%)", NamedTextColor.GRAY),
                    Component.text("Middle: reset to 5%", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack mobsItem() {
        ItemStack item = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int selected = 0;
            for (double chance : plugin.bundleDropMobChances().values()) {
                if (chance > 0.0D) {
                    selected++;
                }
            }
            meta.displayName(Component.text("Select Mobs + Chances", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Mobs with chance > 0: " + selected, NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack counterItem() {
        int used = runtimeService.calculateBundleWeight(plugin.bundleDropRewards());
        ItemStack item = new ItemStack(Material.BUNDLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor color = used >= 64 ? NamedTextColor.RED : NamedTextColor.YELLOW;
            meta.displayName(Component.text("Bundle Capacity: " + used + "/64", color));
            meta.lore(List.of(
                    Component.text("Shows effective bundle fill weight.", NamedTextColor.GRAY),
                    Component.text("Non-stackable items consume large weight.", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack openEditorItem(@NotNull Material material, @NotNull String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Click to open", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack clearItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clear Bundle Config", NamedTextColor.RED));
            meta.lore(List.of(Component.text("Clears mobs, bundle rewards, and extra drops.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Close", NamedTextColor.RED));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Return to bundle drop editor.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean tryImportFromCursor(@NotNull Player player,
                                        @NotNull BundleDropMenuHolder holder,
                                        @NotNull InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= PAGE_SIZE) {
            return false;
        }
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return false;
        }
        if (holder.pageType() == BundleDropMenuPage.EXTRA) {
            addExtraFromInventory(cursor);
        } else {
            addMaterialFromInventory(cursor);
        }
        plugin.saveBundleDropSettings();
        openPaged(player, holder.pageType(), holder.page());
        return true;
    }

    private @NotNull ItemStack pageInfoItem(@NotNull BundleDropMenuPage pageType) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text("Controls", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        switch (pageType) {
            case LOOT -> {
                lore.add(Component.text("Click catalog entries to add loot.", NamedTextColor.GRAY));
                lore.add(Component.text("From your inventory: click/shift-click to import.", NamedTextColor.GRAY));
            }
            case EXTRA -> {
                lore.add(Component.text("Add non-stackables dropped with the bundle.", NamedTextColor.GRAY));
                lore.add(Component.text("From your inventory: click/shift-click to import.", NamedTextColor.GRAY));
            }
            case REWARDS -> {
                lore.add(Component.text("Left/Right: adjust material counts.", NamedTextColor.GRAY));
                lore.add(Component.text("Middle: remove entry.", NamedTextColor.GRAY));
                lore.add(Component.text("Import: click/shift-click items from your inventory.", NamedTextColor.DARK_GRAY));
            }
            default -> lore.add(Component.text("Use item clicks to edit entries.", NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack importHintItem(@NotNull BundleDropMenuPage pageType) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text("Inventory Import", NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Drag onto this menu or click items below.", NamedTextColor.GRAY));
        if (pageType == BundleDropMenuPage.EXTRA) {
            lore.add(Component.text("Extra drops only accept non-stackables.", NamedTextColor.DARK_GRAY));
        } else if (pageType == BundleDropMenuPage.REWARDS) {
            lore.add(Component.text("Imports become material entries in bundle rewards.", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(Component.text("Loot imports merge by material type.", NamedTextColor.DARK_GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack rootHelpItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Quick Flow", NamedTextColor.YELLOW));
            meta.lore(List.of(
                    Component.text("1) Set mob chances", NamedTextColor.GRAY),
                    Component.text("2) Configure bundle rewards", NamedTextColor.GRAY),
                    Component.text("3) Add optional extra non-stackables", NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void decorateRoot(@NotNull Inventory inventory) {
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack field = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < MENU_SIZE; i++) {
            inventory.setItem(i, border);
        }
        int[] fieldSlots = {
                ROOT_ENABLED_SLOT, ROOT_DEFAULT_CHANCE_SLOT, ROOT_MOBS_SLOT, ROOT_COUNTER_SLOT,
                ROOT_ENCHANTS_SLOT, ROOT_POTIONS_SLOT, ROOT_LOOT_SLOT, ROOT_REWARDS_SLOT,
                ROOT_EXTRA_SLOT, ROOT_HELP_SLOT
        };
        for (int slot : fieldSlots) {
            inventory.setItem(slot, field);
        }
    }

    private @NotNull ItemStack pane(@NotNull Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY));
            filler.setItemMeta(meta);
        }
        return filler;
    }
}
