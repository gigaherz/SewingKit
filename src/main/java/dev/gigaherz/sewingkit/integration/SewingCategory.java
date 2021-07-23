package dev.gigaherz.sewingkit.integration;
/*
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SewingCategory implements IRecipeCategory<SewingRecipe>
{
    private static final ResourceLocation GUI_TEXTURE_LOCATION = SewingKitMod.location("textures/gui/sewing_station.png");
    public static final ResourceLocation UID = SewingKitMod.location("drying");

    public static SewingCategory INSTANCE;

    private final IDrawable background;
    private final IDrawable icon;

    public SewingCategory(IGuiHelper guiHelper)
    {
        INSTANCE = this;
        background = guiHelper.createDrawable(GUI_TEXTURE_LOCATION, 6, 12, 159, 61);
        icon = guiHelper.createDrawableIngredient(new ItemStack(SewingKitMod.SEWING_STATION_BLOCK.get()));
    }

    @Nonnull
    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    @Override
    public Class<? extends SewingRecipe> getRecipeClass()
    {
        return SewingRecipe.class;
    }

    @Nonnull
    @Override
    public String getTitle()
    {
        return I18n.get("jei.category.sewingkit.sewing");
    }

    @Nonnull
    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    public void setInputMaterials(IIngredients iIngredients, Ingredient tool, Ingredient pattern, List<SewingRecipe.Material> inputs)
    {
        List<List<ItemStack>> inputLists = new ArrayList<>();

        inputLists.add(Arrays.asList(tool.getItems()));
        inputLists.add(Arrays.asList(pattern.getItems()));

        for (SewingRecipe.Material material : inputs)
        {
            ItemStack[] stacks = material.ingredient.getItems();
            List<ItemStack> expandedInput = Arrays.stream(stacks).map(stack -> {
                ItemStack copy = stack.copy();
                copy.setCount(material.count);
                return copy;
            }).collect(Collectors.toList());
            inputLists.add(expandedInput);
        }

        iIngredients.setInputLists(VanillaTypes.ITEM, inputLists);
    }

    @Override
    public void setIngredients(SewingRecipe dryingRecipe, IIngredients iIngredients)
    {
        setInputMaterials(iIngredients, dryingRecipe.getTool(), dryingRecipe.getPattern(), dryingRecipe.getMaterials());
        iIngredients.setOutput(VanillaTypes.ITEM, dryingRecipe.getResultItem());
    }

    private static final int[] slotX = {
            8 - 7,
            30 - 7,
            10 - 7,
            28 - 7,
            10 - 7,
            28 - 7,
            143 - 7
    };

    private static final int[] slotY = {
            15 - 13,
            15 - 13,
            35 - 13,
            35 - 13,
            53 - 13,
            53 - 13,
            33 - 13
    };

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, SewingRecipe recipeWrapper, IIngredients ingredients)
    {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        for (int i = 0; i <= 6; i++)
        {
            itemStacks.init(i, i < 6, slotX[i], slotY[i]);
        }

        itemStacks.set(ingredients);
    }

    @Override
    public void draw(SewingRecipe recipe, PoseStack matrixStack, double mouseX, double mouseY)
    {
    }
}*/