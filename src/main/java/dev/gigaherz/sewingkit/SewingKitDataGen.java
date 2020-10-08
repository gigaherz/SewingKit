package dev.gigaherz.sewingkit;

import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.api.ToolIngredient;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.function.Consumer;

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
            //gen.addProvider(new BlockStates(gen, itemModels.existingFileHelper));
        }
        if (event.includeServer())
        {

            //BlockTags blockTags = new BlockTags(gen);
            //gen.addProvider(blockTags);
            //gen.addProvider(new ItemTags(gen, blockTags));
            gen.addProvider(new Recipes(gen));
            //gen.addProvider(new LootTables(gen));
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

            add(SewingKitMod.LEATHER_STRIP.get(), "Leather Strip");
            add(SewingKitMod.LEATHER_SHEET.get(), "Leather Sheet");

            Arrays.stream(Needles.values()).forEach(needle -> {
                String type = needle.getType();
                String name = type.substring(0, 1).toUpperCase() + type.substring(1);
                add(needle.getNeedle(), name + " Sewing Needle");
            });
        }
    }

    public static class ItemModels extends ItemModelProvider
    {
        private static final Logger LOGGER = LogManager.getLogger();

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
                    .key('S', Ingredient.fromItems(Items.STRING))
                    .addCriterion("has_wood", hasItem(ItemTags.makeWrapperTag("minecraft:planks")))
                    .build(consumer);

            // Sewing recipes:
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
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()))
                    .addMaterial(Ingredient.fromItems(SewingKitMod.LEATHER_SHEET.get()))
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
        }
    }
}
