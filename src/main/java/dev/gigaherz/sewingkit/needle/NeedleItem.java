package dev.gigaherz.sewingkit.needle;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

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

    @OnlyIn(Dist.CLIENT) // This is one of the only cases where OnlyIn is necessary, don't use it anywhre else unless told to do so by someone who knows what they are talking about
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(new TranslationTextComponent("text.sewingkit.needle.lore_text").mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
    }
}
