package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.block.Block;
import net.minecraft.data.*;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SewingKitDataGen
{
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();

        if (event.includeClient())
        {
            gen.addProvider(new Lang(gen));
            // Let blockstate provider see generated item models by passing its existing file helper
            ItemModelProvider itemModels = new ItemModels(gen, event.getExistingFileHelper());
            gen.addProvider(itemModels);
            gen.addProvider(new BlockStates(gen, itemModels.existingFileHelper));
        }
        if (event.includeServer())
        {

            //BlockTags blockTags = new BlockTags(gen);
            //gen.addProvider(blockTags);
            //gen.addProvider(new ItemTags(gen, blockTags));
            gen.addProvider(new Recipes(gen));
            gen.addProvider(new LootTables(gen));
        }
    }

    public static class Lang extends LanguageProvider
    {
        public Lang(DataGenerator gen)
        {
            super(gen, SewingKitMod.MODID, "en_us");
        }

        @Override
        protected void addTranslations()
        {
            add("itemGroup.sewing_kit", "Sewing Kit");
            add("container.sewingkit.sewing_station", "Sewing Station");
            add("jei.category.sewingkit.sewing", "Sewing");

            add(SewingKitMod.LEATHER_STRIP.get(), "Leather Strip");
            add(SewingKitMod.LEATHER_SHEET.get(), "Leather Sheet");

            add(SewingKitMod.SEWING_STATION_BLOCK.get(), "Sewing Table");
            add(SewingKitMod.STORING_SEWING_STATION_BLOCK.get(), "Sewing Table with Drawers");

            Arrays.stream(Needles.values()).forEach(needle -> {
                String type = needle.getType();
                String name = type.substring(0, 1).toUpperCase() + type.substring(1);
                add(needle.getNeedle(), name + " Sewing Needle");
            });
            add("text.sewingkit.needle.lore_text", "\"Ow! Prickl...zz..z...\"");

            add(SewingKitMod.WOOL_HAT.get(), "Wool Hat");
            add(SewingKitMod.WOOL_SHIRT.get(), "Wool Shirt");
            add(SewingKitMod.WOOL_PANTS.get(), "Wool Pants");
            add(SewingKitMod.WOOL_SHOES.get(), "Wool Shoes");

            add(SewingKitMod.WOOL_ROLL.get(), "Wool Roll");
            add(SewingKitMod.WOOL_TRIM.get(), "Wool Trim");

            add(SewingKitMod.COMMON_PATTERN.get(), "Common Pattern");
            add(SewingKitMod.UNCOMMON_PATTERN.get(), "Uncommon Pattern");
            add(SewingKitMod.RARE_PATTERN.get(), "Rare Pattern");
            add(SewingKitMod.LEGENDARY_PATTERN.get(), "Legendary Pattern");

            add(SewingKitMod.FILE.get(), "Raspy File");

            add("entity.minecraft.villager.sewingkit.tailor", "Tailor");
        }
    }

    public static class BlockStates extends BlockStateProvider
    {

        public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper)
        {
            super(gen, SewingKitMod.MODID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels()
        {
            {
                Block block = SewingKitMod.SEWING_STATION_BLOCK.get();
                horizontalBlock(block, models().getExistingFile(blockTexture(block)));
            }
            {
                Block block = SewingKitMod.STORING_SEWING_STATION_BLOCK.get();
                horizontalBlock(block, models().getExistingFile(blockTexture(block)));
            }
        }
    }

    public static class ItemModels extends ItemModelProvider
    {
        public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper)
        {
            super(generator, SewingKitMod.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels()
        {
            basicIcon(SewingKitMod.LEATHER_STRIP.getId());
            basicIcon(SewingKitMod.LEATHER_SHEET.getId());
            Arrays.stream(Needles.values()).forEach(needle -> basicIcon(needle.getId()));

            basicIcon(SewingKitMod.WOOL_HAT.getId());
            basicIcon(SewingKitMod.WOOL_SHIRT.getId());
            basicIcon(SewingKitMod.WOOL_PANTS.getId());
            basicIcon(SewingKitMod.WOOL_SHOES.getId());

            basicIcon(SewingKitMod.WOOL_ROLL.getId());
            basicIcon(SewingKitMod.WOOL_TRIM.getId());

            basicIcon(SewingKitMod.COMMON_PATTERN.getId());
            basicIcon(SewingKitMod.UNCOMMON_PATTERN.getId());
            basicIcon(SewingKitMod.RARE_PATTERN.getId());
            basicIcon(SewingKitMod.LEGENDARY_PATTERN.getId());

            basicIcon(SewingKitMod.FILE.getId())
                .transforms()
                    .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                        .rotation(62, 180 - 33, 40)
                        .translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    .end()
                    .transform(ModelBuilder.Perspective.THIRDPERSON_LEFT)
                        .rotation(45, -33, -55)
                        .translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    .end()
                    .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                        .rotation(-54, 99, 136)
                        .translation(1.13f, 5f, 1.13f)
                        .scale(0.68f)
                    .end()
                    .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                        .rotation(136, -99, 54)
                        .translation(1.13f, 5f, 1.13f)
                        .scale(0.68f)
                    .end()
                    .transform(ModelBuilder.Perspective.GROUND)
                        .translation(0, 2, 0)
                        .scale(0.5f)
                    .end()
                    .transform(ModelBuilder.Perspective.HEAD)
                        .rotation(-4, 44, 4)
                        .translation(-7.25f, 6.75f, 0.75f)
                    .end()
                    .transform(ModelBuilder.Perspective.FIXED)
                        .rotation(0, 180, 0)
                    .end()
                .end();

            getBuilder(SewingKitMod.SEWING_STATION_ITEM.getId().getPath())
                    .parent(getExistingFile(modelLocation(SewingKitMod.SEWING_STATION_BLOCK.get())));

            getBuilder(SewingKitMod.STORING_SEWING_STATION_ITEM.getId().getPath())
                    .parent(getExistingFile(modelLocation(SewingKitMod.STORING_SEWING_STATION_BLOCK.get())));
        }

        private static ResourceLocation modelLocation(Block block)
        {
            ResourceLocation id = Objects.requireNonNull(block.getRegistryName());
            return new ResourceLocation(id.getNamespace(), "block/" + id.getPath());
        }

        private ItemModelBuilder basicIcon(ResourceLocation item)
        {
            return getBuilder(item.getPath())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", SewingKitMod.location("item/" + item.getPath()));
        }
    }

    private static class Recipes extends RecipeProvider
    {
        public Recipes(DataGenerator gen)
        {
            super(gen);
        }

        @Override
        protected void registerRecipes(Consumer<IFinishedRecipe> consumer)
        {
            Arrays.stream(Needles.values()).forEach(needle -> ShapelessRecipeBuilder.shapelessRecipe(needle.getNeedle())
                                .addIngredient(SewingKitMod.FILE.get())
                                .addIngredient(needle.getRepairMaterial())
                                .addCriterion("has_material", needle.getMaterial().map(this::hasItem, this::hasItem))
                                .build(consumer));

            ShapedRecipeBuilder.shapedRecipe(SewingKitMod.SEWING_STATION_ITEM.get())
                    .patternLine("xxx")
                    .patternLine("P P")
                    .patternLine("S S")
                    .key('x', ItemTags.WOODEN_SLABS)
                    .key('P', ItemTags.PLANKS)
                    .key('S', Items.STICK)
                    .addCriterion("has_wood", hasItem(ItemTags.PLANKS))
                    .build(consumer);

            ShapelessRecipeBuilder.shapelessRecipe(SewingKitMod.STORING_SEWING_STATION_ITEM.get())
                    .addIngredient(SewingKitMod.SEWING_STATION_ITEM.get())
                    .addIngredient(Tags.Items.CHESTS_WOODEN)
                    .addCriterion("has_station", hasItem(SewingKitMod.SEWING_STATION_ITEM.get()))
                    .build(consumer);

            // Sewing recipes: leather
            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_SHEET.get(), 4)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Tags.Items.LEATHER)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_sheet_from_leather"));

            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_SHEET.get(), 1)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Items.RABBIT_HIDE)
                    .addCriterion("has_leather", hasItem(Items.RABBIT_HIDE))
                    .build(consumer, SewingKitMod.location("leather_sheet_from_rabbit_hide"));

            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_STRIP.get(), 3)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Tags.Items.LEATHER)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_strip_from_leather"));

            SewingRecipeBuilder.begin(Items.LEATHER_BOOTS)
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_boots_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_LEGGINGS)
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 4)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 3)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_leggings_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_CHESTPLATE)
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 8)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 2)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_chestplate_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_HELMET)
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_helmet_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_HORSE_ARMOR)
                    .withTool(NeedleItem.SEWING_NEEDLE, Needles.DIAMOND.getHarvestLevel())
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 12)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 6)
                    .addMaterial(Tags.Items.STRING, 8)
                    .addCriterion("has_leather", hasItem(Tags.Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_horse_armor_via_sewing"));

            // Sewing recipes: wool
            SewingRecipeBuilder.begin(SewingKitMod.WOOL_ROLL.get(), 4)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_roll_from_wool"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_ROLL.get(), 1)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.CARPETS)
                    .addCriterion("has_wool", hasItem(ItemTags.CARPETS))
                    .build(consumer, SewingKitMod.location("wool_roll_from_carpet"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_TRIM.get(), 8)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_trim_from_wool"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_TRIM.get(), 3)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.CARPETS)
                    .addCriterion("has_wool", hasItem(ItemTags.CARPETS))
                    .build(consumer, SewingKitMod.location("wool_trim_from_carpet"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_SHOES.get())
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 2)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_shoes_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_PANTS.get())
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 2)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 4)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_pants_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_SHIRT.get())
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 3)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 3)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_shirt_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_HAT.get())
                    .withTool(NeedleItem.SEWING_NEEDLE, 1)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 1)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_hat_via_sewing"));

            ShapedRecipeBuilder.shapedRecipe(SewingKitMod.FILE.get())
                    .patternLine("  I")
                    .patternLine(" I ")
                    .patternLine("P  ")
                    .key('I', Tags.Items.INGOTS_IRON)
                    .key('P', ItemTags.PLANKS)
                    .addCriterion("has_iron", hasItem(Tags.Items.INGOTS_IRON))
                    .build(consumer);

        }
    }

    private static class LootTables extends LootTableProvider implements IDataProvider
    {
        public LootTables(DataGenerator gen)
        {
            super(gen);
        }

        private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> tables = ImmutableList.of(
                Pair.of(LootTables.BlockTables::new, LootParameterSets.BLOCK)
                //Pair.of(FishingLootTables::new, LootParameterSets.FISHING),
                //Pair.of(ChestLootTables::new, LootParameterSets.CHEST),
                //Pair.of(EntityLootTables::new, LootParameterSets.ENTITY),
                //Pair.of(GiftLootTables::new, LootParameterSets.GIFT)
        );

        @Override
        protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables()
        {
            return tables;
        }

        @Override
        protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationtracker)
        {
            map.forEach((p_218436_2_, p_218436_3_) -> {
                LootTableManager.func_227508_a_(validationtracker, p_218436_2_, p_218436_3_);
            });
        }

        public static class BlockTables extends BlockLootTables
        {
            @Override
            protected void addTables()
            {
                this.registerDropSelfLootTable(SewingKitMod.SEWING_STATION_BLOCK.get());
                this.registerDropSelfLootTable(SewingKitMod.STORING_SEWING_STATION_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks()
            {
                return ForgeRegistries.BLOCKS.getValues().stream()
                        .filter(b -> b.getRegistryName().getNamespace().equals(SewingKitMod.MODID))
                        .collect(Collectors.toList());
            }
        }
    }
}
