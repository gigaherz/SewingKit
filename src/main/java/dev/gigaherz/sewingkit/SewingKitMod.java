package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableSet;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.ToolActionIngredient;
import dev.gigaherz.sewingkit.clothing.ClothArmorItem;
import dev.gigaherz.sewingkit.clothing.ClothArmorMaterial;
import dev.gigaherz.sewingkit.file.FileItem;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.patterns.PatternItem;
import dev.gigaherz.sewingkit.table.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.*;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import net.minecraft.world.item.trading.MerchantOffer;

@Mod(SewingKitMod.MODID)
public class SewingKitMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "sewingkit";

    public static final CreativeModeTab SEWING_KIT = new CreativeModeTab("sewing_kit")
    {
        @Override
        public ItemStack makeIcon()
        {
            return new ItemStack(SewingKitMod.WOOD_SEWING_NEEDLE.get());
        }
    };

    public static final TagKey<Block> BONE_TAG = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("sewingkit:needs_bone_tool"));
    public static final Tier BONE_TIER = TierSortingRegistry.registerTier(
            new ForgeTier(Tiers.STONE.getLevel(), 100, 1.0f, 0.0f, 0, BONE_TAG, () -> Ingredient.of(Tags.Items.BONES) ),
            new ResourceLocation("sewingkit:bone"), List.of(Tiers.WOOD), List.of(Tiers.IRON));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MODID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, MODID);

    public static final RegistryObject<Item> LEATHER_STRIP = ITEMS.register("leather_strip",
            () -> new Item(new Item.Properties().stacksTo(64).tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> LEATHER_SHEET = ITEMS.register("leather_sheet",
            () -> new Item(new Item.Properties().stacksTo(64).tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_ROLL = ITEMS.register("wool_roll",
            () -> new Item(new Item.Properties().stacksTo(64).tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_TRIM = ITEMS.register("wool_trim",
            () -> new Item(new Item.Properties().stacksTo(64).tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOD_SEWING_NEEDLE = ITEMS.register("wood_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.WOOD, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> STONE_SEWING_NEEDLE = ITEMS.register("stone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.STONE, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> BONE_SEWING_NEEDLE = ITEMS.register("bone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.BONE, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> GOLD_SEWING_NEEDLE = ITEMS.register("gold_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.GOLD, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> IRON_SEWING_NEEDLE = ITEMS.register("iron_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.IRON, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> DIAMOND_SEWING_NEEDLE = ITEMS.register("diamond_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.DIAMOND, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> NETHERITE_SEWING_NEEDLE = ITEMS.register("netherite_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.NETHERITE, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Block> SEWING_STATION_BLOCK = BLOCKS.register("sewing_station",
            () -> new SewingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F))
    );

    public static final RegistryObject<Item> SEWING_STATION_ITEM = ITEMS.register("sewing_station",
            () -> new BlockItem(SEWING_STATION_BLOCK.get(), new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Block> STORING_SEWING_STATION_BLOCK = BLOCKS.register("storing_sewing_station",
            () -> new StoringSewingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F))
    );

    public static final RegistryObject<Item> STORING_SEWING_STATION_ITEM = ITEMS.register("storing_sewing_station",
            () -> new BlockItem(STORING_SEWING_STATION_BLOCK.get(), new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<BlockEntityType<?>> STORING_SEWING_STATION_TILE_ENTITY = BLOCK_ENTITIES.register("storing_sewing_station",
            () -> BlockEntityType.Builder.of(StoringSewingTableTileEntity::new, STORING_SEWING_STATION_BLOCK.get()).build(null)
    );

    public static final RegistryObject<Item> WOOL_HAT = ITEMS.register("wool_hat",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlot.HEAD, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_SHIRT = ITEMS.register("wool_shirt",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlot.CHEST, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_PANTS = ITEMS.register("wool_pants",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlot.LEGS, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_SHOES = ITEMS.register("wool_shoes",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlot.FEET, new Item.Properties().tab(SEWING_KIT))
    );

    public static final RegistryObject<Item> COMMON_PATTERN = ITEMS.register("common_pattern",
            () -> new PatternItem(new Item.Properties().tab(SEWING_KIT).rarity(Rarity.COMMON))
    );

    public static final RegistryObject<Item> UNCOMMON_PATTERN = ITEMS.register("uncommon_pattern",
            () -> new PatternItem(new Item.Properties().tab(SEWING_KIT).rarity(Rarity.UNCOMMON))
    );

    public static final RegistryObject<Item> RARE_PATTERN = ITEMS.register("rare_pattern",
            () -> new PatternItem(new Item.Properties().tab(SEWING_KIT).rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> LEGENDARY_PATTERN = ITEMS.register("legendary_pattern",
            () -> new PatternItem(new Item.Properties().tab(SEWING_KIT).rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> FILE = ITEMS.register("file",
            () -> new FileItem(new Item.Properties().tab(SEWING_KIT).durability(354))
    );

    public static final RegistryObject<PoiType> TABLE_POI = POI_TYPES.register("tailor",
            () -> new PoiType("tailor", PoiType.getBlockStates(SEWING_STATION_BLOCK.get()), 1, 1)
    );

    @SuppressWarnings("UnstableApiUsage")
    public static final RegistryObject<VillagerProfession> TAILOR = PROFESSIONS.register("tailor",
            () -> new VillagerProfession("tailor", TABLE_POI.get(),
                    Arrays.stream(Needles.values()).map(Needles::getNeedle).collect(ImmutableSet.toImmutableSet()),
                    ImmutableSet.of(), null)
    );

    public SewingKitMod()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::processIMC);
        modBus.addListener(this::gatherData);
        modBus.addGenericListener(RecipeSerializer.class, this::registerRecipes);
        modBus.addGenericListener(MenuType.class, this::registerContainers);

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        BLOCK_ENTITIES.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::villagerTrades);

    }

    private void registerRecipes(RegistryEvent.Register<RecipeSerializer<?>> event)
    {
        CraftingHelper.register(ToolActionIngredient.NAME, ToolActionIngredient.Serializer.INSTANCE);

        event.getRegistry().registerAll(
                new SewingRecipe.Serializer().setRegistryName("sewing")
        );
    }

    private void registerContainers(RegistryEvent.Register<MenuType<?>> event)
    {
        event.getRegistry().registerAll(
                new MenuType<>(SewingTableContainer::new).setRegistryName("sewing_station")
        );
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m -> m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    private void villagerTrades(VillagerTradesEvent event)
    {
        if (event.getType() != TAILOR.get())
            return;

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trademap = event.getTrades();

        trademap.get(1).addAll(Arrays.asList(
                new VillagerTrades.DyedArmorForEmeralds(WOOL_PANTS.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeralds(WOOL_SHOES.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeralds(WOOL_HAT.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeralds(WOOL_SHIRT.get(), 3, 12, 1),

                buyItem(new ItemStack(Items.STRING, 16), 1, 12, 1, 2)
        ));

        trademap.get(2).addAll(Arrays.asList(
                new SellRandomFromTag(ItemTags.CARPETS, 8, 7, 8, 1, 2),
                sellItem(COMMON_PATTERN.get(), 15, 1, 10, 4),

                buyItem(new ItemStack(LEATHER_STRIP.get(), 2), 1, 12, 1, 0.5F),
                buyItem(new ItemStack(Items.STRING, 16), 1, 12, 1, 2)
        ));

        trademap.get(3).addAll(Arrays.asList(
                sellItem(UNCOMMON_PATTERN.get(), 15, 1, 10, 4)

                // Buy something
        ));

        trademap.get(4).addAll(Arrays.asList(
                sellItem(RARE_PATTERN.get(), 15, 1, 10, 4)
        ));

        trademap.get(5).addAll(Arrays.asList(
                sellItem(LEGENDARY_PATTERN.get(), 15, 1, 10, 4)
        ));

            /*
                   2, new VillagerTrades.ITrade[]{
                            new VillagerTrades.EmeraldForItemsTrade(Items.IRON_INGOT, 4, 12, 10),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.BELL), 36, 1, 12, 5, 0.2F),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.CHAINMAIL_BOOTS), 1, 1, 12, 5, 0.2F),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.CHAINMAIL_LEGGINGS), 3, 1, 12, 5, 0.2F)},
                   3, new VillagerTrades.ITrade[]{
                            new VillagerTrades.EmeraldForItemsTrade(Items.LAVA_BUCKET, 1, 12, 20),
                            new VillagerTrades.EmeraldForItemsTrade(Items.DIAMOND, 1, 12, 20),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.CHAINMAIL_HELMET), 1, 1, 12, 10, 0.2F),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.CHAINMAIL_CHESTPLATE), 4, 1, 12, 10, 0.2F),
                            new VillagerTrades.ItemsForEmeraldsTrade(new ItemStack(Items.SHIELD), 5, 1, 12, 10, 0.2F)},
                   4, new VillagerTrades.ITrade[]{
                            new VillagerTrades.EnchantedItemForEmeraldsTrade(Items.DIAMOND_LEGGINGS, 14, 3, 15, 0.2F),
                            new VillagerTrades.EnchantedItemForEmeraldsTrade(Items.DIAMOND_BOOTS, 8, 3, 15, 0.2F)},
                   5, new VillagerTrades.ITrade[]{
                            new VillagerTrades.EnchantedItemForEmeraldsTrade(Items.DIAMOND_HELMET, 8, 3, 30, 0.2F),
                            new VillagerTrades.EnchantedItemForEmeraldsTrade(Items.DIAMOND_CHESTPLATE, 16, 3, 30, 0.2F)})));

             */

    }

    private VillagerTrades.ItemListing sellItem(ItemLike thing, int price, int maxTrades, int xp, float priceMultiplier)
    {
        return sellItem(new ItemStack(thing), price, maxTrades, xp, priceMultiplier);
    }

    private VillagerTrades.ItemListing sellItem(ItemStack thing, int price, int maxTrades, int xp, float priceMultiplier)
    {
        return new BasicItemListing(new ItemStack(Items.EMERALD, price), thing, maxTrades, xp, priceMultiplier);
    }

    private VillagerTrades.ItemListing buyItem(ItemStack thing, int reward, int maxTrades, int xp, float priceMultiplier)
    {
        return new BasicItemListing(thing, new ItemStack(Items.EMERALD, reward), maxTrades, xp, priceMultiplier);
    }

    private static class SellRandomFromTag implements VillagerTrades.ItemListing
    {
        private final TagKey<Item> tagSource;
        private final int quantity;
        private final int price;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        private SellRandomFromTag(TagKey<Item> tagSource, int quantity, int price, int maxUses, int xp, float priceMultiplier)
        {
            this.tagSource = tagSource;
            this.quantity = quantity;
            this.price = price;
            this.maxUses = maxUses;
            this.xp = xp;
            this.priceMultiplier = priceMultiplier;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity trader, Random rand)
        {
            return Registry.ITEM.getTag(tagSource)
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .map(itemHolder -> new MerchantOffer(new ItemStack(Items.EMERALD, price), new ItemStack(itemHolder.value(), quantity), this.maxUses, this.xp, this.priceMultiplier))
                    .orElse(null);
        }
    }

    private void gatherData(GatherDataEvent event)
    {
        SewingKitDataGen.gatherData(event);
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBus
    {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                MenuScreens.register(SewingTableContainer.TYPE, SewingTableScreen::new);
            });
            SewingTableScreen.register();
        }

        @SubscribeEvent
        public static void textureStitch(final TextureStitchEvent.Pre event)
        {
            //noinspection deprecation
            if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS))
            {
                event.addSprite(location("gui/needle_slot_background"));
                event.addSprite(location("gui/pattern_slot_background"));
            }
        }

        @SubscribeEvent
        public static void modelRegistry(final ModelRegistryEvent event)
        {
        }

        @SubscribeEvent
        public static void itemColors(final ColorHandlerEvent.Item event)
        {
            event.getItemColors().register(
                    (stack, color) -> color > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack),
                    WOOL_HAT.get(), WOOL_SHIRT.get(), WOOL_PANTS.get(), WOOL_SHOES.get());
        }
    }
}
