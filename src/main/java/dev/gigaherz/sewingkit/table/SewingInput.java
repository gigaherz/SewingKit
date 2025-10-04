package dev.gigaherz.sewingkit.table;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

public class SewingInput implements RecipeInput
{
    public static ItemStack stackAt(ResourceHandler<ItemResource> inv, int slot)
    {
        return inv.getResource(slot).toStack(inv.getAmountAsInt(slot));
    }

    public static SewingInput ofSewingTableInventory(ResourceHandler<ItemResource> inv)
    {
        var tool = stackAt(inv, 0);
        var pattern = stackAt(inv, 1);
        var materials = List.of(
                stackAt(inv, 2),
                stackAt(inv, 3),
                stackAt(inv, 4),
                stackAt(inv, 5)
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
