package dev.gigaherz.sewingkit.table;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SewingKitMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SewingTableScreen extends ContainerScreen<SewingTableContainer>
{
    private static final ResourceLocation BACKGROUND_TEXTURE = SewingKitMod.location("textures/gui/sewing_station.png");

    private float sliderProgress;
    private boolean clickedOnScroll;
    private int recipeIndexOffset;
    private boolean hasItemsInInputSlot;

    public SewingTableScreen(SewingTableContainer containerIn, PlayerInventory playerInv, ITextComponent titleIn)
    {
        super(containerIn, playerInv, titleIn);
        containerIn.setInventoryUpdateListener(this::onInventoryUpdate);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        super.render(mouseX, mouseY, partialTicks);
        drawRecipeCosts(mouseX, mouseY);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        this.font.drawString(this.title.getFormattedText(), 8.0F, 4.0F, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F, (float) (this.ySize - 94), 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        this.renderBackground();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.blit(i, j, 0, 0, this.xSize, this.ySize);
        int k = (int) (41.0F * this.sliderProgress);
        this.blit(i + 119, j + 15 + k, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int l = this.guiLeft + 52;
        int i1 = this.guiTop + 14;
        int j1 = this.recipeIndexOffset + 12;
        this.drawRecipesBackground(mouseX, mouseY, l, i1, j1);
        this.drawRecipesItems(l, i1, j1);
    }

    protected void renderHoveredToolTip(int x, int y)
    {
        super.renderHoveredToolTip(x, y);
        if (this.hasItemsInInputSlot)
        {
            int i = this.guiLeft + 52;
            int j = this.guiTop + 14;
            int k = this.recipeIndexOffset + 12;
            List<SewingRecipe> list = this.container.getRecipeList();

            for (int l = this.recipeIndexOffset; l < k && l < this.container.getRecipeListSize(); ++l)
            {
                int i1 = l - this.recipeIndexOffset;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18)
                {
                    this.renderTooltip(list.get(l).getRecipeOutput(), x, y);
                    renderHoveredRecipe(x, y, container.getRecipeList().get(l));
                }
            }
        }
    }

    private void drawRecipeCosts(int mouseX, int mouseY)
    {
        int recipeIdx = container.getSelectedRecipe();
        if (recipeIdx < 0 || recipeIdx >= container.getRecipeListSize())
            return;
        SewingRecipe recipe = container.getRecipeList().get(recipeIdx);
        if (recipe == null)
            return;

        Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));

        RenderSystem.pushMatrix();
        RenderSystem.translatef(0, 0, 300);
        for (int i = 0; i < 4; i++)
        {
            Slot slot = container.inventorySlots.get(i + 2);
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

            if (subtract != 1 && slot.getStack().getCount() > 0)
            {
                int x = slot.xPos + guiLeft;
                int y = slot.yPos + guiTop;
                String text = String.format("%s", subtract);
                int w = font.getStringWidth(text);
                drawString(font, text, x + 17 - w, y, TextFormatting.YELLOW.getColor());
            }
        }
        RenderSystem.popMatrix();
    }

    private static int tooltipX = 0;
    private static int tooltipY = 0;
    private static int tooltipWidth = 0;
    private static int tooltipHeight = 0;
    private int ticks = 0;

    private static final ResourceLocation RECIPE_TEXTURE = SewingKitMod.location("textures/gui/recipetooltip.png");

    protected void renderHoveredRecipe(int mouseX, int mouseY, SewingRecipe sewingRecipe)
    {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(0, 0, 300);

        int x = tooltipX;
        int y = tooltipY - 35 - 8;

        Objects.requireNonNull(minecraft).getTextureManager().bindTexture(RECIPE_TEXTURE);
        blit(x, y, 0, 0, 35, 35, 64, 64);
        NonNullList<SewingRecipe.Material> materials = sewingRecipe.getMaterials();
        for (int i = 0; i < materials.size(); i++)
        {
            int xx = x + (i % 2) * 17 + 1;
            int yy = y + (i / 2) * 17 + 1;
            SewingRecipe.Material material = materials.get(i);
            ItemStack[] stacks = material.ingredient.getMatchingStacks();
            if (stacks.length > 0)
            {
                float zz = itemRenderer.zLevel;
                itemRenderer.zLevel = 0;

                ItemStack stack = stacks[(ticks / 32) % stacks.length].copy();
                stack.setCount(material.count);
                itemRenderer.renderItemAndEffectIntoGUI(stack, xx, yy);

                itemRenderer.zLevel = zz;
            }
            else
            {
                Objects.requireNonNull(minecraft).getTextureManager().bindTexture(RECIPE_TEXTURE);
                blit(xx, yy, 36, 0, 16, 16, 64, 64);
            }
            if (material.count != 1)
            {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(0, 0, 300);

                String text = String.format("%d", material.count);
                int w = font.getStringWidth(text);
                font.drawStringWithShadow(text, xx + 17 - w, yy + 9, 0xFFFFFF);

                RenderSystem.popMatrix();
            }
        }

        RenderSystem.popMatrix();
    }

    @Override
    public void tick()
    {
        super.tick();
        ticks++;
    }

    @SubscribeEvent
    public static void tooltipEvent(RenderTooltipEvent.PostText event)
    {
        tooltipX = event.getX();
        tooltipY = event.getY();
        tooltipWidth = event.getWidth();
        tooltipHeight = event.getHeight();
    }

    private void drawRecipesBackground(int mouseX, int mouseY, int left, int top, int recipeIndexOffsetMax)
    {
        for (int i = this.recipeIndexOffset; i < recipeIndexOffsetMax && i < this.container.getRecipeListSize(); ++i)
        {
            int j = i - this.recipeIndexOffset;
            int k = left + j % 4 * 16;
            int l = j / 4;
            int i1 = top + l * 18 + 2;
            int j1 = this.ySize;
            if (i == this.container.getSelectedRecipe())
            {
                j1 += 18;
            }
            else if (mouseX >= k && mouseY >= i1 && mouseX < k + 16 && mouseY < i1 + 18)
            {
                j1 += 36;
            }

            this.blit(k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void drawRecipesItems(int left, int top, int recipeIndexOffsetMax)
    {
        List<SewingRecipe> list = this.container.getRecipeList();

        for (int i = this.recipeIndexOffset; i < recipeIndexOffsetMax && i < this.container.getRecipeListSize(); ++i)
        {
            int j = i - this.recipeIndexOffset;
            int k = left + j % 4 * 16;
            int l = j / 4;
            int i1 = top + l * 18 + 2;
            this.minecraft.getItemRenderer().renderItemAndEffectIntoGUI(list.get(i).getRecipeOutput(), k, i1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        this.clickedOnScroll = false;
        if (this.hasItemsInInputSlot)
        {
            int i = this.guiLeft + 52;
            int j = this.guiTop + 14;
            int k = this.recipeIndexOffset + 12;

            for (int l = this.recipeIndexOffset; l < k; ++l)
            {
                int i1 = l - this.recipeIndexOffset;
                double d0 = mouseX - (double) (i + i1 % 4 * 16);
                double d1 = mouseY - (double) (j + i1 / 4 * 18);
                if (d0 >= 0.0D && d1 >= 0.0D && d0 < 16.0D && d1 < 18.0D && this.container.enchantItem(this.minecraft.player, l))
                {
                    Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.minecraft.playerController.sendEnchantPacket((this.container).windowId, l);
                    return true;
                }
            }

            i = this.guiLeft + 119;
            j = this.guiTop + 9;
            if (mouseX >= (double) i && mouseX < (double) (i + 12) && mouseY >= (double) j && mouseY < (double) (j + 54))
            {
                this.clickedOnScroll = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.clickedOnScroll && this.canScroll())
        {
            int i = this.guiTop + 14;
            int j = i + 54;
            this.sliderProgress = ((float) mouseY - (float) i - 7.5F) / ((float) (j - i) - 15.0F);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) this.getHiddenRows()) + 0.5D) * 4;
            return true;
        }
        else
        {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (this.canScroll())
        {
            int i = this.getHiddenRows();
            this.sliderProgress = (float) ((double) this.sliderProgress - delta / (double) i);
            this.sliderProgress = MathHelper.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) i) + 0.5D) * 4;
        }

        return true;
    }

    private boolean canScroll()
    {
        return this.hasItemsInInputSlot && this.container.getRecipeListSize() > 12;
    }

    protected int getHiddenRows()
    {
        return (this.container.getRecipeListSize() + 4 - 1) / 4 - 3;
    }

    /**
     * Called every time this screen's container is changed (is marked as dirty).
     */
    private void onInventoryUpdate()
    {
        this.hasItemsInInputSlot = this.container.isAbleToCraft();
        if (!this.hasItemsInInputSlot)
        {
            this.sliderProgress = 0.0F;
            this.recipeIndexOffset = 0;
        }
    }
}
