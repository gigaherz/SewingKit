package dev.gigaherz.sewingkit.tools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.List;
import java.util.function.Predicate;

public record ConvertDrops(
        LootItemCondition[] conditions,
        int priority,
        ItemPredicate acceptedInputs,
        ItemStackTemplate result,
        Predicate<LootContext> combinedConditions
) implements IGlobalLootModifier
{
    public static final MapCodec<ConvertDrops> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(ConvertDrops::conditions),
            Codec.INT.optionalFieldOf("priority", IGlobalLootModifier.DEFAULT_PRIORITY).forGetter(ConvertDrops::priority),
            ItemPredicate.CODEC.fieldOf("accepted_inputs").forGetter(ConvertDrops::acceptedInputs),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ConvertDrops::result)
    ).apply(instance, ConvertDrops::new));

    public ConvertDrops(LootItemCondition[] conditions, int priority, ItemPredicate acceptedInputs, ItemStackTemplate result)
    {
        this(conditions, priority, acceptedInputs, result, AllOfCondition.allOf(List.of(conditions)));
    }

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!this.combinedConditions.test(context))
            return generatedLoot;

        return generatedLoot.stream().map(stack -> {
            if (!acceptedInputs.test(stack))
                return stack;

            if (context.getParameter(LootContextParams.TOOL) instanceof ItemStack is) {
                if (context.getParameter(LootContextParams.THIS_ENTITY) instanceof ServerPlayer sp) {
                    var mh = sp.getMainHandItem();
                    if (mh.is(is.getItem())) {

                        if (mh.isDamageableItem() && mh.getDamageValue() < mh.getMaxDamage())
                        {
                            mh.setDamageValue(mh.getDamageValue() + 1);
                        }
                        else
                        {
                            mh.shrink(1);
                        }

                        return result.create();
                    }
                }
            }

            return stack;
        }).collect(ObjectArrayList.toList());
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
}
