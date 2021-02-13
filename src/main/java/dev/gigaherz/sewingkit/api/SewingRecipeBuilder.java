package dev.gigaherz.sewingkit.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.Tag;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
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
    private CompoundNBT tag;
    private final Advancement.Builder advancementBuilder = Advancement.Builder.builder();

    public static SewingRecipeBuilder begin(Item result)
    {
        return begin(result, 1, null);
    }

    public static SewingRecipeBuilder begin(Item result, int count)
    {
        return begin(result, count, null);
    }

    public static SewingRecipeBuilder begin(Item result, CompoundNBT tag)
    {
        return begin(result, 1, tag);
    }

    public static SewingRecipeBuilder begin(Item result, int count, @Nullable CompoundNBT tag)
    {
        return new SewingRecipeBuilder(result, count, tag);
    }

    protected SewingRecipeBuilder(Item result, int count, @Nullable CompoundNBT tag)
    {
        this.result = result;
        this.count = count;
        this.tag = tag;
    }

    public SewingRecipeBuilder withTool(IItemProvider... tool)
    {
        return withTool(Ingredient.fromItems(tool));
    }

    public SewingRecipeBuilder withTool(ITag<Item> tool)
    {
        return withTool(Ingredient.fromTag(tool));
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

    public SewingRecipeBuilder addMaterial(int count, IItemProvider... x)
    {
        return addMaterial(Ingredient.fromItems(x), count);
    }

    public SewingRecipeBuilder addMaterial(IItemProvider x, int count)
    {
        return addMaterial(Ingredient.fromItems(x), count);
    }

    public SewingRecipeBuilder addMaterial(ITag<Item> x, int count)
    {
        return addMaterial(Ingredient.fromTag(x), 1);
    }

    public SewingRecipeBuilder addMaterial(IItemProvider... x)
    {
        return addMaterial(Ingredient.fromItems(x), 1);
    }

    public SewingRecipeBuilder addMaterial(ITag<Item> x)
    {
        return addMaterial(Ingredient.fromTag(x), 1);
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
        materials.add(SewingRecipe.Material.of(x,count));
        return this;
    }

    public SewingRecipeBuilder addCriterion(String name, ICriterionInstance criterionIn) {
        this.advancementBuilder.withCriterion(name, criterionIn);
        return this;
    }

    public SewingRecipeBuilder setGroup(String groupIn) {
        this.group = groupIn;
        return this;
    }

    public void build(Consumer<IFinishedRecipe> consumerIn) {
        //noinspection deprecation
        this.build(consumerIn, Registry.ITEM.getKey(this.result));
    }

    public void build(Consumer<IFinishedRecipe> consumerIn, ResourceLocation id) {
        this.validate(id);
        this.advancementBuilder
                .withParentId(new ResourceLocation("recipes/root"))
                .withCriterion("has_the_recipe", RecipeUnlockedTrigger.create(id))
                .withRewards(AdvancementRewards.Builder.recipe(id))
                .withRequirementsStrategy(IRequirementsStrategy.OR);
        ResourceLocation advancementId = new ResourceLocation(id.getNamespace(), "recipes/" + this.result.getGroup().getPath() + "/" + id.getPath());
        consumerIn.accept(createFinishedRecipe(id, this.group == null ? "" : this.group, this.result, this.count, this.tag, this.tool, this.pattern, this.materials, this.advancementBuilder, advancementId));
    }

    protected IFinishedRecipe createFinishedRecipe(ResourceLocation id, String group, Item result, int count, CompoundNBT tag, Ingredient tool, Ingredient pattern, List<SewingRecipe.Material> materials, Advancement.Builder advancementBuilder, ResourceLocation advancementId)
    {
        return new SewingRecipeBuilder.Result(id, group, result, count, tag, tool, pattern, materials, advancementBuilder, advancementId);
    }

    private void validate(ResourceLocation id) {
        if (this.advancementBuilder.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining sewing recipe " + id);
        }
        if (this.materials.isEmpty()) {
            throw new IllegalStateException("No ingredients for sewing recipe " + id);
        }
    }

    protected static class Result implements IFinishedRecipe
    {
        private final ResourceLocation id;
        private final Item result;
        private final int count;
        @Nullable
        private final CompoundNBT tag;
        private final String group;
        @Nullable
        private final Ingredient tool;
        @Nullable
        private final Ingredient pattern;
        private final List<SewingRecipe.Material> materials;
        private final Advancement.Builder advancementBuilder;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, String group, Item result, int count, @Nullable CompoundNBT tag,
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
        public void serialize(JsonObject recipeJson)
        {
            if (!this.group.isEmpty()) {
                recipeJson.addProperty("group", this.group);
            }

            JsonArray jsonarray = new JsonArray();
            for(SewingRecipe.Material material : this.materials) {
                jsonarray.add(material.serialize());
            }
            recipeJson.add("materials", jsonarray);

            if(tool != null)
            {
                recipeJson.add("tool", tool.serialize());
            }

            if(pattern != null)
            {
                recipeJson.add("tool", pattern.serialize());
            }

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", Registry.ITEM.getKey(this.result).toString());
            if (this.count > 1) {
                resultJson.addProperty("count", this.count);
            }
            if (this.tag != null)
            {
                CompoundNBT.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().ifPresent(
                        result -> resultJson.add("nbt", result)
                );
            }
            recipeJson.add("result", resultJson);
        }

        @Override
        public ResourceLocation getID()
        {
            return id;
        }

        @Override
        public IRecipeSerializer<?> getSerializer()
        {
            return SewingRecipe.SERIALIZER;
        }

        @Nullable
        @Override
        public JsonObject getAdvancementJson()
        {
            return this.advancementBuilder.serialize();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementID()
        {
            return advancementId;
        }
    }
}
