package dev.gigaherz.sewingkit.needle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.item.Item.Properties;

public class NeedleItem extends DiggerItem
{
    public static final ToolType SEWING_NEEDLE = ToolType.get("sewing_needle");
    public static final Tag.Named<Block> BREAKABLE_NEEDLE = BlockTags.createOptional(new ResourceLocation("toolbelt:breakable_needle"));

    public NeedleItem(float attackDamageIn, float attackSpeedIn, INeedleTier tier, Properties builderIn)
    {
        super(attackDamageIn, attackSpeedIn, tier, BREAKABLE_NEEDLE, builderIn
                .addToolType(NeedleItem.SEWING_NEEDLE, tier.getLevel())
                .durability(tier.getUses())
        );
    }

    @OnlyIn(Dist.CLIENT)
    // This is one of the only cases where OnlyIn is necessary, don't use it anywhre else unless told to do so by someone who knows what they are talking about
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
    {
        tooltip.add(new TranslatableComponent("text.sewingkit.needle.lore_text").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
