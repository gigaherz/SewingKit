package dev.gigaherz.sewingkit.api;

import com.mojang.logging.LogUtils;
import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.network.SyncRecipeOrder;
import dev.gigaherz.sewingkit.table.SewingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ClientSewingRecipeAccessor
{
    private static Map<Identifier, RecipeHolder<SewingRecipe>> clientRecipesByName;

    public static Map<Identifier, RecipeHolder<SewingRecipe>> getRecipesByName(Level level)
    {
        if (level.isClientSide())
            return clientRecipesByName;
        throw new IllegalStateException("Cannot call in the server");
    }

    @EventBusSubscriber(value= Dist.CLIENT, modid= SewingKitMod.MODID)
    public static class Client
    {
        public static void handleClientOrder(SyncRecipeOrder packet)
        {
            var menu = Minecraft.getInstance().player.containerMenu;
            if (menu.containerId == packet.menuId() && menu instanceof SewingTableMenu sewingTableMenu)
            {
                sewingTableMenu.setOrderedRecipes(packet.recipes());
            }
        }

        @SubscribeEvent
        public static void recipesReceived(RecipesReceivedEvent event)
        {
            var clientRecipes = event.getRecipeMap().byType(SewingKitMod.SEWING.get());
            clientRecipesByName = clientRecipes.stream().collect(Collectors.toMap(e -> e.id().identifier(), e -> e));
        }
    }
}
