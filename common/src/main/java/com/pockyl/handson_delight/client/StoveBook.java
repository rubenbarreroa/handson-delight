package com.pockyl.handson_delight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.CookingTimes;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveStation;

import java.util.ArrayList;
import java.util.List;

/** The stove's book: what the fire cooks and the grill takes (no eggs), each with the time a piece takes on the grill at the station's pace, both sides. */
final class StoveBook implements RecipeBook.Source {
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
            Ingredient food = StoveCooking.grillable(recipe.input());
            if (food == null || food.isEmpty()) {
                continue;
            }
            int ticks = 2 * StoveCooking.sideTicks(recipe.cookingTime(), CookingTimes.stove());
            Component seconds = Component.translatable("handson_delight.book.seconds", Math.round(ticks / 20.0F));
            pages.add(new RecipeBook.Page(holder.id().identifier(), dish.copy(), List.of(food),
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
                how.putInt(StoveStation.FROM, slot);
                if (!Minecraft.getInstance().hasShiftDown()) {
                    how.putBoolean(StoveStation.ONE, true);
                }
                view.sendAction(StoveStation.ACTION_PLACE, how);
                return;
            }
        }
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        List<ItemStack> there = new ArrayList<>();
        for (int place = 0; place < Grill.PLACES; place++) {
            there.add(view.state().getItem(StoveStation.place(place)));
        }
        return there;
    }

    /** What grills on the first place that holds raw food. */
    @Override
    public Object current(StationView view) {
        for (ItemStack stack : inStation(view)) {
            Object id = SkilletCooking.holder(view.level(), stack).map(holder -> holder.id().identifier()).orElse(null);
            if (id != null) {
                return id;
            }
        }
        return null;
    }
}
