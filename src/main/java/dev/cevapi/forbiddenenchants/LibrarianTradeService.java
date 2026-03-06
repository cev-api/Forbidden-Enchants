package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class LibrarianTradeService {
    private static final int DEFAULT_MAX_USES = 12;
    private static final int DEFAULT_VILLAGER_XP = 2;
    private static final float DEFAULT_PRICE_MULTIPLIER = 0.05F;

    private final ForbiddenEnchantsPlugin plugin;

    LibrarianTradeService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void onVillagerAcquireTrade(@NotNull VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }
        if (!plugin.isLibrarianTradesEnabled()) {
            return;
        }

        List<LibrarianTradeEntry> entries = plugin.librarianTrades();
        if (entries.isEmpty()) {
            return;
        }

        List<MerchantRecipe> currentRecipes = new ArrayList<>(villager.getRecipes());
        boolean changed = false;

        for (LibrarianTradeEntry entry : entries) {
            if (entry.chancePercent() <= 0.0D) {
                continue;
            }
            if (!roll(entry.chancePercent())) {
                continue;
            }
            if (containsConfiguredTrade(currentRecipes, entry)) {
                continue;
            }
            currentRecipes.add(buildRecipe(entry));
            changed = true;
        }

        if (changed) {
            villager.setRecipes(currentRecipes);
        }
    }

    private boolean roll(double chancePercent) {
        return ThreadLocalRandom.current().nextDouble(100.0D) < Math.max(0.0D, Math.min(100.0D, chancePercent));
    }

    private @NotNull MerchantRecipe buildRecipe(@NotNull LibrarianTradeEntry entry) {
        ItemStack result = plugin.createBook(entry.type(), entry.level());
        MerchantRecipe recipe = new MerchantRecipe(result, 0, DEFAULT_MAX_USES, true, DEFAULT_VILLAGER_XP, DEFAULT_PRICE_MULTIPLIER);
        recipe.addIngredient(new ItemStack(Material.EMERALD, clamp(entry.emeraldCost(), 1, 64)));
        if (entry.bookCost() > 0) {
            recipe.addIngredient(new ItemStack(Material.BOOK, clamp(entry.bookCost(), 1, 64)));
        }
        return recipe;
    }

    private boolean containsConfiguredTrade(@NotNull List<MerchantRecipe> recipes, @NotNull LibrarianTradeEntry expected) {
        for (MerchantRecipe recipe : recipes) {
            BookSpec spec = plugin.readBookSpec(recipe.getResult());
            if (spec == null || spec.type() != expected.type() || spec.level() != expected.level()) {
                continue;
            }

            int emeraldCost = ingredientAmount(recipe, Material.EMERALD);
            int bookCost = ingredientAmount(recipe, Material.BOOK);
            if (emeraldCost == clamp(expected.emeraldCost(), 1, 64)
                    && bookCost == clamp(expected.bookCost(), 0, 64)) {
                return true;
            }
        }
        return false;
    }

    private int ingredientAmount(@NotNull MerchantRecipe recipe, @NotNull Material type) {
        int total = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient != null && ingredient.getType() == type) {
                total += ingredient.getAmount();
            }
        }
        return total;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
