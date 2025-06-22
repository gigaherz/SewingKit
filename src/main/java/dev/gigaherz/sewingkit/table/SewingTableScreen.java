package dev.gigaherz.sewingkit.table;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Matrix3x2f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SewingTableScreen extends AbstractContainerScreen<SewingTableMenu>
{
    private static final ResourceLocation BACKGROUND_TEXTURE = SewingKitMod.location("textures/gui/sewing_station.png");

    private static SewingRecipe recipeContext;

    @EventBusSubscriber(value = Dist.CLIENT, modid = SewingKitMod.MODID)
    public static class ClientEvents
    {
        @SubscribeEvent
        public static void register(RegisterClientTooltipComponentFactoriesEvent event)
        {
            event.register(RecipeTooltipComponent.class, ClientRecipeTooltipComponent::new);
        }
    }

    private float sliderProgress;
    private boolean clickedOnScroll;
    private int recipeIndexOffset;
    private boolean hasItemsInInputSlot;

    public SewingTableScreen(SewingTableMenu containerIn, Inventory playerInv, Component titleIn)
    {
        super(containerIn, playerInv, titleIn);
        containerIn.setInventoryUpdateListener(this::onInventoryUpdate);
        --this.titleLabelY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        drawRecipeCosts(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y)
    {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        int scrollPosition = (int) (41.0F * this.sliderProgress);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, left + 119, top + 15 + scrollPosition, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15, 256, 256);

        int leftRecipes = this.leftPos + 52;
        int topRecipes = this.topPos + 14;
        int selectedRecipe = this.recipeIndexOffset + 12;
        this.renderButtons(graphics, x, y, leftRecipes, topRecipes, selectedRecipe);
        this.drawRecipesItems(graphics, leftRecipes, topRecipes, selectedRecipe);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y)
    {
        super.renderTooltip(graphics, x, y);
        if (this.hasItemsInInputSlot)
        {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.recipeIndexOffset + 12;
            List<RecipeHolder<SewingRecipe>> list = this.menu.getRecipeList();

            for (int l = this.recipeIndexOffset; l < k && l < this.menu.getRecipeListSize(); ++l)
            {
                int i1 = l - this.recipeIndexOffset;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (x >= j1 && x < j1 + 16 && y >= k1 && y < k1 + 18)
                {
                    recipeContext = list.get(l).value();
                    ItemStack output = recipeContext.getOutput();
                    var lines = Screen.getTooltipFromItem(this.minecraft, output);
                    var clientComponents = ClientHooks.gatherTooltipComponents(output, lines, output.getTooltipImage(), x, graphics.guiWidth(), graphics.guiHeight(), font);
                    var mutable = new ArrayList<>(clientComponents);
                    mutable.add(ClientTooltipComponent.create(new RecipeTooltipComponent(recipeContext)));
                    graphics.renderTooltip(font, mutable, x, y, DefaultTooltipPositioner.INSTANCE, output.get(DataComponents.TOOLTIP_STYLE), output);
                    recipeContext = null;
                }
            }
        }
    }

    private void drawRecipeCosts(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int recipeIdx = menu.getSelectedRecipe();
        if (recipeIdx < 0 || recipeIdx >= menu.getRecipeListSize())
            return;
        SewingRecipe recipe = menu.getRecipeList().get(recipeIdx).value();
        if (recipe == null)
            return;

        Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(SewingRecipe.Material::ingredient, SewingRecipe.Material::count));

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

            if (slot.getItem().getCount() > 0)
            {
                int x = slot.x + leftPos;
                int y = slot.y + topPos;
                String text = String.format("%s", subtract);
                int w = font.width(text);
                graphics.drawString(font, text, x + 17 - w, y, 0xFFFFFF55);
            }
        }
    }

    public static record RecipeTooltipComponent(SewingRecipe recipe) implements TooltipComponent
    {
    }

    public static class ClientRecipeTooltipComponent implements ClientTooltipComponent
    {
        private static final ResourceLocation RECIPE_TEXTURE = SewingKitMod.location("textures/gui/recipetooltip.png");

        private final SewingRecipe recipe;
        private final Component label;

        private SewingRecipe cachedIngredientRecipe = null;
        private Map<SewingRecipe.Material, List<ItemStack>> cachedIngredientLists = null;

        public ClientRecipeTooltipComponent(RecipeTooltipComponent component)
        {
            this.recipe = component.recipe();
            this.label = Component.translatable("text.sewingkit.recipe");
        }

        @Override
        public int getHeight(Font font)
        {
            return 20 + 9 * 2; // 20 + Font.lineHeight * 2
        }

        @Override
        public int getWidth(Font font)
        {
            return Math.max(18 * 4 + 4, font.width(label));
        }

        @Override
        public void renderImage(Font font, int x, int y, int p_368529_, int p_368584_, GuiGraphics graphics)
        {
            y += font.lineHeight;

            graphics.drawString(font, label, x, y, 0xFFFFFFFF);

            y += font.lineHeight;

            if (cachedIngredientRecipe != recipe)
            {
                cachedIngredientRecipe = recipe;
                cachedIngredientLists = new Reference2ObjectOpenHashMap<>();
                var materials = recipe.getMaterials();
                for (int i = 0; i < materials.size(); i++)
                {
                    SewingRecipe.Material material = materials.get(i);
                    var stacks = material.ingredient().items().map(ItemStack::new).toList();
                    cachedIngredientLists.put(material, stacks);
                }
            }

            NonNullList<SewingRecipe.Material> materials = recipe.getMaterials();
            for (int i = 0; i < materials.size(); i++)
            {
                int xx = x + i * 17 + 4;

                SewingRecipe.Material material = materials.get(i);
                var stacks = cachedIngredientLists.get(material);
                if (stacks.size() > 0)
                {
                    var ticks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
                    ItemStack stack = stacks.get((int) ((ticks / 32) % stacks.size()));
                    graphics.renderItem(stack, xx, y);
                    graphics.renderItemDecorations(font, stack, xx, y);
                }
                else
                {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_TEXTURE, xx, y, 36, 0, 16, 16, 64, 64);
                }
                if (material.count() != 1)
                {
                    String text = String.format("%d", material.count());
                    int w = font.width(text);
                    graphics.drawString(font, text, xx + 17 - w, y + 9, 0xFFFFFFFF);
                }
            }
        }
    }

    private void renderButtons(GuiGraphics graphics, int x, int y, int buttonsLeft, int buttonsTop, int someOffset)
    {
        for (int index = this.recipeIndexOffset; index < someOffset && index < this.menu.getRecipeListSize(); ++index)
        {
            int position = index - this.recipeIndexOffset;
            int xpos = buttonsLeft + position % 4 * 16;
            int row = position / 4;
            int ypos = buttonsTop + row * 18 + 2;
            int v0 = this.imageHeight;
            if (index == this.menu.getSelectedRecipe())
            {
                v0 += 18;
            }
            else if (x >= xpos && y >= ypos && x < xpos + 16 && y < ypos + 18)
            {
                v0 += 36;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, xpos, ypos - 1, 0, v0, 16, 18, 256, 256);
        }
    }

    private void drawRecipesItems(GuiGraphics graphics, int left, int top, int recipeIndexOffsetMax)
    {
        var poseStack = new PoseStack();
        List<RecipeHolder<SewingRecipe>> list = this.menu.getRecipeList();

        for (int i = this.recipeIndexOffset; i < recipeIndexOffsetMax && i < this.menu.getRecipeListSize(); ++i)
        {
            int j = i - this.recipeIndexOffset;
            int k = left + j % 4 * 16;
            int l = j / 4;
            int i1 = top + l * 18 + 2;
            poseStack.translate(0.0F, 0.0F, 0.0F);
            var resultItem = list.get(i).value().getOutput();
            graphics.renderItem(resultItem, k, i1);
            graphics.renderItemDecorations(font, resultItem, k, i1);
        }
    }

    @Override
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
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.clickedOnScroll && this.canScroll())
        {
            int i = this.topPos + 14;
            int j = i + 54;
            this.sliderProgress = ((float) mouseY - (float) i - 7.5F) / ((float) (j - i) - 15.0F);
            this.sliderProgress = Mth.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) this.getHiddenRows()) + 0.5D) * 4;
            return true;
        }
        else
        {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
    {
        if (this.canScroll())
        {
            int i = this.getHiddenRows();
            this.sliderProgress = (float) ((double) this.sliderProgress - deltaY / (double) i);
            this.sliderProgress = Mth.clamp(this.sliderProgress, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) i) + 0.5D) * 4;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
