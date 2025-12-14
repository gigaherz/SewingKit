package dev.gigaherz.sewingkit.needle;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public interface NeedleMaterial
{
    String getType();

    Identifier getId();

    Item getNeedle();

    int getUses();

    ToolMaterial getToolMaterial();
}
