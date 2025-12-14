package dev.gigaherz.sewingkit.table;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipeDisplay;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

import java.util.List;
import java.util.Optional;

public class SewingRecipeBookComponent extends RecipeBookComponent<SewingTableMenu>
{
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted")
    );
    private static final Component RECIPE_FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.craftable");
    private static final List<RecipeBookComponent.TabInfo> TABS = List.of(
            new RecipeBookComponent.TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), SewingKitMod.SEWING_SEARCH.get()),
            new RecipeBookComponent.TabInfo(SewingKitMod.DIAMOND_SEWING_NEEDLE.get(), SewingKitMod.SEWING_MISC.get())
    );
    public SewingRecipeBookComponent(SewingTableMenu menu)
    {
        super(menu, TABS);
    }

    @Override
    protected boolean isCraftingSlot(Slot slot)
    {
        return this.menu.isCraftingSlot(slot);
    }

    private boolean canDisplay(RecipeDisplay recipeDisplay) {
        return recipeDisplay instanceof SewingRecipeDisplay; // TODO: recipedisplay check?
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection possibleRecipes, StackedItemContents stackedItemContents)
    {
        possibleRecipes.selectRecipes(stackedItemContents, this::canDisplay);
    }

    @Override
    protected Component getRecipeFilterName()
    {
        return RECIPE_FILTER_NAME;
    }

    @Override
    protected WidgetSprites getFilterButtonTextures()
    {
        return FILTER_BUTTON_SPRITES;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipeDisplay, ContextMap contextMap)
    {
        ghostSlots.setResult(this.menu.getSlot(6), contextMap, recipeDisplay.result());
        // TODO: RecipeDisplay
        /*switch (recipeDisplay) {
            case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay:
                List<Slot> list1 = this.menu.getInputGridSlots();
                PlaceRecipeHelper.placeRecipe(
                        this.menu.getGridWidth(),
                        this.menu.getGridHeight(),
                        shapedcraftingrecipedisplay.width(),
                        shapedcraftingrecipedisplay.height(),
                        shapedcraftingrecipedisplay.ingredients(),
                        (p_380786_, p_380787_, p_380788_, p_380789_) -> {
                            Slot slot = list1.get(p_380787_);
                            ghostSlots.setInput(slot, contextMap, p_380786_);
                        }
                );
                break;
            case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay:
                label15: {
                    List<Slot> list = this.menu.getInputGridSlots();
                    int i = Math.min(shapelesscraftingrecipedisplay.ingredients().size(), list.size());

                    for (int j = 0; j < i; j++) {
                        ghostSlots.setInput(list.get(j), contextMap, shapelesscraftingrecipedisplay.ingredients().get(j));
                    }
                    break label15;
                }
            default:
        }*/
    }
}
