package dev.gigaherz.sewingkit.needle;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public interface NeedleMaterial
{
    String getType();

    ResourceLocation getId();

    Item getNeedle();

    int getUses();

    Tier getTier();
}
