package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.ToolActionIngredient;
import dev.gigaherz.sewingkit.clothing.ClothArmorItem;
import dev.gigaherz.sewingkit.clothing.ClothArmorMaterial;
import dev.gigaherz.sewingkit.file.FileItem;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.patterns.PatternItem;
import dev.gigaherz.sewingkit.structure.TailorShopProcessor;
import dev.gigaherz.sewingkit.table.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.*;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(SewingKitMod.MODID)
public class SewingKitMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "sewingkit";

    public static CreativeModeTab SEWING_KIT;

    public static final TagKey<Block> BONE_TAG = TagKey.create(Registries.BLOCK, new ResourceLocation("sewingkit:needs_bone_tool"));
    public static final Tier BONE_TIER = TierSortingRegistry.registerTier(
            new ForgeTier(Tiers.STONE.getLevel(), 100, 1.0f, 0.0f, 0, BONE_TAG, () -> Ingredient.of(Tags.Items.BONES)),
            new ResourceLocation("sewingkit:bone"), List.of(Tiers.WOOD), List.of(Tiers.IRON));

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID);
    private static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MODID);

    public static final RegistryObject<Item> LEATHER_STRIP = ITEMS.register("leather_strip",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> LEATHER_SHEET = ITEMS.register("leather_sheet",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> WOOL_ROLL = ITEMS.register("wool_roll",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> WOOL_TRIM = ITEMS.register("wool_trim",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> WOOD_SEWING_NEEDLE = ITEMS.register("wood_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.WOOD, new Item.Properties())
    );

    public static final RegistryObject<Item> STONE_SEWING_NEEDLE = ITEMS.register("stone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.STONE, new Item.Properties())
    );

    public static final RegistryObject<Item> BONE_SEWING_NEEDLE = ITEMS.register("bone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.BONE, new Item.Properties())
    );

    public static final RegistryObject<Item> GOLD_SEWING_NEEDLE = ITEMS.register("gold_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.GOLD, new Item.Properties())
    );

    public static final RegistryObject<Item> IRON_SEWING_NEEDLE = ITEMS.register("iron_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.IRON, new Item.Properties())
    );

    public static final RegistryObject<Item> DIAMOND_SEWING_NEEDLE = ITEMS.register("diamond_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.DIAMOND, new Item.Properties())
    );

    public static final RegistryObject<Item> NETHERITE_SEWING_NEEDLE = ITEMS.register("netherite_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.NETHERITE, new Item.Properties())
    );

    public static final RegistryObject<Block> SEWING_STATION_BLOCK = BLOCKS.register("sewing_station",
            () -> new SewingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F))
    );

    public static final RegistryObject<Item> SEWING_STATION_ITEM = ITEMS.register("sewing_station",
            () -> new BlockItem(SEWING_STATION_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<Block> STORING_SEWING_STATION_BLOCK = BLOCKS.register("storing_sewing_station",
            () -> new StoringSewingTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F))
    );

    public static final RegistryObject<Item> STORING_SEWING_STATION_ITEM = ITEMS.register("storing_sewing_station",
            () -> new BlockItem(STORING_SEWING_STATION_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<?>> STORING_SEWING_STATION_BLOCK_ENTITY = BLOCK_ENTITIES.register("storing_sewing_station",
            () -> BlockEntityType.Builder.of(StoringSewingTableBlockEntity::new, STORING_SEWING_STATION_BLOCK.get()).build(null)
    );

    public static final RegistryObject<Item> WOOL_HAT = ITEMS.register("wool_hat",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, ArmorItem.Type.HELMET, new Item.Properties())
    );

    public static final RegistryObject<Item> WOOL_SHIRT = ITEMS.register("wool_shirt",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );

    public static final RegistryObject<Item> WOOL_PANTS = ITEMS.register("wool_pants",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, ArmorItem.Type.LEGGINGS, new Item.Properties())
    );

    public static final RegistryObject<Item> WOOL_SHOES = ITEMS.register("wool_shoes",
            () -> new ClothArmorItem(ClothArmorMaterial.WOOL, ArmorItem.Type.BOOTS, new Item.Properties())
    );

    public static final RegistryObject<Item> COMMON_PATTERN = ITEMS.register("common_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.COMMON))
    );

    public static final RegistryObject<Item> UNCOMMON_PATTERN = ITEMS.register("uncommon_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.UNCOMMON))
    );

    public static final RegistryObject<Item> RARE_PATTERN = ITEMS.register("rare_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> LEGENDARY_PATTERN = ITEMS.register("legendary_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> FILE = ITEMS.register("file",
            () -> new FileItem(new Item.Properties().durability(354))
    );

    public static final RegistryObject<PoiType> TABLE_POI = POI_TYPES.register("tailor",
            () -> new PoiType(Stream.concat(
                    SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream(),
                    STORING_SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream()
            ).collect(Collectors.toUnmodifiableSet()), 1, 1)
    );

    public static final RegistryObject<VillagerProfession> TAILOR = PROFESSIONS.register("tailor",
            () -> {
                var key = Objects.requireNonNull(TABLE_POI.getKey());
                return new VillagerProfession("tailor",
                        holder -> holder.is(key),
                        holder -> holder.is(key),
                        Arrays.stream(Needles.values()).map(Needles::getNeedle).collect(ImmutableSet.toImmutableSet()),
                        ImmutableSet.of(), null);
            }
    );

    public static final RegistryObject<RecipeType<SewingRecipe>> SEWING = RECIPE_TYPES.register("sewing", () -> new RecipeType<>()
    {
        @Override
        public String toString()
        {
            return "sewingkit:sewing";
        }
    });

    public static final RegistryObject<RecipeSerializer<SewingRecipe>> SEWING_RECIPE = RECIPE_SERIALIZERS.register("sewing", () -> new SewingRecipe.Serializer());

    public static final RegistryObject<MenuType<SewingTableMenu>> SEWING_STATION_MENU = MENU_TYPES.register("sewing_station", () -> new MenuType<>(SewingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<LootItemFunctionType> RANDOM_DYE =
            LOOT_FUNCTIONS.register("random_dye", () -> new LootItemFunctionType(new RandomDye.Serializer()));

    public static final RegistryObject<StructureProcessorType<TailorShopProcessor>> TAILOR_SHOP_PROCESSOR =
            STRUCTURE_PROCESSORS.register("tailor_shop_processor", () -> TailorShopProcessor::codec);

    private static final ResourceKey<StructureProcessorList> TAILOR_SHOP_PROCESSOR_LIST_KEY =
            ResourceKey.create(Registries.PROCESSOR_LIST, location("tailor_shop_processors"));

    public SewingKitMod()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::construct);
        modBus.addListener(this::gatherData);
        modBus.addListener(this::registerTabs);

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        MENU_TYPES.register(modBus);
        RECIPE_TYPES.register(modBus);
        LOOT_FUNCTIONS.register(modBus);
        STRUCTURE_PROCESSORS.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::villagerTrades);
        MinecraftForge.EVENT_BUS.addListener(this::addBuildingToVillages);
    }

    private void construct(FMLConstructModEvent event)
    {
        event.enqueueWork(() -> {
            CraftingHelper.register(ToolActionIngredient.NAME, ToolActionIngredient.Serializer.INSTANCE);
        });
    }

    private void registerTabs(CreativeModeTabEvent.Register event)
    {
        SEWING_KIT = event.registerCreativeModeTab(location("sewing_kit"), builder -> builder
                .icon(() -> new ItemStack(WOOD_SEWING_NEEDLE.get()))
                .title(Component.translatable(""))
                .displayItems((featureFlags, output) -> {
                    output.accept(SEWING_STATION_ITEM.get());
                    output.accept(STORING_SEWING_STATION_ITEM.get());
                    output.accept(WOOD_SEWING_NEEDLE.get());
                    output.accept(STONE_SEWING_NEEDLE.get());
                    output.accept(BONE_SEWING_NEEDLE.get());
                    output.accept(GOLD_SEWING_NEEDLE.get());
                    output.accept(IRON_SEWING_NEEDLE.get());
                    output.accept(DIAMOND_SEWING_NEEDLE.get());
                    output.accept(NETHERITE_SEWING_NEEDLE.get());
                    output.accept(FILE.get());
                    output.accept(LEATHER_STRIP.get());
                    output.accept(LEATHER_SHEET.get());
                    output.accept(WOOL_ROLL.get());
                    output.accept(WOOL_TRIM.get());
                    output.accept(WOOL_HAT.get());
                    output.accept(WOOL_SHIRT.get());
                    output.accept(WOOL_PANTS.get());
                    output.accept(WOOL_SHOES.get());
                    output.accept(COMMON_PATTERN.get());
                    output.accept(UNCOMMON_PATTERN.get());
                    output.accept(RARE_PATTERN.get());
                    output.accept(LEGENDARY_PATTERN.get());
                })
        );
    }

    public void addBuildingToVillages(final ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
        Registry<StructureProcessorList> processorListRegistry = event.getServer().registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

        // Adds our piece to all village houses pool
        // Note, the resourcelocation is getting the pool files from the data folder. Not assets folder.
        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                new ResourceLocation("minecraft:village/plains/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                new ResourceLocation("minecraft:village/snowy/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                new ResourceLocation("minecraft:village/savanna/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                new ResourceLocation("minecraft:village/taiga/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                new ResourceLocation("minecraft:village/desert/houses"),
                "sewingkit:tailor_shop", 5);
    }

    private static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry,
                                          Registry<StructureProcessorList> processorListRegistry,
                                          ResourceLocation poolRL,
                                          String nbtPieceRL,
                                          int weight)
    {
        Holder<StructureProcessorList> emptyProcessorList = processorListRegistry.getHolderOrThrow(TAILOR_SHOP_PROCESSOR_LIST_KEY);

        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return;

        SinglePoolElement piece = SinglePoolElement.single(nbtPieceRL, emptyProcessorList).apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++)
        {
            pool.templates.add(piece);
        }

        // for completeness
        List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
        listOfPieceEntries.add(new Pair<>(piece, weight));
        pool.rawTemplates = listOfPieceEntries;
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
                new SellRandomFromTag(ItemTags.WOOL_CARPETS, 8, 7, 8, 1, 2),
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

        @org.jetbrains.annotations.Nullable
        @Override
        public MerchantOffer getOffer(Entity p_219693_, RandomSource rand)
        {
            return Optional.ofNullable(ForgeRegistries.ITEMS.tags())
                    .map(tags -> tags.getTag(tagSource))
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .map(itemHolder -> new MerchantOffer(
                            new ItemStack(Items.EMERALD, price),
                            new ItemStack(itemHolder, quantity), this.maxUses, this.xp, this.priceMultiplier))
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
                MenuScreens.register(SEWING_STATION_MENU.get(), SewingTableScreen::new);
            });
        }

        @SubscribeEvent
        public static void itemColors(final RegisterColorHandlersEvent.Item event)
        {
            event.register(
                    (stack, color) -> color > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack),
                    WOOL_HAT.get(), WOOL_SHIRT.get(), WOOL_PANTS.get(), WOOL_SHOES.get());
        }
    }
}
