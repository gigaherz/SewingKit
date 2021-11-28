package dev.gigaherz.sewingkit.api;

import com.google.gson.JsonObject;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.item.crafting.Ingredient.IItemList;

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

    private static class ItemList implements IItemList
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
                    ToolType.get(JSONUtils.getAsString(json, "tool_type")),
                    JSONUtils.getAsInt(json, "tool_level")
            );
        }
    }
}
