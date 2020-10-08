package dev.gigaherz.sewingkit.needle;

import com.mojang.datafixers.util.Either;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.needle.INeedleTier;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Supplier;

public enum Needles implements INeedleTier
{
    WOOD("wood", 1, 10, 2, 0, 15, SewingKitMod.WOOD_SEWING_NEEDLE, ItemTags.makeWrapperTag("minecraft:planks")),
    STONE("stone", 2, 15, 4, 1, 5, SewingKitMod.STONE_SEWING_NEEDLE, ItemTags.makeWrapperTag("minecraft:stone_crafting_materials")),
    IRON("iron", 3, 150, 6, 2, 4, SewingKitMod.IRON_SEWING_NEEDLE, Items.IRON_INGOT),
    DIAMOND("diamond", 4, 250, 8, 3, 10, SewingKitMod.DIAMOND_SEWING_NEEDLE, Items.DIAMOND),
    GOLD("gold", 1, 25, 12, 0, 22, SewingKitMod.GOLD_SEWING_NEEDLE, Items.GOLD_INGOT),
    NETHERITE("netherite", 5, 350, 9, 4, 15, SewingKitMod.NETHERITE_SEWING_NEEDLE, Items.NETHERITE_INGOT),
    BONE("bone", 2, 50, 4, 1, 12, SewingKitMod.BONE_SEWING_NEEDLE, Items.BONE);

    private final String type;
    private final int toolLevel;
    private final int uses;
    private final float efficiency;
    private final float attackDamage;
    private final int enchantability;
    private final RegistryObject<Item> needleSupplier;
    private final Lazy<Ingredient> repairMaterial;

    private final Either<ITag<Item>,Item> material;

    Needles(String type, int toolLevel, int uses, float efficiency, float attackDamage, int enchantability, RegistryObject<Item> needleSupplier, ITag<Item> materialTag)
    {
        this(type, toolLevel, uses, efficiency, attackDamage, enchantability, needleSupplier, Either.left(materialTag));
    }

    Needles(String type, int toolLevel, int uses, float efficiency, float attackDamage, int enchantability, RegistryObject<Item> needleSupplier, Item materialItem)
    {
        this(type, toolLevel, uses, efficiency, attackDamage, enchantability, needleSupplier, Either.right(materialItem));
    }

    Needles(String type, int toolLevel, int uses, float efficiency, float attackDamage, int enchantability, RegistryObject<Item> needleSupplier, Either<ITag<Item>,Item> material)
    {
        this.type = type;
        this.toolLevel = toolLevel;
        this.uses = uses;
        this.efficiency = efficiency;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.needleSupplier = needleSupplier;
        this.material = material;
        this.repairMaterial = Lazy.of(() -> material.map(Ingredient::fromTag, Ingredient::fromItems));
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
    public int getMaxUses()
    {
        return uses;
    }

    @Override
    public float getEfficiency()
    {
        return efficiency;
    }

    @Override
    public float getAttackDamage()
    {
        return attackDamage;
    }

    @Override
    public int getHarvestLevel()
    {
        return toolLevel;
    }

    @Override
    public int getEnchantability()
    {
        return enchantability;
    }

    @Override
    public Ingredient getRepairMaterial()
    {
        return repairMaterial.get();
    }

    public Either<ITag<Item>, Item> getMaterial()
    {
        return material;
    }
}
