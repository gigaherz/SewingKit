package dev.gigaherz.sewingkit.tools;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record MatchBlock(HolderSet<Block> tag) implements LootItemCondition
{
    public static final MapCodec<MatchBlock> CODEC =
            HolderSetCodec.create(Registries.BLOCK, BuiltInRegistries.BLOCK.holderByNameCodec(), false)
                    .fieldOf("blocks").xmap(MatchBlock::new, MatchBlock::tag);

    @Override
    public MapCodec<? extends LootItemCondition> codec()
    {
        return CODEC;
    }

    @Override
    public boolean test(LootContext lootContext)
    {
        return lootContext.hasParameter(LootContextParams.BLOCK_STATE) && lootContext.getParameter(LootContextParams.BLOCK_STATE).is(tag);
    }
}
