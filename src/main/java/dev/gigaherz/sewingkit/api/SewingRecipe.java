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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SewingRecipe implements Recipe<SewingInput>
{
    public static <T extends SewingRecipe> Products.P7<RecordCodecBuilder.Mu<T>, String, RecipeBookCategory,
            NonNullList<Material>, Optional<Ingredient>, Optional<Ingredient>, ItemStack, Boolean>
    defaultSewingFields(RecordCodecBuilder.Instance<T> instance)
    {
        return instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(SewingRecipe::group),
                BuiltInRegistries.RECIPE_BOOK_CATEGORY.byNameCodec().fieldOf("category").forGetter(SewingRecipe::recipeBookCategory),
                NonNullList.codecOf(Material.CODEC).fieldOf("materials").forGetter(SewingRecipe::getMaterials),
                Ingredient.CODEC.optionalFieldOf("pattern").forGetter(SewingRecipe::getPattern),
                Ingredient.CODEC.optionalFieldOf("tool").forGetter(SewingRecipe::getTool),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SewingRecipe::getOutput),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(SewingRecipe::showNotification)
        );
    }

    public static final MapCodec<SewingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> defaultSewingFields(instance).apply(instance, SewingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SewingRecipe> STREAM_CODEC = StreamCodec.composite(
            SewingKitMod.nullable(ByteBufCodecs.STRING_UTF8), SewingRecipe::group,
            ByteBufCodecs.registry(Registries.RECIPE_BOOK_CATEGORY), SewingRecipe::recipeBookCategory,
            ByteBufCodecs.collection(NonNullList::createWithCapacity, Material.STREAM_CODEC), SewingRecipe::getMaterials,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), SewingRecipe::getPattern,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), SewingRecipe::getTool,
            ItemStack.STREAM_CODEC, SewingRecipe::getOutput,
            ByteBufCodecs.BOOL, SewingRecipe::showNotification,
            SewingRecipe::new
    );

    // TODO start using instead of Optional<> next breaking change cycle
    private static <T> MapCodec<@Nullable T> nullableFieldOf(Codec<T> codec, String name)
    {
        return codec.optionalFieldOf(name).xmap(
                a -> a.orElse(null),
                Optional::ofNullable
        );
    }

    private final String group;
    private final RecipeBookCategory recipeBookCategory;
    private final NonNullList<Material> materials;
    @Nullable
    private final Ingredient pattern;
    @Nullable
    private final Ingredient tool;
    private final ItemStack output;
    private final boolean showNotification;

    private PlacementInfo placementInfo;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected SewingRecipe(String group, RecipeBookCategory recipeBookCategory, NonNullList<Material> materials, Optional<Ingredient> pattern, Optional<Ingredient> tool, ItemStack output, boolean showNotification)
    {
        this(group, recipeBookCategory, materials, pattern.orElse(null), tool.orElse(null), output, showNotification);
    }

    public SewingRecipe(String group, RecipeBookCategory recipeBookCategory, NonNullList<Material> materials, @Nullable Ingredient pattern, @Nullable Ingredient tool, ItemStack output, boolean showNotification)
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
        ItemStack toolStack = input.getTool();
        var hasTool = tool != null ? toolStack.getCount() > 0 && tool.test(toolStack) : toolStack.getCount() == 0;
        if (!hasTool)
            return false;

        ItemStack patternStack = input.getPattern();
        var hasPattern = pattern != null ? patternStack.getCount() > 0 && pattern.test(patternStack) : patternStack.getCount() == 0;
        if (!hasPattern)
            return false;

        Map<Ingredient, Integer> missing = materials.stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));
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

        var hasMaterials = missing.values().stream().noneMatch(v -> v > 0);
        return hasMaterials;
    }

    @Override
    public ItemStack assemble(SewingInput container, HolderLookup.Provider provider)
    {
        return output.copy();
    }

    public NonNullList<Material> getMaterials()
    {
        return materials;
    }

    @Override
    public PlacementInfo placementInfo()
    {
        if (placementInfo == null)
        {
            List<Optional<Ingredient>> ingredients = new ArrayList<>();
            ingredients.add(Optional.ofNullable(pattern));
            ingredients.add(Optional.ofNullable(tool));
            materials.stream().map(m -> Optional.of(m.ingredient)).forEach(ingredients::add);

            placementInfo = PlacementInfo.createFromOptionals(ingredients);
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory()
    {
        return recipeBookCategory;
    }

    public Optional<Ingredient> getTool()
    {
        return Optional.ofNullable(tool);
    }

    public Optional<Ingredient> getPattern()
    {
        return Optional.ofNullable(pattern);
    }

    public ItemStack getOutput()
    {
        return output;
    }

    @Override
    public boolean showNotification()
    {
        return this.showNotification;
    }

    public static class Serializer implements RecipeSerializer<SewingRecipe>
    {
        @Override
        public MapCodec<SewingRecipe> codec()
        {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SewingRecipe> streamCodec()
        {
            return STREAM_CODEC;
        }
    }

    public static record Material(Ingredient ingredient, int count) implements Predicate<ItemStack>
    {
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Material::ingredient),
                Codec.INT.fieldOf("count").forGetter(Material::count)
        ).apply(instance, Material::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Material::ingredient,
                ByteBufCodecs.INT, Material::count,
                Material::new
        );

        public static Material of(Ingredient ingredient, int count)
        {
            return new Material(ingredient, count);
        }

        @Override
        public boolean test(ItemStack itemStack)
        {
            return ingredient.test(itemStack) && itemStack.getCount() >= count;
        }
    }
}