package com.pockyl.handson_delight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.diegesis.api.client.DiegesisBuffers;
import com.pockyl.diegesis.api.client.NodePainter;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationView;

/**
 * Placeholder painter for mods that attach a custom BlockEntityRenderer to CookingPotBlockEntity.
 * Farmer's Delight renders the cooking pot purely via its blockstate model (overridden by PotModels),
 * so no extra BlockEntityRenderer pass is needed on 26.3.
 */
final class PotBody implements NodePainter {
    PotBody(StationView view, NodeView node) {
    }

    @Override
    public void paint(PoseStack poseStack, DiegesisBuffers buffers, int light) {
    }
}
