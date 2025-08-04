package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.ClientSewingRecipeAccessor;
import dev.gigaherz.sewingkit.network.SyncRecipeOrder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

public class SewingTableMenu extends AbstractContainerMenu
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

    public IItemHandlerModifiable inputInventory;
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

        this.addSlot(new SlotItemHandler(this.inputInventory, 0, 8, 15)
        {
            {
                this.setBackground(SewingKitMod.location("needle_slot_background"));
            }
        });
        this.addSlot(new SlotItemHandler(this.inputInventory, 1, 30, 15)
        {
            {
                this.setBackground(SewingKitMod.location("pattern_slot_background"));
            }
        });
        this.addSlot(new SlotItemHandler(this.inputInventory, 2, 10, 35));
        this.addSlot(new SlotItemHandler(this.inputInventory, 3, 28, 35));
        this.addSlot(new SlotItemHandler(this.inputInventory, 4, 10, 53));
        this.addSlot(new SlotItemHandler(this.inputInventory, 5, 28, 53));
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
                    Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(SewingRecipe.Material::ingredient, SewingRecipe.Material::count));
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
        if (level.isClientSide) return;

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
                            recipes.stream().map(r -> r.id().location()).toList()));
    }

    public void setOrderedRecipes(List<ResourceLocation> recipes)
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
            if (stackInSlot.getMaxStackSize() == 1 && slots.get(0).getItem().getCount() == 0)
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
            for (int j = 0; j < inputInventory.getSlots(); j++)
            {
                pPlayer.drop(inputInventory.getStackInSlot(j), false);
            }
        }
        else
        {
            for (int i = 0; i < inputInventory.getSlots(); i++)
            {
                Inventory inventory = pPlayer.getInventory();
                if (inventory.player instanceof ServerPlayer)
                {
                    inventory.placeItemBackInInventory(inputInventory.getStackInSlot(i));
                }
            }
        }
        inputInventory = null;
    }
}
