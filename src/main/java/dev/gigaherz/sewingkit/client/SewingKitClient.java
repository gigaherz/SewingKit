package dev.gigaherz.sewingkit.client;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterRecipeBookSearchCategoriesEvent;

@Mod(value = SewingKitMod.MODID, dist= Dist.CLIENT)
public class SewingKitClient
{
    public SewingKitClient(IEventBus modBus)
    {
        modBus.addListener(this::registerSearchCategories);
    }

    private void registerSearchCategories(RegisterRecipeBookSearchCategoriesEvent event)
    {
        event.register(SewingKitMod.SEWING_SEARCH.get(), SewingKitMod.SEWING_MISC.get());
    }
}
