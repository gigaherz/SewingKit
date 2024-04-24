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

    @Override
    public RecipeType<SewingRecipe> getRecipeType()
    {
        return SEWING;
    }

    @Nonnull
    @Override
    public Component getTitle()
    {
        return Component.translatable("jei.category.sewingkit.sewing");
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SewingRecipe recipe, IFocusGroup focuses)
    {

        var tool = recipe.getTool().stream().flatMap(s -> Arrays.stream(s.getItems())).toList();
        var pattern =recipe.getPattern().stream().flatMap(s -> Arrays.stream(s.getItems())).toList();
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

        for (int i = 0; i < 4; i++)
        {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX[2 + i], slotY[2 + i])
                    .addItemStacks((i < inputLists.size()) ? inputLists.get(i) : List.of());
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, slotX[6], slotY[6])
                .addItemStack(recipe.getResultItem());
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