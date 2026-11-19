package com.pockyl.handson_delight.station;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import vectorwing.farmersdelight.common.block.PieBlock;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.Nbt26;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Farmer's Delight's cutting recipes, read for the board's station: what an item is cut with and into how many strokes. */
public final class Cutting {
    public static final int MOST_MARKS = 3;
    private static final int STRIPS = 3;
    private static final Map<Level, Cached> CACHE = new WeakHashMap<>();

    private Cutting() {
    }

    public static List<CuttingBoardRecipe> everything(Level level) {
        return cached(level).everything();
    }

    public static List<CuttingBoardRecipe> all(Level level) {
        return cached(level).ordered();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Cached cached(Level level) {
        List<RecipeHolder<?>> found = (List) Nbt26.allRecipesFor(level, (RecipeType) FarmersDelightContent.CUTTING.get());
        Object marker = found.isEmpty() ? null : found.get(0);
        synchronized (CACHE) {
            Cached cached = CACHE.get(level);
            if (cached == null || cached.size() != found.size() || cached.first() != marker) {
                List<CuttingBoardRecipe> everything = new ArrayList<>();
                Map<CuttingBoardRecipe, Identifier> ids = new IdentityHashMap<>();
                for (RecipeHolder<?> holder : found) {
                    if (holder.value() instanceof CuttingBoardRecipe cutting && !cutting.getInput().isEmpty()) {
                        everything.add(cutting);
                        ids.put(cutting, holder.id().identifier());
                    }
                }
                everything.sort(Comparator.comparing(ids::get));
                List<CookingPotRecipe> pot = Platform.INSTANCE.cookingRecipes(level).stream().map(RecipeHolder::value).toList();
                List<CuttingBoardRecipe> ordered = new ArrayList<>(everything.stream().filter(recipe -> isCooking(recipe, pot)).toList());
                everything.stream().filter(recipe -> !ordered.contains(recipe)).forEach(ordered::add);
                cached = new Cached(found.size(), marker, List.copyOf(everything), List.copyOf(ordered), ids);
                CACHE.put(level, cached);
            }
            return cached;
        }
    }

    public static boolean isCooking(CuttingBoardRecipe recipe, List<CookingPotRecipe> pot) {
        for (ItemStack input : Nbt26.ingredientItems(recipe.getInput())) {
            if (input.has(DataComponents.FOOD)) {
                return true;
            }
        }
        List<ItemStack> results = Nbt26.instantiateAll(recipe.getResults());
        for (ItemStack result : results) {
            if (result.has(DataComponents.FOOD) || pot.stream().anyMatch(cooking -> cooking.input().stream().anyMatch(in -> in.test(result)))) {
                return true;
            }
        }
        ItemStack knife = new ItemStack(BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("iron_knife")));
        return !results.isEmpty() && tool(recipe).test(knife) && results.stream().allMatch(result -> pot.stream()
                .map(Nbt26::potContainer).anyMatch(container -> !container.isEmpty() && container.is(result.getItem())));
    }

    private record Cached(int size, Object first, List<CuttingBoardRecipe> everything, List<CuttingBoardRecipe> ordered,
                          Map<CuttingBoardRecipe, Identifier> ids) {
    }

    public static Identifier id(Level level, CuttingBoardRecipe recipe) {
        return cached(level).ids().get(recipe);
    }

    public static List<CuttingBoardRecipe> recipesFor(Level level, ItemStack item) {
        return recipesFor(all(level), item);
    }

    private static List<CuttingBoardRecipe> recipesFor(List<CuttingBoardRecipe> all, ItemStack item) {
        List<CuttingBoardRecipe> found = new ArrayList<>();
        if (!item.isEmpty()) {
            all.stream().filter(recipe -> recipe.getInput().test(item)).forEach(found::add);
        }
        return found;
    }

    public enum Style {
        ACROSS,
        STRAIGHT,
        WEDGES,
        STRIPS
    }

    public record Plan(int marks, Style style) {
    }

    public static Plan plan(Level level, ItemStack item) {
        Style style = item.is(KitchenTags.CUT_IN_WEDGES) || isRound(item) ? Style.WEDGES
                : item.is(KitchenTags.CUT_IN_STRIPS) ? Style.STRIPS
                : item.is(KitchenTags.SLICED_STRAIGHT) ? Style.STRAIGHT : Style.ACROSS;
        if (style == Style.STRIPS) {
            return new Plan(STRIPS, style);
        }
        int pieces = recipesFor(cached(level).ordered(), item).stream().findFirst().map(Cutting::pieces).orElse(2);
        int marks = style == Style.WEDGES ? (pieces + 1) / 2 : pieces - 1;
        return new Plan(Mth.clamp(marks, 1, MOST_MARKS), style);
    }

    private static boolean isRound(ItemStack item) {
        return item.getItem() instanceof BlockItem block && (block.getBlock() instanceof PieBlock || block.getBlock() instanceof CakeBlock);
    }

    static int pieces(CuttingBoardRecipe recipe) {
        List<ItemStack> results = Nbt26.instantiateAll(recipe.getResults());
        if (results.isEmpty()) {
            return 2;
        }
        return results.get(0).getCount() + results.size() - 1;
    }

    public static int toolSlot(Level level, ItemStack item, Inventory inventory) {
        List<CuttingBoardRecipe> recipes = recipesFor(level, item);
        if (findWith(recipes, inventory.getSelectedItem()).isPresent()) {
            return inventory.getSelectedSlot();
        }
        if (findWith(recipes, inventory.getItem(Inventory.SLOT_OFFHAND)).isPresent()) {
            return Inventory.SLOT_OFFHAND;
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (findWith(recipes, inventory.getItem(slot)).isPresent()) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean canStick(ItemStack item) {
        return item.has(DataComponents.TOOL) || item.is(ItemTags.SWORDS)
                || item.getItem() instanceof TridentItem || item.getItem() instanceof ShearsItem;
    }

    /** Whether {@code item} is what some cutting recipe cuts with: a knife, an axe, an addon's rolling pin. */
    public static boolean isTool(Level level, ItemStack item) {
        return !item.isEmpty() && everything(level).stream().anyMatch(recipe -> tool(recipe).test(item));
    }

    /**
     * Whether {@code item} on the cursor is the board's tool, never laid down: a thing some cut is made with that cuts {@code onBoard},
     * or that goes into none of {@code dishes} as a layer. An addon cutting a mould with a glass bottle (Fright's Delight) or a pod with
     * a stick (Immortaler's Delight) leaves the bottle and the stick the first layers of drinks and skewers; a knife is no layer and
     * knocks. The server passes its dishes, a client those its server sent: both decide alike.
     */
    public static boolean isTool(Level level, ItemStack item, ItemStack onBoard, List<AssemblyRecipes.Assembly> dishes) {
        return isTool(level, item) && (findWith(level, onBoard, item).isPresent()
                || dishes.stream().noneMatch(dish -> dish.ingredients().stream().anyMatch(ingredient -> ingredient.test(item))));
    }

    /**
     * What {@code recipe} cuts with, to test a tool by and to show: through {@link Platform#unwrap}, as an addon's tool action on
     * Refabricated throws when it is tested as Fabric wraps it.
     */
    public static Ingredient tool(CuttingBoardRecipe recipe) {
        return Platform.INSTANCE.unwrap(recipe.getTool());
    }

    public static Optional<CuttingBoardRecipe> findWith(Level level, ItemStack item, ItemStack tool) {
        return tool.isEmpty() ? Optional.empty() : findWith(recipesFor(level, item), tool);
    }

    /** The first of an item's {@code recipes} ({@link #recipesFor}) that {@code tool} cuts. */
    public static Optional<CuttingBoardRecipe> findWith(List<CuttingBoardRecipe> recipes, ItemStack tool) {
        return tool.isEmpty() ? Optional.empty() : recipes.stream().filter(recipe -> tool(recipe).test(tool)).findFirst();
    }
}
