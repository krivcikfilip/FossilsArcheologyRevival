package com.github.teamfossilsarcheology.fossil.network.debug;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.network.PayloadChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.github.teamfossilsarcheology.fossil.client.gui.debug.DebugScreen;
import com.github.teamfossilsarcheology.fossil.util.Version;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class S2CMarkMessage implements CustomPacketPayload {
    public static final Type<S2CMarkMessage> TYPE = new Type<>(FossilMod.location("s2c_mark"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMarkMessage> STREAM_CODEC = StreamCodec.ofMember(S2CMarkMessage::write, S2CMarkMessage::new);

    private final List<BlockPos> targets;
    private final List<BlockState> blocks;
    private final boolean below;

    private S2CMarkMessage(FriendlyByteBuf buf) {
        this(buf.readVarIntArray(), buf.readBoolean());
    }

    public S2CMarkMessage(int[] targetArray, boolean below) {
        this.targets = new ArrayList<>();
        this.blocks = new ArrayList<>();
        for (int i = 0; i < targetArray.length / 4; i++) {
            this.targets.add(new BlockPos(targetArray[4 * i], targetArray[4 * i + 1], targetArray[4 * i + 2]));
            this.blocks.add(Block.stateById(targetArray[4 * i + 3]));
        }
        this.below = below;
    }

    public S2CMarkMessage(int[] targetArray, BlockState[] blocks, boolean below) {
        this.targets = new ArrayList<>();
        this.blocks = new ArrayList<>();
        for (int i = 0; i < blocks.length; i++) {
            this.targets.add(new BlockPos(targetArray[3 * i], targetArray[3 * i + 1], targetArray[3 * i + 2]));
            this.blocks.add(blocks[i]);
        }
        this.below = below;
    }

    private void write(FriendlyByteBuf buf) {
        int[] targetsOut = new int[targets.size() * 4];
        for (int i = 0; i < targets.size(); i++) {
            targetsOut[4 * i] = targets.get(i).getX();
            targetsOut[4 * i + 1] = targets.get(i).getY();
            targetsOut[4 * i + 2] = targets.get(i).getZ();
            targetsOut[4 * i + 3] = Block.getId(blocks.get(i));
        }
        buf.writeVarIntArray(targetsOut);
        buf.writeBoolean(below);
    }

    private void apply(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (Version.debugEnabled()) {
                DebugScreen.showPath(context.getPlayer(), targets, blocks, below);
            }
        });
    }

    @Override
    public Type<S2CMarkMessage> type() {
        return TYPE;
    }

    public static void register(PayloadChannel channel) {
        channel.registerS2C(TYPE, STREAM_CODEC, S2CMarkMessage::apply);
    }
}
