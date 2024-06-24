package dev.gigaherz.sewingkit.table;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class SewingInput implements RecipeInput
{
    public static SewingInput ofSewingTableInventory(IItemHandler inv)
    {
        var tool = inv.getStackInSlot(0);
        var pattern = inv.getStackInSlot(1);
        var materials = List.of(
                inv.getStackInSlot(1),
                inv.getStackInSlot(2),
                inv.getStackInSlot(3),
                inv.getStackInSlot(4)
        );
        return new SewingInput(materials, tool, pattern);
    }

    private final List<ItemStack> materials;
    private final ItemStack tool;
    private final ItemStack pattern;

    public SewingInput(List<ItemStack> materials, ItemStack tool, ItemStack pattern)
    {
        this.materials = materials;
        this.tool = tool;
        this.pattern = pattern;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        if (slot == 0) return tool;
        if (slot == 1) return pattern;
        return materials.get(slot - 2);
    }

    public ItemStack getTool()
    {
        return tool;
    }

    public ItemStack getPattern()
    {
        return pattern;
    }

    public ItemStack getMaterial(int mat)
    {
        return materials.get(mat);
    }

    @Override
    public int size()
    {
        return 6;
    }
}
