package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SewingTableContainer extends Container
{
    @ObjectHolder("sewingkit:sewing_station")
    public static ContainerType<SewingTableContainer> TYPE;

    private static final int NUM_INPUTS = 6;
    private static final int NUM_OUTPUTS = 1;
    private static final int NUM_INVENTORY = 9 * 3;
    private static final int NUM_HOTBAR = 9;
    private static final int OUTPUTS_START = NUM_INPUTS;
    private static final int PLAYER_START = OUTPUTS_START + NUM_OUTPUTS;
    private static final int HOTBAR_START = PLAYER_START + NUM_INVENTORY;
    private static final int PLAYER_END = HOTBAR_START + NUM_HOTBAR;

    private final World world;
    private final IWorldPosCallable openedFrom;
    private final IntReferenceHolder selectedRecipe = IntReferenceHolder.single();
    private final ItemStack[] inputStacksCache = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    @Nullable
    private final StoringSewingTableTileEntity te;
    private List<SewingRecipe> recipes = Lists.newArrayList();
    private long lastTimeSoundPlayed;

    private Runnable inventoryUpdateListener = () -> {
    };

    public final IItemHandlerModifiable inputInventory;
    /**
     * The inventory that stores the output of the crafting recipe.
     */
    private final CraftResultInventory inventory = new CraftResultInventory();

    public SewingTableContainer(int windowIdIn, PlayerInventory playerInventoryIn)
    {
        this(windowIdIn, playerInventoryIn, IWorldPosCallable.DUMMY);
    }

    public SewingTableContainer(int windowIdIn, PlayerInventory playerInventoryIn, final IWorldPosCallable worldPosCallableIn)
    {
        this(windowIdIn, playerInventoryIn, worldPosCallableIn, null);
    }

    public SewingTableContainer(int windowIdIn, PlayerInventory playerInventoryIn, final IWorldPosCallable worldPosCallableIn, StoringSewingTableTileEntity te)
    {
        super(TYPE, windowIdIn);
        this.openedFrom = worldPosCallableIn;
        this.world = playerInventoryIn.player.world;
        this.te = te;
        if (te != null)
        {
            this.inputInventory = te.getInventory();
            te.addWeakListener(this);
        }
        else
        {
            this.inputInventory = new ItemStackHandler(6)
            {
                @Override
                protected void onContentsChanged(int slot)
                {
                    super.onContentsChanged(slot);
                    onInventoryChanged();
                }
            };
        }

        this.addSlot(new SlotItemHandler(this.inputInventory, 0, 8, 15)
        {
            {
                this.setBackground(PlayerContainer.LOCATION_BLOCKS_TEXTURE, SewingKitMod.location("gui/needle_slot_background"));
            }
        });
        this.addSlot(new SlotItemHandler(this.inputInventory, 1, 30, 15)
        {
            {
                this.setBackground(PlayerContainer.LOCATION_BLOCKS_TEXTURE, SewingKitMod.location("gui/pattern_slot_background"));
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
            public boolean isItemValid(ItemStack stack)
            {
                return false;
            }

            public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack)
            {
                SewingRecipe recipe = recipes.get(getSelectedRecipe());
                Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));
                stack.onCrafting(thePlayer.world, thePlayer, stack.getCount());
                SewingTableContainer.this.inventory.onCrafting(thePlayer);
                boolean needsUpdate = false;
                for (int i = 0; i < 6; i++)
                {
                    Slot slot = inventorySlots.get(i);
                    ItemStack itemstack;
                    if (i == 0)
                    {
                        slot.getStack().damageItem(1, thePlayer, player -> {
                            slot.decrStackSize(1);
                        });
                        itemstack = slot.getStack();
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
                            ItemStack stack1 = slot.getStack();
                            if (ing.test(stack1))
                            {
                                int remaining1 = Math.max(0, value - (stack1.getCount() + subtract));
                                subtract += (value - remaining1);
                                mat.setValue(remaining1);
                            }
                        }
                        itemstack = slot.decrStackSize(subtract);
                    }
                    if (!itemstack.isEmpty())
                    {
                        needsUpdate = true;
                    }
                }
                if (needsUpdate)
                {
                    updateRecipeResultSlot();
                }

                worldPosCallableIn.consume((world, pos) -> {
                    long l = world.getGameTime();
                    if (lastTimeSoundPlayed != l)
                    {
                        world.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        lastTimeSoundPlayed = l;
                    }
                });
                return super.onTake(thePlayer, stack);
            }
        });

        bindPlayerInventory(playerInventoryIn);

        this.trackInt(this.selectedRecipe);
    }

    private void bindPlayerInventory(PlayerInventory playerInventoryIn)
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
        onCraftMatrixChanged(new RecipeWrapper(inputInventory));
        inventoryUpdateListener.run();
    }

    public int getSelectedRecipe()
    {
        return this.selectedRecipe.get();
    }

    public List<SewingRecipe> getRecipeList()
    {
        return this.recipes;
    }

    public int getRecipeListSize()
    {
        return this.recipes.size();
    }

    public boolean hasItemsinInputSlots()
    {
        return inventorySlots.stream().skip(2).limit(4).anyMatch(Slot::getHasStack);
    }

    public boolean isAbleToCraft()
    {
        return hasItemsinInputSlots() && !this.recipes.isEmpty();
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(PlayerEntity playerIn)
    {
        return isWithinUsableDistance(this.openedFrom, playerIn, SewingKitMod.SEWING_STATION_BLOCK.get(), SewingKitMod.STORING_SEWING_STATION_BLOCK.get());
    }

    protected static boolean isWithinUsableDistance(IWorldPosCallable worldPos, PlayerEntity playerIn, Block... targetBlocks)
    {
        return worldPos.applyOrElse((world, pos) -> {
            BlockState blockState = world.getBlockState(pos);
            if (Arrays.stream(targetBlocks).noneMatch(block -> blockState.getBlock() == block)) return false;

            return playerIn.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    /**
     * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
     */
    public boolean enchantItem(PlayerEntity playerIn, int id)
    {
        if (this.func_241818_d_(id))
        {
            this.selectedRecipe.set(id);
            this.updateRecipeResultSlot();
        }

        return true;
    }

    private boolean func_241818_d_(int p_241818_1_)
    {
        return p_241818_1_ >= 0 && p_241818_1_ < this.recipes.size();
    }

    public void onCraftMatrixChanged(IInventory inventoryIn)
    {
        boolean anyChanged = false;
        for (int i = 0; i < 6; i++)
        {
            ItemStack itemstack = inventorySlots.get(i).getStack();
            if (!ItemStack.areItemStacksEqual(itemstack, this.inputStacksCache[i]))
            {
                this.inputStacksCache[i] = itemstack.copy();
                anyChanged = true;
            }
        }
        if (anyChanged)
            this.updateAvailableRecipes(inventoryIn);
    }

    private void updateAvailableRecipes(IInventory inventoryIn)
    {
        SewingRecipe recipe = getSelectedRecipe() >= 0 && recipes.size() > 0 ? recipes.get(getSelectedRecipe()) : null;
        this.recipes.clear();
        this.selectedRecipe.set(-1);
        this.inventorySlots.get(OUTPUTS_START).putStack(ItemStack.EMPTY);
        if (hasItemsinInputSlots())
        {
            this.recipes = this.world.getRecipeManager().getRecipes(SewingRecipe.SEWING, inventoryIn, this.world);
        }
        if (recipes.size() > 0 && recipe != null)
        {
            int index = recipes.indexOf(recipe);
            if (index >= 0)
            {
                selectedRecipe.set(index);
                updateRecipeResultSlot();
            }
        }
    }

    private void updateRecipeResultSlot()
    {
        if (!this.recipes.isEmpty() && this.func_241818_d_(this.selectedRecipe.get()))
        {
            SewingRecipe stonecuttingrecipe = this.recipes.get(this.selectedRecipe.get());
            this.inventory.setRecipeUsed(stonecuttingrecipe);
            this.inventorySlots.get(OUTPUTS_START).putStack(stonecuttingrecipe.getCraftingResult(new RecipeWrapper(this.inputInventory)));
        }
        else
        {
            this.inventorySlots.get(OUTPUTS_START).putStack(ItemStack.EMPTY);
        }

        this.detectAndSendChanges();
    }

    @OnlyIn(Dist.CLIENT)
    public void setInventoryUpdateListener(Runnable listenerIn)
    {
        this.inventoryUpdateListener = listenerIn;
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canMergeSlot(ItemStack stack, Slot slotIn)
    {
        return slotIn.inventory != this.inventory && super.canMergeSlot(stack, slotIn);
    }

    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index)
    {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack())
            return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getStack();
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
            if (stackInSlot.getMaxStackSize() == 1 && inventorySlots.get(0).getStack().getCount() == 0)
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
            if (notify) item.onCreated(stackInSlot, playerIn.world, playerIn);
            if (!this.mergeItemStack(stackInSlot, startIndex, endIndex, reverse))
            {
                return ItemStack.EMPTY;
            }
            if (notify) slot.onSlotChange(stackInSlot, stackCopy);
        }

        if (stackInSlot.isEmpty())
        {
            slot.putStack(ItemStack.EMPTY);
        }

        slot.onSlotChanged();
        if (stackInSlot.getCount() == stackCopy.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(playerIn, stackInSlot);
        this.detectAndSendChanges();

        return stackCopy;
    }

    private boolean hasRecipe(ItemStack stackInSlot)
    {
        return this.world.getRecipeManager().getRecipe(SewingRecipe.SEWING, new Inventory(stackInSlot), this.world).isPresent();
    }

    public void onContainerClosed(PlayerEntity playerIn)
    {
        super.onContainerClosed(playerIn);
        this.inventory.removeStackFromSlot(0);
        if (te == null)
        {
            this.openedFrom.consume((world, pos) -> this.clearContainer(playerIn, playerIn.world, new RecipeWrapper(this.inputInventory)));
        }
    }
}
