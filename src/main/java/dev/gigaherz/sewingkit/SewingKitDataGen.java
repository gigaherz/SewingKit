package dev.gigaherz.sewingkit;

import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.recipes.*;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SewingKitDataGen
{
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();

        gen.addProvider(event.includeClient(), new Lang(gen));
        // Let blockstate provider see generated item models by passing its existing file helper
        ItemModelProvider itemModels = new ItemModels(gen, event.getExistingFileHelper());
        gen.addProvider(event.includeClient(), itemModels);
        gen.addProvider(event.includeClient(), new BlockStates(gen, itemModels.existingFileHelper));

        gen.addProvider(event.includeServer(), new BlockTagGen(gen.getPackOutput(), event.getExistingFileHelper()));
        gen.addProvider(event.includeServer(), new ItemTagGen(gen.getPackOutput(), event.getExistingFileHelper()));
        //gen.addProvider(new ItemTags(gen, blockTags));
        gen.addProvider(event.includeServer(), new Recipes(gen.getPackOutput(), event.getLookupProvider()));
        gen.addProvider(event.includeServer(), Loot.create(gen.getPackOutput(), event.getLookupProvider()));
    }

    public static class Lang extends LanguageProvider
    {
        public Lang(DataGenerator gen)
        {
            super(gen.getPackOutput(), SewingKitMod.MODID, "en_us");
        }

        @Override
        protected void addTranslations()
        {
            add("tab.sewing_kit", "Sewing Kit");
            add("container.sewingkit.sewing_station", "Sewing Station");
            add("container.sewingkit.storing_sewing_station", "Sewing Station with Drawers");
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
            add("text.sewingkit.needle.lore_text", "\"Ouch!\"");

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

            add("text.sewingkit.recipe", "Required materials:");

            add("text.sewingkit.pattern.wip", "This feature is not implemented yet.");
            add("text.sewingkit.pattern.may_be_removed", "This item may be removed in the future.");
        }
    }

    public static class BlockStates extends BlockStateProvider
    {

        public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper)
        {
            super(gen.getPackOutput(), SewingKitMod.MODID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels()
        {
            {
                Block block = SewingKitMod.SEWING_STATION_BLOCK.get();
                horizontalBlock(block, models().getExistingFile(ModelLocationUtils.getModelLocation(block)));
            }
            {
                Block block = SewingKitMod.STORING_SEWING_STATION_BLOCK.get();
                horizontalBlock(block, models().getExistingFile(ModelLocationUtils.getModelLocation(block)));
            }
        }
    }

    public static class ItemModels extends ItemModelProvider
    {
        public ItemModels(DataGenerator gen, ExistingFileHelper existingFileHelper)
        {
            super(gen.getPackOutput(), SewingKitMod.MODID, existingFileHelper);
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
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(62, 180 - 33, 40)
                    .translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    .end()
                    .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    .rotation(45, -33, -55)
                    .translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(-54, 99, 136)
                    .translation(1.13f, 5f, 1.13f)
                    .scale(0.68f)
                    .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(136, -99, 54)
                    .translation(1.13f, 5f, 1.13f)
                    .scale(0.68f)
                    .end()
                    .transform(ItemDisplayContext.GROUND)
                    .translation(0, 2, 0)
                    .scale(0.5f)
                    .end()
                    .transform(ItemDisplayContext.HEAD)
                    .rotation(-4, 44, 4)
                    .translation(-7.25f, 6.75f, 0.75f)
                    .end()
                    .transform(ItemDisplayContext.FIXED)
                    .rotation(0, 180, 0)
                    .end()
                    .end();

            getBuilder(SewingKitMod.SEWING_STATION_ITEM.getId().getPath())
                    .parent(getExistingFile(ModelLocationUtils
                            .getModelLocation(SewingKitMod.SEWING_STATION_BLOCK.get())));

            getBuilder(SewingKitMod.STORING_SEWING_STATION_ITEM.getId().getPath())
                    .parent(getExistingFile(ModelLocationUtils
                            .getModelLocation(SewingKitMod.STORING_SEWING_STATION_BLOCK.get())));
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
        public Recipes(PackOutput gen, CompletableFuture<HolderLookup.Provider> lookup)
        {
            super(gen, lookup);
        }

        @Override
        protected void buildRecipes(RecipeOutput consumer)
        {
            Arrays.stream(Needles.values()).forEach(needle -> ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, needle.getNeedle())
                    .requires(SewingKitMod.FILE.get())
                    .requires(needle.getTier().getRepairIngredient())
                    .unlockedBy("has_material", has(needle.getMaterial()))
                    .save(consumer));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SewingKitMod.SEWING_STATION_ITEM.get())
                    .pattern("xxx")
                    .pattern("P P")
                    .pattern("S S")
                    .define('x', ItemTags.WOODEN_SLABS)
                    .define('P', ItemTags.PLANKS)
                    .define('S', Items.STICK)
                    .unlockedBy("has_wood", has(ItemTags.PLANKS))
                    .save(consumer);

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SewingKitMod.STORING_SEWING_STATION_ITEM.get())
                    .requires(SewingKitMod.SEWING_STATION_ITEM.get())
                    .requires(Tags.Items.CHESTS_WOODEN)
                    .unlockedBy("has_station", has(SewingKitMod.SEWING_STATION_ITEM.get()))
                    .save(consumer);

            // Sewing recipes: leather
            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 4)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(Tags.Items.LEATHERS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_sheet_from_leather"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 1)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(Items.RABBIT_HIDE)
                    .addCriterion("has_leather", has(Items.RABBIT_HIDE))
                    .save(consumer, SewingKitMod.location("leather_sheet_from_rabbit_hide"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_STRIP.get(), 3)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(Tags.Items.LEATHERS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_strip_from_leather"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_BOOTS)
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_boots_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_LEGGINGS)
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 4)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 3)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_leggings_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_CHESTPLATE)
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 8)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 2)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_chestplate_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_HELMET)
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_helmet_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
                    .withTool(SewingKitMod.NETHERITE_OR_HIGHER)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 12)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 6)
                    .addMaterial(Tags.Items.STRINGS, 8)
                    .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                    .save(consumer, SewingKitMod.location("leather_horse_armor_via_sewing"));

            // Sewing recipes: wool
            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 4)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_roll_from_wool"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 1)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(ItemTags.WOOL_CARPETS)
                    .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                    .save(consumer, SewingKitMod.location("wool_roll_from_carpet"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 8)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_trim_from_wool"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 3)
                    .withTool(Tags.Items.TOOLS_SHEAR)
                    .addMaterial(ItemTags.WOOL_CARPETS)
                    .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                    .save(consumer, SewingKitMod.location("wool_trim_from_carpet"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_SHOES.get())
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 2)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_shoes_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_PANTS.get())
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 2)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 4)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_pants_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_SHIRT.get())
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 3)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 3)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_shirt_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_HAT.get())
                    .withTool(SewingKitMod.WOOD_OR_HIGHER)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 1)
                    .addMaterial(Tags.Items.STRINGS)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_hat_via_sewing"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SewingKitMod.FILE.get())
                    .pattern("  I")
                    .pattern(" I ")
                    .pattern("P  ")
                    .define('I', Tags.Items.INGOTS_IRON)
                    .define('P', ItemTags.PLANKS)
                    .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                    .save(consumer);
        }
    }

    private static class Loot
    {
        public static LootTableProvider create(PackOutput gen, CompletableFuture<HolderLookup.Provider> lookup)
        {
            return new LootTableProvider(gen, Set.of(), List.of(
                    new LootTableProvider.SubProviderEntry(Loot.BlockTables::new, LootContextParamSets.BLOCK),
                    new LootTableProvider.SubProviderEntry(Loot.ChestTables::new, LootContextParamSets.CHEST)
            ), lookup);
        }

        public static class BlockTables extends BlockLootSubProvider
        {
            protected BlockTables(HolderLookup.Provider provider)
            {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
            }

            @Override
            protected void generate()
            {
                this.dropSelf(SewingKitMod.SEWING_STATION_BLOCK.get());
                this.dropSelf(SewingKitMod.STORING_SEWING_STATION_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks()
            {
                return BuiltInRegistries.BLOCK.entrySet().stream()
                        .filter(e -> e.getKey().location().getNamespace().equals(SewingKitMod.MODID))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
            }
        }

        public static class ChestTables implements LootTableSubProvider
        {
            public ChestTables(HolderLookup.Provider provider)
            {
            }

            @Override
            public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer)
            {
                consumer.accept(ResourceKey.create(Registries.LOOT_TABLE, SewingKitMod.location("chest/tailor_shop_upper_floor")), LootTable.lootTable()
                        // armor
                        .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(0,2))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_HAT.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_SHIRT.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_PANTS.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_SHOES.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(Items.LEATHER_HELMET).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(Items.LEATHER_LEGGINGS).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(1).apply(RandomDye.builder()))
                        )
                        // needle
                        .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(0, 1))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOD_SEWING_NEEDLE.get()).setWeight(100))
                                .add(LootItem.lootTableItem(SewingKitMod.BONE_SEWING_NEEDLE.get()).setWeight(50))
                                .add(LootItem.lootTableItem(SewingKitMod.IRON_SEWING_NEEDLE.get()).setWeight(20))
                                .add(LootItem.lootTableItem(SewingKitMod.DIAMOND_SEWING_NEEDLE.get()).setWeight(5))
                                .add(LootItem.lootTableItem(SewingKitMod.NETHERITE_SEWING_NEEDLE.get()).setWeight(1))
                                .add(LootItem.lootTableItem(SewingKitMod.GOLD_SEWING_NEEDLE.get()).setWeight(1))
                        )
                        // materials
                        .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 5))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_ROLL.get()).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_TRIM.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(SewingKitMod.LEATHER_SHEET.get()).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(SewingKitMod.LEATHER_STRIP.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.STRING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        )
                );
            }
        }
    }

    private static class ItemTagGen extends IntrinsicHolderTagsProvider<Item>
    {
        public ItemTagGen(PackOutput packOutput, ExistingFileHelper existingFileHelper)
        {
            super(packOutput, Registries.ITEM, CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor()),
                    (item) -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow(), SewingKitMod.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup)
        {
            tag(ItemTags.DYEABLE)
                    .add(SewingKitMod.WOOL_HAT.get())
                    .add(SewingKitMod.WOOL_SHIRT.get())
                    .add(SewingKitMod.WOOL_PANTS.get())
                    .add(SewingKitMod.WOOL_SHOES.get());

            var list = List.of(
                    Pair.of(SewingKitMod.WOOD_OR_HIGHER, List.of(SewingKitMod.WOOD_SEWING_NEEDLE.get(), SewingKitMod.GOLD_SEWING_NEEDLE.get())),
                    Pair.of(SewingKitMod.BONE_OR_HIGHER, List.of(SewingKitMod.BONE_SEWING_NEEDLE.get(), SewingKitMod.STONE_SEWING_NEEDLE.get())),
                    Pair.of(SewingKitMod.IRON_OR_HIGHER, List.of(SewingKitMod.IRON_SEWING_NEEDLE.get())),
                    Pair.of(SewingKitMod.DIAMOND_OR_HIGHER, List.of(SewingKitMod.DIAMOND_SEWING_NEEDLE.get())),
                    Pair.of(SewingKitMod.NETHERITE_OR_HIGHER, List.of(SewingKitMod.NETHERITE_SEWING_NEEDLE.get()))
            );

            for(int i=0;i<list.size();i++)
            {
                var entry = list.get(i);
                var tag = entry.getFirst();
                var items = entry.getSecond();

                var tagBuilder = tag(tag);
                for(var item : items)
                    tagBuilder.add(item);

                int j = i+1;
                if (j < list.size())
                {
                    tagBuilder.addTag(list.get(j).getFirst());
                }
            }
        }
    }

    private static class BlockTagGen extends IntrinsicHolderTagsProvider<Block>
    {
        public BlockTagGen(PackOutput packOutput, ExistingFileHelper existingFileHelper)
        {
            super(packOutput, Registries.BLOCK, CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor()),
                    (block) -> BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow(), SewingKitMod.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup)
        {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .add(SewingKitMod.SEWING_STATION_BLOCK.get())
                    .add(SewingKitMod.STORING_SEWING_STATION_BLOCK.get());
        }
    }
}
