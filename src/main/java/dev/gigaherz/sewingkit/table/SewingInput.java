package dev.gigaherz.sewingkit.table;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public class SewingInput implements RecipeInput
{
    public static SewingInput ofSewingTableInventory(Container inv)
    {
        var tool = inv.getItem(0);
        var pattern = inv.getItem(1);
        var materials = List.of(
                inv.getItem(2),
                inv.getItem(3),
                inv.getItem(4),
                inv.getItem(5)
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
