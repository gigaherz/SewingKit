package dev.gigaherz.sewingkit.table;

import com.mojang.blaze3d.matrix.MatrixStack;
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
        --this.titleLabelY;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawRecipeCosts(matrixStack, mouseX, mouseY);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y)
    {
        this.renderBackground(matrixStack);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(BACKGROUND_TEXTURE);
        int i = this.leftPos;
        int j = this.topPos;
        this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
        int k = (int) (41.0F * this.sliderProgress);
        this.blit(matrixStack, i + 119, j + 15 + k, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int l = this.leftPos + 52;
        int i1 = this.topPos + 14;
        int j1 = this.recipeIndexOffset + 12;
        this.renderButtons(matrixStack, x, y, l, i1, j1);
        this.drawRecipesItems(l, i1, j1);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int x, int y)
    {
        super.renderLabels(matrixStack, x, y);

        drawString(matrixStack, this.font, "This is a string, can also be a text component instead", 0, 0, -1);
    }

    protected void renderTooltip(MatrixStack matrixStack, int x, int y)
    {
        super.renderTooltip(matrixStack, x, y);
        if (this.hasItemsInInputSlot)
        {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.recipeIndexOffset + 12;
            List<SewingRecipe> list = this.menu.getRecipeList();

            for (int l = this.recipeIndexOffset; l < k && l < this.menu.getRecipeListSize(); ++l)
            {
                int i1 = l - this.recipeIndexOffset;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18)
                {
                    this.renderTooltip(matrixStack, list.get(l).getResultItem(), x, y);
                    renderHoveredRecipe(matrixStack, x, y, menu.getRecipeList().get(l));
                }
            }
        }
    }

    private void drawRecipeCosts(MatrixStack matrixStack, int mouseX, int mouseY)
    {
        int recipeIdx = menu.getSelectedRecipe();
        if (recipeIdx < 0 || recipeIdx >= menu.getRecipeListSize())
            return;
        SewingRecipe recipe = menu.getRecipeList().get(recipeIdx);
        if (recipe == null)
            return;

        Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));

        matrixStack.pushPose();
        matrixStack.translate(0, 0, 300);
        for (int i = 0; i < 4; i++)
        {
            Slot slot = menu.slots.get(i + 2);
            int subtract = 0;
            for (Map.Entry<Ingredient, Integer> mat : remaining.entrySet())
            {
                Ingredient ing = mat.getKey();
                int value = mat.getValue();
                ItemStack stack1 = slot.getItem();
                if (ing.test(stack1))
                {
                    int remaining1 = Math.max(0, value - (stack1.getCount() + subtract));
                    subtract += (value - remaining1);
                    mat.setValue(remaining1);
                }
            }

            if (subtract != 1 && slot.getItem().getCount() > 0)
            {
                int x = slot.x + leftPos;
                int y = slot.y + topPos;
                String text = String.format("%s", subtract);
                int w = font.width(text);
                drawString(matrixStack, font, text, x + 17 - w, y, TextFormatting.YELLOW.getColor());
            }
        }
        matrixStack.popPose();
    }

    private static int tooltipX = 0;
    private static int tooltipY = 0;
    private static int tooltipWidth = 0;
    private static int tooltipHeight = 0;
    private int ticks = 0;

    private static final ResourceLocation RECIPE_TEXTURE = SewingKitMod.location("textures/gui/recipetooltip.png");

    protected void renderHoveredRecipe(MatrixStack matrixStack, int mouseX, int mouseY, SewingRecipe sewingRecipe)
    {
        matrixStack.pushPose();
        matrixStack.translate(0, 0, 300);

        int x = tooltipX;
        int y = tooltipY - 35 - 8;

        Objects.requireNonNull(minecraft).getTextureManager().bind(RECIPE_TEXTURE);
        blit(matrixStack, x, y, 0, 0, 35, 35, 64, 64);
        NonNullList<SewingRecipe.Material> materials = sewingRecipe.getMaterials();
        for (int i = 0; i < materials.size(); i++)
        {
            int xx = x + (i % 2) * 17 + 1;
            int yy = y + (i / 2) * 17 + 1;
            SewingRecipe.Material material = materials.get(i);
            ItemStack[] stacks = material.ingredient.getItems();
            if (stacks.length > 0)
            {
                float zz = itemRenderer.blitOffset;
                itemRenderer.blitOffset = 0;
                RenderSystem.pushMatrix();
                RenderSystem.multMatrix(matrixStack.last().pose());

                ItemStack stack = stacks[(ticks / 32) % stacks.length].copy();
                stack.setCount(material.count);
                itemRenderer.renderAndDecorateItem(stack, xx, yy);

                RenderSystem.popMatrix();
                itemRenderer.blitOffset = zz;
            }
            else
            {
                Objects.requireNonNull(minecraft).getTextureManager().bind(RECIPE_TEXTURE);
                blit(matrixStack, xx, yy, 36, 0, 16, 16, 64, 64);
            }
            if (material.count != 1)
            {
                matrixStack.pushPose();
                matrixStack.translate(0, 0, 300);

                String text = String.format("%d", material.count);
                int w = font.width(text);
                font.drawShadow(matrixStack, text, xx + 17 - w, yy + 9, 0xFFFFFF);

                matrixStack.popPose();
            }
        }

        matrixStack.popPose();
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

    private void renderButtons(MatrixStack matrixStack, int x, int y, int pX, int pY, int pLastVisibleElementIndex)
    {
        for (int i = this.recipeIndexOffset; i < pLastVisibleElementIndex && i < this.menu.getRecipeListSize(); ++i)
        {
            int j = i - this.recipeIndexOffset;
            int k = pX + j % 4 * 16;
            int l = j / 4;
            int i1 = pY + l * 18 + 2;
            int j1 = this.imageHeight;
            if (i == this.menu.getSelectedRecipe())
            {
                j1 += 18;
            }
            else if (x >= k && y >= i1 && x < k + 16 && y < i1 + 18)
            {
                j1 += 36;
            }

            this.blit(matrixStack, k, i1 - 1, 0, j1, 16, 18);
        }
    }

    private void drawRecipesItems(int left, int top, int recipeIndexOffsetMax)
    {
        List<SewingRecipe> list = this.menu.getRecipeList();

        for (int i = this.recipeIndexOffset; i < recipeIndexOffsetMax && i < this.menu.getRecipeListSize(); ++i)
        {
            int j = i - this.recipeIndexOffset;
            int k = left + j % 4 * 16;
            int l = j / 4;
            int i1 = top + l * 18 + 2;
            this.minecraft.getItemRenderer().renderAndDecorateItem(list.get(i).getResultItem(), k, i1);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        this.clickedOnScroll = false;
        if (this.hasItemsInInputSlot)
        {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.recipeIndexOffset + 12;

            for (int l = this.recipeIndexOffset; l < k; ++l)
            {
                int i1 = l - this.recipeIndexOffset;
                double d0 = mouseX - (double) (i + i1 % 4 * 16);
                double d1 = mouseY - (double) (j + i1 / 4 * 18);
                if (d0 >= 0.0D && d1 >= 0.0D && d0 < 16.0D && d1 < 18.0D && this.menu.clickMenuButton(this.minecraft.player, l))
                {
                    Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, l);
                    return true;
                }
            }

            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (mouseX >= (double) i && mouseX < (double) (i + 12) && mouseY >= (double) j && mouseY < (double) (j + 54))
            {
                this.clickedOnScroll = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.clickedOnScroll && this.canScroll())
        {
            int i = this.topPos + 14;
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
        return this.hasItemsInInputSlot && this.menu.getRecipeListSize() > 12;
    }

    protected int getHiddenRows()
    {
        return (this.menu.getRecipeListSize() + 4 - 1) / 4 - 3;
    }

    /**
     * Called every time this screen's container is changed (is marked as dirty).
     */
    private void onInventoryUpdate()
    {
        this.hasItemsInInputSlot = this.menu.isAbleToCraft();
        if (!this.hasItemsInInputSlot)
        {
            this.sliderProgress = 0.0F;
            this.recipeIndexOffset = 0;
        }
    }
}
