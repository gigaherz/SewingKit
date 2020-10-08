package dev.gigaherz.sewingkit.needle;

import net.minecraft.block.Block;
import net.minecraft.item.ToolItem;
import net.minecraftforge.common.ToolType;

import java.util.Collections;
import java.util.Set;

public class NeedleItem extends ToolItem
{
    public static final ToolType SEWING_NEEDLE = ToolType.get("sewing_needle");

    public NeedleItem(float attackDamageIn, float attackSpeedIn, INeedleTier tier, Properties builderIn)
    {
        super(attackDamageIn, attackSpeedIn, tier, Collections.emptySet(), builderIn
                .addToolType(NeedleItem.SEWING_NEEDLE, tier.getHarvestLevel())
                .maxDamage(tier.getMaxUses())
        );
    }
}
