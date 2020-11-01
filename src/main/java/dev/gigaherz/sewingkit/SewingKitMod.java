package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableSet;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.ToolIngredient;
import dev.gigaherz.sewingkit.clothing.ClothArmorItem;
import dev.gigaherz.sewingkit.clothing.ClothArmorMaterial;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.patterns.PatternItem;
import dev.gigaherz.sewingkit.table.SewingTableBlock;
import dev.gigaherz.sewingkit.table.SewingTableContainer;
import dev.gigaherz.sewingkit.table.SewingTableScreen;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.BasicTrade;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Mod(SewingKitMod.MODID)
public class SewingKitMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "sewingkit";

    public static final ItemGroup SEWING_KIT = new ItemGroup("sewing_kit")
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(SewingKitMod.WOOD_SEWING_NEEDLE.get());
        }
    };

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<PointOfInterestType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.PROFESSIONS, MODID);

    public static final RegistryObject<Item> LEATHER_STRIP = ITEMS.register("leather_strip",
            () -> new Item(new Item.Properties().maxStackSize(64).group(SEWING_KIT))
    );

    public static final RegistryObject<Item> LEATHER_SHEET = ITEMS.register("leather_sheet",
            () -> new Item(new Item.Properties().maxStackSize(64).group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_ROLL = ITEMS.register("wool_roll",
            () -> new Item(new Item.Properties().maxStackSize(64).group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_TRIM = ITEMS.register("wool_trim",
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
            () -> new SewingTableBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F))
    );

    public static final RegistryObject<Item> SEWING_STATION_ITEM = ITEMS.register("sewing_station",
            () -> new BlockItem(SEWING_STATION_BLOCK.get(), new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_HAT = ITEMS.register("wool_hat",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlotType.HEAD, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_SHIRT = ITEMS.register("wool_shirt",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlotType.CHEST, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_PANTS = ITEMS.register("wool_pants",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlotType.LEGS, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<Item> WOOL_SHOES = ITEMS.register("wool_shoes",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, EquipmentSlotType.FEET, new Item.Properties().group(SEWING_KIT))
    );

    public static final RegistryObject<PointOfInterestType> TABLE_POI = POI_TYPES.register("tailor",
            () -> new PointOfInterestType("tailor", PointOfInterestType.getAllStates(SEWING_STATION_BLOCK.get()), 1, 1)
    );

    public static final RegistryObject<Item> COMMON_PATTERN = ITEMS.register("common_pattern",
            () -> new PatternItem(new Item.Properties().group(SEWING_KIT).rarity(Rarity.COMMON))
    );

    public static final RegistryObject<Item> UNCOMMON_PATTERN = ITEMS.register("uncommon_pattern",
            () -> new PatternItem(new Item.Properties().group(SEWING_KIT).rarity(Rarity.UNCOMMON))
    );

    public static final RegistryObject<Item> RARE_PATTERN = ITEMS.register("rare_pattern",
            () -> new PatternItem(new Item.Properties().group(SEWING_KIT).rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> LEGENDARY_PATTERN = ITEMS.register("legendary_pattern",
            () -> new PatternItem(new Item.Properties().group(SEWING_KIT).rarity(Rarity.EPIC))
    );

    @SuppressWarnings("UnstableApiUsage")
    public static final RegistryObject<VillagerProfession> TAILOR = PROFESSIONS.register("tailor",
            () -> new VillagerProfession("tailor", TABLE_POI.get(),
                    Arrays.stream(Needles.values()).map(Needles::getNeedle).collect(ImmutableSet.toImmutableSet()),
                    ImmutableSet.of(), null)
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
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::villagerTrades);
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
        event.enqueueWork(() -> {
            PointOfInterestType.registerBlockStates(TABLE_POI.get());
            PointOfInterestType.BLOCKS_OF_INTEREST.addAll(TABLE_POI.get().blockStates);
        });
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    private void villagerTrades(VillagerTradesEvent event)
    {
        if (event.getType() != TAILOR.get())
            return;

        Int2ObjectMap<List<VillagerTrades.ITrade>> trademap = event.getTrades();

        trademap.get(1).addAll(Arrays.asList(
                new VillagerTrades.DyedArmorForEmeraldsTrade(WOOL_PANTS.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeraldsTrade(WOOL_SHOES.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeraldsTrade(WOOL_HAT.get(), 3, 12, 1),
                new VillagerTrades.DyedArmorForEmeraldsTrade(WOOL_SHIRT.get(), 3, 12, 1),

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

    private VillagerTrades.ITrade sellItem(IItemProvider thing, int price, int maxTrades, int xp, float priceMultiplier)
    {
        return sellItem(new ItemStack(thing), price, maxTrades, xp, priceMultiplier);
    }

    private VillagerTrades.ITrade sellItem(ItemStack thing, int price, int maxTrades, int xp, float priceMultiplier)
    {
        return new BasicTrade(new ItemStack(Items.EMERALD, price), thing, maxTrades, xp, priceMultiplier);
    }

    private VillagerTrades.ITrade buyItem(ItemStack thing, int reward, int maxTrades, int xp, float priceMultiplier)
    {
        return new BasicTrade(thing, new ItemStack(Items.EMERALD, reward), maxTrades, xp, priceMultiplier);
    }

    private static class SellRandomFromTag implements VillagerTrades.ITrade
    {
        private final ITag<Item> tagSource;
        private final int quantity;
        private final int price;
        private final int maxUses;
        private final int xp;
        private final float priceMultiplier;

        private SellRandomFromTag(ITag<Item> tagSource, int quantity, int price, int maxUses, int xp, float priceMultiplier)
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
            Item random = tagSource.getRandomElement(rand);
            return new MerchantOffer(new ItemStack(Items.EMERALD, price), new ItemStack(random, quantity), this.maxUses, this.xp, this.priceMultiplier);
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
            //noinspection deprecation
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
        @SubscribeEvent
        public static void itemColors(final ColorHandlerEvent.Item event)
        {
            event.getItemColors().register(
                    (stack, color) -> color > 0 ? -1 : ((IDyeableArmorItem)stack.getItem()).getColor(stack),
                    WOOL_HAT.get(), WOOL_SHIRT.get(), WOOL_PANTS.get(), WOOL_SHOES.get());
        }
    }
}
