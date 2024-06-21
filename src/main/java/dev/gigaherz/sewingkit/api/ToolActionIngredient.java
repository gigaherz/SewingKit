package dev.gigaherz.sewingkit.api;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.TierSortingRegistry;
import net.neoforged.neoforge.common.ToolAction;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolActionIngredient extends Ingredient
{
    public static final Codec<Tier> TIER_CODEC = ResourceLocation.CODEC.flatXmap(
            name -> {
                var tier = TierSortingRegistry.byName(name);
                if (tier == null)
                    return DataResult.error(() -> "Tier name is not in the sorting registry!");
                return DataResult.success(tier);
            },
            tier -> {
                var name = TierSortingRegistry.getName(tier);
                if (name == null)
                    return DataResult.error(() -> "Tier is not in the sorting registry!");
                return DataResult.success(name);
            }
    );

    public static final Codec<ToolActionIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ToolAction.CODEC.fieldOf("tool_type").forGetter(i -> i.toolType),
            TIER_CODEC.optionalFieldOf("tool_level").forGetter(i -> Optional.ofNullable(i.tier))
    ).apply(instance, (toolType, tierOpt) -> new ToolActionIngredient(toolType, tierOpt.orElse(null))));

    private final ToolAction toolType;
    private final @Nullable Tier tier;

    public static final ResourceLocation NAME = SewingKitMod.location("tool_ingredient");

    public static ToolActionIngredient fromTool(ToolAction toolType, @Nullable Tier tier)
    {
        return new ToolActionIngredient(toolType, tier);
    }

    protected ToolActionIngredient(ToolAction toolType, @Nullable Tier tier)
    {
        super(Stream.of(new ItemList(toolType, tier)));
        this.toolType = toolType;
        this.tier = tier;
    }

    @Override
    public IngredientType<?> getType()
    {
        return SewingKitMod.TOOL_ACTION_INGREDIENT.get();
    }

    private record ItemList(ToolAction toolType,
                            @Nullable Tier tier) implements Value
    {

        @Override
        public Collection<ItemStack> getItems()
        {
            return BuiltInRegistries.ITEM.entrySet()
                    .stream()
                    .map(e -> new ItemStack(e.getValue()))
                    .filter(stack -> stack.canPerformAction(toolType) && checkTier(stack))
                    .collect(Collectors.toList());
        }

        private boolean checkTier(ItemStack stack)
        {
            if (this.tier == null)
            {
                return true;
            }

            if (stack.getItem() instanceof TieredItem tieredItem)
            {
                var tier = tieredItem.getTier();
                if (tier == this.tier) return true;
                return TierSortingRegistry.getTiersLowerThan(tier).contains(this.tier);
            }

            return false;
        }
    }
}
