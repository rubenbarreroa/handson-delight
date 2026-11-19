package com.pockyl.handson_delight.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.HandsOnDelight;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Replaces Farmer's Delight's cooking pot blockstate model with this mod's spoon-less hollow pot body
 * so the diegetic station can render its own moving spoon and deep broth interior.
 */
final class PotModels {
    private static final String FILE = "pot_pack/assets/farmersdelight/blockstates/cooking_pot.json";

    private PotModels() {
    }

    static void register() {
        if (!HandsOnConfig.cookingPot()) {
            return;
        }
        ModelLoadingPlugin.register(context -> {
            if (HandsOnConfig.cookingPot()) {
                resolve(context, FarmersDelightContent.COOKING_POT.get(), FILE);
            }
        });
    }

    private static void resolve(ModelLoadingPlugin.Context context, Block block, String file) {
        if (block == Blocks.AIR) {
            return;
        }
        try {
            context.registerBlockStateResolver(block, resolver -> {
                Optional<Map<BlockState, BlockStateModel.UnbakedRoot>> models = read(block, file);
                if (models.isEmpty()) {
                    return;
                }
                for (Map.Entry<BlockState, BlockStateModel.UnbakedRoot> entry : models.get().entrySet()) {
                    resolver.setModel(entry.getKey(), entry.getValue());
                }
            });
        } catch (IllegalArgumentException e) {
            HandsOnDelight.LOGGER.warn("The models of {} are resolved by another mod; this mod's are not used", block, e);
        }
    }

    private static Optional<Map<BlockState, BlockStateModel.UnbakedRoot>> read(Block block, String name) {
        Optional<Path> file = FabricLoader.getInstance().getModContainer(HandsOnDelight.MOD_ID).flatMap(mod -> mod.findPath(name));
        if (file.isEmpty()) {
            HandsOnDelight.LOGGER.error("{} is missing from the mod", name);
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file.get())) {
            JsonElement json = JsonParser.parseReader(reader);
            BlockStateModelDispatcher dispatcher = BlockStateModelDispatcher.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow(msg -> new IllegalStateException("Cannot parse " + name + ": " + msg));
            return Optional.of(dispatcher.instantiate(block.getStateDefinition(), () -> name));
        } catch (IOException | RuntimeException e) {
            HandsOnDelight.LOGGER.error("Cannot read {}", name, e);
            return Optional.empty();
        }
    }
}
