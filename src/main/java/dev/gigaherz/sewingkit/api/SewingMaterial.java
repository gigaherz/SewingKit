package dev.gigaherz.sewingkit.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.function.Predicate;

public record SewingMaterial(Ingredient ingredient, int count) implements Predicate<ItemStack>
{
    public static final Codec<SewingMaterial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SewingMaterial::ingredient),
            Codec.INT.fieldOf("count").forGetter(SewingMaterial::count)
    ).apply(instance, SewingMaterial::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SewingMaterial> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SewingMaterial::ingredient,
            ByteBufCodecs.INT, SewingMaterial::count,
            SewingMaterial::new
    );

    public static SewingMaterial of(Ingredient ingredient, int count)
    {
        return new SewingMaterial(ingredient, count);
    }

    @Override
    public boolean test(ItemStack itemStack)
    {
        return ingredient.test(itemStack) && itemStack.getCount() >= count;
    }

    public SlotDisplay display()
    {
        return new SewingMaterialSlotDisplay(ingredient.display(), count);
    }
}
