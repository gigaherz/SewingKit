package dev.gigaherz.sewingkit.clothing;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;

public class ClothArmorItem extends DyeableArmorItem
{
    public ClothArmorItem(IArmorMaterial material, EquipmentSlotType slot, Properties properties)
    {
        super(material, slot, properties);
    }

    @Override
    public int getColor(ItemStack stack)
    {
        if (!hasColor(stack))
            return 0xFFFFFF;
        return super.getColor(stack);
    }
}
