package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum Needles implements NeedleMaterial
{
    WOOD("wood", 10, Tiers.WOOD, SewingKitMod.WOOD_SEWING_NEEDLE, ItemTags.PLANKS),
    STONE("stone", 15, Tiers.STONE, SewingKitMod.STONE_SEWING_NEEDLE, ItemTags.STONE_CRAFTING_MATERIALS),
    IRON("iron", 150, Tiers.IRON, SewingKitMod.IRON_SEWING_NEEDLE, Tags.Items.INGOTS_IRON),
    DIAMOND("diamond", 250, Tiers.DIAMOND, SewingKitMod.DIAMOND_SEWING_NEEDLE, Tags.Items.GEMS_DIAMOND),
    GOLD("gold", 25, Tiers.GOLD, SewingKitMod.GOLD_SEWING_NEEDLE, Tags.Items.INGOTS_GOLD),
    NETHERITE("netherite", 350, Tiers.NETHERITE, SewingKitMod.NETHERITE_SEWING_NEEDLE, Tags.Items.INGOTS_NETHERITE),
    BONE("bone", 50, SewingKitMod.BONE_TIER, SewingKitMod.BONE_SEWING_NEEDLE, Tags.Items.BONES);

    private final String type;
    private final int uses;
    private final Tier tier;
    private final DeferredHolder<Item, ? extends Item> needleSupplier;

    private final TagKey<Item> materialTag;

    Needles(String type, int uses, Tier tier, DeferredHolder<Item, ? extends Item> needleSupplier, TagKey<Item> materialTag)
    {
        this.type = type;
        this.uses = uses;
        this.tier = tier;
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
    public Tier getTier()
    {
        return tier;
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
