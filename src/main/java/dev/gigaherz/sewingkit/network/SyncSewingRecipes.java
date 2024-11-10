package dev.gigaherz.sewingkit.network;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.SewingRecipe;
import dev.gigaherz.sewingkit.api.SewingRecipeAccessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncSewingRecipes(List<RecipeHolder<SewingRecipe>> recipes) implements CustomPacketPayload
{
    public static final ResourceLocation ID = SewingKitMod.location("key_change");
    public static final Type<SyncSewingRecipes> TYPE = new Type<>(ID);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final StreamCodec<RegistryFriendlyByteBuf, RecipeHolder<SewingRecipe>>
            HOLDER_CODEC = (StreamCodec)RecipeHolder.STREAM_CODEC;

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSewingRecipes> STREAM_CODEC = StreamCodec.composite(
            HOLDER_CODEC.apply(ByteBufCodecs.list()), SyncSewingRecipes::recipes,
            SyncSewingRecipes::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public void handle(IPayloadContext context)
    {
        context.enqueueWork(() -> SewingRecipeAccessor.handleClientRecipes(this));
    }
}
