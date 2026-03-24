package dev.gigaherz.sewingkit.file;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class FileItem extends Item
{
    public FileItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance)
    {
        int damage;
        int maxDamage;
        DataComponentPatch patch;
        switch (instance)
        {
            case ItemStack stack:
                patch = stack.getComponentsPatch();
                damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
                maxDamage = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
                break;
            case ItemStackTemplate template:
                patch = template.components();
                damage = template.getOrDefault(DataComponents.DAMAGE, 0);
                maxDamage = template.getOrDefault(DataComponents.MAX_DAMAGE, 0);
                break;
            default:
                return null;
        }
        if (damage < maxDamage)
        {
            var builder = DataComponentPatch.builder();

            for(var entry : patch.entrySet())
            {
                @SuppressWarnings("rawtypes")
                DataComponentType e = entry.getKey();
                var optional = entry.getValue();
                if (optional.isPresent())
                {
                    //noinspection unchecked
                    builder = builder.set(e, optional.get());
                }
                else
                {
                    //noinspection unchecked
                    builder.remove(e);
                }
            }

            patch = builder.set(DataComponents.DAMAGE, damage + 1).build();

            return new ItemStackTemplate(instance.typeHolder(), instance.count(), patch);
        }
        else
        {
            return null;
        }
    }

    /*@Override
    public ItemStack getCraftingRemainder(ItemStack itemStack)
    {
        if (itemStack.getDamageValue() < itemStack.getMaxDamage())
        {
            ItemStack stack = itemStack.copy();
            stack.setDamageValue(itemStack.getDamageValue() + 1);
            return stack;
        }
        return ItemStack.EMPTY;
    }*/
}
