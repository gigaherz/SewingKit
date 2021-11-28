package dev.gigaherz.sewingkit.integration;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

@JeiPlugin
public class JEIPlugin implements IModPlugin
{
    private static final ResourceLocation ID = SewingKitMod.location("jei_plugin");

    @Override
    public ResourceLocation getPluginUid()
    {
        return ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        registration.addRecipeCatalyst(new ItemStack(SewingKitMod.SEWING_STATION_BLOCK.get()), SewingCategory.UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry)
    {
        registry.addRecipeCategories(new SewingCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        ClientWorld world = Objects.requireNonNull(Minecraft.getInstance().level);
        registration.addRecipes(SewingRecipe.getAllRecipes(world), SewingCategory.UID);
    }
}