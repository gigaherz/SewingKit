package dev.gigaherz.sewingkit.integration;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingMaterial;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SewingCategory implements IRecipeCategory<SewingRecipe>
{
    private static final Identifier GUI_TEXTURE_LOCATION = SewingKitMod.location("textures/gui/sewing_station.png");
    public static final Identifier UID = SewingKitMod.location("drying");
    public static final IRecipeType<SewingRecipe> SEWING = IRecipeType.create(UID, SewingRecipe.class);

    public static SewingCategory INSTANCE;

    private final IDrawable background;
    private final IDrawable icon;

    public SewingCategory(IGuiHelper guiHelper)
    {
        INSTANCE = this;
        background = guiHelper.createDrawable(GUI_TEXTURE_LOCATION, 6, 12, 159, 61);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(SewingKitMod.SEWING_STATION_BLOCK.get()));
    }

    @Override
    public IRecipeType<SewingRecipe> getRecipeType()
    {
        return SEWING;
    }

    @Override
    public Component getTitle()
    {
        return Component.translatable("jei.category.sewingkit.sewing");
    }

    @Override
    public int getWidth()
    {
        return 159;
    }

    @Override
    public int getHeight()
    {
        return 61;
    }

    @Override
    public void draw(SewingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY)
    {
        background.draw(guiGraphics);
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SewingRecipe recipe, IFocusGroup focuses)
    {
        var tool = recipe.tool();
        var pattern = recipe.pattern();
        var inputs = recipe.materials();

        List<SlotDisplay> inputLists = new ArrayList<>();
        for (SewingMaterial material : inputs)
        {
            inputLists.add(material.display());
        }

        var builder1 = builder.addSlot(RecipeIngredientRole.INPUT, slotX[0], slotY[0]);

        if (tool != null) builder1.add(tool);
        builder1.setSlotName("tool");

        var builder2 = builder.addSlot(RecipeIngredientRole.INPUT, slotX[1], slotY[1]);
        if (pattern != null) builder2.add(pattern);
        builder2.setSlotName("pattern");

        for (int i = 0; i < 4; i++)
        {
            var builder3 = builder.addSlot(RecipeIngredientRole.INPUT, slotX[2 + i], slotY[2 + i]);
            if (i < inputLists.size())
            {
                var input = inputLists.get(i);
                if (input != null)
                    builder3.add(input);
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, slotX[6], slotY[6])
                .add(new SlotDisplay.ItemStackSlotDisplay(recipe.output()));
    }

    private static final int[] slotX = {
            8 - 6,
            30 - 6,
            10 - 6,
            28 - 6,
            10 - 6,
            28 - 6,
            143 - 6
    };

    private static final int[] slotY = {
            15 - 12,
            15 - 12,
            35 - 12,
            35 - 12,
            53 - 12,
            53 - 12,
            33 - 12
    };
}