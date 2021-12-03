package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.tags.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.RegistryObject;

public enum Needles implements NeedleMaterial
{
    WOOD("wood", 10, Tiers.WOOD, SewingKitMod.WOOD_SEWING_NEEDLE, ItemTags.bind("minecraft:planks")),
    STONE("stone", 15, Tiers.STONE, SewingKitMod.STONE_SEWING_NEEDLE, ItemTags.bind("minecraft:stone_crafting_materials")),
    IRON("iron", 150, Tiers.IRON, SewingKitMod.IRON_SEWING_NEEDLE, ItemTags.bind("forge:ingots/iron")),
    DIAMOND("diamond", 250, Tiers.DIAMOND, SewingKitMod.DIAMOND_SEWING_NEEDLE, ItemTags.bind("forge:gems/diamond")),
    GOLD("gold", 25, Tiers.GOLD, SewingKitMod.GOLD_SEWING_NEEDLE, ItemTags.bind("forge:ingots/gold")),
    NETHERITE("netherite", 350, Tiers.NETHERITE, SewingKitMod.NETHERITE_SEWING_NEEDLE, ItemTags.bind("forge:ingots/netherite")),
    BONE("bone", 50, SewingKitMod.BONE_TIER, SewingKitMod.BONE_SEWING_NEEDLE, ItemTags.bind("forge:bones"));

    private final String type;
    private final int uses;
    private final Tier tier;
    private final RegistryObject<Item> needleSupplier;

    private final Tag<Item> materialTag;

    Needles(String type, int uses, Tier tier, RegistryObject<Item> needleSupplier, Tag<Item> materialTag)
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

    public Tag<Item> getMaterial()
    {
        return materialTag;
    }
}
