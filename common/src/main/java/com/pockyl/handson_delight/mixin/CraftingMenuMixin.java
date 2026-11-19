package com.pockyl.handson_delight.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.Optional;

/**
 * Sandwiches and board dishes are assembled on the cutting board by hand rather than in a player's crafting grid.
 */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
    @ModifyVariable(method = "slotChangedCraftingGrid", at = @At("STORE"), ordinal = 0)
    private static Optional<RecipeHolder<CraftingRecipe>> handson$onTheBoardInstead(Optional<RecipeHolder<CraftingRecipe>> found,
            AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer grid, ResultContainer result,
            RecipeHolder<CraftingRecipe> last) {
        if (found.isPresent() && HandsOnConfig.removeCraftingRecipes() && AssemblyRecipes.isMadeOnTheBoard(found.get().id().identifier())
                && !(player instanceof ServerPlayer server && !Platform.INSTANCE.hasBoard(server))) {
            return Optional.empty();
        }
        return found;
    }
}
