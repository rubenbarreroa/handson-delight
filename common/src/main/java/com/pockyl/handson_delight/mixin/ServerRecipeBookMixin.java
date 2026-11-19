package com.pockyl.handson_delight.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Keeps board-assembled dishes out of vanilla's crafting recipe book for players with the cutting board station enabled.
 */
@Mixin(ServerRecipeBook.class)
abstract class ServerRecipeBookMixin extends RecipeBook {
    @Shadow
    @Final
    protected Set<ResourceKey<Recipe<?>>> known;

    @Shadow
    public abstract void remove(ResourceKey<Recipe<?>> recipe);

    @Shadow
    public abstract int addRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player);

    @Inject(method = "addRecipes", at = @At("HEAD"), cancellable = true)
    private void handson$notTheBoards(Collection<RecipeHolder<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> callback) {
        if (!HandsOnConfig.removeCraftingRecipes() || !Platform.INSTANCE.hasBoard(player)
                || recipes.stream().noneMatch(recipe -> AssemblyRecipes.isMadeOnTheBoard(recipe.id().identifier()))) {
            return;
        }
        callback.setReturnValue(addRecipes(
                recipes.stream().filter(recipe -> !AssemblyRecipes.isMadeOnTheBoard(recipe.id().identifier())).toList(),
                player));
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"))
    private void handson$forgetTheBoards(ServerPlayer player, CallbackInfo callback) {
        if (!HandsOnConfig.removeCraftingRecipes() || !Platform.INSTANCE.hasBoard(player)) {
            return;
        }
        for (ResourceKey<Recipe<?>> key : List.copyOf(known)) {
            if (AssemblyRecipes.isMadeOnTheBoard(key.identifier())) {
                remove(key);
            }
        }
    }
}
