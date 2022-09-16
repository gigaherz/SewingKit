package dev.gigaherz.sewingkit.structure;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Objects;

public class TailorShopProcessor extends StructureProcessor
{
    public static final Codec<TailorShopProcessor> CODEC = Codec.EMPTY.xmap(u -> new TailorShopProcessor(), p -> Unit.INSTANCE).codec();

    public static Codec<TailorShopProcessor> codec() { return CODEC; }

    @Override
    protected StructureProcessorType<?> getType()
    {
        return SewingKitMod.TAILOR_SHOP_PROCESSOR.get();
    }

    @SuppressWarnings("NullableProblems")
    @Nullable
    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureTemplate.StructureEntityInfo rawEntityInfo, StructureTemplate.StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
    {
        var id = entityInfo.nbt.getString("id");
        if (Objects.equals(id, "minecraft:armor_stand"))
        {
            RandomSource s = world instanceof ServerLevel sl ? sl.random : RandomSource.create();

            if (s.nextDouble() < 0.1) return null;

            // TODO: choose random armor pieces with random color
        }
        return entityInfo;
    }
}
