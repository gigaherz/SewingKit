package dev.gigaherz.sewingkit.api;

import com.google.gson.JsonObject;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolActionIngredient extends Ingredient
{
    public static final ResourceLocation NAME = SewingKitMod.location("tool_ingredient");

    public static ToolActionIngredient fromTool(ToolAction toolType, @Nullable Tier level)
    {
        return new ToolActionIngredient(toolType, level);
    }

    protected ToolActionIngredient(ToolAction toolType, @Nullable Tier toolLevel)
    {
        super(Stream.of(new ItemList(toolType, toolLevel)));
    }

    private record ItemList(ToolAction toolType,
                            @Nullable Tier toolLevel) implements Value
    {

        @Override
        public Collection<ItemStack> getItems()
        {
            return ForgeRegistries.ITEMS.getValues()
                    .stream()
                    .map(ItemStack::new)
                    .filter(stack -> stack.canPerformAction(toolType) && checkTier(stack))
                    .collect(Collectors.toList());
        }

        private boolean checkTier(ItemStack stack)
        {
            return toolLevel == null || (stack.getItem() instanceof TieredItem tieredItem) && TierSortingRegistry.getTiersLowerThan(tieredItem.getTier()).contains(toolLevel);
        }

        @Override
        public JsonObject serialize()
        {
            JsonObject object = new JsonObject();
            object.addProperty("type", NAME.toString());
            object.addProperty("tool_type", toolType.name());
            if (toolLevel != null)
                object.addProperty("tool_level", Objects.requireNonNull(TierSortingRegistry.getName(toolLevel), "Tool level not found: " + toolLevel).toString());
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
            return new ToolActionIngredient(
                    ToolAction.get(GsonHelper.getAsString(json, "tool_type")),
                    getTier(GsonHelper.getAsString(json, "tool_level", null))
            );
        }

        @Nullable
        private Tier getTier(@Nullable String str)
        {
            return str == null ? null : TierSortingRegistry.byName(new ResourceLocation(str));
        }
    }
}
