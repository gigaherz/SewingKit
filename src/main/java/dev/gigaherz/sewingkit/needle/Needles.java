package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

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

    public static final TagKey<Block> BREAKABLE_NEEDLE = TagKey.create(Registries.BLOCK, SewingKitMod.location("breakable_needle"));

    public static Item.Properties fillProperties(NeedleMaterial material, Item.Properties props)
    {
        return props
                .durability(material.getUses())
                .tool(material.getToolMaterial(), BREAKABLE_NEEDLE, 0, 1, 0)
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("text.sewingkit.needle.lore_text")/*.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)*/
                )));
    }
}
