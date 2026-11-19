package com.pockyl.handson_delight;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.DoughRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Small Minecraft 26.3 compatibility helpers for NBT ItemStack serialization, crafting remainders, ingredients, and recipes. */
public final class Nbt26 {
    private Nbt26() {
    }

    public static Ingredient ingredientOfStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return null;
        }
        List<ItemStack> nonEmpty = stacks.stream().filter(s -> !s.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            return null;
        }
        return Ingredient.of(nonEmpty.stream().map(ItemStack::getItem));
    }

    public static ItemStack potContainer(CookingPotRecipe recipe) {
        ItemStackTemplate c = recipe.container();
        return c == null ? ItemStack.EMPTY : c.create();
    }

    public static ItemStack recipeResult(Recipe<?> recipe) {
        if (recipe instanceof CookingPotRecipe pot) {
            return pot.result().create();
        }
        if (recipe instanceof CuttingBoardRecipe cutting) {
            List<ItemStackTemplate> results = cutting.getResults();
            return results.isEmpty() ? ItemStack.EMPTY : results.get(0).create();
        }
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.assemble(CraftingInput.EMPTY);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return shapeless.assemble(CraftingInput.EMPTY);
        }
        if (recipe instanceof DoughRecipe dough) {
            return dough.assemble(CraftingInput.EMPTY);
        }
        if (recipe instanceof CraftingRecipe crafting) {
            try {
                return crafting.assemble(CraftingInput.EMPTY);
            } catch (RuntimeException ignored) {
                return ItemStack.EMPTY;
            }
        }
        if (recipe instanceof SingleItemRecipe single) {
            return single.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        }
        return ItemStack.EMPTY;
    }

    public static List<Ingredient> recipeIngredients(Recipe<?> recipe) {
        if (recipe instanceof CookingPotRecipe pot) {
            return pot.input();
        }
        if (recipe instanceof CuttingBoardRecipe cutting) {
            return List.of(cutting.getInput());
        }
        if (recipe instanceof ShapedRecipe shaped) {
            List<Ingredient> out = new ArrayList<>();
            for (Optional<Ingredient> opt : shaped.getIngredients()) {
                opt.ifPresent(out::add);
            }
            return out;
        }
        if (recipe instanceof SingleItemRecipe single) {
            return List.of(single.input());
        }
        return recipe.placementInfo().ingredients();
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> allRecipesFor(Level level, RecipeType<T> type) {
        if (level == null) {
            return List.of();
        }
        if (level instanceof ServerLevel serverLevel) {
            return List.copyOf(serverLevel.recipeAccess().getAllOfType(type));
        }
        return List.copyOf(level.recipeAccess().getSynchronizedRecipes().getAllOfType(type));
    }

    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(Level level, RecipeType<T> type, I input) {
        if (level == null) {
            return Optional.empty();
        }
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess().getRecipeFor(type, input, level);
        }
        return level.recipeAccess().getSynchronizedRecipes().getFirstMatch(type, input, level);
    }

    public static List<ItemStack> instantiateAll(List<ItemStackTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }
        return templates.stream().map(ItemStackTemplate::create).toList();
    }

    public static Tag saveOptional(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        return ItemStack.OPTIONAL_CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .result()
                .orElseGet(CompoundTag::new);
    }

    public static ItemStack parseOptional(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.OPTIONAL_CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                .result()
                .orElse(ItemStack.EMPTY);
    }

    public static ItemStack craftingRemainder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    public static ItemStack[] ingredientItems(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return new ItemStack[0];
        }
        List<ItemStack> list = ingredient.items().map(ItemStack::new).toList();
        return list.toArray(ItemStack[]::new);
    }
}
