package dev.gigaherz.sewingkit.clothing;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;

public class ClothArmorItem extends DyeableArmorItem
{
    public ClothArmorItem(ArmorMaterial material, EquipmentSlot slot, Properties properties)
    {
        super(material, slot, properties);
    }

    @Override
    public int getColor(ItemStack stack)
    {
        if (!hasCustomColor(stack))
            return 0xFFFFFF;
        return super.getColor(stack);
    }
}
