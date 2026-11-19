package com.pockyl.handson_delight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.BoardDishes;

/** Fabric entry point, both sides. */
public final class HandsOnDelightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HandsOnDelight.init();
        PayloadTypeRegistry.clientboundPlay().register(BoardDishes.Payload.TYPE, BoardDishes.Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BoardDishes.NoBoard.TYPE, BoardDishes.NoBoard.CODEC);
        // Ensure client-side recipe books and station previews receive campfire, smelting & crafting recipes on 26.3.
        RecipeSynchronization.synchronizeRecipeSerializer(CampfireCookingRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(SmeltingRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapedRecipe.SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessRecipe.SERIALIZER);
    }
}
