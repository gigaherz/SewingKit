package dev.gigaherz.sewingkit.api;

import com.google.gson.JsonObject;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.world.item.crafting.Ingredient.Value;

public class ToolIngredient extends Ingredient
{
    public static final ResourceLocation NAME = SewingKitMod.location("tool_ingredient");

    public static ToolIngredient fromTool(ToolType toolType, int level)
    {
        return new ToolIngredient(toolType, level);
    }

    protected ToolIngredient(ToolType toolType, int toolLevel)
    {
        super(Stream.of(new ItemList(toolType, toolLevel)));
    }

    private static class ItemList implements Value
    {
        private final ToolType toolType;
        private final int toolLevel;

        public ItemList(ToolType toolType, int toolLevel)
        {
            this.toolType = toolType;
            this.toolLevel = toolLevel;
        }

        @Override
        public Collection<ItemStack> getItems()
        {
            return ForgeRegistries.ITEMS.getValues()
                    .stream()
                    .map(ItemStack::new)
                    .filter(stack -> stack.getHarvestLevel(toolType, null, null) >= toolLevel)
                    .collect(Collectors.toList());
        }

        @Override
        public JsonObject serialize()
        {
            JsonObject object = new JsonObject();
            object.addProperty("type", NAME.toString());
            object.addProperty("tool_type", toolType.getName());
            object.addProperty("tool_level", toolLevel);
            return object;
        }
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer()
    {
        return Serializer.INSTANCE;
    }

    public static class Serializer extends VanillaIngredientSerializer
    {
        public static final IIngredientSerializer<? extends Ingredient> INSTANCE = new Serializer();

        @Override
        public Ingredient parse(JsonObject json)
        {
            return new ToolIngredient(
                    ToolType.get(GsonHelper.getAsString(json, "tool_type")),
                    GsonHelper.getAsInt(json, "tool_level")
            );
        }
    }
}
