package com.pockyl.handson_delight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.block.SkilletBlock;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.CookingTimes;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.SkilletStation;

import java.util.ArrayList;
import java.util.List;

/** The skillet's book: what the campfire cooks, and how long one takes in the pan, both sides, at the station's pace. */
final class SkilletBook implements RecipeBook.Source {
    @Override
    public List<RecipeBook.Page> pages(StationView view) {
        Level level = view.level();
        List<RecipeBook.Page> pages = new ArrayList<>();
        for (RecipeHolder<CampfireCookingRecipe> holder : com.pockyl.handson_delight.Nbt26.allRecipesFor(level, RecipeType.CAMPFIRE_COOKING)) {
            CampfireCookingRecipe recipe = holder.value();
            ItemStack dish = com.pockyl.handson_delight.Nbt26.recipeResult(recipe);
            if (dish.isEmpty() || recipe.input().isEmpty()) {
                continue;
            }
            int skillet = SkilletBlock.getSkilletCookingTime(recipe.cookingTime(), 0);
            int ticks = 2 * SkilletCooking.sideTicks(skillet, 1, CookingTimes.skillet());
            Component seconds = Component.translatable("handson_delight.book.seconds", Math.round(ticks / 20.0F));
            pages.add(new RecipeBook.Page(holder.id().identifier(), dish.copy(), List.of(recipe.input()),
                    List.of(new ItemStack(Items.CLOCK)), seconds, false));
        }
        return pages;
    }

    @Override
    public void picked(StationView view, RecipeBook.Page page) {
        if (page.ingredients().isEmpty()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (page.ingredients().get(0).test(inventory.getItem(slot))) {
                CompoundTag how = new CompoundTag();
                how.putInt(SkilletStation.FROM, slot);
                if (!Minecraft.getInstance().hasShiftDown()) {
                    how.putBoolean(SkilletStation.ONE, true);
                }
                view.sendAction(SkilletStation.ACTION_PLACE, how);
                return;
            }
        }
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        return List.of(view.state().getItem(SkilletStation.PAN_SLOT));
    }

    /** What fries in the pan. */
    @Override
    public Object current(StationView view) {
        return SkilletCooking.holder(view.level(), view.state().getItem(SkilletStation.PAN_SLOT)).map(holder -> holder.id().identifier()).orElse(null);
    }
}
