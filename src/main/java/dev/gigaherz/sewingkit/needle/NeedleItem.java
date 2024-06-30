package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;


public class NeedleItem extends DiggerItem
{
    public static final TagKey<Block> BREAKABLE_NEEDLE = TagKey.create(Registries.BLOCK, SewingKitMod.location("breakable_needle"));

    public NeedleItem(float attackDamageIn, float attackSpeedIn, NeedleMaterial material, Properties builderIn)
    {
        // :thonk: what to do with attackDamageIn, attackSpeedIn ???
        super(material.getTier(), BREAKABLE_NEEDLE, builderIn.durability(material.getUses()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn)
    {
        tooltip.add(Component.translatable("text.sewingkit.needle.lore_text").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
