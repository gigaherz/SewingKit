package dev.gigaherz.sewingkit.loot;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class RandomDye extends LootItemConditionalFunction
{
    public static final MapCodec<RandomDye> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance)
                    .apply(instance, RandomDye::new)
    );

    public static LootItemConditionalFunction.Builder<?> builder()
    {
        return LootItemConditionalFunction.simpleBuilder(RandomDye::new);
    }

    protected RandomDye(List<LootItemCondition> conditions)
    {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<RandomDye> getType()
    {
        return SewingKitMod.RANDOM_DYE.get();
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext lootContext)
    {
        return getRandomDye(itemStack, lootContext.getRandom());
    }

    public static ItemStack getRandomDye(ItemStack original, RandomSource rand) {
        if (original.is(ItemTags.DYEABLE))
        {
            List<DyeItem> list = Lists.newArrayList();
            list.add(getRandomDye(rand));
            if (rand.nextFloat() > 0.7F) {
                list.add(getRandomDye(rand));
            }

            if (rand.nextFloat() > 0.8F) {
                list.add(getRandomDye(rand));
            }

            original = DyedItemColor.applyDyes(original, list);
        }

        return original;
    }

    public static DyeItem getRandomDye(RandomSource rand) {
        return DyeItem.byColor(DyeColor.byId(rand.nextInt(16)));
    }
}
