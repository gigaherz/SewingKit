package dev.gigaherz.sewingkit.tools;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid=SewingKitMod.MODID)
public class StickWebHandler
{
    public static final TagKey<Block> C_COBWEBS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "cobwebs"));
    public static final TagKey<Block> PROVIDES_THREAD = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("sewingkit", "provides_thread"));

    public static final TagKey<Item> C_WOODEN_STICKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "sticks/wooden"));
    public static final TagKey<Item> PRODUCES_THREAD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("sewingkit", "produces_thread"));

    public static final TagKey<Item> CONVERTS_TO_THREAD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("sewingkit", "converts_to_thread"));

    public static final float COBWEB_STICK_SPEED = 8.0f;

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed ev)
    {
        if (ev.getState().is(PROVIDES_THREAD) && ev.getEntity().getMainHandItem().is(PRODUCES_THREAD))
        {
            ev.setNewSpeed(Math.max(ev.getNewSpeed(), COBWEB_STICK_SPEED));
        }
    }

    @SubscribeEvent
    public static void harvestCheck(PlayerEvent.HarvestCheck ev)
    {
        if (ev.getTargetBlock().is(PROVIDES_THREAD) && ev.getEntity().getMainHandItem().is(PRODUCES_THREAD))
        {
            ev.setCanHarvest(true);
        }
    }
}
