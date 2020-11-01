package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.api.ToolIngredient;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.block.Block;
import net.minecraft.data.*;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

            Arrays.stream(Needles.values()).forEach(needle -> {
                String type = needle.getType();
                String name = type.substring(0, 1).toUpperCase() + type.substring(1);
                add(needle.getNeedle(), name + " Sewing Needle");
            });

            add(SewingKitMod.WOOL_HAT.get(), "Wool Hat");
            add(SewingKitMod.WOOL_SHIRT.get(), "Wool Shirt");
            add(SewingKitMod.WOOL_PANTS.get(), "Wool Pants");
            add(SewingKitMod.WOOL_SHOES.get(), "Wool Shoes");

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
            Block block = SewingKitMod.SEWING_STATION_BLOCK.get();
            horizontalBlock(block, models().getExistingFile(ModelsResourceUtil.func_240221_a_(block)));
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

            basicIcon(SewingKitMod.COMMON_PATTERN.getId());
            basicIcon(SewingKitMod.UNCOMMON_PATTERN.getId());
            basicIcon(SewingKitMod.RARE_PATTERN.getId());
            basicIcon(SewingKitMod.LEGENDARY_PATTERN.getId());

            getBuilder(SewingKitMod.SEWING_STATION_ITEM.getId().getPath())
                    .parent(getExistingFile(ModelsResourceUtil
                            .func_240221_a_(SewingKitMod.SEWING_STATION_BLOCK.get())));
        }

        private void basicIcon(ResourceLocation item)
        {
            getBuilder(item.getPath())
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
            Arrays.stream(Needles.values()).forEach(needle -> ShapedRecipeBuilder.shapedRecipe(needle.getNeedle())
                                .patternLine(" D")
                                .patternLine("D ")
                                .key('D', needle.getRepairMaterial())
                                .addCriterion("has_material", needle.getMaterial().map(RecipeProvider::hasItem, RecipeProvider::hasItem))
                                .build(consumer));

            ShapedRecipeBuilder.shapedRecipe(SewingKitMod.SEWING_STATION_ITEM.get())
                    .patternLine("xxx")
                    .patternLine("P P")
                    .patternLine("S S")
                    .key('x', Ingredient.fromTag(ItemTags.makeWrapperTag("minecraft:wooden_slabs")))
                    .key('P', Ingredient.fromTag(ItemTags.makeWrapperTag("minecraft:planks")))
                    .key('S', Ingredient.fromItems(Items.STICK))
                    .addCriterion("has_wood", hasItem(ItemTags.makeWrapperTag("minecraft:planks")))
                    .build(consumer);

            // Sewing recipes: leather
            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_SHEET.get(), 4)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromItems(Items.LEATHER))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_sheet_from_leather"));

            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_SHEET.get(), 1)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromItems(Items.RABBIT_HIDE))
                    .addCriterion("has_leather", hasItem(Items.RABBIT_HIDE))
                    .build(consumer, SewingKitMod.location("leather_sheet_from_rabbit_hide"));

            SewingRecipeBuilder.begin(SewingKitMod.LEATHER_STRIP.get(), 3)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromItems(Items.LEATHER))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_strip_from_leather"));

            SewingRecipeBuilder.begin(Items.LEATHER_BOOTS)
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()), 2)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_STRIP.get()))
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_boots_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_LEGGINGS)
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()), 4)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_STRIP.get()), 3)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_leggings_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_CHESTPLATE)
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()), 8)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_STRIP.get()), 2)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_chestplate_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_HELMET)
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()), 2)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_STRIP.get()))
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_helmet_via_sewing"));

            SewingRecipeBuilder.begin(Items.LEATHER_HORSE_ARMOR)
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, Needles.NETHERITE.getHarvestLevel()))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()), 12)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_STRIP.get()), 6)
                    .addMaterial(Ingredient.fromItems(Items.STRING), 8)
                    .addCriterion("has_leather", hasItem(Items.LEATHER))
                    .build(consumer, SewingKitMod.location("leather_horse_armor_via_sewing"));

            // Sewing recipes: wool
            SewingRecipeBuilder.begin(SewingKitMod.WOOL_ROLL.get(), 4)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromTag(ItemTags.WOOL))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_roll_from_wool"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_ROLL.get(), 1)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromTag(ItemTags.CARPETS))
                    .addCriterion("has_wool", hasItem(ItemTags.CARPETS))
                    .build(consumer, SewingKitMod.location("wool_roll_from_carpet"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_TRIM.get(), 8)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromTag(ItemTags.WOOL))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_trim_from_wool"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_TRIM.get(), 3)
                    .withTool(Ingredient.fromItems(Items.SHEARS))
                    .addMaterial(Ingredient.fromTag(ItemTags.WOOL))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_trim_from_carpet"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_SHOES.get())
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_ROLL.get()), 1)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_TRIM.get()), 2)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_shoes_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_PANTS.get())
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_ROLL.get()), 2)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_TRIM.get()), 4)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_pants_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_SHIRT.get())
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_ROLL.get()), 3)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_TRIM.get()), 3)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_shirt_via_sewing"));

            SewingRecipeBuilder.begin(SewingKitMod.WOOL_HAT.get())
                    .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_ROLL.get()), 1)
                    .addMaterial(Ingredient.fromItems(SewingKitMod.WOOL_TRIM.get()), 1)
                    .addMaterial(Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_wool", hasItem(ItemTags.WOOL))
                    .build(consumer, SewingKitMod.location("wool_hat_via_sewing"));

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
                LootTableManager.validateLootTable(validationtracker, p_218436_2_, p_218436_3_);
            });
        }

        public static class BlockTables extends BlockLootTables
        {
            @Override
            protected void addTables()
            {
                this.registerDropSelfLootTable(SewingKitMod.SEWING_STATION_BLOCK.get());
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
