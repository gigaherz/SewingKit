package dev.gigaherz.sewingkit.needle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ToolAction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;


public class NeedleItem extends DiggerItem
{
    public static final ToolAction SEW = ToolAction.get("sewingkit_sew");

    public static final TagKey<Block> BREAKABLE_NEEDLE = TagKey.create(Registries.BLOCK, new ResourceLocation("toolbelt:breakable_needle"));

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

    private static final Set<ToolAction> actions = Set.of(SEW);

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction)
    {
        return actions.contains(toolAction);
    }
}
