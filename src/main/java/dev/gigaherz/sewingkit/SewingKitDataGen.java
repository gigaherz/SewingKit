package dev.gigaherz.sewingkit;

import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.tools.MatchBlock;
import dev.gigaherz.sewingkit.tools.ConvertDrops;
import dev.gigaherz.sewingkit.tools.StickWebHandler;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.*;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.DiscardItem;
import net.minecraft.world.level.storage.loot.functions.FilteredFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetRandomDyesFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.Sum;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SewingKitDataGen
{

    public static final int SILK_DEFAULT_COLOR = 0xFFE3AEAA;

    public static void gatherData(GatherDataEvent.Client event)
    {
        DataGenerator gen = event.getGenerator();

        gen.addProvider(true, new Lang(gen));
        gen.addProvider(true, new EquipmentAssets(gen.getPackOutput()));
        gen.addProvider(true, new ModelsAndClientItems(gen.getPackOutput()));

        gen.addProvider(true, new BlockTagGen(gen.getPackOutput(), event.getLookupProvider()));
        gen.addProvider(true, new ItemTagGen(gen.getPackOutput(), event.getLookupProvider()));
        gen.addProvider(true, new Recipes(gen.getPackOutput(), event.getLookupProvider()));
        gen.addProvider(true, Loot.create(gen.getPackOutput(), event.getLookupProvider()));

        var reg = new RegistryProvider(gen.getPackOutput(), event.getLookupProvider());
        gen.addProvider(true, reg);
        gen.addProvider(true, new VillagerTradeTagGen(gen.getPackOutput(), reg.getRegistryProvider()));

        gen.addProvider(true, new LootModifiers(gen.getPackOutput(), reg.getRegistryProvider()));

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

            add(SewingKitMod.THREAD_ON_A_STICK.get(), "Thread on a Stick");
            add(SewingKitMod.SILK_CLOTH.get(), "Silk Cloth");
            add(SewingKitMod.SILK_CAP.get(), "Silk Cap");
            add(SewingKitMod.SILK_SHIRT.get(), "Silk Shirt");
            add(SewingKitMod.SILK_PANTS.get(), "Silk Pants");
            add(SewingKitMod.SILK_SOCKS.get(), "Silk Socks");

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

    private static class EquipmentAssets extends EquipmentAssetProvider
    {
        public EquipmentAssets(PackOutput output)
        {
            super(output);
        }

        @Override
        protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output)
        {
            Identifier woolTex = SewingKitMod.location("wool");
            output.accept(SewingKitMod.WOOL_ASSET, EquipmentClientInfo.builder()
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID, dyeable(woolTex, -1))
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, dyeable(woolTex ,-1))
                    .build());

            Identifier silkTex = SewingKitMod.location("silk");
            //Identifier silkTexOverlay = SewingKitMod.location("silk_overlay");
            output.accept(SewingKitMod.SILK_ASSET, EquipmentClientInfo.builder()
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID, dyeable(silkTex, SILK_DEFAULT_COLOR) /*, overlay(silkTexOverlay)*/)
                    .addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, dyeable(silkTex, SILK_DEFAULT_COLOR) /*, overlay(silkTexOverlay)*/)
                    .build());
        }

        public static EquipmentClientInfo.Layer dyeable(Identifier textureId, int defaultColor)
        {
            return new EquipmentClientInfo.Layer(
                    textureId, Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(defaultColor))), false
            );
        }

        public static EquipmentClientInfo.Layer overlay(Identifier textureId)
        {
            return new EquipmentClientInfo.Layer(
                    textureId, Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())), false
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

            itemModels.generateFlatItem(SewingKitMod.THREAD_ON_A_STICK.get(), ModelTemplates.FLAT_ITEM);

            itemModels.generateFlatItem(SewingKitMod.LEATHER_STRIP.get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(SewingKitMod.LEATHER_SHEET.get(), ModelTemplates.FLAT_ITEM);
            for (Needles needle : Needles.values())
            {
                itemModels.generateFlatItem(needle.getNeedle(), ModelTemplates.FLAT_ITEM);
            }

            dyedItem(itemModels, SewingKitMod.WOOL_HAT, -1);
            dyedItem(itemModels, SewingKitMod.WOOL_SHIRT, -1);
            dyedItem(itemModels, SewingKitMod.WOOL_PANTS, -1);
            dyedItem(itemModels, SewingKitMod.WOOL_SHOES, -1);

            Item silk = SewingKitMod.SILK_CLOTH.get();
            itemModels.createFlatItemModel(silk, ModelTemplates.FLAT_ITEM);
            itemModels.itemModelOutput.accept(silk, ItemModelUtils.tintedModel(ModelLocationUtils.getModelLocation(silk), new Constant(SILK_DEFAULT_COLOR)));
            dyedItem(itemModels, SewingKitMod.SILK_CAP, SILK_DEFAULT_COLOR);
            dyedItem(itemModels, SewingKitMod.SILK_SHIRT, SILK_DEFAULT_COLOR);
            dyedItem(itemModels, SewingKitMod.SILK_PANTS, SILK_DEFAULT_COLOR);
            dyedItem(itemModels, SewingKitMod.SILK_SOCKS, SILK_DEFAULT_COLOR);

            itemModels.generateFlatItem(SewingKitMod.WOOL_ROLL.get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(SewingKitMod.WOOL_TRIM.get(), ModelTemplates.FLAT_ITEM);

            itemModels.generateFlatItem(SewingKitMod.COMMON_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(SewingKitMod.UNCOMMON_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(SewingKitMod.RARE_PATTERN.get(), ModelTemplates.FLAT_ITEM);
            itemModels.generateFlatItem(SewingKitMod.LEGENDARY_PATTERN.get(), ModelTemplates.FLAT_ITEM);

            itemModels.generateFlatItem(SewingKitMod.FILE.get(), ModelTemplates.FLAT_ITEM.extend()
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
        }

        private static void dyedItem(ItemModelGenerators itemModels, DeferredItem<Item> item, int defaultColor)
        {
            itemModels.createFlatItemModel(item.get(), ModelTemplates.FLAT_ITEM);
            itemModels.itemModelOutput.accept(item.get(),
                    ItemModelUtils.tintedModel(ModelLocationUtils.getModelLocation(item.get()), new Dye(defaultColor)));
        }

        private static void horizontalWithExistingModel(BlockModelGenerators blockModels, Block block)
        {
            Identifier modelLocation = ModelLocationUtils.getModelLocation(block);
            var variant = new Variant(modelLocation);
            var multiVariant = new MultiVariant(WeightedList.of(variant));
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, multiVariant).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
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
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_boots_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_LEGGINGS)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 4)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 3)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_leggings_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_CHESTPLATE)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 8)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 2)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_chestplate_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_HELMET)
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                            .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_leather", has(Tags.Items.LEATHERS))
                            .save(output, SewingKitMod.location("leather_helmet_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
                            .withTool(SewingKitMod.NETHERITE_OR_HIGHER_NEEDLE)
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
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 2)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_shoes_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_PANTS.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 2)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 4)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_pants_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_SHIRT.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 3)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 3)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_shirt_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.WOOL_HAT.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                            .addMaterial(SewingKitMod.WOOL_TRIM.get(), 1)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(ItemTags.WOOL))
                            .save(output, SewingKitMod.location("wool_hat_via_sewing"));

                    // Silk stuffs

                    shaped(RecipeCategory.MISC, SewingKitMod.SILK_CLOTH.get())
                            .pattern("ss")
                            .pattern("ss")
                            .define('s', SewingKitMod.THREAD_ON_A_STICK)
                            .unlockedBy("has_silk", has(SewingKitMod.THREAD_ON_A_STICK))
                            .save(output);

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.SILK_SOCKS.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.SILK_CLOTH, 3)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(SewingKitMod.SILKS))
                            .save(output, SewingKitMod.location("silk_socks_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.SILK_PANTS.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.SILK_CLOTH, 4)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(SewingKitMod.SILKS))
                            .save(output, SewingKitMod.location("silk_pants_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.SILK_SHIRT.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.SILK_CLOTH, 5)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(SewingKitMod.SILKS))
                            .save(output, SewingKitMod.location("silk_shirt_via_sewing"));

                    SewingRecipeBuilder.begin(items, RecipeCategory.MISC, SewingKitMod.SILK_CAP.get())
                            .withTool(SewingKitMod.WOOD_OR_HIGHER_NEEDLE)
                            .addMaterial(SewingKitMod.SILK_CLOTH, 2)
                            .addMaterial(Tags.Items.STRINGS)
                            .addCriterion("has_wool", has(SewingKitMod.SILKS))
                            .save(output, SewingKitMod.location("silk_cap_via_sewing"));


                    // Dyeables
                    dyedItem(SewingKitMod.WOOL_HAT.get(), "dyed_armor");
                    dyedItem(SewingKitMod.WOOL_SHIRT.get(), "dyed_armor");
                    dyedItem(SewingKitMod.WOOL_PANTS.get(), "dyed_armor");
                    dyedItem(SewingKitMod.WOOL_SHOES.get(), "dyed_armor");

                    // Dyeables
                    dyedItem(SewingKitMod.SILK_CAP.get(), "dyed_armor");
                    dyedItem(SewingKitMod.SILK_SHIRT.get(), "dyed_armor");
                    dyedItem(SewingKitMod.SILK_PANTS.get(), "dyed_armor");
                    dyedItem(SewingKitMod.SILK_SOCKS.get(), "dyed_armor");
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
                        .filter(e -> e.getKey().identifier().getNamespace().equals(SewingKitMod.MODID))
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
                        .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(0, 2))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_HAT.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_SHIRT.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_PANTS.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.WOOL_SHOES.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.SILK_CAP.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.SILK_SHIRT.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.SILK_PANTS.get()).setWeight(1).apply(RandomDye.builder()))
                                .add(LootItem.lootTableItem(SewingKitMod.SILK_SOCKS.get()).setWeight(1).apply(RandomDye.builder()))
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
                                .add(LootItem.lootTableItem(SewingKitMod.SILK_CLOTH.get()).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(SewingKitMod.LEATHER_SHEET.get()).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(SewingKitMod.LEATHER_STRIP.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.STRING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        )
                        // patterns (TODO)
                );
            }
        }
    }

    private static class ItemTagGen extends TagsProvider<Item>
    {
        public ItemTagGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(packOutput, Registries.ITEM, lookupProvider, SewingKitMod.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup)
        {
            tag(SewingKitMod.REPAIRS_WOOL_CLOTHES)
                    .addTag(ItemTags.WOOL);

            tag(SewingKitMod.REPAIRS_SILK_CLOTHES)
                    .addTag(SewingKitMod.SILKS);

            tag(SewingKitMod.SILKS)
                    .add(SewingKitMod.SILK_CLOTH.getKey());

            tag(ItemTags.FREEZE_IMMUNE_WEARABLES)
                    .add(SewingKitMod.WOOL_HAT.getKey())
                    .add(SewingKitMod.WOOL_SHIRT.getKey())
                    .add(SewingKitMod.WOOL_PANTS.getKey())
                    .add(SewingKitMod.WOOL_SHOES.getKey());

            var list = List.of(
                    Pair.of(SewingKitMod.WOOD_OR_HIGHER_NEEDLE, List.of(SewingKitMod.WOOD_SEWING_NEEDLE.getKey(), SewingKitMod.GOLD_SEWING_NEEDLE.getKey())),
                    Pair.of(SewingKitMod.BONE_OR_HIGHER_NEEDLE, List.of(SewingKitMod.BONE_SEWING_NEEDLE.getKey(), SewingKitMod.STONE_SEWING_NEEDLE.getKey())),
                    Pair.of(SewingKitMod.IRON_OR_HIGHER_NEEDLE, List.of(SewingKitMod.IRON_SEWING_NEEDLE.getKey())),
                    Pair.of(SewingKitMod.DIAMOND_OR_HIGHER_NEEDLE, List.of(SewingKitMod.DIAMOND_SEWING_NEEDLE.getKey())),
                    Pair.of(SewingKitMod.NETHERITE_OR_HIGHER_NEEDLE, List.of(SewingKitMod.NETHERITE_SEWING_NEEDLE.getKey()))
            );
            for (int i = 0; i < list.size(); i++)
            {
                var entry = list.get(i);
                var tag = entry.getFirst();
                var items = entry.getSecond();

                var tagBuilder = tag(tag);
                for (var item : items)
                    tagBuilder.add(item);

                int j = i + 1;
                if (j < list.size())
                {
                    tagBuilder.addTag(list.get(j).getFirst());
                }
            }


            tag(StickWebHandler.C_WOODEN_STICKS)
                    .add(ItemIds.STICK);

            tag(StickWebHandler.PRODUCES_THREAD)
                    .addTag(StickWebHandler.C_WOODEN_STICKS);

            tag(StickWebHandler.CONVERTS_TO_THREAD)
                    .addTag(Tags.Items.STRINGS);
        }
    }

    private static class BlockTagGen extends TagsProvider<Block>
    {
        public BlockTagGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(packOutput, Registries.BLOCK, lookupProvider, SewingKitMod.MODID);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup)
        {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .add(SewingKitMod.SEWING_STATION_BLOCK.getKey())
                    .add(SewingKitMod.STORING_SEWING_STATION_BLOCK.getKey());

            tag(StickWebHandler.C_COBWEBS)
                    .add(BlockItemIds.COBWEB.block());

            tag(StickWebHandler.PROVIDES_THREAD)
                    .addTag(StickWebHandler.C_COBWEBS);
        }
    }

    private static class VillagerTradeTagGen extends TagsProvider<VillagerTrade>
    {
        public VillagerTradeTagGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(packOutput, Registries.VILLAGER_TRADE, lookupProvider, SewingKitMod.MODID);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void addTags(HolderLookup.Provider lookup)
        {
            // Level 1
            var level1 = tag(RegistryProvider.TAILOR_LEVEL_1);
            level1.add(
                    RegistryProvider.EMERALD_WOOL_PANTS,
                    RegistryProvider.EMERALD_WOOL_HAT,
                    RegistryProvider.STRING_EMERALD);
            for(var value : RegistryProvider.DYE_EMERALD_LIST.values())
            {
                level1.add(value);
            }
            // Level 2
            tag(RegistryProvider.TAILOR_LEVEL_2).add(
                    RegistryProvider.EMERALD_WOOL_SHOES,
                    RegistryProvider.EMERALD_WOOL_SHIRT,
                    RegistryProvider.LEATHER_STRIP_EMERALD,
                    RegistryProvider.EMERALD_COMMON_PATTERN);
            // Level 3
            tag(RegistryProvider.TAILOR_LEVEL_3).add(
                    RegistryProvider.EMERALD_SILK_SHIRT,
                    RegistryProvider.EMERALD_SILK_SOCKS,
                    RegistryProvider.EMERALD_UNCOMMON_PATTERN,
                    RegistryProvider.WOOL_TRIM_EMERALD,
                    RegistryProvider.WOOL_ROLL_EMERALD);
            // Level 4
            tag(RegistryProvider.TAILOR_LEVEL_4).add(
                    RegistryProvider.EMERALD_SILK_PANTS,
                    RegistryProvider.EMERALD_SILK_CAP,
                    RegistryProvider.EMERALD_RARE_PATTERN
            );
            // Level 5
            tag(RegistryProvider.TAILOR_LEVEL_5).add(
                    RegistryProvider.SILK_CLOTH_EMERALD,
                    RegistryProvider.EMERALD_LEGENDARY_PATTERN
            );
        }
    }


    public static class RegistryProvider extends DatapackBuiltinEntriesProvider
    {
        public RegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(output, lookupProvider, BUILDER, Set.of(SewingKitMod.MODID));
        }

        @Override
        public String getName()
        {
            return "Datapack registries";
        }

        // Tags
        public static final TagKey<VillagerTrade> TAILOR_LEVEL_1 = tag(Registries.VILLAGER_TRADE, "trades_level_1");
        public static final TagKey<VillagerTrade> TAILOR_LEVEL_2 = tag(Registries.VILLAGER_TRADE, "trades_level_2");
        public static final TagKey<VillagerTrade> TAILOR_LEVEL_3 = tag(Registries.VILLAGER_TRADE, "trades_level_3");
        public static final TagKey<VillagerTrade> TAILOR_LEVEL_4 = tag(Registries.VILLAGER_TRADE, "trades_level_4");
        public static final TagKey<VillagerTrade> TAILOR_LEVEL_5 = tag(Registries.VILLAGER_TRADE, "trades_level_5");

        // Level 1
        public static final ResourceKey<VillagerTrade> EMERALD_WOOL_PANTS = key(Registries.VILLAGER_TRADE, "tailor/1/emerald_wool_pants");
        public static final ResourceKey<VillagerTrade> EMERALD_WOOL_HAT = key(Registries.VILLAGER_TRADE, "tailor/1/emerald_wool_hat");
        public static final ResourceKey<VillagerTrade> STRING_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/1/string_emerald");
        public static final Map<Item, ResourceKey<VillagerTrade>> DYE_EMERALD_LIST = Util.make(new HashMap<>(), map -> {
            map.put(Items.DYE.white(), key(Registries.VILLAGER_TRADE, "trades/1/white_dye_emerald"));
            map.put(Items.DYE.orange(), key(Registries.VILLAGER_TRADE, "trades/1/orange_dye_emerald"));
            map.put(Items.DYE.magenta(), key(Registries.VILLAGER_TRADE, "trades/1/magenta_dye_emerald"));
            map.put(Items.DYE.lightBlue(), key(Registries.VILLAGER_TRADE, "trades/1/light_blue_dye_emerald"));
            map.put(Items.DYE.yellow(), key(Registries.VILLAGER_TRADE, "trades/1/yellow_dye_emerald"));
            map.put(Items.DYE.lime(), key(Registries.VILLAGER_TRADE, "trades/1/lime_dye_emerald"));
            map.put(Items.DYE.pink(), key(Registries.VILLAGER_TRADE, "trades/1/pink_dye_emerald"));
            map.put(Items.DYE.gray(), key(Registries.VILLAGER_TRADE, "trades/1/gray_dye_emerald"));
            map.put(Items.DYE.lightGray(), key(Registries.VILLAGER_TRADE, "trades/1/light_gray_dye_emerald"));
            map.put(Items.DYE.cyan(), key(Registries.VILLAGER_TRADE, "trades/1/cyan_dye_emerald"));
            map.put(Items.DYE.purple(), key(Registries.VILLAGER_TRADE, "trades/1/purple_dye_emerald"));
            map.put(Items.DYE.blue(), key(Registries.VILLAGER_TRADE, "trades/1/blue_dye_emerald"));
            map.put(Items.DYE.brown(), key(Registries.VILLAGER_TRADE, "trades/1/brown_dye_emerald"));
            map.put(Items.DYE.green(), key(Registries.VILLAGER_TRADE, "trades/1/green_dye_emerald"));
            map.put(Items.DYE.red(), key(Registries.VILLAGER_TRADE, "trades/1/red_dye_emerald"));
            map.put(Items.DYE.black(), key(Registries.VILLAGER_TRADE, "trades/1/black_dye_emerald"));
        });

        // Level 2
        public static final ResourceKey<VillagerTrade> EMERALD_WOOL_SHOES = key(Registries.VILLAGER_TRADE, "tailor/2/emerald_wool_shoes");
        public static final ResourceKey<VillagerTrade> EMERALD_WOOL_SHIRT = key(Registries.VILLAGER_TRADE, "tailor/2/emerald_wool_shirt");
        public static final ResourceKey<VillagerTrade> LEATHER_STRIP_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/2/leather_strip_emerald");
        public static final ResourceKey<VillagerTrade> LEATHER_SHEET_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/2/leather_sheet_emerald");
        public static final ResourceKey<VillagerTrade> EMERALD_COMMON_PATTERN = key(Registries.VILLAGER_TRADE, "tailor/2/emerald_common_pattern");

        // Level 3
        public static final ResourceKey<VillagerTrade> EMERALD_UNCOMMON_PATTERN = key(Registries.VILLAGER_TRADE, "tailor/3/emerald_uncommon_pattern");
        public static final ResourceKey<VillagerTrade> WOOL_TRIM_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/3/wool_trim_emerald");
        public static final ResourceKey<VillagerTrade> WOOL_ROLL_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/3/wool_roll_emerald");
        public static final ResourceKey<VillagerTrade> EMERALD_SILK_SOCKS = key(Registries.VILLAGER_TRADE, "tailor/3/emerald_silk_socks");
        public static final ResourceKey<VillagerTrade> EMERALD_SILK_SHIRT = key(Registries.VILLAGER_TRADE, "tailor/3/emerald_silk_shirt");

        // Level 4
        public static final ResourceKey<VillagerTrade> EMERALD_RARE_PATTERN = key(Registries.VILLAGER_TRADE, "tailor/4/emerald_rare_pattern");
        public static final ResourceKey<VillagerTrade> EMERALD_SILK_PANTS = key(Registries.VILLAGER_TRADE, "tailor/4/emerald_silk_pants");
        public static final ResourceKey<VillagerTrade> EMERALD_SILK_CAP = key(Registries.VILLAGER_TRADE, "tailor/4/emerald_silk_cap");

        // Level 5
        public static final ResourceKey<VillagerTrade> EMERALD_LEGENDARY_PATTERN = key(Registries.VILLAGER_TRADE, "tailor/5/emerald_legendary_pattern");
        public static final ResourceKey<VillagerTrade> SILK_CLOTH_EMERALD = key(Registries.VILLAGER_TRADE, "tailor/5/silk_cloth_emerald");

        private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.TRADE_SET, context -> {
                    context.register(SewingKitMod.TAILOR_LEVEL_1, new TradeSet(holderSet(context, TAILOR_LEVEL_1), ConstantValue.exactly(2), false, Optional.empty()));
                    context.register(SewingKitMod.TAILOR_LEVEL_2, new TradeSet(holderSet(context, TAILOR_LEVEL_2), ConstantValue.exactly(2), false, Optional.empty()));
                    context.register(SewingKitMod.TAILOR_LEVEL_3, new TradeSet(holderSet(context, TAILOR_LEVEL_3), ConstantValue.exactly(2), false, Optional.empty()));
                    context.register(SewingKitMod.TAILOR_LEVEL_4, new TradeSet(holderSet(context, TAILOR_LEVEL_4), ConstantValue.exactly(2), false, Optional.empty()));
                    context.register(SewingKitMod.TAILOR_LEVEL_5, new TradeSet(holderSet(context, TAILOR_LEVEL_5), ConstantValue.exactly(2), false, Optional.empty()));
                })
                .add(Registries.VILLAGER_TRADE, context -> {
                    HolderGetter<Item> items = context.lookup(Registries.ITEM);
                    // Level 1
                    context.register(EMERALD_WOOL_PANTS, makeDyedItemTrade(items, SewingKitMod.WOOL_PANTS, 3));
                    context.register(EMERALD_WOOL_HAT, makeDyedItemTrade(items, SewingKitMod.WOOL_HAT, 3));
                    context.register(STRING_EMERALD, buyItem(Items.STRING, 16));
                    for(var entry : DYE_EMERALD_LIST.entrySet())
                    {
                        context.register(entry.getValue(), new VillagerTrade(
                                new TradeCost(entry.getKey(), UniformGenerator.between(5,9)), new ItemStackTemplate(Items.EMERALD),
                                16, 2, 0.05f, Optional.empty(), List.of()));
                    }
                    // Level 2
                    context.register(EMERALD_WOOL_SHOES, makeDyedItemTrade(items, SewingKitMod.WOOL_SHOES, 3));
                    context.register(EMERALD_WOOL_SHIRT, makeDyedItemTrade(items, SewingKitMod.WOOL_SHIRT, 5));
                    context.register(EMERALD_COMMON_PATTERN, sellItem(SewingKitMod.COMMON_PATTERN, 5));
                    context.register(LEATHER_STRIP_EMERALD, buyItem(SewingKitMod.LEATHER_STRIP, 2));
                    context.register(LEATHER_SHEET_EMERALD, buyItem(SewingKitMod.LEATHER_SHEET, 2));
                    // Level 3
                    context.register(EMERALD_UNCOMMON_PATTERN, sellItem(SewingKitMod.UNCOMMON_PATTERN, 9));
                    context.register(WOOL_TRIM_EMERALD, sellItem(SewingKitMod.WOOL_TRIM, 8));
                    context.register(WOOL_ROLL_EMERALD, sellItem(SewingKitMod.WOOL_ROLL, 4));
                    context.register(EMERALD_SILK_SOCKS, makeDyedItemTrade(items, SewingKitMod.SILK_SOCKS, 3));
                    context.register(EMERALD_SILK_SHIRT, makeDyedItemTrade(items, SewingKitMod.SILK_SHIRT, 5));
                    // Level 4
                    context.register(EMERALD_RARE_PATTERN, sellItem(SewingKitMod.RARE_PATTERN, 9));
                    context.register(EMERALD_SILK_PANTS, makeDyedItemTrade(items, SewingKitMod.SILK_PANTS, 4));
                    context.register(EMERALD_SILK_CAP, makeDyedItemTrade(items, SewingKitMod.SILK_CAP, 3));
                    // Level 5
                    context.register(EMERALD_LEGENDARY_PATTERN, sellItem(SewingKitMod.UNCOMMON_PATTERN, 9));
                    context.register(SILK_CLOTH_EMERALD, sellItem(SewingKitMod.SILK_CLOTH, 3));
                });

        private static VillagerTrade buyItem(ItemLike what, int count)
        {
            return new VillagerTrade(
                    new TradeCost(what, ConstantValue.exactly(count)), new ItemStackTemplate(Items.EMERALD),
                    16, 2, 0.05f, Optional.empty(), List.of());
        }

        private static VillagerTrade sellItem(ItemLike what, int count)
        {
            return new VillagerTrade(
                    new TradeCost(Items.EMERALD, ConstantValue.exactly(count)), new ItemStackTemplate(what.asItem()),
                    16, 2, 0.05f, Optional.empty(), List.of());
        }

        private static VillagerTrade makeDyedItemTrade(HolderGetter<Item> items, DeferredItem<Item> theItem, int price)
        {
            return new VillagerTrade(
                    new TradeCost(Items.EMERALD, new ConstantValue(price)), new ItemStackTemplate(theItem),
                    12, 1, 0.05f, Optional.empty(),
                    List.of(
                            SetRandomDyesFunction.withCount(
                                    Sum.sum(ConstantValue.exactly(1),
                                            BinomialDistributionGenerator.binomial(2, 0.75f))).build(),
                            FilteredFunction.filtered(new ItemPredicate.Builder().of(items, theItem.get())
                                            .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.DYED_COLOR).build()).build())
                                    .onFail(Optional.of(DiscardItem.discardItem().build())).build()
                    ));
        }

        @SuppressWarnings("SameParameterValue")
        private static <T> HolderSet<T> holderSet(BootstrapContext<TradeSet> context, TagKey<T> tagKey) {
            return context.lookup(tagKey.registry()).get(tagKey).orElseThrow();
        }

        @SuppressWarnings("SameParameterValue")
        private static <T> TagKey<T> tag(ResourceKey<? extends Registry<T>> reg, String tagName) {
            return TagKey.create(reg, SewingKitMod.location(tagName));
        }

        @SuppressWarnings("SameParameterValue")
        private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> reg, String keyName) {
            return ResourceKey.create(reg, SewingKitMod.location(keyName));
        }
    }

    public static class LootModifiers extends GlobalLootModifierProvider
    {
        // Get the parameters from the `GatherDataEvent`s.
        public LootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, SewingKitMod.MODID);
        }

        @Override
        protected void start() {

            var blockLookup = this.registries.lookupOrThrow(Registries.BLOCK);
            var itemLookup = this.registries.lookupOrThrow(Registries.ITEM);

            this.add("replace_string_with_thread",
                    new ConvertDrops(
                            new LootItemCondition[]{
                                    new MatchBlock(blockLookup.getOrThrow(StickWebHandler.PROVIDES_THREAD)),
                                    MatchTool.toolMatches(ItemPredicate.Builder.item().of(itemLookup, StickWebHandler.PRODUCES_THREAD)).build()
                            },
                            IGlobalLootModifier.DEFAULT_PRIORITY,
                            ItemPredicate.Builder.item().of(itemLookup, StickWebHandler.CONVERTS_TO_THREAD).build(),
                            new ItemStackTemplate(SewingKitMod.THREAD_ON_A_STICK)
                    )
            );
        }
    }
}
