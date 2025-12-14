package dev.gigaherz.sewingkit;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.gigaherz.sewingkit.api.SewingMaterial;
import dev.gigaherz.sewingkit.api.SewingMaterialSlotDisplay;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.SewingRecipeDisplay;
import dev.gigaherz.sewingkit.file.FileItem;
import dev.gigaherz.sewingkit.loot.RandomDye;
import dev.gigaherz.sewingkit.needle.Needles;
import dev.gigaherz.sewingkit.network.SyncRecipeOrder;
import dev.gigaherz.sewingkit.structure.TailorShopProcessor;
import dev.gigaherz.sewingkit.table.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
    public static final ToolMaterial BONE_TIER = new ToolMaterial(INCORRECT_BONE_TAG, 100, 1.0f, 0.0f, 12, Tags.Items.BONES);

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
    private static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORY = DeferredRegister.create(BuiltInRegistries.RECIPE_BOOK_CATEGORY, MODID);
    private static final DeferredRegister<SlotDisplay.Type<?>> SLOT_DISPLAY = DeferredRegister.create(BuiltInRegistries.SLOT_DISPLAY, MODID);
    private static final DeferredRegister<RecipeDisplay.Type<?>> RECIPE_DISPLAY = DeferredRegister.create(BuiltInRegistries.RECIPE_DISPLAY, MODID);

    public static final DeferredItem<Item> LEATHER_STRIP = ITEMS.registerItem("leather_strip",
            props -> new Item(props.stacksTo(64))
    );

    public static final DeferredItem<Item> LEATHER_SHEET = ITEMS.registerItem("leather_sheet",
            props -> new Item(props.stacksTo(64))
    );

    public static final DeferredItem<Item> WOOL_ROLL = ITEMS.registerItem("wool_roll",
            props -> new Item(props.stacksTo(64))
    );

    public static final DeferredItem<Item> WOOL_TRIM = ITEMS.registerItem("wool_trim",
            props -> new Item(props.stacksTo(64))
    );

    public static final DeferredItem<Item> WOOD_SEWING_NEEDLE = ITEMS.registerItem("wood_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.WOOD, props))
    );

    public static final DeferredItem<Item> STONE_SEWING_NEEDLE = ITEMS.registerItem("stone_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.STONE, props))
    );

    public static final DeferredItem<Item> BONE_SEWING_NEEDLE = ITEMS.registerItem("bone_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.BONE, props))
    );

    public static final DeferredItem<Item> GOLD_SEWING_NEEDLE = ITEMS.registerItem("gold_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.GOLD, props))
    );

    public static final DeferredItem<Item> IRON_SEWING_NEEDLE = ITEMS.registerItem("iron_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.IRON, props))
    );

    public static final DeferredItem<Item> DIAMOND_SEWING_NEEDLE = ITEMS.registerItem("diamond_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.DIAMOND, props))
    );

    public static final DeferredItem<Item> NETHERITE_SEWING_NEEDLE = ITEMS.registerItem("netherite_sewing_needle",
            props -> new Item(Needles.fillProperties(Needles.NETHERITE, props))
    );

    public static final DeferredBlock<Block> SEWING_STATION_BLOCK = BLOCKS.registerBlock("sewing_station",
            props -> new SewingTableBlock(props.mapColor(MapColor.WOOD).strength(2.5F))
    );

    public static final DeferredItem<Item> SEWING_STATION_ITEM = ITEMS.registerItem("sewing_station",
            props -> new BlockItem(SEWING_STATION_BLOCK.get(), props.useBlockDescriptionPrefix())
    );

    public static final DeferredBlock<Block> STORING_SEWING_STATION_BLOCK = BLOCKS.registerBlock("storing_sewing_station",
            props -> new StoringSewingTableBlock(props.mapColor(MapColor.WOOD).strength(2.5F))
    );

    public static final DeferredItem<Item> STORING_SEWING_STATION_ITEM = ITEMS.registerItem("storing_sewing_station",
            props -> new BlockItem(STORING_SEWING_STATION_BLOCK.get(), props.useBlockDescriptionPrefix())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoringSewingTableBlockEntity>>
            STORING_SEWING_STATION_BLOCK_ENTITY = BLOCK_ENTITIES.register("storing_sewing_station",
            () -> new BlockEntityType<>(StoringSewingTableBlockEntity::new, STORING_SEWING_STATION_BLOCK.get())
    );

    public static final ResourceKey<EquipmentAsset> WOOL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, location("wool"));
    private static final EnumMap<ArmorType, Integer> WOOL_ARMOR_VALUES = Util.make(new EnumMap<>(ArmorType.class), map -> {
        map.put(ArmorType.BOOTS, 0);
        map.put(ArmorType.LEGGINGS, 0);
        map.put(ArmorType.CHESTPLATE, 0);
        map.put(ArmorType.HELMET, 0);
        map.put(ArmorType.BODY, 0);
    });
    public static final ArmorMaterial WOOL = new ArmorMaterial(25, WOOL_ARMOR_VALUES, 25, SoundEvents.ARMOR_EQUIP_GENERIC,
            0.0F, 0.01F, ItemTags.WOOL, WOOL_ASSET);

    public static final DeferredItem<Item> WOOL_HAT = ITEMS.registerItem("wool_hat",
            props -> new Item(props.humanoidArmor(WOOL, ArmorType.HELMET))
    );

    public static final DeferredItem<Item> WOOL_SHIRT = ITEMS.registerItem("wool_shirt",
            props -> new Item(props.humanoidArmor(WOOL, ArmorType.CHESTPLATE))
    );

    public static final DeferredItem<Item> WOOL_PANTS = ITEMS.registerItem("wool_pants",
            props -> new Item(props.humanoidArmor(WOOL, ArmorType.LEGGINGS))
    );

    public static final DeferredItem<Item> WOOL_SHOES = ITEMS.registerItem("wool_shoes",
            props -> new Item(props.humanoidArmor(WOOL, ArmorType.BOOTS)) {
                @Override
                public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer)
                {
                    return true;
                }
            }
    );

    public static final DeferredItem<Item> COMMON_PATTERN = ITEMS.registerItem("common_pattern",
            props -> new Item(fillPatternProps(props.rarity(Rarity.COMMON)))
    );

    public static final DeferredItem<Item> UNCOMMON_PATTERN = ITEMS.registerItem("uncommon_pattern",
            props -> new Item(fillPatternProps(props.rarity(Rarity.UNCOMMON)))
    );

    public static final DeferredItem<Item> RARE_PATTERN = ITEMS.registerItem("rare_pattern",
            props -> new Item(fillPatternProps(props.rarity(Rarity.RARE)))
    );

    public static final DeferredItem<Item> LEGENDARY_PATTERN = ITEMS.registerItem("legendary_pattern",
            props -> new Item(fillPatternProps(props.rarity(Rarity.EPIC)))
    );

    private static Item.Properties fillPatternProps(Item.Properties props)
    {
        return props.component(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("text.sewingkit.pattern.wip").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                Component.translatable("text.sewingkit.pattern.may_be_removed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        )));
    }

    public static final DeferredItem<Item>
            FILE = ITEMS.registerItem("file", props -> new FileItem(props.durability(354)));

    public static final DeferredHolder<PoiType, PoiType>
            TABLE_POI = POI_TYPES.register("tailor", () -> new PoiType(Stream.concat(
            SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream(),
            STORING_SEWING_STATION_BLOCK.get().getStateDefinition().getPossibleStates().stream()
    ).collect(Collectors.toUnmodifiableSet()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession>
            TAILOR = PROFESSIONS.register("tailor", () -> {
        var key = Objects.requireNonNull(TABLE_POI.getKey());
        return new VillagerProfession(Component.translatable("entity.minecraft.villager.sewingkit.tailor"),
                holder -> holder.is(key),
                holder -> holder.is(key),
                Arrays.stream(Needles.values()).map(Needles::getNeedle).collect(ImmutableSet.toImmutableSet()),
                ImmutableSet.of(), null);
    });

    public static final RecipeBookType SEWING_BOOK_CATEGORY = Enum.valueOf(RecipeBookType.class, "SEWINGKIT_SEWING");
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory>
            SEWING_SEARCH = RECIPE_BOOK_CATEGORY.register("sewing_search", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory>
            SEWING_MISC = RECIPE_BOOK_CATEGORY.register("sewing_misc", RecipeBookCategory::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SewingRecipe>>
            SEWING = RECIPE_TYPES.register("sewing", RecipeType::simple);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SewingRecipe>>
            SEWING_RECIPE = RECIPE_SERIALIZERS.register("sewing", SewingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeDisplay.Type<?>,RecipeDisplay.Type<SewingRecipeDisplay>> SEWING_DISPLAY = RECIPE_DISPLAY.register("sewing",
            () -> new RecipeDisplay.Type<>(SewingRecipeDisplay.MAP_CODEC, SewingRecipeDisplay.STREAM_CODEC)
    );

    public static final DeferredHolder<SlotDisplay.Type<?>,SlotDisplay.Type<SewingMaterialSlotDisplay>> MATERIAL_SLOT_DISPLAY = SLOT_DISPLAY.register("material",
            () -> new SlotDisplay.Type<>(SewingMaterialSlotDisplay.MAP_CODEC, SewingMaterialSlotDisplay.STREAM_CODEC)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<SewingTableMenu>>
            SEWING_STATION_MENU = MENU_TYPES.register("sewing_station", () -> new MenuType<>(SewingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RandomDye>>
            RANDOM_DYE = LOOT_FUNCTIONS.register("random_dye", () -> new LootItemFunctionType<>(RandomDye.CODEC));

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<TailorShopProcessor>>
            TAILOR_SHOP_PROCESSOR = STRUCTURE_PROCESSORS.register("tailor_shop_processor", () -> TailorShopProcessor::codec);

    private static final ResourceKey<StructureProcessorList>
            TAILOR_SHOP_PROCESSOR_LIST_KEY = ResourceKey.create(Registries.PROCESSOR_LIST, location("tailor_shop_processors"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab>
            SEWING_KIT_TAB = CREATIVE_TABS.register("sewing_kit", () -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0)
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
        RECIPE_BOOK_CATEGORY.register(modBus);
        RECIPE_DISPLAY.register(modBus);
        SLOT_DISPLAY.register(modBus);

        modBus.addListener(this::networkSetup);

        NeoForge.EVENT_BUS.addListener(this::villagerTrades);
        NeoForge.EVENT_BUS.addListener(this::addBuildingToVillages);
        NeoForge.EVENT_BUS.addListener(this::dataSync);
    }

    private void networkSetup(RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");
        registrar.playToClient(SyncRecipeOrder.TYPE, SyncRecipeOrder.STREAM_CODEC, SyncRecipeOrder::handle);
    }

    private void dataSync(OnDatapackSyncEvent event)
    {
        event.sendRecipes(SEWING.get());
    }

    public void addBuildingToVillages(final ServerAboutToStartEvent event)
    {
        Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorListRegistry = event.getServer().registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST);

        // Adds our piece to all village houses pool
        // Note, the Identifier is getting the pool files from the data folder. Not assets folder.
        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                Identifier.parse("minecraft:village/plains/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                Identifier.parse("minecraft:village/snowy/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                Identifier.parse("minecraft:village/savanna/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                Identifier.parse("minecraft:village/taiga/houses"),
                "sewingkit:tailor_shop", 5);

        addBuildingToPool(templatePoolRegistry, processorListRegistry,
                Identifier.parse("minecraft:village/desert/houses"),
                "sewingkit:tailor_shop", 5);
    }

    private static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry,
                                          Registry<StructureProcessorList> processorListRegistry,
                                          Identifier poolRL,
                                          String nbtPieceRL,
                                          int weight)
    {
        Holder<StructureProcessorList> emptyProcessorList = processorListRegistry.getOrThrow(TAILOR_SHOP_PROCESSOR_LIST_KEY);

        StructureTemplatePool pool = templatePoolRegistry.getOptional(poolRL).orElse(null);
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
        if (event.getType() != TAILOR.getKey())
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

        @Override
        public @org.jspecify.annotations.Nullable MerchantOffer getOffer(ServerLevel serverLevel, Entity entity, RandomSource randomSource)
        {
            return BuiltInRegistries.ITEM.getRandomElementOf(tagSource, randomSource)
                    .map(itemHolder -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, price),
                            new ItemStack(itemHolder, quantity), this.maxUses, this.xp, this.priceMultiplier))
                    .orElse(null);
        }
    }

    private void gatherData(GatherDataEvent.Client event)
    {
        SewingKitDataGen.gatherData(event);
    }

    public static Identifier location(String path)
    {
        return Identifier.fromNamespaceAndPath(MODID, path);
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

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientModBus
    {
        @SubscribeEvent
        public static void registerMenuScreens(final RegisterMenuScreensEvent event)
        {
            event.register(SEWING_STATION_MENU.get(), SewingTableScreen::new);
        }
    }
}
