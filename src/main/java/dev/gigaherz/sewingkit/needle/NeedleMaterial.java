package dev.gigaherz.sewingkit.needle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public interface NeedleMaterial
{
    String getType();

    ResourceLocation getId();

    Item getNeedle();

    int getUses();

    ToolMaterial getToolMaterial();
}
