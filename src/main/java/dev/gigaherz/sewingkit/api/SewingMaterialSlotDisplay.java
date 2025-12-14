package dev.gigaherz.sewingkit.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.stream.Stream;

public record SewingMaterialSlotDisplay(SlotDisplay display, int count) implements SlotDisplay
{
    public static final MapCodec<SewingMaterialSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SlotDisplay.CODEC.fieldOf("display").forGetter(SewingMaterialSlotDisplay::display),
                    Codec.INT.fieldOf("count").forGetter(SewingMaterialSlotDisplay::count)
            ).apply(instance, SewingMaterialSlotDisplay::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SewingMaterialSlotDisplay> STREAM_CODEC = StreamCodec.composite(

            SlotDisplay.STREAM_CODEC, SewingMaterialSlotDisplay::display,
            ByteBufCodecs.INT, SewingMaterialSlotDisplay::count,
            SewingMaterialSlotDisplay::new
    );

    @Override
    public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> output)
    {
        return display.resolve(context, output).map(s -> {
            //noinspection unchecked
            return s instanceof ItemStack stack ? (T) stack.copyWithCount(count) : s;
        });
    }

    @Override
    public Type<? extends SlotDisplay> type()
    {
        return SewingKitMod.MATERIAL_SLOT_DISPLAY.get();
    }
}
