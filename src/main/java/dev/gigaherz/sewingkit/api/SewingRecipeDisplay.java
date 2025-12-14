package dev.gigaherz.sewingkit.api;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

public record SewingRecipeDisplay(List<SlotDisplay> inputs, SlotDisplay result, SlotDisplay craftingStation)
        implements RecipeDisplay
{
    public static final MapCodec<SewingRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SlotDisplay.CODEC.listOf().fieldOf("inputs").forGetter(SewingRecipeDisplay::inputs),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(SewingRecipeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("craftingStation").forGetter(SewingRecipeDisplay::craftingStation)
                    ).apply(instance, SewingRecipeDisplay::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SewingRecipeDisplay> STREAM_CODEC = StreamCodec.composite(
            SlotDisplay.STREAM_CODEC.apply(ByteBufCodecs.list()), SewingRecipeDisplay::inputs,
            SlotDisplay.STREAM_CODEC, SewingRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, SewingRecipeDisplay::craftingStation,
            SewingRecipeDisplay::new
    );

    @Override
    public Type<? extends RecipeDisplay> type()
    {
        return SewingKitMod.SEWING_DISPLAY.get();
    }
}
