package dev.gigaherz.sewingkit.network;

import dev.gigaherz.sewingkit.SewingKitMod;
import dev.gigaherz.sewingkit.api.ClientSewingRecipeAccessor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncRecipeOrder(int menuId, List<Identifier> recipes) implements CustomPacketPayload
{
    public static final Identifier ID = SewingKitMod.location("key_change");
    public static final Type<SyncRecipeOrder> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecipeOrder> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRecipeOrder::menuId,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRecipeOrder::recipes,
            SyncRecipeOrder::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public void handle(IPayloadContext context)
    {
        context.enqueueWork(() -> ClientSewingRecipeAccessor.Client.handleClientOrder(this));
    }
}
