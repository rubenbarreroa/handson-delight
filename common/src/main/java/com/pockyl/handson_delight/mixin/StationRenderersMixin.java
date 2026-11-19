package com.pockyl.handson_delight.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

import com.pockyl.handson_delight.client.HandsOnDelightClient;
import com.pockyl.handson_delight.station.KitchenBoard;

/**
 * Suppresses vanilla/Farmer's Delight block-entity rendering while a Diegesis scene is actively drawing
 * the food on the Cutting Board, Skillet, Stove, or Cooking Pot, avoiding duplicate rendering.
 */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class StationRenderersMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void handson$drawnByScene(E blockEntity, float partialTick,
            ModelFeatureRenderer.CrumblingOverlay crumbling, boolean isSpecial, CallbackInfoReturnable<S> callback) {
        boolean drawnByScene = blockEntity instanceof AbstractStoveBlockEntity
                || blockEntity instanceof SkilletBlockEntity
                || blockEntity instanceof CookingPotBlockEntity
                || (blockEntity instanceof KitchenBoard board && !board.handson$isCarving() && !board.handson$rendersBoard());
        if (drawnByScene && HandsOnDelightClient.isSceneDrawn(blockEntity.getBlockPos())) {
            callback.setReturnValue(null);
        }
    }
}
