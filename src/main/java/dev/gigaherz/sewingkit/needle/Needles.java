package dev.gigaherz.sewingkit.needle;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.RegistryObject;

public enum Needles implements NeedleMaterial
{
    IRON("iron", 40, Tiers.IRON, SewingKitMod.IRON_SEWING_NEEDLE, bind("forge:ingots/iron")),
    GOLD("gold", 40, Tiers.GOLD, SewingKitMod.GOLD_SEWING_NEEDLE, bind("forge:ingots/gold")),
    DIAMOND("diamond", 100, Tiers.DIAMOND, SewingKitMod.DIAMOND_SEWING_NEEDLE, bind("forge:gems/diamond")),
    NETHERITE("netherite", 100, Tiers.NETHERITE, SewingKitMod.NETHERITE_SEWING_NEEDLE, bind("forge:ingots/netherite"));

    private final String type;
    private final int uses;
    private final Tier tier;
    private final RegistryObject<Item> needleSupplier;

    private final TagKey<Item> materialTag;

    Needles(String type, int uses, Tier tier, RegistryObject<Item> needleSupplier, TagKey<Item> materialTag)
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

    private static TagKey<Item> bind(String name)
    {
        return TagKey.create(Registries.ITEM, new ResourceLocation(name));
    }
}
