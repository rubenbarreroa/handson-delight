package com.pockyl.handson_delight.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_delight.assembly.AssemblyRecipes;

/**
 * Loads cutting-board assembly dishes from the server's loaded recipes once recipe finalization finishes on Minecraft 26.3.
 */
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Unique
    private HolderLookup.Provider handson$registries;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void handson$captureRegistries(HolderLookup.Provider registries, CallbackInfo callback) {
        this.handson$registries = registries;
    }

    @Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
    private void handson$keepAssembled(FeatureFlagSet flags, CallbackInfo callback) {
        if (handson$registries != null) {
            AssemblyRecipes.loadFromManager((RecipeManager) (Object) this, handson$registries);
        }
    }
}
