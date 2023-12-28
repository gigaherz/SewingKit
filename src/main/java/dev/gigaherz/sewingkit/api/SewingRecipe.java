package dev.gigaherz.sewingkit.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SewingRecipe implements Recipe<Container>
{

    public static final Codec<SewingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.strictOptionalField(Codec.STRING,"group", "").forGetter(SewingRecipe::getGroup),
            NonNullList.codecOf(Material.CODEC).fieldOf("materials").forGetter(SewingRecipe::getMaterials),
            ExtraCodecs.strictOptionalField(Ingredient.CODEC, "pattern").forGetter(SewingRecipe::getPattern),
            ExtraCodecs.strictOptionalField(Ingredient.CODEC, "tool").forGetter(SewingRecipe::getTool),
            ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
            ExtraCodecs.strictOptionalField(Codec.BOOL, "show_notification", true).forGetter(SewingRecipe::showNotification)
    ).apply(instance, SewingRecipe::new));

    private final String group;

    private final NonNullList<Material> materials;
    @Nullable
    private final Ingredient pattern;
    @Nullable
    private final Ingredient tool;
    private final ItemStack output;

    private final boolean showNotification;


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private SewingRecipe(String group, NonNullList<Material> materials, Optional<Ingredient> pattern, Optional<Ingredient> tool, ItemStack output, boolean showNotification)
    {
        this(group, materials, pattern.orElse(null), tool.orElse(null), output, showNotification);
    }

    public SewingRecipe(String group, NonNullList<Material> materials, @Nullable Ingredient pattern, @Nullable Ingredient tool, ItemStack output, boolean showNotification)
    {
        this.group = group;
        this.materials = materials;
        this.pattern = pattern;
        this.tool = tool;
        this.output = output;
        this.showNotification = showNotification;
    }

    public static Collection<RecipeHolder<SewingRecipe>> getAllRecipes(Level world)
    {
        return world.getRecipeManager().getAllRecipesFor(SewingKitMod.SEWING.get());
    }

    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 4;
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        NonNullList<Ingredient> allIngredients = NonNullList.create();
        allIngredients.add(pattern != null ? pattern : Ingredient.EMPTY);
        allIngredients.add(tool != null ? tool : Ingredient.EMPTY);
        materials.stream().map(m -> m.ingredient).forEach(allIngredients::add);
        return allIngredients;
    }

    @Override
    public boolean matches(Container inv, Level worldIn)
    {
        ItemStack toolStack = inv.getItem(0);
        ItemStack patternStack = inv.getItem(1);

        Map<Ingredient, Integer> missing = materials.stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));
        for (int i = 0; i < 4; i++)
        {
            for (Map.Entry<Ingredient, Integer> mat : missing.entrySet())
            {
                Ingredient ing = mat.getKey();
                int value = mat.getValue();
                ItemStack stack = inv.getItem(i + 2);
                if (ing.test(stack))
                {
                    int remaining = Math.max(0, value - stack.getCount());
                    mat.setValue(remaining);
                }
            }
        }

        return missing.values().stream().noneMatch(v -> v > 0)
                && (pattern != null ? patternStack.getCount() > 0 && pattern.test(patternStack) : patternStack.getCount() == 0)
                && (tool != null ? toolStack.getCount() > 0 && tool.test(toolStack) : toolStack.getCount() == 0);
    }

    @Override
    public ItemStack assemble(Container p_44001_, RegistryAccess registryAccess)
    {
        return getResultItem(registryAccess).copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess)
    {
        return output;
    }

    public ItemStack getResultItem()
    {
        return output;
    }

    public NonNullList<Material> getMaterials()
    {
        return materials;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SewingKitMod.SEWING_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType()
    {
        return SewingKitMod.SEWING.get();
    }

    @Override
    public ItemStack getToastSymbol()
    {
        return new ItemStack(SewingKitMod.WOOD_SEWING_NEEDLE.get());
    }

    @Nullable
    public Optional<Ingredient> getTool()
    {
        return Optional.ofNullable(tool);
    }

    @Nullable
    public Optional<Ingredient> getPattern()
    {
        return Optional.ofNullable(pattern);
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    public static class Serializer
            implements RecipeSerializer<SewingRecipe>
    {
        @Override
        public Codec<SewingRecipe> codec()
        {
            return CODEC;
        }

        @Override
        public SewingRecipe fromNetwork(FriendlyByteBuf buffer)
        {
            String group = buffer.readUtf(32767);
            int numMaterials = buffer.readVarInt();
            NonNullList<Material> materials = NonNullList.create();
            for (int i = 0; i < numMaterials; i++)
            {
                materials.add(Material.read(buffer));
            }
            boolean hasPattern = buffer.readBoolean();
            Ingredient pattern = hasPattern ? Ingredient.fromNetwork(buffer) : null;
            boolean hasTool = buffer.readBoolean();
            Ingredient tool = hasTool ? Ingredient.fromNetwork(buffer) : null;
            ItemStack result = buffer.readItem();
            boolean showNofitication = buffer.readBoolean();
            return new SewingRecipe(group, materials, pattern, tool, result, showNofitication);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SewingRecipe recipe)
        {
            buffer.writeUtf(recipe.group);
            buffer.writeVarInt(recipe.materials.size());
            for (Material input : recipe.materials)
            {
                input.write(buffer);
            }
            boolean hasPattern = recipe.pattern != null;
            buffer.writeBoolean(hasPattern);
            if (hasPattern)
                recipe.pattern.toNetwork(buffer);
            boolean hasTool = recipe.tool != null;
            buffer.writeBoolean(hasTool);
            if (hasTool)
                recipe.tool.toNetwork(buffer);
            buffer.writeItem(recipe.output);
            buffer.writeBoolean(recipe.showNotification);
        }
    }

    public static class Material implements Predicate<ItemStack>
    {
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(mat -> mat.ingredient),
            Codec.INT.fieldOf("count").forGetter(mat -> mat.count)
        ).apply(instance, Material::new));

        public final Ingredient ingredient;
        public final int count;

        private Material(Ingredient ingredient, int count)
        {
            this.ingredient = ingredient;
            this.count = count;
        }

        public static Material of(Ingredient ingredient, int count)
        {
            return new Material(ingredient, count);
        }

        @Override
        public boolean test(ItemStack itemStack)
        {
            return ingredient.test(itemStack) && itemStack.getCount() >= count;
        }

        public void write(FriendlyByteBuf packet)
        {
            packet.writeVarInt(count);
            ingredient.toNetwork(packet);
        }

        public static Material read(FriendlyByteBuf packet)
        {
            int count = packet.readVarInt();
            Ingredient ingredient = Ingredient.fromNetwork(packet);
            return new Material(ingredient, count);
        }
    }
}