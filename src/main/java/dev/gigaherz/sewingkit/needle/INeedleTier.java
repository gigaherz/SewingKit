package dev.gigaherz.sewingkit.needle;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public interface INeedleTier extends Tier
{
    String getType();

    ResourceLocation getId();

    Item getNeedle();
}
