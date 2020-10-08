package dev.gigaherz.sewingkit;

import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.SewingRecipeBuilder;
import dev.gigaherz.sewingkit.api.ToolIngredient;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.table.SewingTableBlock;
import dev.gigaherz.sewingkit.table.SewingTableContainer;
import dev.gigaherz.sewingkit.table.SewingTableScreen;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.data.*;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod(SewingKitMod.MODID)
public class SewingKitMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "sewingkit";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final ItemGroup SEWING_KIT = new ItemGroup("sewing_kit")
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(SewingKitMod.WOOD_SEWING_NEEDLE.get());
        }
    };

    public static final RegistryObject<Item> LEATHER_STRIP = ITEMS.register("leather_strip",
            () -> new Item(new Item.Properties().maxStackSize(64).group(SEWING_KIT))
    );

    public static final RegistryObject<Item> LEATHER_SHEET = ITEMS.register("leather_sheet",
            () -> new Item(new Item.Properties().maxStackSize(64).group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOD_SEWING_NEEDLE = ITEMS.register("wood_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.WOOD, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> STONE_SEWING_NEEDLE = ITEMS.register("stone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.STONE, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> BONE_SEWING_NEEDLE = ITEMS.register("bone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.BONE, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> GOLD_SEWING_NEEDLE = ITEMS.register("gold_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.GOLD, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> IRON_SEWING_NEEDLE = ITEMS.register("iron_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.IRON, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> DIAMOND_SEWING_NEEDLE = ITEMS.register("diamond_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.DIAMOND, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> NETHERITE_SEWING_NEEDLE = ITEMS.register("netherite_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.NETHERITE, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Block> SEWING_STATION_BLOCK = BLOCKS.register("sewing_station",
            () -> new SewingTableBlock(AbstractBlock.Properties.create(Material.WOOD))
    );

    public static final RegistryObject<Item> SEWING_STATION_ITEM = ITEMS.register("sewing_station",
            () -> new BlockItem(SEWING_STATION_BLOCK.get(), new Item.Properties().group(SEWING_KIT))
    );

    public SewingKitMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
        modBus.addListener(this::processIMC);
        modBus.addListener(this::gatherData);
        modBus.addGenericListener(IRecipeSerializer.class, this::registerRecipes);
        modBus.addGenericListener(ContainerType.class, this::registerContainers);

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
    }

    private void registerRecipes(RegistryEvent.Register<IRecipeSerializer<?>> event)
    {
        CraftingHelper.register(ToolIngredient.NAME, ToolIngredient.Serializer.INSTANCE);

        event.getRegistry().registerAll(
                new SewingRecipe.Serializer().setRegistryName("sewing")
        );
    }

    private void registerContainers(RegistryEvent.Register<ContainerType<?>> event)
    {
        event.getRegistry().registerAll(
                new ContainerType<>(SewingTableContainer::new).setRegistryName("sewing_station")
        );
    }

    private void setup(final FMLCommonSetupEvent event)
    {
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    public void gatherData(GatherDataEvent event)
    {
        DataGen.gatherData(event);
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(value= Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBus {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event)
        {
            ScreenManager.registerFactory(SewingTableContainer.TYPE, SewingTableScreen::new);
        }

        @SubscribeEvent
        public static void textureStitch(final TextureStitchEvent.Pre event)
        {
            if (event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE))
            {
                event.addSprite(location("gui/needle_slot_background"));
                event.addSprite(location("gui/pattern_slot_background"));
            }
        }
        @SubscribeEvent
        public static void modelRegistry(final ModelRegistryEvent event)
        {
        }
    }

    public static class DataGen
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
                super(gen, MODID, "en_us");
            }

            @Override
            protected void addTranslations()
            {
                add("itemGroup.sewing_kit", "Sewing Kit");
                add("container.sewingkit.sewing_station", "Sewing Station");

                add(LEATHER_STRIP.get(), "Leather Strip");
                add(LEATHER_SHEET.get(), "Leather Sheet");

                Arrays.stream(Needles.values()).forEach(needle -> {
                    String type = needle.getType();
                    String name = type.substring(0,1).toUpperCase() + type.substring(1);
                    add(needle.getNeedle(), name + " Sewing Needle");
                });
            }
        }

        public static class ItemModels extends ItemModelProvider
        {
            private static final Logger LOGGER = LogManager.getLogger();

            public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper)
            {
                super(generator, MODID, existingFileHelper);
            }

            @Override
            protected void registerModels()
            {
                basicIcon(LEATHER_STRIP.getId());
                basicIcon(LEATHER_SHEET.getId());
                Arrays.stream(Needles.values()).forEach(needle -> basicIcon(needle.getId()));
            }

            private void basicIcon(ResourceLocation item)
            {
                getBuilder(item.getPath())
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", location("item/" + item.getPath()));
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
                SewingRecipeBuilder.begin(LEATHER_SHEET.get(), 4)
                        .withTool(Ingredient.fromItems(Items.SHEARS))
                        .addMaterial(Ingredient.fromItems(Items.LEATHER))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_sheet_from_leather"));

                SewingRecipeBuilder.begin(LEATHER_SHEET.get(), 1)
                        .withTool(Ingredient.fromItems(Items.SHEARS))
                        .addMaterial(Ingredient.fromItems(Items.RABBIT_HIDE))
                        .addCriterion("has_leather", hasItem(Items.RABBIT_HIDE))
                        .build(consumer, location("leather_sheet_from_rabbit_hide"));

                SewingRecipeBuilder.begin(LEATHER_STRIP.get(), 3)
                        .withTool(Ingredient.fromItems(Items.SHEARS))
                        .addMaterial(Ingredient.fromItems(Items.LEATHER))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_strip_from_leather"));

                SewingRecipeBuilder.begin(Items.LEATHER_BOOTS)
                        .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                        .addMaterial(Ingredient.fromItems(LEATHER_SHEET.get()))
                        .addMaterial(Ingredient.fromItems(LEATHER_SHEET.get()))
                        .addMaterial(Ingredient.fromItems(LEATHER_STRIP.get()))
                        .addMaterial(Ingredient.fromItems(Items.STRING))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_boots_via_sewing"));

                SewingRecipeBuilder.begin(Items.LEATHER_LEGGINGS)
                        .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                        .addMaterial(Ingredient.fromItems(LEATHER_SHEET.get()), 4)
                        .addMaterial(Ingredient.fromItems(LEATHER_STRIP.get()), 3)
                        .addMaterial(Ingredient.fromItems(Items.STRING))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_leggings_via_sewing"));

                SewingRecipeBuilder.begin(Items.LEATHER_CHESTPLATE)
                        .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                        .addMaterial(Ingredient.fromItems(LEATHER_SHEET.get()), 8)
                        .addMaterial(Ingredient.fromItems(LEATHER_STRIP.get()), 2)
                        .addMaterial(Ingredient.fromItems(Items.STRING))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_chestplate_via_sewing"));

                SewingRecipeBuilder.begin(Items.LEATHER_HELMET)
                        .withTool(ToolIngredient.fromTool(NeedleItem.SEWING_NEEDLE, 1))
                        .addMaterial(Ingredient.fromItems(LEATHER_SHEET.get()), 2)
                        .addMaterial(Ingredient.fromItems(LEATHER_STRIP.get()))
                        .addMaterial(Ingredient.fromItems(Items.STRING))
                        .addCriterion("has_leather", hasItem(Items.LEATHER))
                        .build(consumer, location("leather_helmet_via_sewing"));
            }
        }
    }
}
