package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum Needles implements NeedleMaterial
{
    WOOD("wood", 10, ToolMaterial.WOOD, SewingKitMod.WOOD_SEWING_NEEDLE, ItemTags.PLANKS),
    STONE("stone", 15, ToolMaterial.STONE, SewingKitMod.STONE_SEWING_NEEDLE, ItemTags.STONE_CRAFTING_MATERIALS),
    IRON("iron", 150, ToolMaterial.IRON, SewingKitMod.IRON_SEWING_NEEDLE, Tags.Items.INGOTS_IRON),
    DIAMOND("diamond", 250, ToolMaterial.DIAMOND, SewingKitMod.DIAMOND_SEWING_NEEDLE, Tags.Items.GEMS_DIAMOND),
    GOLD("gold", 25, ToolMaterial.GOLD, SewingKitMod.GOLD_SEWING_NEEDLE, Tags.Items.INGOTS_GOLD),
    NETHERITE("netherite", 350, ToolMaterial.NETHERITE, SewingKitMod.NETHERITE_SEWING_NEEDLE, Tags.Items.INGOTS_NETHERITE),
    BONE("bone", 50, SewingKitMod.BONE_TIER, SewingKitMod.BONE_SEWING_NEEDLE, Tags.Items.BONES);

    private final String type;
    private final int uses;
    private final ToolMaterial toolMaterial;
    private final DeferredHolder<Item, ? extends Item> needleSupplier;

    private final TagKey<Item> materialTag;

    Needles(String type, int uses, ToolMaterial toolMaterial, DeferredHolder<Item, ? extends Item> needleSupplier, TagKey<Item> materialTag)
    {
        this.type = type;
        this.uses = uses;
        this.toolMaterial = toolMaterial;
        this.needleSupplier = needleSupplier;
        this.materialTag = materialTag;
    }

    public String getType()
    {
        return type;
    }

    public ResourceLocation getId()
    {
        return needleSupplier.getId();
    }

    public Item getNeedle()
    {
        return needleSupplier.get();
    }

    @Override
    public ToolMaterial getToolMaterial()
    {
        return toolMaterial;
    }

    @Override
    public int getUses()
    {
        return uses;
    }

    public TagKey<Item> getMaterial()
    {
        return materialTag;
    }
}
