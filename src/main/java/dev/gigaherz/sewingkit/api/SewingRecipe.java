package dev.gigaherz.sewingkit.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SewingRecipe implements IRecipe<IInventory>
{
    @ObjectHolder("sewingkit:sewing")
    public static IRecipeSerializer<?> SERIALIZER = null;

    public static IRecipeType<SewingRecipe> SEWING = IRecipeType.register(SewingKitMod.location("sewing").toString());

    private final String group;
    private final ResourceLocation id;

    private final NonNullList<Material> materials;
    @Nullable
    private final Ingredient pattern;
    @Nullable
    private final Ingredient tool;
    private final ItemStack output;

    public SewingRecipe(ResourceLocation id, String group, NonNullList<Material> materials, @Nullable Ingredient pattern, @Nullable Ingredient tool, ItemStack output)
    {
        this.group = group;
        this.id = id;
        this.materials = materials;
        this.pattern = pattern;
        this.tool = tool;
        this.output = output;
    }

    public static Collection<SewingRecipe> getAllRecipes(World world)
    {
        return world.getRecipeManager().getRecipes().stream().filter(rcp ->rcp.getType() == SEWING).map(rcp -> (SewingRecipe)rcp).collect(Collectors.toSet());
    }

    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public boolean canFit(int width, int height)
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
    public boolean matches(IInventory inv, World worldIn)
    {
        ItemStack toolStack = inv.getStackInSlot(0);
        ItemStack patternStack = inv.getStackInSlot(1);

        Map<Ingredient, Integer> missing = materials.stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));
        for(int i=0;i<4;i++)
        {
            for (Map.Entry<Ingredient, Integer> mat : missing.entrySet())
            {
                Ingredient ing = mat.getKey();
                int value = mat.getValue();
                ItemStack stack = inv.getStackInSlot(i + 2);
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
    public ItemStack getCraftingResult(IInventory inv)
    {
        return getRecipeOutput().copy();
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return output;
    }

    public NonNullList<Material> getMaterials()
    {
        return materials;
    }

    @Override
    public ResourceLocation getId()
    {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType()
    {
        return SEWING;
    }

    @Override
    public ItemStack getIcon()
    {
        return new ItemStack(SewingKitMod.WOOD_SEWING_NEEDLE.get());
    }

    public Ingredient getTool()
    {
        return tool != null ? tool : Ingredient.EMPTY;
    }

    public Ingredient getPattern()
    {
        return pattern != null ? pattern : Ingredient.EMPTY;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<SewingRecipe>
    {
        protected SewingRecipe createRecipe(ResourceLocation recipeId, String group, NonNullList<Material> materials, Ingredient pattern, Ingredient tool, ItemStack result)
        {
            return new SewingRecipe(recipeId, group, materials, pattern, tool, result);
        }

        @Override
        public SewingRecipe read(ResourceLocation recipeId, JsonObject json)
        {
            String group = JSONUtils.getString(json, "group", "");
            JsonArray materialsJson = JSONUtils.getJsonArray(json, "materials");
            NonNullList<Material> materials = NonNullList.create();
            for(int i=0;i<materialsJson.size();i++)
            {
                materials.add(Material.deserialize(materialsJson.get(i).getAsJsonObject()));
            }
            Ingredient pattern = json.has("pattern") ? CraftingHelper.getIngredient(json.get("ingredient")) : null;
            Ingredient tool = json.has("tool") ? CraftingHelper.getIngredient(json.get("tool")) : null;
            ItemStack result = CraftingHelper.getItemStack(JSONUtils.getJsonObject(json, "result"), true);
            return createRecipe(recipeId, group, materials, pattern, tool, result);
        }

        @Override
        public SewingRecipe read(ResourceLocation recipeId, PacketBuffer buffer)
        {
            String group = buffer.readString(32767);
            int numMaterials = buffer.readVarInt();
            NonNullList<Material> materials = NonNullList.create();
            for(int i=0;i<numMaterials;i++)
            {
                materials.add(Material.read(buffer));
            }
            boolean hasPattern = buffer.readBoolean();
            Ingredient pattern = hasPattern ? Ingredient.read(buffer) : null;
            boolean hasTool = buffer.readBoolean();
            Ingredient tool = hasTool ? Ingredient.read(buffer) : null;
            ItemStack result = buffer.readItemStack();
            return createRecipe(recipeId, group, materials, pattern, tool, result);
        }

        @Override
        public void write(PacketBuffer buffer, SewingRecipe recipe)
        {
            buffer.writeString(recipe.group);
            buffer.writeVarInt(recipe.materials.size());
            for(Material input : recipe.materials)
            {
                input.write(buffer);
            }
            boolean hasPattern = recipe.pattern != null;
            buffer.writeBoolean(hasPattern);
            if (hasPattern)
                recipe.pattern.write(buffer);
            boolean hasTool = recipe.tool != null;
            buffer.writeBoolean(hasTool);
            if (hasTool)
                recipe.tool.write(buffer);
            buffer.writeItemStack(recipe.output);
        }
    }

    public static class Material implements Predicate<ItemStack>
    {
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

        public JsonObject serialize()
        {
            JsonObject material = new JsonObject();
            material.add("ingredient", ingredient.serialize());
            material.addProperty("count", count);
            return material;
        }

        public static Material deserialize(JsonObject object)
        {
            Ingredient ingredient = CraftingHelper.getIngredient(object.get("ingredient"));
            int count = JSONUtils.getInt(object, "count", 1);
            if (count <= 0)
            {
                throw new IllegalArgumentException("Material count must be a positive integer.");
            }
            return new Material(ingredient, count);
        }

        public void write(PacketBuffer packet)
        {
            packet.writeVarInt(count);
            ingredient.write(packet);
        }

        public static Material read(PacketBuffer packet)
        {
            int count = packet.readVarInt();
            Ingredient ingredient = Ingredient.read(packet);
            return new Material(ingredient, count);
        }
    }
}