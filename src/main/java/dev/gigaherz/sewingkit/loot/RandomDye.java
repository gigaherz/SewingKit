package dev.gigaherz.sewingkit.loot;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class RandomDye extends LootItemConditionalFunction
{
    public static final Codec<RandomDye> CODEC = RecordCodecBuilder.create(
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
    public LootItemFunctionType getType()
    {
        return SewingKitMod.RANDOM_DYE.get();
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext lootContext)
    {
        return getRandomDye(itemStack, lootContext.getRandom());
    }

    public static ItemStack getRandomDye(ItemStack original, RandomSource rand) {
        if (original.getItem() instanceof DyeableArmorItem) {
            List<DyeItem> list = Lists.newArrayList();
            list.add(getRandomDye(rand));
            if (rand.nextFloat() > 0.7F) {
                list.add(getRandomDye(rand));
            }

            if (rand.nextFloat() > 0.8F) {
                list.add(getRandomDye(rand));
            }

            original = DyeableLeatherItem.dyeArmor(original, list);
        }

        return original;
    }

    public static DyeItem getRandomDye(RandomSource rand) {
        return DyeItem.byColor(DyeColor.byId(rand.nextInt(16)));
    }
}
