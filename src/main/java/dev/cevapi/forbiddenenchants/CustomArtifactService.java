package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class CustomArtifactService {
    private static final String ID_MINERS_VOID_STEP = "miners_void_step";
    private static final String ID_VOID_STICK = "void_stick";
    private static final String ID_SWAP = "swap";
    private static final String ID_FARMERS_DREAM = "farmers_dream";
    private static final String ID_LAVA_STEP = "lava_step";
    private static final String ID_VITALITY_THIEF = "vitality_thief";
    private static final String ID_LIFE_SPIRIT = "life_spirit";
    private static final String ID_LIFE_STEAL = "life_steal";
    private static final String ID_FIREBALL = "fireball";

    private static final int VOID_STICK_MAX_USES = 5;
    private static final double FARMERS_DREAM_MAX_DISTANCE = 500.0D;
    private static final double LAVA_STEP_MAX_DISTANCE = 1000.0D;
    private static final int LAVA_STEP_REVERT_TICKS = 80;
    private static final double MAX_GOLDEN_HEARTS_ABSORPTION = 20.0D;

    private final NamespacedKey artifactIdKey;
    private final NamespacedKey artifactUsesKey;
    private final NamespacedKey artifactDistanceKey;
    private final Map<String, TempLavaBlock> temporaryLavaPath = new HashMap<>();
    private final Map<UUID, Integer> lifeStealHeartsGained = new HashMap<>();
    private final Map<UUID, Integer> lifeSpiritHeartsGained = new HashMap<>();

    CustomArtifactService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.artifactIdKey = new NamespacedKey(plugin, "artifact_id");
        this.artifactUsesKey = new NamespacedKey(plugin, "artifact_uses_remaining");
        this.artifactDistanceKey = new NamespacedKey(plugin, "artifact_distance_remaining");
    }

    void processTick(long tickCounter) {
        Iterator<Map.Entry<String, TempLavaBlock>> it = temporaryLavaPath.entrySet().iterator();
        while (it.hasNext()) {
            TempLavaBlock t = it.next().getValue();
            if (tickCounter < t.revertTick()) {
                continue;
            }
            World world = Bukkit.getWorld(t.worldName());
            if (world != null) {
                Block block = world.getBlockAt(t.x(), t.y(), t.z());
                if (block.getType() == Material.OBSIDIAN) {
                    block.setBlockData(t.originalData(), false);
                }
            }
            it.remove();
        }
    }

    void clearRuntime() {
        for (TempLavaBlock t : temporaryLavaPath.values()) {
            World world = Bukkit.getWorld(t.worldName());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(t.x(), t.y(), t.z());
            if (block.getType() == Material.OBSIDIAN) {
                block.setBlockData(t.originalData(), false);
            }
        }
        temporaryLavaPath.clear();
    }

    @NotNull List<String> artifactKeys() {
        return List.of(ID_MINERS_VOID_STEP, ID_VOID_STICK, ID_SWAP, ID_FARMERS_DREAM, ID_LAVA_STEP, ID_VITALITY_THIEF, ID_LIFE_SPIRIT, ID_LIFE_STEAL, ID_FIREBALL);
    }

    @Nullable ItemStack createArtifactItem(@NotNull String requested) {
        String key = normalize(requested);
        return switch (key) {
            case ID_MINERS_VOID_STEP -> createPotion(ID_MINERS_VOID_STEP, "Miners Void Step", Color.TEAL, List.of("Underground: teleport to surface.", "Above ground: drop to nearest safe low cave."));
            case ID_SWAP -> createPotion(ID_SWAP, "Swap", Color.FUCHSIA, List.of("Swap positions with the nearest player.", "No players online: teleport to spawn."));
            case ID_VITALITY_THIEF -> createPotion(ID_VITALITY_THIEF, "Vitality Thief", Color.RED, List.of("Steal nearest player's health and vitality.", "No player nearby: purge mobs and gain 5 gold hearts."));
            case ID_LIFE_SPIRIT -> createPotion(ID_LIFE_SPIRIT, "Life Spirit", Color.LIME, List.of("Gain one permanent extra heart."));
            case ID_LIFE_STEAL -> createPotion(ID_LIFE_STEAL, "Life Steal", Color.MAROON, List.of("Steal one max heart from nearest player.", "That heart is added to your own."));
            case ID_VOID_STICK -> createVoidStick();
            case ID_FIREBALL -> createFireballStick();
            case ID_FARMERS_DREAM -> createDistanceBoots(ID_FARMERS_DREAM, "Farmer's Dream", FARMERS_DREAM_MAX_DISTANCE, List.of("Walking over crops instantly grows them.", "Expires after 500 blocks."));
            case ID_LAVA_STEP -> createDistanceBoots(ID_LAVA_STEP, "Lava Step", LAVA_STEP_MAX_DISTANCE, List.of("Walk over lava by solidifying it briefly.", "Expires after 1000 blocks."));
            default -> null;
        };
    }

    void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        String artifact = artifactId(event.getItem());
        if (artifact == null) {
            return;
        }
        Player player = event.getPlayer();
        switch (artifact) {
            case ID_MINERS_VOID_STEP -> consumeMinersVoidStep(player);
            case ID_SWAP -> consumeSwap(player);
            case ID_VITALITY_THIEF -> consumeVitalityThief(player);
            case ID_LIFE_SPIRIT -> consumeLifeSpirit(player);
            case ID_LIFE_STEAL -> consumeLifeSteal(player);
            default -> {
            }
        }
    }

    void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        // Left-click behavior is handled in interact; no-op here to avoid conflicts.
    }

    void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Player player = event.getPlayer();
        if (isArtifact(item, ID_VOID_STICK)) {
            Location destination = findFarthestSafeAlongDirection(player.getLocation(), 200.0D);
            if (destination == null) {
                player.sendActionBar(Component.text("Void Stick: no safe destination found.", NamedTextColor.RED));
                return;
            }
            player.teleport(destination);
            player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.1F);
            decrementVoidStickUse(player, item);
            event.setCancelled(true);
            return;
        }
        if (isArtifact(item, ID_FIREBALL)) {
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setDirection(player.getLocation().getDirection().normalize());
            fireball.setVelocity(player.getLocation().getDirection().normalize().multiply(1.6D));
            fireball.setYield(2.0F);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8F, 1.0F);
            event.setCancelled(true);
        }
    }

    void onPlayerMove(@NotNull PlayerMoveEvent event, long tickCounter) {
        Location to = event.getTo();
        if (to == null || event.getFrom().getWorld() != to.getWorld()) {
            return;
        }
        double dx = to.getX() - event.getFrom().getX();
        double dz = to.getZ() - event.getFrom().getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType() == Material.AIR) {
            return;
        }

        if (isArtifact(boots, ID_FARMERS_DREAM)) {
            if (consumeBootDistance(player, boots, horizontal)) {
                growNearbyCrops(to.getBlock());
            }
            return;
        }
        if (isArtifact(boots, ID_LAVA_STEP) && consumeBootDistance(player, boots, horizontal)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false, false), true);
            solidifyLavaUnderPlayer(to, tickCounter);
        }
    }

    void onPlayerDeath(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        int lifeStealHearts = lifeStealHeartsGained.getOrDefault(playerId, 0);
        int lifeSpiritHearts = lifeSpiritHeartsGained.getOrDefault(playerId, 0);
        if (lifeStealHearts <= 0 && lifeSpiritHearts <= 0) {
            return;
        }

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (lifeStealHearts > 0) {
            lifeStealHeartsGained.remove(playerId);
            maxHealth.setBaseValue(Math.max(2.0D, maxHealth.getBaseValue() - (lifeStealHearts * 2.0D)));
        }
        if (lifeSpiritHearts > 0) {
            int remaining = lifeSpiritHearts - 1;
            if (remaining > 0) {
                lifeSpiritHeartsGained.put(playerId, remaining);
            } else {
                lifeSpiritHeartsGained.remove(playerId);
            }
            maxHealth.setBaseValue(Math.max(2.0D, maxHealth.getBaseValue() - 2.0D));
        }
    }

    void onEnchantItem(@NotNull EnchantItemEvent event) {
        if (!isArtifactBoots(event.getItem())) {
            return;
        }
        event.getEnchantsToAdd().remove(Enchantment.UNBREAKING);
        event.getEnchantsToAdd().remove(Enchantment.MENDING);
    }

    void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);
        if (!isArtifactBoots(left) && !isArtifactBoots(right)) {
            return;
        }
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        Map<Enchantment, Integer> enchants = result.getEnchantments();
        if (enchants.containsKey(Enchantment.UNBREAKING) || enchants.containsKey(Enchantment.MENDING)) {
            event.setResult(null);
        }
    }

    private @NotNull ItemStack createPotion(@NotNull String id, @NotNull String name, @NotNull Color color, @NotNull List<String> loreLines) {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(color);
            meta = potionMeta;
        }
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.AQUA));
            meta.lore(toLore(loreLines, false));
            meta.getPersistentDataContainer().set(artifactIdKey, PersistentDataType.STRING, id);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private @NotNull ItemStack createVoidStick() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Void Stick", NamedTextColor.DARK_AQUA));
            meta.lore(toLore(List.of("Attack to teleport forward up to 200 blocks.", "Breaks after 5 teleports."), false));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(artifactIdKey, PersistentDataType.STRING, ID_VOID_STICK);
            pdc.set(artifactUsesKey, PersistentDataType.INTEGER, VOID_STICK_MAX_USES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createFireballStick() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fireball", NamedTextColor.GOLD));
            meta.lore(toLore(List.of("Right-click to launch a fireball."), false));
            meta.getPersistentDataContainer().set(artifactIdKey, PersistentDataType.STRING, ID_FIREBALL);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createDistanceBoots(@NotNull String id, @NotNull String name, double distance, @NotNull List<String> loreLines) {
        ItemStack item = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GREEN));
            meta.lore(toLore(loreLines, true));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(artifactIdKey, PersistentDataType.STRING, id);
            pdc.set(artifactDistanceKey, PersistentDataType.DOUBLE, distance);
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull List<Component> toLore(@NotNull List<String> lines, boolean includeRestriction) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        if (includeRestriction) {
            lore.add(Component.text("Unbreaking and Mending cannot be applied.", NamedTextColor.DARK_GRAY));
        }
        return lore;
    }

    private void consumeMinersVoidStep(@NotNull Player player) {
        Location current = player.getLocation();
        World world = current.getWorld();
        if (world == null) {
            return;
        }

        int x = current.getBlockX();
        int z = current.getBlockZ();
        int highest = world.getHighestBlockYAt(x, z);
        if (current.getY() < highest - 1) {
            Location surface = findSafeAtOrAbove(world, x, z, highest + 1);
            if (surface != null) {
                player.teleport(surface);
                player.sendActionBar(Component.text("Miners Void Step: surfaced.", NamedTextColor.AQUA));
            }
            return;
        }

        Location cave = findLowestNearbyCave(player, 24);
        if (cave == null) {
            cave = findLowestInColumn(world, x, z, (int) Math.floor(current.getY()));
        }
        if (cave != null) {
            player.teleport(cave);
            player.sendActionBar(Component.text("Miners Void Step: dropped to a safe cave.", NamedTextColor.AQUA));
        }
    }

    private void consumeSwap(@NotNull Player player) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            player.teleport(player.getWorld().getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
            player.sendActionBar(Component.text("Swap: no players online, warped to spawn.", NamedTextColor.LIGHT_PURPLE));
            return;
        }

        Location playerLoc = player.getLocation().clone();
        Location nearestLoc = nearest.getLocation().clone();
        player.teleport(nearestLoc);
        nearest.teleport(playerLoc);
        player.sendActionBar(Component.text("Swap: position swapped.", NamedTextColor.LIGHT_PURPLE));
        nearest.sendActionBar(Component.text("Swap: position swapped.", NamedTextColor.LIGHT_PURPLE));
    }

    private void consumeVitalityThief(@NotNull Player player) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            int killed = 0;
            int heartsGained = 0;
            double absorption = player.getAbsorptionAmount();
            int heartsUntilCap = (int) Math.floor((MAX_GOLDEN_HEARTS_ABSORPTION - absorption) / 2.0D);
            if (heartsUntilCap <= 0) {
                player.sendActionBar(Component.text("Vitality Thief: golden hearts already maxed.", NamedTextColor.RED));
                return;
            }
            for (Entity entity : player.getNearbyEntities(20.0D, 20.0D, 20.0D)) {
                if (heartsUntilCap <= 0) {
                    break;
                }
                if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                    living.damage(1000.0D, player);
                    killed++;
                    heartsGained++;
                    heartsUntilCap--;
                }
            }
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                player.setHealth(maxHealth.getValue());
            }
            player.setAbsorptionAmount(Math.min(MAX_GOLDEN_HEARTS_ABSORPTION, player.getAbsorptionAmount() + (heartsGained * 2.0D)));
            player.sendActionBar(Component.text("Vitality Thief: drained " + killed + " mobs and gained " + heartsGained + " gold hearts.", NamedTextColor.RED));
            return;
        }

        AttributeInstance selfHealth = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance otherHealth = nearest.getAttribute(Attribute.MAX_HEALTH);
        if (selfHealth == null || otherHealth == null) {
            return;
        }

        double selfBase = selfHealth.getBaseValue();
        double otherBase = otherHealth.getBaseValue();
        double selfCurrent = player.getHealth();
        double otherCurrent = nearest.getHealth();
        double selfAbsorption = player.getAbsorptionAmount();
        double otherAbsorption = nearest.getAbsorptionAmount();
        selfHealth.setBaseValue(otherBase);
        otherHealth.setBaseValue(selfBase);
        player.setHealth(Math.min(otherCurrent, selfHealth.getValue()));
        nearest.setHealth(Math.min(selfCurrent, otherHealth.getValue()));
        player.setAbsorptionAmount(otherAbsorption);
        nearest.setAbsorptionAmount(selfAbsorption);

        player.sendActionBar(Component.text("Vitality Thief: vitality swapped.", NamedTextColor.RED));
        nearest.sendActionBar(Component.text("Vitality Thief: vitality swapped.", NamedTextColor.RED));
    }

    private void consumeLifeSpirit(@NotNull Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.setBaseValue(maxHealth.getBaseValue() + 2.0D);
        UUID playerId = player.getUniqueId();
        lifeSpiritHeartsGained.put(playerId, lifeSpiritHeartsGained.getOrDefault(playerId, 0) + 1);
        player.sendActionBar(Component.text("Life Spirit: +1 heart gained.", NamedTextColor.GREEN));
    }

    private void consumeLifeSteal(@NotNull Player player) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            player.sendActionBar(Component.text("Life Steal: no target player.", NamedTextColor.RED));
            return;
        }

        AttributeInstance selfMax = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance otherMax = nearest.getAttribute(Attribute.MAX_HEALTH);
        if (selfMax == null || otherMax == null) {
            return;
        }
        double otherBase = otherMax.getBaseValue();
        if (otherBase <= 2.0D) {
            player.sendActionBar(Component.text("Life Steal: target cannot lose more hearts.", NamedTextColor.RED));
            return;
        }

        selfMax.setBaseValue(selfMax.getBaseValue() + 2.0D);
        otherMax.setBaseValue(Math.max(2.0D, otherBase - 2.0D));
        lifeStealHeartsGained.put(player.getUniqueId(), lifeStealHeartsGained.getOrDefault(player.getUniqueId(), 0) + 1);
        nearest.setHealth(Math.min(nearest.getHealth(), otherMax.getValue()));
        player.sendActionBar(Component.text("Life Steal: stole 1 heart.", NamedTextColor.DARK_RED));
        nearest.sendActionBar(Component.text("Life Steal: 1 heart stolen.", NamedTextColor.DARK_RED));
    }

    private boolean consumeBootDistance(@NotNull Player player, @NotNull ItemStack boots, double distanceMoved) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return false;
        }

        String id = artifactId(boots);
        double defaultDistance = ID_LAVA_STEP.equals(id) ? LAVA_STEP_MAX_DISTANCE : FARMERS_DREAM_MAX_DISTANCE;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double stored = pdc.get(artifactDistanceKey, PersistentDataType.DOUBLE);
        double remaining = (stored == null ? defaultDistance : stored) - distanceMoved;

        if (remaining <= 0.0D) {
            player.getInventory().setBoots(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.sendActionBar(Component.text(prettyName(id) + " has broken.", NamedTextColor.RED));
            return false;
        }

        pdc.set(artifactDistanceKey, PersistentDataType.DOUBLE, remaining);
        boots.setItemMeta(meta);
        player.getInventory().setBoots(boots);
        return true;
    }

    private void growNearbyCrops(@NotNull Block reference) {
        growCrop(reference);
        growCrop(reference.getRelative(0, -1, 0));
    }

    private void growCrop(@NotNull Block block) {
        Material type = block.getType();
        if (!Tag.CROPS.isTagged(type) && type != Material.NETHER_WART && type != Material.COCOA) {
            return;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }
        ageable.setAge(ageable.getMaximumAge());
        block.setBlockData(ageable, false);
    }

    private void solidifyLavaUnderPlayer(@NotNull Location location, long tickCounter) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int baseY = (int) Math.floor(location.getY()) - 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(location.getBlockX() + dx, baseY, location.getBlockZ() + dz);
                if (block.getType() != Material.LAVA) {
                    continue;
                }
                String key = tempKey(block);
                TempLavaBlock existing = temporaryLavaPath.get(key);
                TempLavaBlock next = existing == null
                        ? new TempLavaBlock(world.getName(), block.getX(), block.getY(), block.getZ(), block.getBlockData().clone(), tickCounter + LAVA_STEP_REVERT_TICKS)
                        : new TempLavaBlock(existing.worldName(), existing.x(), existing.y(), existing.z(), existing.originalData(), tickCounter + LAVA_STEP_REVERT_TICKS);
                temporaryLavaPath.put(key, next);
                block.setType(Material.OBSIDIAN, false);
            }
        }
    }

    private void decrementVoidStickUse(@NotNull Player player, @NotNull ItemStack stick) {
        ItemMeta meta = stick.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer usesValue = pdc.get(artifactUsesKey, PersistentDataType.INTEGER);
        int uses = (usesValue == null ? VOID_STICK_MAX_USES : usesValue) - 1;
        if (uses <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.sendActionBar(Component.text("Void Stick shattered.", NamedTextColor.RED));
            return;
        }
        pdc.set(artifactUsesKey, PersistentDataType.INTEGER, uses);
        meta.lore(toLore(List.of("Attack to teleport forward up to 200 blocks.", "Breaks after 5 teleports.", "Uses remaining: " + uses), false));
        stick.setItemMeta(meta);
        player.getInventory().setItemInMainHand(stick);
    }

    private @Nullable Location findFarthestSafeAlongDirection(@NotNull Location start, double maxDistance) {
        World world = start.getWorld();
        if (world == null) {
            return null;
        }
        Vector direction = start.getDirection().clone().normalize();
        for (int distance = (int) Math.floor(maxDistance); distance >= 1; distance--) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            if (isSafeStandSpot(world, point.getBlockX(), point.getBlockY(), point.getBlockZ())) {
                return centered(world, point.getBlockX(), point.getBlockY(), point.getBlockZ(), start.getYaw(), start.getPitch());
            }
        }
        return null;
    }

    private @Nullable Location findSafeAtOrAbove(@NotNull World world, int x, int z, int startY) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        int safeStart = Math.max(minY, Math.min(startY, maxY));
        for (int y = safeStart; y <= Math.min(maxY, safeStart + 8); y++) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        for (int y = safeStart - 1; y >= minY; y--) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        return null;
    }

    private @Nullable Location findLowestNearbyCave(@NotNull Player player, int radius) {
        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        int minY = world.getMinHeight() + 1;
        int ox = origin.getBlockX();
        int oz = origin.getBlockZ();
        int oy = origin.getBlockY();
        Location best = null;
        int bestY = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = ox + dx;
                int z = oz + dz;
                int surface = world.getHighestBlockYAt(x, z);
                int startY = Math.min(oy - 1, surface - 2);
                for (int y = startY; y >= minY; y--) {
                    if (!isSafeStandSpot(world, x, y, z) || y >= surface - 1) {
                        continue;
                    }
                    Location candidate = centered(world, x, y, z, origin.getYaw(), origin.getPitch());
                    double distance = candidate.distanceSquared(origin);
                    if (y < bestY || (y == bestY && distance < bestDistance)) {
                        bestY = y;
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private @Nullable Location findLowestInColumn(@NotNull World world, int x, int z, int fromY) {
        int minY = world.getMinHeight() + 1;
        for (int y = Math.max(minY, fromY); y >= minY; y--) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        return null;
    }

    private boolean isSafeStandSpot(@NotNull World world, int x, int y, int z) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        if (y < minY || y > maxY) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);
        if (!feet.isPassable() || !head.isPassable() || !below.getType().isSolid()) {
            return false;
        }
        return !isDangerous(feet.getType()) && !isDangerous(head.getType()) && !isDangerous(below.getType());
    }

    private boolean isDangerous(@NotNull Material material) {
        return material == Material.LAVA || material == Material.FIRE || material == Material.SOUL_FIRE || material == Material.CACTUS || material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE || material == Material.MAGMA_BLOCK || material == Material.SWEET_BERRY_BUSH;
    }

    private @NotNull Location centered(@NotNull World world, int x, int y, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5D, y, z + 0.5D, yaw, pitch);
    }

    private @Nullable Player nearestOtherPlayer(@NotNull Player source) {
        Location src = source.getLocation();
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(source.getUniqueId()) || candidate.getWorld() != source.getWorld()) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(src);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (!candidate.getUniqueId().equals(source.getUniqueId())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isArtifactBoots(@Nullable ItemStack item) {
        return isArtifact(item, ID_FARMERS_DREAM) || isArtifact(item, ID_LAVA_STEP);
    }

    private boolean isArtifact(@Nullable ItemStack item, @NotNull String id) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(artifactIdKey, PersistentDataType.STRING);
        return id.equals(value);
    }

    private @Nullable String artifactId(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(artifactIdKey, PersistentDataType.STRING);
    }

    private @NotNull String tempKey(@NotNull Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private @NotNull String normalize(@NotNull String text) {
        String normalized = text.toLowerCase(Locale.ROOT).trim().replace("'", "").replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "minersvoidstep", "miners_voidstep", "miners_step" -> ID_MINERS_VOID_STEP;
            case "voidstick" -> ID_VOID_STICK;
            case "farmersdream", "farmer_dream" -> ID_FARMERS_DREAM;
            case "lavastep", "lava_boots" -> ID_LAVA_STEP;
            case "lifespirit" -> ID_LIFE_SPIRIT;
            case "lifesteal" -> ID_LIFE_STEAL;
            case "vitalitythief" -> ID_VITALITY_THIEF;
            default -> normalized;
        };
    }

    private @NotNull String prettyName(@Nullable String id) {
        if (id == null) {
            return "Item";
        }
        return switch (id) {
            case ID_FARMERS_DREAM -> "Farmer's Dream";
            case ID_LAVA_STEP -> "Lava Step";
            case ID_MINERS_VOID_STEP -> "Miners Void Step";
            case ID_VITALITY_THIEF -> "Vitality Thief";
            case ID_LIFE_SPIRIT -> "Life Spirit";
            case ID_LIFE_STEAL -> "Life Steal";
            case ID_VOID_STICK -> "Void Stick";
            case ID_FIREBALL -> "Fireball";
            case ID_SWAP -> "Swap";
            default -> id;
        };
    }

    private record TempLavaBlock(@NotNull String worldName, int x, int y, int z, @NotNull BlockData originalData, long revertTick) {
    }
}
