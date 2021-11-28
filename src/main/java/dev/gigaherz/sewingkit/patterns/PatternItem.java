package dev.gigaherz.sewingkit.patterns;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.item.Item.Properties;

public class PatternItem extends Item
{
    public PatternItem(Properties properties)
    {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("This feature is not implemented yet.").withStyle(TextFormatting.YELLOW, TextFormatting.BOLD));
        tooltip.add(new TranslationTextComponent("This item may be removed in the future.").withStyle(TextFormatting.RED, TextFormatting.BOLD));
    }
}
