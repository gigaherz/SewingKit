package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.file.FileItem;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.NeedleItem;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.patterns.PatternItem;
import dev.gigaherz.sewingkit.structure.TailorShopProcessor;
import dev.gigaherz.sewingkit.table.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.ItemCost;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(SewingKitMod.MODID)
public class SewingKitMod
{

    public static final String MODID = "sewingkit";

    public static final TagKey<Block> INCORRECT_BONE_TAG = TagKey.create(Registries.BLOCK, location("incorrect_for_bone_tool"));
    public static final Tier BONE_TIER = new SimpleTier(INCORRECT_BONE_TAG, 100, 1.0f, 0.0f, 0, () -> Ingredient.of(Tags.Items.BONES));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR, MODID);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(BuiltInRegistries.LOOT_FUNCTION_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MODID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPE = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, MODID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, MODID);

    public static final DeferredItem<Item> LEATHER_STRIP = ITEMS.register("leather_strip",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<Item> LEATHER_SHEET = ITEMS.register("leather_sheet",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<Item> WOOL_ROLL = ITEMS.register("wool_roll",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<Item> WOOL_TRIM = ITEMS.register("wool_trim",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<NeedleItem> WOOD_SEWING_NEEDLE = ITEMS.register("wood_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.WOOD, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> STONE_SEWING_NEEDLE = ITEMS.register("stone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.STONE, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> BONE_SEWING_NEEDLE = ITEMS.register("bone_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.BONE, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> GOLD_SEWING_NEEDLE = ITEMS.register("gold_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.GOLD, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> IRON_SEWING_NEEDLE = ITEMS.register("iron_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.IRON, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> DIAMOND_SEWING_NEEDLE = ITEMS.register("diamond_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.DIAMOND, new Item.Properties())
    );

    public static final DeferredItem<NeedleItem> NETHERITE_SEWING_NEEDLE = ITEMS.register("netherite_sewing_needle",
            () -> new NeedleItem(0, 1, Needles.NETHERITE, new Item.Properties())
    );

    public static final DeferredBlock<Block> SEWING_STATION_BLOCK = BLOCKS.register("sewing_station",
            () -> new SewingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F))
    );

    public static final DeferredItem<Item> SEWING_STATION_ITEM = ITEMS.register("sewing_station",
            () -> new BlockItem(SEWING_STATION_BLOCK.get(), new Item.Properties())
    );

    public static final DeferredBlock<Block> STORING_SEWING_STATION_BLOCK = BLOCKS.register("storing_sewing_station",
            () -> new StoringSewingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F))
    );

    public static final DeferredItem<Item> STORING_SEWING_STATION_ITEM = ITEMS.register("storing_sewing_station",
            () -> new BlockItem(STORING_SEWING_STATION_BLOCK.get(), new Item.Properties())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoringSewingTableBlockEntity>> STORING_SEWING_STATION_BLOCK_ENTITY = BLOCK_ENTITIES.register("storing_sewing_station",
            () -> BlockEntityType.Builder.of(StoringSewingTableBlockEntity::new, STORING_SEWING_STATION_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WOOL = ARMOR_MATERIALS.register("wool", () ->
            new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 0);
                map.put(ArmorItem.Type.LEGGINGS, 0);
                map.put(ArmorItem.Type.CHESTPLATE, 0);
                map.put(ArmorItem.Type.HELMET, 0);
                map.put(ArmorItem.Type.BODY, 0);
            }), 25, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(ItemTags.WOOL),
                    List.of(new ArmorMaterial.Layer(location("wool"), "", true)), 0.0F, 0.01F));

    public static final DeferredItem<Item> WOOL_HAT = ITEMS.register("wool_hat",
            () ->
                    new ArmorItem(WOOL, ArmorItem.Type.HELMET, new Item.Properties())
    );

    public static final DeferredItem<Item> WOOL_SHIRT = ITEMS.register("wool_shirt",
            () -> new ArmorItem(WOOL, ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );

    public static final DeferredItem<Item> WOOL_PANTS = ITEMS.register("wool_pants",
            () -> new ArmorItem(WOOL, ArmorItem.Type.LEGGINGS, new Item.Properties())
    );

    public static final DeferredItem<Item> WOOL_SHOES = ITEMS.register("wool_shoes",
            () -> new ArmorItem(WOOL, ArmorItem.Type.BOOTS, new Item.Properties())
    );

    public static final DeferredItem<Item> COMMON_PATTERN = ITEMS.register("common_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.COMMON))
    );

    public static final DeferredItem<Item> UNCOMMON_PATTERN = ITEMS.register("uncommon_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.UNCOMMON))
    );

    public static final DeferredItem<Item> RARE_PATTERN = ITEMS.register("rare_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.RARE))
    );

    public static final DeferredItem<Item> LEGENDARY_PATTERN = ITEMS.register("legendary_pattern",
            () -> new PatternItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final DeferredItem<Item>
            FILE = ITEMS.register("file", () -> new FileItem(new Item.Properties().durability(354)));

    public static final DeferredHolder<PoiType, PoiType>
            TABLE_POI = POI_TYPES.register("tailor", () -> new PoiType(Stream.concat(
                    SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream(),
                    STORING_SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream()
            ).collect(Collectors.toUnmodifiableSet()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession>
            TAILOR = PROFESSIONS.register("tailor", () -> {
                var key = Objects.requireNonNull(TABLE_POI.getKey());
                return new VillagerProfession("tailor",
                        holder -> holder.is(key),
                        holder -> holder.is(key),
                        Arrays.stream(Needles.values()).map(Needles::getNeedle).collect(ImmutableSet.toImmutableSet()),
                        ImmutableSet.of(), null);
            });

    public static final DeferredHolder<RecipeType<?>, RecipeType<SewingRecipe>>
            SEWING = RECIPE_TYPES.register("sewing", RecipeType::simple);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SewingRecipe>>
            SEWING_RECIPE = RECIPE_SERIALIZERS.register("sewing", SewingRecipe.Serializer::new);

    public static final DeferredHolder<MenuType<?>, MenuType<SewingTableMenu>>
            SEWING_STATION_MENU = MENU_TYPES.register("sewing_station", () -> new MenuType<>(SewingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RandomDye>>
            RANDOM_DYE = LOOT_FUNCTIONS.register("random_dye", () -> new LootItemFunctionType<>(RandomDye.CODEC));

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<TailorShopProcessor>>
            TAILOR_SHOP_PROCESSOR = STRUCTURE_PROCESSORS.register("tailor_shop_processor", () -> TailorShopProcessor::codec);

    private static final ResourceKey<StructureProcessorList>
            TAILOR_SHOP_PROCESSOR_LIST_KEY = ResourceKey.create(Registries.PROCESSOR_LIST, location("tailor_shop_processors"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            SEWING_KIT_TAB = CREATIVE_TABS.register("sewing_kit", () -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP,0)
                    .icon(() -> new ItemStack(WOOD_SEWING_NEEDLE.get()))
                    .title(Component.translatable("tab.sewing_kit"))
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
                    }).build()
        );

    public static final TagKey<Item> WOOD_OR_HIGHER = TagKey.create(Registries.ITEM, SewingKitMod.location("needles/wood_or_higher"));
    public static final TagKey<Item> BONE_OR_HIGHER = TagKey.create(Registries.ITEM, SewingKitMod.location("needles/bone_or_higher"));
    public static final TagKey<Item> IRON_OR_HIGHER = TagKey.create(Registries.ITEM, SewingKitMod.location("needles/iron_or_higher"));
    public static final TagKey<Item> DIAMOND_OR_HIGHER = TagKey.create(Registries.ITEM, SewingKitMod.location("needles/diamond_or_higher"));
    public static final TagKey<Item> NETHERITE_OR_HIGHER = TagKey.create(Registries.ITEM, SewingKitMod.location("needles/netherite_or_higher"));

    public SewingKitMod(IEventBus modBus)
    {
        modBus.addListener(this::gatherData);

        ARMOR_MATERIALS.register(modBus);
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
        CREATIVE_TABS.register(modBus);
        INGREDIENT_TYPE.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::villagerTrades);
        NeoForge.EVENT_BUS.addListener(this::addBuildingToVillages);
    }

    public void addBuildingToVillages(final ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
        Registry<StructureProcessorList> processorListRegistry = event.getServer().registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

        // Adds our piece to all village houses pool
        // Note, the resourcelocation is getting the pool files from the data folder. Not assets folder.
        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                ResourceLocation.parse("minecraft:village/plains/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                ResourceLocation.parse("minecraft:village/snowy/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                ResourceLocation.parse("minecraft:village/savanna/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                ResourceLocation.parse("minecraft:village/taiga/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                ResourceLocation.parse("minecraft:village/desert/houses"),
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
            return BuiltInRegistries.ITEM.getTag(tagSource)
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .map(itemHolder -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, price),
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
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SuppressWarnings("SameParameterValue")
    public static <B extends ByteBuf, V> StreamCodec<B, @Nullable V> nullable(StreamCodec<B, V> streamCodec)
    {
        return new StreamCodec<>()
        {
            @Override
            public V decode(B buff)
            {
                var present = buff.readBoolean();
                //noinspection DataFlowIssue
                return present ? streamCodec.decode(buff) : null;
            }

            @Override
            public void encode(B buff, @Nullable V value)
            {
                buff.writeBoolean(value != null);
                if (value != null)
                {
                    streamCodec.encode(buff, value);
                }
            }
        };
    }


    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModBus
    {
        @SubscribeEvent
        public static void registerMenuScreens(final RegisterMenuScreensEvent event)
        {
            event.register(SEWING_STATION_MENU.get(), SewingTableScreen::new);
        }

        @SubscribeEvent
        public static void itemColors(final RegisterColorHandlersEvent.Item event)
        {
            event.register(
                    (stack, color) -> color > 0 ? -1 : (0xFF000000 | getColor(stack)),
                    WOOL_HAT.get(), WOOL_SHIRT.get(), WOOL_PANTS.get(), WOOL_SHOES.get());
        }

        private static int getColor(ItemStack stack)
        {
            var colorData = stack.get(DataComponents.DYED_COLOR);
            return colorData != null ? colorData.rgb() : -1;
        }
    }
}
