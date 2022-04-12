package dev.gigaherz.sewingkit.integration;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SewingCategory implements IRecipeCategory<SewingRecipe>
{
    private static final ResourceLocation GUI_TEXTURE_LOCATION = SewingKitMod.location("textures/gui/sewing_station.png");
    public static final ResourceLocation UID = SewingKitMod.location("drying");
    public static final RecipeType<SewingRecipe> SEWING = new RecipeType<>(UID, SewingRecipe.class);

    public static SewingCategory INSTANCE;

    private final IDrawable background;
    private final IDrawable icon;

    public SewingCategory(IGuiHelper guiHelper)
    {
        INSTANCE = this;
        background = guiHelper.createDrawable(GUI_TEXTURE_LOCATION, 6, 12, 159, 61);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(SewingKitMod.SEWING_STATION_BLOCK.get()));
    }

    @SuppressWarnings("removal")
    @Deprecated
    @Nonnull
    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    @SuppressWarnings("removal")
    @Deprecated
    @Override
    public Class<? extends SewingRecipe> getRecipeClass()
    {
        return SewingRecipe.class;
    }

    @Override
    public RecipeType<SewingRecipe> getRecipeType()
    {
        return SEWING;
    }

    @Nonnull
    @Override
    public Component getTitle()
    {
        return new TranslatableComponent("jei.category.sewingkit.sewing");
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

    /**
     * Sets all the recipe's ingredients by filling out an instance of {@link IRecipeLayoutBuilder}.
     * This is used by JEI for lookups, to figure out what ingredients are inputs and outputs for a recipe.
     *
     * @since 9.4.0
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SewingRecipe recipe, IFocusGroup focuses)
    {

        var tool = Arrays.stream(recipe.getTool().getItems()).toList();
        var pattern = Arrays.stream(recipe.getPattern().getItems()).toList();
        var inputs = recipe.getMaterials();

        List<List<ItemStack>> inputLists = new ArrayList<>();
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

        builder.addSlot(RecipeIngredientRole.CATALYST, slotX[0], slotY[0])
                .addItemStacks(tool)
                .setSlotName("tool");
        builder.addSlot(RecipeIngredientRole.CATALYST, slotX[1], slotY[1])
                .addItemStacks(pattern)
                .setSlotName("pattern");

        for(int i=0;i<4;i++)
        {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX[2+i], slotY[2+i])
                    .addItemStacks((i < inputLists.size()) ? inputLists.get(i) : List.of());
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, slotX[6], slotY[6]);
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
}