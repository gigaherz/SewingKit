package dev.gigaherz.sewingkit.needle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;


public class NeedleItem extends DiggerItem
{
    public static final ToolAction SEW = ToolAction.get("sewingkit_sew");

    public static final TagKey<Block> BREAKABLE_NEEDLE = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("toolbelt:breakable_needle"));

    public NeedleItem(float attackDamageIn, float attackSpeedIn, NeedleMaterial material, Properties builderIn)
    {
        super(attackDamageIn, attackSpeedIn, material.getTier(), BREAKABLE_NEEDLE, builderIn.durability(material.getUses()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
    {
        tooltip.add(Component.translatable("text.sewingkit.needle.lore_text").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static final Set<ToolAction> actions = Set.of(SEW);

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction)
    {
        return actions.contains(toolAction);
    }
}
