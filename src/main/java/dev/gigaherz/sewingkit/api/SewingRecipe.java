package dev.gigaherz.sewingkit.api;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.table.SewingInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SewingRecipe implements Recipe<SewingInput>
{
    public static <T extends SewingRecipe> Products.P7<RecordCodecBuilder.Mu<T>, String, RecipeBookCategory,
            NonNullList<SewingMaterial>, Optional<Ingredient>, Optional<Ingredient>, ItemStack, Boolean>
    defaultSewingFields(RecordCodecBuilder.Instance<T> instance)
    {
        return instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(SewingRecipe::group),
                BuiltInRegistries.RECIPE_BOOK_CATEGORY.byNameCodec().fieldOf("category").forGetter(SewingRecipe::recipeBookCategory),
                NonNullList.codecOf(SewingMaterial.CODEC).fieldOf("materials").forGetter(SewingRecipe::materials),
                Ingredient.CODEC.optionalFieldOf("pattern").forGetter(recipe -> Optional.ofNullable(recipe.pattern())),
                Ingredient.CODEC.optionalFieldOf("tool").forGetter(recipe -> Optional.ofNullable(recipe.tool())),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SewingRecipe::output),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(SewingRecipe::showNotification)
        );
    }

    public static final MapCodec<SewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> defaultSewingFields(instance).apply(instance, SewingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SewingRecipe> STREAM_CODEC = StreamCodec.composite(
            SewingKitMod.nullable(ByteBufCodecs.STRING_UTF8), SewingRecipe::group,
            ByteBufCodecs.registry(Registries.RECIPE_BOOK_CATEGORY), SewingRecipe::recipeBookCategory,
            ByteBufCodecs.collection(NonNullList::createWithCapacity, SewingMaterial.STREAM_CODEC), SewingRecipe::materials,
            SewingKitMod.nullable(Ingredient.CONTENTS_STREAM_CODEC), SewingRecipe::pattern,
            SewingKitMod.nullable(Ingredient.CONTENTS_STREAM_CODEC), SewingRecipe::tool,
            ItemStack.STREAM_CODEC, SewingRecipe::output,
            ByteBufCodecs.BOOL, SewingRecipe::showNotification,
            SewingRecipe::new
    );

    private final String group;
    private final RecipeBookCategory recipeBookCategory;
    private final NonNullList<SewingMaterial> materials;
    @Nullable
    private final Ingredient pattern;
    @Nullable
    private final Ingredient tool;
    private final ItemStack output;
    private final boolean showNotification;

    private PlacementInfo placementInfo;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected SewingRecipe(String group, RecipeBookCategory recipeBookCategory, NonNullList<SewingMaterial> materials, Optional<Ingredient> pattern, Optional<Ingredient> tool, ItemStack output, boolean showNotification)
    {
        this(group, recipeBookCategory, materials, pattern.orElse(null), tool.orElse(null), output, showNotification);
    }

    public SewingRecipe(String group, RecipeBookCategory recipeBookCategory, NonNullList<SewingMaterial> materials, @Nullable Ingredient pattern, @Nullable Ingredient tool, ItemStack output, boolean showNotification)
    {
        this.group = group;
        this.recipeBookCategory = recipeBookCategory;
        this.materials = materials;
        this.pattern = pattern;
        this.tool = tool;
        this.output = output;
        this.showNotification = showNotification;
    }

    @Override
    public String group()
    {
        return group;
    }

    @Override
    public RecipeSerializer<? extends Recipe<SewingInput>> getSerializer()
    {
        return SewingKitMod.SEWING_RECIPE.get();
    }

    @Override
    public RecipeType<? extends Recipe<SewingInput>> getType()
    {
        return SewingKitMod.SEWING.get();
    }

    @Override
    public boolean matches(SewingInput input, Level worldIn)
    {
        var toolStack = input.getTool();
        var hasTool = tool != null ? !toolStack.isEmpty() && tool.test(toolStack) : toolStack.isEmpty();
        if (!hasTool)
            return false;

        var patternStack = input.getPattern();
        var hasPattern = pattern != null ? !patternStack.isEmpty() && pattern.test(patternStack) : patternStack.isEmpty();
        if (!hasPattern)
            return false;

        Map<Ingredient, Integer> missing = materials.stream().collect(Collectors.toMap(SewingMaterial::ingredient, SewingMaterial::count));
        for (int i = 0; i < 4; i++)
        {
            for (Map.Entry<Ingredient, Integer> mat : missing.entrySet())
            {
                Ingredient ing = mat.getKey();
                int value = mat.getValue();
                ItemStack stack = input.getMaterial(i);
                if (ing.test(stack))
                {
                    int remaining = Math.max(0, value - stack.getCount());
                    mat.setValue(remaining);
                }
            }
        }

        return missing.values().stream().noneMatch(v -> v > 0);
    }

    @Override
    public ItemStack assemble(SewingInput container, HolderLookup.Provider provider)
    {
        return output.copy();
    }

    public NonNullList<SewingMaterial> materials()
    {
        return materials;
    }

    @Override
    public PlacementInfo placementInfo()
    {
        if (placementInfo == null)
        {
            List<Optional<Ingredient>> ingredients = new ArrayList<>();
            ingredients.add(Optional.ofNullable(tool));
            ingredients.add(Optional.ofNullable(pattern));
            materials.stream().map(m -> Optional.of(m.ingredient())).forEach(ingredients::add);

            placementInfo = PlacementInfo.createFromOptionals(ingredients);
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory()
    {
        return recipeBookCategory;
    }

    @Nullable
    public Ingredient tool()
    {
        return tool;
    }

    @Nullable
    public Ingredient pattern()
    {
        return pattern;
    }

    public ItemStack output()
    {
        return output;
    }

    @Override
    public boolean showNotification()
    {
        return this.showNotification;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new SewingRecipeDisplay(
                        this.materials.stream().map(SewingMaterial::display).toList(),
                        new SlotDisplay.ItemStackSlotDisplay(this.output),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
                )
        );
    }

    public static class Serializer implements RecipeSerializer<SewingRecipe>
    {
        @Override
        public MapCodec<SewingRecipe> codec()
        {
            return CODEC;
        }

        @Deprecated
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SewingRecipe> streamCodec()
        {
            return STREAM_CODEC;
        }
    }
}