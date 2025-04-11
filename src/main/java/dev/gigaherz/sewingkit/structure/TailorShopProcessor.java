package dev.gigaherz.sewingkit.structure;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.loot.RandomDye;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Objects;

public class TailorShopProcessor extends StructureProcessor
{
    public static final MapCodec<TailorShopProcessor> CODEC = Codec.EMPTY.xmap(u -> new TailorShopProcessor(), p -> Unit.INSTANCE);

    public static MapCodec<TailorShopProcessor> codec()
    {
        return CODEC;
    }

    @Override
    protected StructureProcessorType<?> getType()
    {
        return SewingKitMod.TAILOR_SHOP_PROCESSOR.get();
    }

    @SuppressWarnings("NullableProblems")
    @Nullable
    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos seedPos, StructureTemplate.StructureEntityInfo rawEntityInfo, StructureTemplate.StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
    {
        var id = entityInfo.nbt.getString("id");
        if (Objects.equals(id, "minecraft:armor_stand"))
        {
            var s = placementSettings.getRandom(entityInfo.blockPos);

            if (s.nextDouble() < 0.1) return null;

            entityInfo = new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, entityInfo.nbt.copy());

            // nbt -> ArmorItems[4] -> item NBT
            var armorTag = entityInfo.nbt.getListOrEmpty("ArmorItems");
            if (!entityInfo.nbt.contains("ArmorItems"))
                entityInfo.nbt.put("ArmorItems", armorTag);

            var provider = level.registryAccess();
            putArmorPieceMaybe(armorTag, 3, s, provider, Items.LEATHER_HELMET, SewingKitMod.WOOL_HAT.get());
            putArmorPieceMaybe(armorTag, 2, s, provider, Items.LEATHER_CHESTPLATE, SewingKitMod.WOOL_SHIRT.get());
            putArmorPieceMaybe(armorTag, 1, s, provider, Items.LEATHER_LEGGINGS, SewingKitMod.WOOL_PANTS.get());
            putArmorPieceMaybe(armorTag, 0, s, provider, Items.LEATHER_BOOTS, SewingKitMod.WOOL_SHOES.get());
        }
        return entityInfo;
    }

    private void putArmorPieceMaybe(ListTag armorTag, int index, RandomSource rand, HolderLookup.Provider provider, Item... items)
    {
        if (items.length > 0 && rand.nextDouble() < 0.25f)
        {
            var item = items[rand.nextInt(items.length)];
            var stack = RandomDye.getRandomDye(new ItemStack(item), rand);
            armorTag.set(index, stack.save(provider));
        }
        /*else
        {
            armorTag.set(index, new CompoundTag());
        }*/
    }
}
