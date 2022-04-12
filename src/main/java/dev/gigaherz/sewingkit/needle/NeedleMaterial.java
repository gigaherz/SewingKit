package dev.gigaherz.sewingkit.needle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public interface NeedleMaterial
{
    String getType();

    ResourceLocation getId();

    Item getNeedle();

    int getUses();

    Tier getTier();
}
