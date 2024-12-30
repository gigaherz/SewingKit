package dev.gigaherz.sewingkit;

import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.Util;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.Variant;
import net.minecraft.client.data.models.blockstates.VariantProperties;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.*;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SewingKitDataGen
{
    public static void gatherData(GatherDataEvent.Client event)
    {
        DataGenerator gen = event.getGenerator();

        gen.addProvider(true, new Lang(gen));
        gen.addProvider(true, new EquipmentAsses(gen.getPackOutput()));
        gen.addProvider(true, new ModelsAndClientItems(gen.getPackOutput()));

        gen.addProvider(true, new BlockTagGen(gen.getPackOutput()));
        gen.addProvider(true, new ItemTagGen(gen.getPackOutput()));
        gen.addProvider(true, new Recipes(gen.getPackOutput(), event.getLookupProvider()));
        gen.addProvider(true, Loot.create(gen.getPackOutput(), event.getLookupProvider()));
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

    private static class EquipmentAsses extends EquipmentAssetProvider
    {
        public EquipmentAsses(PackOutput output)
        {
            super(output);
        }

        @Override
        protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output)
        {
            ResourceLocation textureId = SewingKitMod.location("wool");
            output.accept(SewingKitMod.WOOL_ASSET, EquipmentClientInfo.builder()
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, whiteDefaultDyeable(textureId))
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID, whiteDefaultDyeable(textureId))
                .build());
        }

        public static EquipmentClientInfo.Layer whiteDefaultDyeable(ResourceLocation textureId) {
            return new EquipmentClientInfo.Layer(
                    textureId, Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-1))), false
            );
        }
    }

    private static class ModelsAndClientItems extends ModelProvider
    {
        public ModelsAndClientItems(PackOutput output)
        {
            super(output, SewingKitMod.MODID);
        }

        @Override
        protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels)
        {
            horizontalWithExistingModel(blockModels, SewingKitMod.SEWING_STATION_BLOCK.get());
            horizontalWithExistingModel(blockModels, SewingKitMod.STORING_SEWING_STATION_BLOCK.get());

            itemModels.createFlatItemModel(SewingKitMod.LEATHER_STRIP.get(), ModelTemplates.FLAT_ITEM);
            itemModels.createFlatItemModel(SewingKitMod.LEATHER_SHEET.get(), ModelTemplates.FLAT_ITEM);
            for (Needles needle : Needles.values())
            {
                itemModels.createFlatItemModel(needle.getNeedle(), ModelTemplates.FLAT_ITEM);
            }

            armorItem(itemModels, SewingKitMod.WOOL_HAT);
            armorItem(itemModels, SewingKitMod.WOOL_SHIRT);
            armorItem(itemModels, SewingKitMod.WOOL_PANTS);
            armorItem(itemModels, SewingKitMod.WOOL_SHOES);

            itemModels.createFlatItemModel(SewingKitMod.WOOL_ROLL.get(), ModelTemplates.FLAT_ITEM);
            itemModels.createFlatItemModel(SewingKitMod.WOOL_TRIM.get(), ModelTemplates.FLAT_ITEM);

            itemModels.createFlatItemModel(SewingKitMod.COMMON_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.createFlatItemModel(SewingKitMod.UNCOMMON_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.createFlatItemModel(SewingKitMod.RARE_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.createFlatItemModel(SewingKitMod.LEGENDARY_PATTERN.get(), ModelTemplates.FLAT_ITEM);

            itemModels.createFlatItemModel(SewingKitMod.FILE.get(), ModelTemplates.FLAT_ITEM.extend()
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, b -> b
                            .rotation(62, 180 - 33, 40).translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    )
                    .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, b -> b
                            .rotation(45, -33, -55).translation(-2.25f, 1.5f, -0.25f).scale(0.48f)
                    )
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, b -> b
                            .rotation(-54, 99, 136).translation(1.13f, 5f, 1.13f).scale(0.68f)
                    )
                    .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, b -> b
                            .rotation(136, -99, 54).translation(1.13f, 5f, 1.13f).scale(0.68f)
                    )
                    .transform(ItemDisplayContext.GROUND, b -> b
                            .translation(0, 2, 0).scale(0.5f)
                    )
                    .transform(ItemDisplayContext.HEAD, b -> b
                            .rotation(-4, 44, 4).translation(-7.25f, 6.75f, 0.75f)
                    )
                    .transform(ItemDisplayContext.FIXED, b -> b
                            .rotation(0, 180, 0)
                    )
                    .build());

            justClientItemPlease(itemModels, SewingKitMod.LEATHER_STRIP);
            justClientItemPlease(itemModels, SewingKitMod.LEATHER_SHEET);
            justClientItemPlease(itemModels, SewingKitMod.WOOL_ROLL);
            justClientItemPlease(itemModels, SewingKitMod.WOOL_TRIM);
            justClientItemPlease(itemModels, SewingKitMod.WOOD_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.STONE_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.BONE_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.GOLD_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.IRON_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.DIAMOND_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.NETHERITE_SEWING_NEEDLE);
            justClientItemPlease(itemModels, SewingKitMod.COMMON_PATTERN);
            justClientItemPlease(itemModels, SewingKitMod.UNCOMMON_PATTERN);
            justClientItemPlease(itemModels, SewingKitMod.RARE_PATTERN);
            justClientItemPlease(itemModels, SewingKitMod.LEGENDARY_PATTERN);
            justClientItemPlease(itemModels, SewingKitMod.FILE);
        }

        private void justClientItemPlease(ItemModelGenerators itemModels, DeferredItem<? extends Item> item)
        {
            itemModels.itemModelOutput.accept(item.get(),
                    ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item.get())));
        }

        private static void armorItem(ItemModelGenerators itemModels, DeferredItem<Item> item)
        {
            itemModels.createFlatItemModel(item.get(), ModelTemplates.FLAT_ITEM);
            itemModels.itemModelOutput.accept(item.get(),
                    ItemModelUtils.tintedModel(ModelLocationUtils.getModelLocation(item.get()), new Dye(-1)));
        }

        private static void horizontalWithExistingModel(BlockModelGenerators blockModels, Block block)
        {
            blockModels.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block, Variant.variant()
                            .with(VariantProperties.MODEL, ModelLocationUtils.getModelLocation(block)))
                            .with(BlockModelGenerators.createHorizontalFacingDispatch()));
        }
    }

    private static class Recipes extends RecipeProvider.Runner
    {
        public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput output)
        {
            return new VanillaRecipeProvider(lookup, output)
            {

                @Override
                protected void buildRecipes()
                {
                    var items = lookup.lookupOrThrow(Registries.ITEM);

                    shaped(RecipeCategory.MISC, SewingKitMod.FILE.get())
                            .pattern("  I")
                            .pattern(" I ")
                            .pattern("P  ")
                            .define('I', Tags.Items.INGOTS_IRON)
                            .define('P', ItemTags.PLANKS)
                            .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                            .save(output);

                    Arrays.stream(Needles.values()).forEach(needle -> shapeless(RecipeCategory.TOOLS, needle.getNeedle())
                            .requires(SewingKitMod.FILE.get())
                            .requires(needle.getToolMaterial().repairItems())
                            .unlockedBy("has_material", has(needle.getMaterial()))
                            .save(output));

                    shaped(RecipeCategory.MISC, SewingKitMod.SEWING_STATION_ITEM.get())
                            .pattern("xxx")
                            .pattern("P P")
                            .pattern("S S")
                            .define('x', ItemTags.WOODEN_SLABS)
                            .define('P', ItemTags.PLANKS)
                            .define('S', Items.STICK)
                            .unlockedBy("has_wood", has(ItemTags.PLANKS))
                            .save(output);

                    shapeless(RecipeCategory.MISC, SewingKitMod.STORING_SEWING_STATION_ITEM.get())
                            .requires(SewingKitMod.SEWING_STATION_ITEM.get())
                            .requires(Tags.Items.CHESTS_WOODEN)
                            .unlockedBy("has_station", has(SewingKitMod.SEWING_STATION_ITEM.get()))
                            .save(output);

                    // Sewing recipes: leather
                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 4)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(Tags.Items.LEATHERS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_sheet_from_leather"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 1)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(Items.RABBIT_HIDE)
                            .addCriterion("has_leather", has(Items.RABBIT_HIDE))
                            .save(output, SewingKitMod.location("leather_sheet_from_rabbit_hide"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.LEATHER_STRIP.get(), 3)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(Tags.Items.LEATHERS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_strip_from_leather"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_BOOTS)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_boots_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_LEGGINGS)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 4)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 3)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_leggings_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_CHESTPLATE)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 8)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 2)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_chestplate_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_HELMET)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_helmet_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
                            .withTool(SewingKitMod.NETHERITE_OR_HIGHER)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 12)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 6)
                            .addMaterial(Tags.Items.STRINGS, 8)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_horse_armor_via_sewing"));

                    // Sewing recipes: wool
                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 4)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(ItemTags.WOOL)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_roll_from_wool"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 1)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(ItemTags.WOOL_CARPETS)
                            .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                            .save(output, SewingKitMod.location("wool_roll_from_carpet"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 8)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(ItemTags.WOOL)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_trim_from_wool"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 3)
                            .withTool(Tags.Items.TOOLS_SHEAR)
                            .addMaterial(ItemTags.WOOL_CARPETS)
                            .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                            .save(output, SewingKitMod.location("wool_trim_from_carpet"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_SHOES.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 2)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_shoes_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_PANTS.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 2)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 4)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_pants_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_SHIRT.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 3)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 3)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_shirt_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_HAT.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 1)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_hat_via_sewing"));
                }
            };
        }

        @Override
        public String getName()
        {
            return "Recipes";
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
        public ItemTagGen(PackOutput packOutput)
        {
            super(packOutput, Registries.ITEM, CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor()),
                    (item) -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow(), SewingKitMod.MODID);
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
        public BlockTagGen(PackOutput packOutput)
        {
            super(packOutput, Registries.BLOCK, CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor()),
                    (block) -> BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow(), SewingKitMod.MODID);
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
