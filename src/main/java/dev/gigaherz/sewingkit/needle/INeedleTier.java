package dev.gigaherz.sewingkit.needle;

import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;

public interface INeedleTier extends IItemTier
{
    String getType();

    ResourceLocation getId();

    Item getNeedle();
}
