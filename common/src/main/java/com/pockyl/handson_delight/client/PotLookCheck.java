package com.pockyl.handson_delight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.handson_delight.HandsOnDelight;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks once per model reload that the cooking pot block model has this mod's deep hollow floor
 * (without Farmer's Delight's built-in spoon) so the diegetic spoon and soup fit properly.
 */
final class PotLookCheck {
    private static final Identifier STATE_FILE = Identifier.fromNamespaceAndPath("farmersdelight", "blockstates/cooking_pot.json");
    private static final float FLOOR = 2.0F / 16.0F;
    private static final float EPSILON = 1.0E-4F;

    private static BlockStateModel checked;

    private PotLookCheck() {
    }

    static void check(BlockState state, BlockEntity pot) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
        if (model == checked) {
            return;
        }
        checked = model;
        BlockEntityRenderer<?, ?> renderer = pot == null ? null : minecraft.getBlockEntityRenderDispatcher().getRenderer(pot);
        boolean ours = hasOurFloor(model);
        if (ours && renderer == null) {
            return;
        }
        List<Resource> stack = minecraft.getResourceManager().getResourceStack(STATE_FILE);
        String pack = stack.isEmpty() ? "none" : stack.get(stack.size() - 1).sourcePackId();
        HandsOnDelight.LOGGER.warn("Hands-On Delight: the cooking pot in the world is drawn by another mod or resource pack, so the station's "
                        + "spoon and soup may not fit it. BlockStateModel: {} ({}), block state from pack: {}, block entity renderer: {}.",
                model.getClass().getName(), ours ? "this mod's floor found" : "this mod's floor missing", pack,
                renderer == null ? "none" : renderer.getClass().getName());
    }

    private static boolean hasOurFloor(BlockStateModel model) {
        RandomSource random = RandomSource.create(42L);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            for (Direction side : new Direction[] {Direction.UP, null}) {
                for (BakedQuad quad : part.getQuads(side)) {
                    if (quad.direction() == Direction.UP && allAt(quad, FLOOR)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean allAt(BakedQuad quad, float y) {
        return Math.abs(quad.position0().y() - y) <= EPSILON
                && Math.abs(quad.position1().y() - y) <= EPSILON
                && Math.abs(quad.position2().y() - y) <= EPSILON
                && Math.abs(quad.position3().y() - y) <= EPSILON;
    }
}
