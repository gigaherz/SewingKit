package dev.gigaherz.sewingkit.api;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.ToolAction;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SewingRecipeBuilder
{
    private final RecipeCategory category;
    private String group;
    private Ingredient tool;
    private Ingredient pattern;
    private final NonNullList<SewingRecipe.Material> materials = NonNullList.create();
    private final ItemStack result;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private boolean showNotification = true;

    public static SewingRecipeBuilder begin(RecipeCategory cat, Item result)
    {
        return begin(cat, new ItemStack(result));
    }

    public static SewingRecipeBuilder begin(RecipeCategory cat, Item result, int count)
    {
        return begin(cat, new ItemStack(result, count));
    }

    public static SewingRecipeBuilder begin(RecipeCategory cat, ItemStack result)
    {
        return new SewingRecipeBuilder(cat, result);
    }

    protected SewingRecipeBuilder(RecipeCategory cat, ItemStack result)
    {
        this.category = cat;
        this.result = result;
    }

    public SewingRecipeBuilder withTool(ItemLike... tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public SewingRecipeBuilder withTool(TagKey<Item> tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public SewingRecipeBuilder withTool(Ingredient tool)
    {
        this.tool = tool;
        return this;
    }

    public SewingRecipeBuilder withPattern(Ingredient pattern)
    {
        this.pattern = pattern;
        return this;
    }

    public SewingRecipeBuilder addMaterial(int count, ItemLike... x)
    {
        return addMaterial(Ingredient.of(x), count);
    }

    public SewingRecipeBuilder addMaterial(ItemLike x, int count)
    {
        return addMaterial(Ingredient.of(x), count);
    }

    public SewingRecipeBuilder addMaterial(TagKey<Item> x, int count)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public SewingRecipeBuilder addMaterial(ItemLike... x)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public SewingRecipeBuilder addMaterial(TagKey<Item> x)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public SewingRecipeBuilder addMaterial(Ingredient x)
    {
        return addMaterial(x, 1);
    }

    public SewingRecipeBuilder addMaterial(Ingredient x, int count)
    {
        if (materials.size() >= 4)
        {
            throw new IllegalArgumentException("There can only be up to 4 materials!");
        }
        if (count <= 0)
        {
            throw new IllegalArgumentException("Count must be a positive integer!");
        }
        materials.add(SewingRecipe.Material.of(x, count));
        return this;
    }

    public SewingRecipeBuilder addCriterion(String name, Criterion<?> criterionIn)
    {
        this.criteria.put(name, criterionIn);
        return this;
    }

    public SewingRecipeBuilder setGroup(String groupIn)
    {
        this.group = groupIn;
        return this;
    }

    public SewingRecipeBuilder showNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    public void save(RecipeOutput consumerIn, ResourceLocation id)
    {
        this.validate(id);

        var advancementBuilder = Advancement.Builder.advancement();
        advancementBuilder
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancementBuilder::addCriterion);
        ResourceLocation advancementId = id.withPrefix("recipes/" + category.getFolderName() + "/" );

        var recipe = build(
                Objects.requireNonNullElse(this.group, ""),
                /*RecipeBuilder.determineBookCategory(this.category),*/
                this.materials,
                this.pattern,
                this.tool,
                this.result,
                this.showNotification);

        consumerIn.accept(
                id,
                recipe,
                advancementBuilder.build(advancementId));
    }

    protected SewingRecipe build(String group, NonNullList<SewingRecipe.Material> materials, Ingredient pattern, Ingredient tool, ItemStack result, boolean showNotification)
    {
        return new SewingRecipe(group, materials, pattern, tool, result, showNotification);
    }

    private void validate(ResourceLocation id)
    {
        if (this.criteria.isEmpty())
        {
            throw new IllegalStateException("No way of obtaining sewing recipe " + id);
        }
        if (this.materials.isEmpty())
        {
            throw new IllegalStateException("No ingredients for sewing recipe " + id);
        }
    }
}
