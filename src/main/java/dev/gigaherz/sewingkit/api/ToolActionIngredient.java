package dev.gigaherz.sewingkit.api;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.ToolAction;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

public record ToolActionIngredient(ToolAction action) implements ICustomIngredient
{
    public static final MapCodec<ToolActionIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ToolAction.CODEC.fieldOf("tool_type").forGetter(ToolActionIngredient::action)
    ).apply(instance, ToolActionIngredient::new));

    public static StreamCodec<ByteBuf, ToolAction> TOOL_ACTION_STREAM_CODEC = ByteBufCodecs.stringUtf8(32768).map(ToolAction::get, ToolAction::name);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolActionIngredient> STREAM_CODEC = StreamCodec.composite(
            TOOL_ACTION_STREAM_CODEC, ToolActionIngredient::action,
            ToolActionIngredient::new
    );

    public static final ResourceLocation NAME = SewingKitMod.location("tool_ingredient");

    public static Ingredient fromTool(ToolAction toolType)
    {
        return (new ToolActionIngredient(toolType)).toVanilla();
    }

    @Override
    public IngredientType<?> getType()
    {
        return SewingKitMod.TOOL_ACTION_INGREDIENT.get();
    }


    @Override
    public boolean test(ItemStack stack)
    {
        return stack.canPerformAction(action);
    }

    @Override
    public Stream<ItemStack> getItems()
    {
        return BuiltInRegistries.ITEM.entrySet()
                .stream()
                .map(e -> new ItemStack(e.getValue()))
                .filter(this::test);
    }

    @Override
    public boolean isSimple()
    {
        return false;
    }
}
