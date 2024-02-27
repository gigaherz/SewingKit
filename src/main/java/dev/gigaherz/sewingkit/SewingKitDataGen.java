package dev.gigaherz.sewingkit;

import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

        gen.addProvider(event.includeServer(), new BlockTags(gen.getPackOutput(), event.getExistingFileHelper()));
        //gen.addProvider(new ItemTags(gen, blockTags));
        gen.addProvider(event.includeServer(), new Recipes(gen.getPackOutput()));
        gen.addProvider(event.includeServer(), Loot.create(gen.getPackOutput()));
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
            add("jei.category.sewingkit.sewing", "Sewing");

            add(SewingKitMod.LEATHER_STRIP.get(), "Leather Strip");
            add(SewingKitMod.LEATHER_SHEET.get(), "Leather Sheet");

            add(SewingKitMod.SEWING_STATION_BLOCK.get(), "Sewing Table");

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
        public Recipes(PackOutput gen)
        {
            super(gen);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer)
        {
            Arrays.stream(Needles.values()).forEach(needle -> ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, needle.getNeedle())
                    .requires(SewingKitMod.FILE.get())
                    .requires(needle.getTier().getRepairIngredient())
                    .save(consumer));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SewingKitMod.SEWING_STATION_ITEM.get())
                    .pattern("xxx")
                    .pattern("P P")
                    .pattern("S S")
                    .define('x', ItemTags.WOODEN_SLABS)
                    .define('P', ItemTags.PLANKS)
                    .define('S', Items.STICK)
                    .save(consumer);

            // Sewing recipes: leather
            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 4)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Tags.Items.LEATHER)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_sheet_from_leather"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_SHEET.get(), 1)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Items.RABBIT_HIDE)
                    .addCriterion("has_leather", has(Items.RABBIT_HIDE))
                    .save(consumer, SewingKitMod.location("leather_sheet_from_rabbit_hide"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.LEATHER_STRIP.get(), 3)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(Tags.Items.LEATHER)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_strip_from_leather"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_BOOTS)
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_boots_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_LEGGINGS)
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 4)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 3)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_leggings_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_CHESTPLATE)
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 8)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 2)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_chestplate_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_HELMET)
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 2)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get())
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_helmet_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
                    .withTool(NeedleItem.SEW, Tiers.NETHERITE)
                    .addMaterial(SewingKitMod.LEATHER_SHEET.get(), 12)
                    .addMaterial(SewingKitMod.LEATHER_STRIP.get(), 6)
                    .addMaterial(Tags.Items.STRING, 8)
                    .addCriterion("has_leather", has(Tags.Items.LEATHER))
                    .save(consumer, SewingKitMod.location("leather_horse_armor_via_sewing"));

            // Sewing recipes: wool
            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 4)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_roll_from_wool"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_ROLL.get(), 1)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL_CARPETS)
                    .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                    .save(consumer, SewingKitMod.location("wool_roll_from_carpet"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 8)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_trim_from_wool"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_TRIM.get(), 3)
                    .withTool(Tags.Items.SHEARS)
                    .addMaterial(ItemTags.WOOL_CARPETS)
                    .addCriterion("has_wool", has(ItemTags.WOOL_CARPETS))
                    .save(consumer, SewingKitMod.location("wool_trim_from_carpet"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_SHOES.get())
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 2)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_shoes_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_PANTS.get())
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 2)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 4)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_pants_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_SHIRT.get())
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 3)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 3)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_shirt_via_sewing"));

            SewingRecipeBuilder.begin(RecipeCategory.MISC, SewingKitMod.WOOL_HAT.get())
                    .withTool(NeedleItem.SEW)
                    .addMaterial(SewingKitMod.WOOL_ROLL.get(), 1)
                    .addMaterial(SewingKitMod.WOOL_TRIM.get(), 1)
                    .addMaterial(Tags.Items.STRING)
                    .addCriterion("has_wool", has(ItemTags.WOOL))
                    .save(consumer, SewingKitMod.location("wool_hat_via_sewing"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SewingKitMod.FILE.get())
                    .pattern("  I")
                    .pattern(" I ")
                    .pattern("P  ")
                    .define('I', Tags.Items.INGOTS_IRON)
                    .define('P', ItemTags.PLANKS)
                    .save(consumer);
        }
    }

    private static class Loot
    {
        public static LootTableProvider create(PackOutput gen)
        {
            return new LootTableProvider(gen, Set.of(), List.of(
                    new LootTableProvider.SubProviderEntry(Loot.BlockTables::new, LootContextParamSets.BLOCK),
                    new LootTableProvider.SubProviderEntry(Loot.ChestTables::new, LootContextParamSets.CHEST)
            ));
        }

        public static class BlockTables extends BlockLootSubProvider
        {
            protected BlockTables()
            {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags());
            }

            @Override
            protected void generate()
            {
                this.dropSelf(SewingKitMod.SEWING_STATION_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks()
            {
                return ForgeRegistries.BLOCKS.getEntries().stream()
                        .filter(e -> e.getKey().location().getNamespace().equals(SewingKitMod.MODID))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
            }
        }

        public static class ChestTables implements LootTableSubProvider
        {
            @Override
            public void generate(BiConsumer<ResourceLocation, LootTable.Builder> consumer)
            {
                consumer.accept(SewingKitMod.location("chest/tailor_shop_chest"), LootTable.lootTable()
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

    private static class BlockTags extends IntrinsicHolderTagsProvider<Block>
    {
        public BlockTags(PackOutput packOutput, ExistingFileHelper existingFileHelper)
        {
            super(packOutput, Registries.BLOCK, CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor()),
                    (p_255627_) -> p_255627_.builtInRegistryHolder().key(), SewingKitMod.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider p_255662_)
        {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .add(SewingKitMod.SEWING_STATION_BLOCK.get());
        }
    }
}
