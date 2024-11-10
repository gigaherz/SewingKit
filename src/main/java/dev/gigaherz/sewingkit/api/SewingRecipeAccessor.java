package dev.gigaherz.sewingkit.api;

import com.mojang.logging.LogUtils;
import dev.gigaherz.sewingkit.network.SyncSewingRecipes;
import dev.gigaherz.sewingkit.table.SewingInput;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SewingRecipeAccessor
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<RecipeHolder<SewingRecipe>> clientRecipes;

    private static List<RecipeHolder<SewingRecipe>> serverRecipes;

    public static List<RecipeHolder<SewingRecipe>> getRecipes(Level level)
    {
        return level.isClientSide ? clientRecipes : serverRecipes;
    }

    public static List<RecipeHolder<SewingRecipe>> getRecipes(Level level, SewingInput input)
    {
        return getRecipes(level).stream().filter(
                recipe -> recipe.value().matches(input, level)
        ).toList();
    }

    public static void handleClientRecipes(SyncSewingRecipes data)
    {
        clientRecipes = data.recipes();
    }

    public static List<RecipeHolder<SewingRecipe>> handleServerRecipes(FeatureFlagSet flags, RecipeManager access)
    {
        List<RecipeHolder<SewingRecipe>> recipes = new ArrayList<>();
        access.getRecipes().forEach(recipeHolder -> {
            Recipe<?> recipe = recipeHolder.value();
            if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", recipeHolder.id().location());
            } else {
                if (recipe instanceof SewingRecipe sewingRecipe
                        && isIngredientEnabled(flags, sewingRecipe.getTool())
                        && isIngredientEnabled(flags, sewingRecipe.getPattern())
                        && sewingRecipe.getMaterials().stream().allMatch(mat -> isIngredientEnabled(flags, mat))
                        && sewingRecipe.getOutput().isItemEnabled(flags)) {
                    //noinspection unchecked
                    recipes.add((RecipeHolder<SewingRecipe>) recipeHolder);
                }
            }
        });
        serverRecipes = recipes;
        return recipes;
    }

    private static boolean isIngredientEnabled(FeatureFlagSet flags, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Ingredient> optionalIngredient)
    {
        return optionalIngredient.map(ingredient -> isIngredientEnabled(flags, ingredient)).orElse(true);
    }

    private static boolean isIngredientEnabled(FeatureFlagSet flags, SewingRecipe.Material ingredient)
    {
        return isIngredientEnabled(flags, ingredient.ingredient());
    }

    private static boolean isIngredientEnabled(FeatureFlagSet flags, Ingredient ingredient)
    {
        return ingredient.items().stream().allMatch(itemHolder -> itemHolder.value().isEnabled(flags));
    }
}
