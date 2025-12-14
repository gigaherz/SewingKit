package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.ClientSewingRecipeAccessor;
import dev.gigaherz.sewingkit.api.SewingMaterial;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.network.SyncRecipeOrder;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class SewingTableMenu extends RecipeBookMenu
{
    private static final int NUM_INPUTS = 6;
    private static final int NUM_OUTPUTS = 1;
    private static final int NUM_INVENTORY = 9 * 3;
    private static final int NUM_HOTBAR = 9;
    private static final int OUTPUTS_START = NUM_INPUTS;
    private static final int PLAYER_START = OUTPUTS_START + NUM_OUTPUTS;
    private static final int HOTBAR_START = PLAYER_START + NUM_INVENTORY;
    private static final int PLAYER_END = HOTBAR_START + NUM_HOTBAR;

    private final Level level;
    private final ContainerLevelAccess openedFrom;
    private final DataSlot selectedRecipe = DataSlot.standalone();
    private final ItemStack[] inputStacksCache = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    private final InventoryProvider inventoryProvider;
    private final Player player;
    private List<RecipeHolder<SewingRecipe>> recipes = Lists.newArrayList();
    private List<RecipeHolder<SewingRecipe>> oldRecipes = null;
    private long lastTimeSoundPlayed;

    private Runnable inventoryUpdateListener = () -> {
    };

    public Container inputInventory;
    /**
     * The inventory that stores the output of the crafting recipe.
     */
    private final ResultContainer inventory = new ResultContainer();

    public SewingTableMenu(int windowIdIn, Inventory playerInventoryIn)
    {
        this(windowIdIn, playerInventoryIn, ContainerLevelAccess.NULL);
    }

    public SewingTableMenu(int windowIdIn, Inventory playerInventoryIn, final ContainerLevelAccess worldPosCallableIn)
    {
        this(windowIdIn, playerInventoryIn, worldPosCallableIn, new SewingTableInventory());
    }

    public SewingTableMenu(int windowIdIn, Inventory playerInventoryIn, final ContainerLevelAccess worldPosCallableIn, InventoryProvider inventoryProvider)
    {
        super(SewingKitMod.SEWING_STATION_MENU.get(), windowIdIn);
        this.openedFrom = worldPosCallableIn;
        this.player = playerInventoryIn.player;
        this.level = playerInventoryIn.player.level();
        this.inputInventory = inventoryProvider.getInventory();
        this.inventoryProvider = inventoryProvider;
        inventoryProvider.addWeakListener(this);

        this.addSlot(new Slot(this.inputInventory, 0, 8, 15)
        {
            {
                this.setBackground(SewingKitMod.location("needle_slot_background"));
            }
        });
        this.addSlot(new Slot(this.inputInventory, 1, 30, 15)
        {
            {
                this.setBackground(SewingKitMod.location("pattern_slot_background"));
            }
        });
        this.addSlot(new Slot(this.inputInventory, 2, 10, 35));
        this.addSlot(new Slot(this.inputInventory, 3, 28, 35));
        this.addSlot(new Slot(this.inputInventory, 4, 10, 53));
        this.addSlot(new Slot(this.inputInventory, 5, 28, 53));
        this.addSlot(new Slot(this.inventory, 1, 143, 33)
        {
            /**
             * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
             */
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }

            public void onTake(Player player, ItemStack stack)
            {
                if (player instanceof ServerPlayer serverPlayer)
                {
                    stack.onCraftedBy(player, stack.getCount());

                    List<ItemStack> consumed = new ArrayList<>();

                    SewingRecipe recipe = recipes.get(getSelectedRecipe()).value();
                    Map<Ingredient, Integer> remaining = recipe.materials().stream().collect(Collectors.toMap(SewingMaterial::ingredient, SewingMaterial::count));
                    if (consumeCraftingMaterials(serverPlayer, remaining, consumed))
                    {
                        updateRecipeResultSlot();
                    }


                    SewingTableMenu.this.inventory.awardUsedRecipes(player, consumed);

                    worldPosCallableIn.execute((world, pos) -> {
                        long l = world.getGameTime();
                        if (lastTimeSoundPlayed != l)
                        {
                            world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                            lastTimeSoundPlayed = l;
                        }
                    });

                    onInventoryChanged();
                }
                super.onTake(player, stack);
            }
        });

        bindPlayerInventory(playerInventoryIn);

        this.addDataSlot(this.selectedRecipe);

        onInventoryChanged();
    }

    private boolean consumeCraftingMaterials(ServerPlayer serverPlayer, Map<Ingredient, Integer> remaining, List<ItemStack> consumed)
    {
        boolean needsUpdate = false;
        for (int i = 0; i < 6; i++)
        {
            Slot slot = slots.get(i);
            ItemStack itemstack;
            if (i == 0)
            {
                slot.getItem().hurtAndBreak(1, (ServerLevel) serverPlayer.level(), serverPlayer, item -> {
                    slot.set(ItemStack.EMPTY);
                });
                itemstack = slot.getItem();
            }
            else if (i == 1)
            {
                itemstack = ItemStack.EMPTY;
            }
            else
            {
                int subtract = 0;
                for (Map.Entry<Ingredient, Integer> mat : remaining.entrySet())
                {
                    Ingredient ing = mat.getKey();
                    int value = mat.getValue();
                    ItemStack stack1 = slot.getItem();
                    if (ing.test(stack1))
                    {
                        consumed.add(stack1.copy());
                        int remaining1 = Math.max(0, value - (stack1.getCount() + subtract));
                        subtract += (value - remaining1);
                        mat.setValue(remaining1);
                    }
                }
                itemstack = slot.remove(subtract);
            }
            if (!itemstack.isEmpty())
            {
                needsUpdate = true;
            }
        }
        return needsUpdate;
    }

    private void bindPlayerInventory(Inventory playerInventoryIn)
    {
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlot(new Slot(playerInventoryIn, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k)
        {
            this.addSlot(new Slot(playerInventoryIn, k, 8 + k * 18, 142));
        }
    }

    public void onInventoryChanged()
    {
        slotsChanged(null);
        inventoryUpdateListener.run();
    }

    public int getSelectedRecipe()
    {
        return this.selectedRecipe.get();
    }

    public List<RecipeHolder<SewingRecipe>> getRecipeList()
    {
        return this.recipes;
    }

    public int getRecipeListSize()
    {
        return this.recipes.size();
    }

    public boolean hasItemsinInputSlots()
    {
        if (inputInventory == null) return false;
        return slots.stream().skip(2).limit(4).anyMatch(Slot::hasItem);
    }

    public boolean isAbleToCraft()
    {
        return hasItemsinInputSlots() && !this.recipes.isEmpty();
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(Player playerIn)
    {
        return isWithinUsableDistance(this.openedFrom, playerIn, SewingKitMod.SEWING_STATION_BLOCK.get(), SewingKitMod.STORING_SEWING_STATION_BLOCK.get());
    }

    protected static boolean isWithinUsableDistance(ContainerLevelAccess worldPos, Player playerIn, Block... targetBlocks)
    {
        return worldPos.evaluate((world, pos) -> {
            BlockState blockState = world.getBlockState(pos);
            if (Arrays.stream(targetBlocks).noneMatch(blockState::is)) return false;

            return playerIn.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    /**
     * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
     */
    public boolean clickMenuButton(Player playerIn, int id)
    {
        if (this.isValidRecipeIndex(id))
        {
            this.selectedRecipe.set(id);
            this.updateRecipeResultSlot();
        }

        return true;
    }

    private boolean isValidRecipeIndex(int p_241818_1_)
    {
        return p_241818_1_ >= 0 && p_241818_1_ < this.recipes.size();
    }

    @Override
    public void slotsChanged(Container unused)
    {
        super.slotsChanged(unused);

        boolean anyChanged = false;
        for (int i = 0; i < 6; i++)
        {
            ItemStack itemstack = slots.get(i).getItem();
            if (!ItemStack.matches(itemstack, this.inputStacksCache[i]))
            {
                this.inputStacksCache[i] = itemstack.copy();
                anyChanged = true;
            }
        }
        if (anyChanged)
            this.updateAvailableRecipes();
    }

    @Override
    public void broadcastFullState()
    {
        super.broadcastFullState();

        sendOrderedRecipes();
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (this.recipes != this.oldRecipes)
        {
            sendOrderedRecipes();
            oldRecipes = recipes;
        }
    }

    private void updateAvailableRecipes()
    {
        if (level.isClientSide()) return;

        SewingRecipe recipe = getSelectedRecipe() >= 0 && !recipes.isEmpty() ? recipes.get(getSelectedRecipe()).value() : null;
        this.recipes = Lists.newArrayList();
        this.selectedRecipe.set(-1);
        this.slots.get(OUTPUTS_START).set(ItemStack.EMPTY);
        if (hasItemsinInputSlots())
        {
            var input = SewingInput.ofSewingTableInventory(inputInventory);
            this.recipes = getRecipes(input, level);
        }
        if (!recipes.isEmpty() && recipe != null)
        {
            int index = recipes.indexOf(recipe);
            if (index >= 0)
            {
                selectedRecipe.set(index);
                updateRecipeResultSlot();
            }
        }
        //sendOrderedRecipes();
    }

    public static List<RecipeHolder<SewingRecipe>> getRecipes(SewingInput input, Level level)
    {
        var recipeMap = level.getServer().getRecipeManager().recipeMap();
        var recipes = recipeMap.byType(SewingKitMod.SEWING.get());
        return recipes.stream().filter(
                recipe -> recipe.value().matches(input, level)
        ).toList();
    }

    private void sendOrderedRecipes()
    {
        if (this.player instanceof ServerPlayer sp)
            PacketDistributor.sendToPlayer(sp,
                    new SyncRecipeOrder(containerId,
                            recipes.stream().map(r -> r.id().identifier()).toList()));
    }

    public void setOrderedRecipes(List<Identifier> recipes)
    {
        var allRecipes = ClientSewingRecipeAccessor.getRecipesByName(this.level);
        this.recipes = recipes.stream().map(allRecipes::get).toList();

        onInventoryChanged();
    }

    private void updateRecipeResultSlot()
    {
        if (inputInventory == null) return;
        if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipe.get()))
        {
            var sewingRecipe = this.recipes.get(this.selectedRecipe.get());
            this.inventory.setRecipeUsed(sewingRecipe);
            var input = SewingInput.ofSewingTableInventory(inputInventory);
            this.slots.get(OUTPUTS_START).set(sewingRecipe.value().assemble(input, level.registryAccess()));
        }
        else
        {
            this.slots.get(OUTPUTS_START).set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    public void setInventoryUpdateListener(Runnable listenerIn)
    {
        this.inventoryUpdateListener = listenerIn;
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn)
    {
        return slotIn.container != this.inventory && super.canTakeItemForPickAll(stack, slotIn);
    }

    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        Item item = stackInSlot.getItem();
        ItemStack stackCopy = stackInSlot.copy();

        int startIndex = PLAYER_START;
        int endIndex = PLAYER_END;
        boolean reverse = false;
        boolean notify = false;
        if (index == OUTPUTS_START)
        {
            // Drop onto player inventory, reverse fill order.
            reverse = true;
            notify = true;
        }
        else if (index >= OUTPUTS_START)
        {
            if (stackInSlot.getMaxStackSize() == 1 && slots.get(0).getItem().isEmpty())
            {
                startIndex = 0;
                endIndex = 1;
            }
            /*else if (stackInSlot.getItem().isIn(SewingKitMod.PATTERNS_TAG))
            {
                startIndex = 1;e
                endIndex = 2;
            }*/
            else
            {
                startIndex = 2;
                endIndex = OUTPUTS_START;
            }
        }

        if (endIndex > startIndex)
        {
            if (notify) item.onCraftedBy(stackInSlot, player);
            if (!this.moveItemStackTo(stackInSlot, startIndex, endIndex, reverse))
            {
                return ItemStack.EMPTY;
            }
            if (notify) slot.onQuickCraft(stackInSlot, stackCopy);
        }

        if (stackInSlot.isEmpty())
        {
            slot.set(ItemStack.EMPTY);
        }

        slot.setChanged();
        if (stackInSlot.getCount() == stackCopy.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        this.broadcastChanges();

        return stackCopy;
    }

    public void removed(Player playerIn)
    {
        super.removed(playerIn);
        this.inventory.removeItemNoUpdate(0);
        if (inventoryProvider.isDummy())
        {
            this.openedFrom.execute((world, pos) -> this.returnAllItemsToPlayer(playerIn));
        }
    }

    protected void returnAllItemsToPlayer(Player pPlayer)
    {
        if (inputInventory == null) return;
        if (!pPlayer.isAlive() || pPlayer instanceof ServerPlayer && ((ServerPlayer) pPlayer).hasDisconnected())
        {
            for (int j = 0; j < inputInventory.getContainerSize(); j++)
            {
                pPlayer.drop(inputInventory.getItem(j), false);
            }
        }
        else
        {
            for (int i = 0; i < inputInventory.getContainerSize(); i++)
            {
                Inventory inventory = pPlayer.getInventory();
                if (inventory.player instanceof ServerPlayer)
                {
                    inventory.placeItemBackInInventory(inputInventory.getItem(i));
                }
            }
        }
        inputInventory = null;
    }

    // Recipe book stuffs
    public boolean isCraftingSlot(Slot slot)
    {
        return this.slots.indexOf(slot) < 6;
    }

    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(
            boolean useMaxItems, boolean isCreative, RecipeHolder<?> recipe, final ServerLevel level, Inventory playerInventory
    )
    {
        if (!isCreative && !testClearGrid(playerInventory))
        {
            return PostPlaceAction.NOTHING;
        }
        else
        {
            StackedItemContents stackeditemcontents = new StackedItemContents();
            playerInventory.fillStackedContents(stackeditemcontents);
            SewingTableMenu.this.fillCraftSlotsStackedContents(stackeditemcontents);
            //noinspection unchecked
            return tryPlaceRecipe(playerInventory, (RecipeHolder<SewingRecipe>) recipe, stackeditemcontents, useMaxItems);
        }
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedItemContents)
    {
        if (this.inputInventory instanceof StackedContentsCompatible stackedContentsCompatible)
        {
            stackedContentsCompatible.fillStackedContents(stackedItemContents);
        }
    }

    @Override
    public RecipeBookType getRecipeBookType()
    {
        return SewingKitMod.SEWING_BOOK_CATEGORY;
    }

    private boolean testClearGrid(Inventory playerInventory)
    {
        List<ItemStack> list = Lists.newArrayList();
        int freeSlots = getAmountOfFreeSlotsInInventory(playerInventory);

        for (int i = 0; i < 6; i++)
        {
            var slot = SewingTableMenu.this.getSlot(i);
            ItemStack itemstack = slot.getItem().copy();
            if (!itemstack.isEmpty())
            {
                int j = playerInventory.getSlotWithRemainingSpace(itemstack);
                if (j == ITEM_NOT_FOUND && list.size() <= freeSlots)
                {
                    for (ItemStack itemstack1 : list)
                    {
                        if (ItemStack.isSameItem(itemstack1, itemstack)
                                && itemstack1.getCount() != itemstack1.getMaxStackSize()
                                && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize())
                        {
                            itemstack1.grow(itemstack.getCount());
                            itemstack.setCount(0);
                            break;
                        }
                    }

                    if (!itemstack.isEmpty())
                    {
                        if (list.size() >= freeSlots)
                        {
                            return false;
                        }

                        list.add(itemstack);
                    }
                }
                else if (j == ITEM_NOT_FOUND)
                {
                    return false;
                }
            }
        }

        return true;
    }

    private static int getAmountOfFreeSlotsInInventory(Inventory platerInventory)
    {
        int i = 0;

        for (ItemStack itemstack : platerInventory.getNonEquipmentItems())
        {
            if (itemstack.isEmpty())
            {
                i++;
            }
        }

        return i;
    }

    public static final int ITEM_NOT_FOUND = -1;

    private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(Inventory inventory, RecipeHolder<SewingRecipe> recipe, StackedItemContents stackedItemContents, boolean useMaxItems)
    {
        if (stackedItemContents.canCraft(recipe.value(), null))
        {
            this.placeRecipe(inventory, recipe, stackedItemContents, useMaxItems);
            inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        else
        {
            this.clearGrid(inventory);
            inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }
    }

    private void clearGrid(Inventory inventory)
    {
        for (int i = 0; i < 6; i++)
        {
            var slot = SewingTableMenu.this.getSlot(i);
            ItemStack itemstack = slot.getItem().copy();
            inventory.placeItemBackInInventory(itemstack, false);
            slot.set(itemstack);
        }

        SewingTableMenu.this.inputInventory.clearContent();
    }

    private void placeRecipe(Inventory inventory, RecipeHolder<SewingRecipe> recipeHolder, StackedItemContents stackedItemContents, boolean useMaxItems)
    {
        var recipe = recipeHolder.value();
        var tool = recipe.tool();
        var pattern = recipe.pattern();
        var materials = recipe.materials();

        var input = SewingInput.ofSewingTableInventory(inputInventory);

        boolean recipeMatches = recipe.matches(input, level);

        Int2IntArrayMap slotToAmount = new Int2IntArrayMap();
        int amountToCraft = getMaxCraft(tool, pattern, materials, stackedItemContents, slotToAmount);

        if (!useMaxItems)
        {
            amountToCraft = 1;
            if (recipeMatches) // try to craft one additional item to however many can be crafted with whatever is already on the table
            {
                var stacked = new StackedItemContents();

                for (int i = 0; i < 6; i++)
                {
                    var slot2 = this.getSlot(i);
                    stacked.accountStack(slot2.getItem());
                }

                var amountCraftable = getMaxCraft(tool, pattern, materials, stacked, slotToAmount);

                if (amountCraftable < amountToCraft && canFitInGrid(materials, amountCraftable + 1))
                    amountToCraft = amountCraftable + 1;
            }
        }

        List<Holder<Item>> list = new ArrayList<>();
        if (tryPickItemsForCraft(stackedItemContents, recipe, amountToCraft, list))
            return;

        int amount = amountToCraft;
        for (Holder<Item> holder1 : list)
        {
            amount = Math.min(amount, holder1.value().getDefaultMaxStackSize());
        }

        if (amount != amountToCraft)
        {
            list.clear();
            if (tryPickItemsForCraft(stackedItemContents, recipe, amount, list))
                return;
        }

        this.clearGrid(inventory);

        var iterator = recipe.placementInfo().slotsToIngredientIndex().iterator();
        for (int i = 0; i < 6; i++) {
            if (!iterator.hasNext()) {
                return;
            }
            var item = iterator.nextInt();
            if (item != ITEM_NOT_FOUND)
            {
                Slot targetSlot = SewingTableMenu.this.getSlot(i);
                Holder<Item> holder = list.get(item);
                int countPerItem = slotToAmount.getOrDefault(i, 0);
                int remaining = amount * countPerItem;
                while (remaining > 0)
                {
                    var inSlot = targetSlot.getItem();
                    int matchingSlot = inventory.findSlotMatchingCraftingIngredient(holder, inSlot);

                    int result;
                    if (matchingSlot == ITEM_NOT_FOUND)
                    {
                        result = ITEM_NOT_FOUND;
                    }
                    else
                    {
                        ItemStack itemstack1 = inventory.getItem(matchingSlot);
                        ItemStack itemstack2 = remaining < itemstack1.getCount()
                                ? inventory.removeItem(matchingSlot, remaining)
                                : inventory.removeItemNoUpdate(matchingSlot);

                        int j = itemstack2.getCount();
                        if (inSlot.isEmpty())
                        {
                            targetSlot.set(itemstack2);
                        }
                        else
                        {
                            inSlot.grow(j);
                        }

                        result = remaining - j;
                    }
                    remaining = result;
                    if (remaining == ITEM_NOT_FOUND)
                    {
                        return;
                    }
                }
            }
        }
    }

    private boolean tryPickItemsForCraft(StackedItemContents stackedItemContents, SewingRecipe recipe, int amountToCraft, List<Holder<Item>> list)
    {
        IntList amounts = new IntArrayList();
        if (recipe.tool() != null ) amounts.add(-1);
        if (recipe.pattern() != null ) amounts.add(-1);
        for(var mat : recipe.materials())
            amounts.add(mat.count());

        PlacementInfo placementinfo = recipe.placementInfo();
        if (placementinfo.isImpossibleToPlace()) return true;
        var ingredients = placementinfo.ingredients();
        var picker = new CustomRecipePicker<>(stackedItemContents.raw, ingredients, amounts);
        return !picker.tryPick(amountToCraft, list::add);
    }

    private static class CustomRecipePicker<T>
    {
        private final StackedContents<T> raw;
        private final List<? extends StackedContents.IngredientInfo<T>> ingredients;
        private final int ingredientCount;
        private final List<T> items;
        private final IntList amountsList;
        private final int itemCount;
        private final BitSet data;
        private final IntList path = new IntArrayList();

        public CustomRecipePicker(StackedContents<T> raw, List<? extends StackedContents.IngredientInfo<T>> ingredients, IntList amountsList) {
            this.raw = raw;
            this.ingredients = ingredients;
            this.ingredientCount = ingredients.size();
            this.items = this.getUniqueAvailableIngredientItems(ingredients);
            this.amountsList = amountsList;
            this.itemCount = this.items.size();
            this.data = new BitSet(
                    this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount()
            );
            this.setInitialConnections();
        }

        List<T> getUniqueAvailableIngredientItems(Iterable<? extends StackedContents.IngredientInfo<T>> ingredients) {
            List<T> list = new ArrayList<>();

            for (Reference2IntMap.Entry<T> entry : Reference2IntMaps.fastIterable(this.raw.amounts)) {
                if (entry.getIntValue() > 0 && anyIngredientMatches(ingredients, entry.getKey())) {
                    list.add(entry.getKey());
                }
            }

            return list;
        }

        private static <T> boolean anyIngredientMatches(Iterable<? extends StackedContents.IngredientInfo<T>> ingredients, T item) {
            for (StackedContents.IngredientInfo<T> ingredientinfo : ingredients) {
                if (ingredientinfo.acceptsItem(item)) {
                    return true;
                }
            }

            return false;
        }

        private void setInitialConnections() {
            for (int i = 0; i < this.ingredientCount; i++) {
                StackedContents.IngredientInfo<T> ingredientinfo = this.ingredients.get(i);

                for (int j = 0; j < this.itemCount; j++) {
                    if (ingredientinfo.acceptsItem(this.items.get(j))) {
                        this.setConnection(j, i);
                    }
                }
            }
        }

        public boolean tryPick(int amount, @javax.annotation.Nullable StackedContents.Output<T> output) {
            if (amount <= 0) {
                return true;
            } else {
                int i = 0;

                while (true) {
                    var ingredientAmount = i < ingredientCount ? amountsList.getInt(i) < 0 ? -amountsList.getInt(i) :  amount * amountsList.getInt(i) : amount;
                    IntList intlist = this.tryAssigningNewItem(ingredientAmount);
                    if (intlist == null) {
                        boolean flag = i == this.ingredientCount;
                        boolean flag1 = flag && output != null;
                        this.clearAllVisited();
                        this.clearSatisfied();

                        for (int k1 = 0; k1 < this.ingredientCount; k1++) {
                            var ingredientAmount1 = amountsList.getInt(k1) < 0 ? -amountsList.getInt(k1) :  amount * amountsList.getInt(k1);
                            for (int l1 = 0; l1 < this.itemCount; l1++) {
                                if (this.isAssigned(l1, k1)) {
                                    this.unassign(l1, k1);
                                    raw.put(this.items.get(l1), ingredientAmount1);
                                    if (flag1) {
                                        output.accept(this.items.get(l1));
                                    }
                                    break;
                                }
                            }
                        }

                        assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();

                        return flag;
                    }

                    int j = intlist.getInt(0);
                    raw.take(this.items.get(j), ingredientAmount);
                    int k = intlist.size() - 1;
                    this.setSatisfied(intlist.getInt(k));
                    i++;

                    for (int l = 0; l < intlist.size() - 1; l++) {
                        if (isPathIndexItem(l)) {
                            int i1 = intlist.getInt(l);
                            int j1 = intlist.getInt(l + 1);
                            this.assign(i1, j1);
                        } else {
                            int i2 = intlist.getInt(l + 1);
                            int j2 = intlist.getInt(l);
                            this.unassign(i2, j2);
                        }
                    }
                }
            }
        }

        private static boolean isPathIndexItem(int index) {
            return (index & 1) == 0;
        }

        @javax.annotation.Nullable
        private IntList tryAssigningNewItem(int amount) {
            this.clearAllVisited();

            for (int i = 0; i < this.itemCount; i++) {
                if (raw.hasAtLeast(this.items.get(i), amount)) {
                    IntList intlist = this.findNewItemAssignmentPath(i);
                    if (intlist != null) {
                        return intlist;
                    }
                }
            }

            return null;
        }

        @javax.annotation.Nullable
        private IntList findNewItemAssignmentPath(int amount) {
            this.path.clear();
            this.visitItem(amount);
            this.path.add(amount);

            while (!this.path.isEmpty()) {
                int i = this.path.size();
                if (isPathIndexItem(i - 1)) {
                    int l = this.path.getInt(i - 1);

                    for (int j1 = 0; j1 < this.ingredientCount; j1++) {
                        if (!this.hasVisitedIngredient(j1) && this.hasConnection(l, j1) && !this.isAssigned(l, j1)) {
                            this.visitIngredient(j1);
                            this.path.add(j1);
                            break;
                        }
                    }
                } else {
                    int j = this.path.getInt(i - 1);
                    if (!this.isSatisfied(j)) {
                        return this.path;
                    }

                    for (int k = 0; k < this.itemCount; k++) {
                        if (!this.hasVisitedItem(k) && this.isAssigned(k, j)) {
                            assert this.hasConnection(k, j);

                            this.visitItem(k);
                            this.path.add(k);
                            break;
                        }
                    }
                }

                int i1 = this.path.size();
                if (i1 == i) {
                    this.path.removeInt(i1 - 1);
                }
            }

            return null;
        }

        private int visitedIngredientOffset() {
            return 0;
        }

        private int visitedIngredientCount() {
            return this.ingredientCount;
        }

        private int visitedItemOffset() {
            return this.visitedIngredientOffset() + this.visitedIngredientCount();
        }

        private int visitedItemCount() {
            return this.itemCount;
        }

        private int satisfiedOffset() {
            return this.visitedItemOffset() + this.visitedItemCount();
        }

        private int satisfiedCount() {
            return this.ingredientCount;
        }

        private int connectionOffset() {
            return this.satisfiedOffset() + this.satisfiedCount();
        }

        private int connectionCount() {
            return this.ingredientCount * this.itemCount;
        }

        private int residualOffset() {
            return this.connectionOffset() + this.connectionCount();
        }

        private int residualCount() {
            return this.ingredientCount * this.itemCount;
        }

        private boolean isSatisfied(int stackingIndex) {
            return this.data.get(this.getSatisfiedIndex(stackingIndex));
        }

        private void setSatisfied(int stackingIndex) {
            this.data.set(this.getSatisfiedIndex(stackingIndex));
        }

        private int getSatisfiedIndex(int stackingIndex) {
            assert stackingIndex >= 0 && stackingIndex < this.ingredientCount;

            return this.satisfiedOffset() + stackingIndex;
        }

        private void clearSatisfied() {
            this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
        }

        private void setConnection(int itemIndex, int ingredientIndex) {
            this.data.set(this.getConnectionIndex(itemIndex, ingredientIndex));
        }

        private boolean hasConnection(int itemIndex, int ingredientIndex) {
            return this.data.get(this.getConnectionIndex(itemIndex, ingredientIndex));
        }

        private int getConnectionIndex(int itemIndex, int ingredientIndex) {
            assert itemIndex >= 0 && itemIndex < this.itemCount;

            assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

            return this.connectionOffset() + itemIndex * this.ingredientCount + ingredientIndex;
        }

        private boolean isAssigned(int itemIndex, int ingredientIndex) {
            return this.data.get(this.getResidualIndex(itemIndex, ingredientIndex));
        }

        private void assign(int itemIndex, int ingredientIndex) {
            int i = this.getResidualIndex(itemIndex, ingredientIndex);

            assert !this.data.get(i);

            this.data.set(i);
        }

        private void unassign(int itemIndex, int ingredientIndex) {
            int i = this.getResidualIndex(itemIndex, ingredientIndex);

            assert this.data.get(i);

            this.data.clear(i);
        }

        private int getResidualIndex(int itemIndex, int ingredientIndex) {
            assert itemIndex >= 0 && itemIndex < this.itemCount;

            assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

            return this.residualOffset() + itemIndex * this.ingredientCount + ingredientIndex;
        }

        private void visitIngredient(int ingredientIndex) {
            this.data.set(this.getVisitedIngredientIndex(ingredientIndex));
        }

        private boolean hasVisitedIngredient(int ingredientIndex) {
            return this.data.get(this.getVisitedIngredientIndex(ingredientIndex));
        }

        private int getVisitedIngredientIndex(int ingredientIndex) {
            assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;

            return this.visitedIngredientOffset() + ingredientIndex;
        }

        private void visitItem(int itemIndex) {
            this.data.set(this.getVisitiedItemIndex(itemIndex));
        }

        private boolean hasVisitedItem(int itemIndex) {
            return this.data.get(this.getVisitiedItemIndex(itemIndex));
        }

        private int getVisitiedItemIndex(int itemIndex) {
            assert itemIndex >= 0 && itemIndex < this.itemCount;

            return this.visitedItemOffset() + itemIndex;
        }

        private void clearAllVisited() {
            this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
            this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
        }

        private void clearRange(int offset, int count) {
            this.data.clear(offset, offset + count);
        }
    }

    private boolean canFitInGrid(NonNullList<SewingMaterial> materials, int quantity)
    {
        // FIXME: allow empty slot to contain one additional stack of one material

        for (SewingMaterial mat : materials)
        {
            var ingredient = mat.ingredient();

            for (int i = 2; i < 6; i++)
            {
                var inSlot = getSlot(i).getItem();

                if (ingredient.test(inSlot))
                {
                    if (mat.count() * quantity > inSlot.getMaxStackSize())
                        return false;
                    break;
                }
            }
        }

        return true;
    }

    private static int getMaxCraft(@Nullable Ingredient tool, @Nullable Ingredient pattern, NonNullList<SewingMaterial> materials,
                                   StackedItemContents stackedItemContents, Int2IntArrayMap slotToAmount)
    {
        var am = stackedItemContents.raw.amounts;

        int size = Integer.MAX_VALUE;

        slotToAmount.clear();

        if (tool != null)
        {
            slotToAmount.put(0, 1);
            if (am.reference2IntEntrySet().stream().noneMatch(kv -> tool.acceptsItem(kv.getKey()) && kv.getIntValue() > 0))
            {
                size = 0;
            }
        }

        if (size > 0 && pattern != null)
        {
            slotToAmount.put(1, 1);
            if (am.reference2IntEntrySet().stream().noneMatch(kv -> pattern.acceptsItem(kv.getKey()) && kv.getIntValue() > 0))
            {
                size = 0;
            }
        }

        for (int i = 0; size > 0 && i < materials.size(); i++)
        {
            var mat = materials.get(i);
            slotToAmount.put(i+2, mat.count());
            int count = am.reference2IntEntrySet().stream().mapToInt(kv -> mat.ingredient().acceptsItem(kv.getKey()) ? kv.getIntValue() : 0).sum();
            count /= mat.count();
            size = Math.min(size, count);
        }
        return size;
    }
}
