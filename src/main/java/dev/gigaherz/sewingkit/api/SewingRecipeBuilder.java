package dev.gigaherz.sewingkit.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class SewingRecipeBuilder
{
    private String group;
    private Ingredient tool;
    private Ingredient pattern;
    private final List<SewingRecipe.Material> materials = Lists.newArrayList();
    private final Item result;
    private final int count;
    private CompoundTag tag;
    private final Advancement.Builder advancementBuilder = Advancement.Builder.advancement();

    public static SewingRecipeBuilder begin(Item result)
    {
        return begin(result, 1, null);
    }

    public static SewingRecipeBuilder begin(Item result, int count)
    {
        return begin(result, count, null);
    }

    public static SewingRecipeBuilder begin(Item result, CompoundTag tag)
    {
        return begin(result, 1, tag);
    }

    public static SewingRecipeBuilder begin(Item result, int count, @Nullable CompoundTag tag)
    {
        return new SewingRecipeBuilder(result, count, tag);
    }

    protected SewingRecipeBuilder(Item result, int count, @Nullable CompoundTag tag)
    {
        this.result = result;
        this.count = count;
        this.tag = tag;
    }

    public SewingRecipeBuilder withTool(ItemLike... tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public SewingRecipeBuilder withTool(Tag<Item> tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public SewingRecipeBuilder withTool(ToolType tool, int level)
    {
        return withTool(ToolIngredient.fromTool(tool, level));
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

    public SewingRecipeBuilder addMaterial(Tag<Item> x, int count)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public SewingRecipeBuilder addMaterial(ItemLike... x)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public SewingRecipeBuilder addMaterial(Tag<Item> x)
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

    public SewingRecipeBuilder addCriterion(String name, CriterionTriggerInstance criterionIn)
    {
        this.advancementBuilder.addCriterion(name, criterionIn);
        return this;
    }

    public SewingRecipeBuilder setGroup(String groupIn)
    {
        this.group = groupIn;
        return this;
    }

    public void build(Consumer<FinishedRecipe> consumerIn)
    {
        //noinspection deprecation
        this.build(consumerIn, Registry.ITEM.getKey(this.result));
    }

    public void build(Consumer<FinishedRecipe> consumerIn, ResourceLocation id)
    {
        this.validate(id);
        this.advancementBuilder
                .parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(RequirementsStrategy.OR);
        ResourceLocation advancementId = new ResourceLocation(id.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + id.getPath());
        consumerIn.accept(createFinishedRecipe(id, this.group == null ? "" : this.group, this.result, this.count, this.tag, this.tool, this.pattern, this.materials, this.advancementBuilder, advancementId));
    }

    protected FinishedRecipe createFinishedRecipe(ResourceLocation id, String group, Item result, int count, CompoundTag tag, Ingredient tool, Ingredient pattern, List<SewingRecipe.Material> materials, Advancement.Builder advancementBuilder, ResourceLocation advancementId)
    {
        return new SewingRecipeBuilder.Result(id, group, result, count, tag, tool, pattern, materials, advancementBuilder, advancementId);
    }

    private void validate(ResourceLocation id)
    {
        if (this.advancementBuilder.getCriteria().isEmpty())
        {
            throw new IllegalStateException("No way of obtaining sewing recipe " + id);
        }
        if (this.materials.isEmpty())
        {
            throw new IllegalStateException("No ingredients for sewing recipe " + id);
        }
    }

    protected static class Result implements FinishedRecipe
    {
        private final ResourceLocation id;
        private final Item result;
        private final int count;
        @Nullable
        private final CompoundTag tag;
        private final String group;
        @Nullable
        private final Ingredient tool;
        @Nullable
        private final Ingredient pattern;
        private final List<SewingRecipe.Material> materials;
        private final Advancement.Builder advancementBuilder;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, String group, Item result, int count, @Nullable CompoundTag tag,
                      @Nullable Ingredient tool, @Nullable Ingredient pattern, List<SewingRecipe.Material> materials,
                      Advancement.Builder advancementBuilder, ResourceLocation advancementId)
        {
            this.id = id;
            this.result = result;
            this.count = count;
            this.tag = tag;
            this.group = group;
            this.tool = tool;
            this.pattern = pattern;
            this.materials = materials;
            this.advancementBuilder = advancementBuilder;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject recipeJson)
        {
            if (!this.group.isEmpty())
            {
                recipeJson.addProperty("group", this.group);
            }

            JsonArray jsonarray = new JsonArray();
            for (SewingRecipe.Material material : this.materials)
            {
                jsonarray.add(material.serialize());
            }
            recipeJson.add("materials", jsonarray);

            if (tool != null)
            {
                recipeJson.add("tool", tool.toJson());
            }

            if (pattern != null)
            {
                recipeJson.add("tool", pattern.toJson());
            }

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", Registry.ITEM.getKey(this.result).toString());
            if (this.count > 1)
            {
                resultJson.addProperty("count", this.count);
            }
            if (this.tag != null)
            {
                CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().ifPresent(
                        result -> resultJson.add("nbt", result)
                );
            }
            recipeJson.add("result", resultJson);
        }

        @Override
        public ResourceLocation getId()
        {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType()
        {
            return SewingRecipe.SERIALIZER;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement()
        {
            return this.advancementBuilder.serializeToJson();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId()
        {
            return advancementId;
        }
    }
}
