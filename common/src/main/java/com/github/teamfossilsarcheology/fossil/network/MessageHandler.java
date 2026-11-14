package com.github.teamfossilsarcheology.fossil.network;

import com.github.teamfossilsarcheology.fossil.network.debug.*;
import com.github.teamfossilsarcheology.fossil.util.Version;

public class MessageHandler {
    //PORT(1.21.1): NetworkChannel (deprecated for removal in Architectury 13) replaced by PayloadChannel. Call sites
    //(sendToPlayer/sendToPlayers/sendToServer) are unchanged, every message is now a CustomPacketPayload.
    public static final PayloadChannel DEBUG_CHANNEL;
    public static final PayloadChannel CAP_CHANNEL = PayloadChannel.create("cap_channel");
    public static final PayloadChannel SYNC_CHANNEL = PayloadChannel.create("sync_channel");

    static {
        if (Version.debugEnabled()) {
            DEBUG_CHANNEL = PayloadChannel.create("debug_channel");
        } else {
            //Absolutely make sure this is not enabled by default
            DEBUG_CHANNEL = null;
        }
    }

    public static void register() {
        if (Version.debugEnabled()) {
            C2SDisableAIMessage.register(DEBUG_CHANNEL);
            C2SDiscardMessage.register(DEBUG_CHANNEL);
            C2SForceAnimationMessage.register(DEBUG_CHANNEL);
            C2SRotationMessage.register(DEBUG_CHANNEL);
            C2SSlowMessage.register(DEBUG_CHANNEL);
            C2SStructureMessage.register(DEBUG_CHANNEL);
            C2STameMessage.register(DEBUG_CHANNEL);
            InstructionMessage.register(DEBUG_CHANNEL);
            S2CCancelAnimationMessage.register(DEBUG_CHANNEL);
            S2CMarkMessage.register(DEBUG_CHANNEL);
            SyncDebugInfoMessage.register(DEBUG_CHANNEL);
        }

        S2CMammalCapMessage.register(CAP_CHANNEL);
        S2CMusicMessage.register(SYNC_CHANNEL);
        S2CSyncEntityInfoMessage.register(SYNC_CHANNEL);
        S2CSyncFoodMappingsMessage.register(SYNC_CHANNEL);
        S2CSyncEntityVariantsMessage.register(SYNC_CHANNEL);
        S2CSyncConfigMessage.register(SYNC_CHANNEL);
        S2CSyncActiveAnimationMessage.register(SYNC_CHANNEL);
        S2CSyncToyAnimationMessage.register(SYNC_CHANNEL);
        C2SHitPlayerMessage.register(SYNC_CHANNEL);
        C2SRiderForceFlyingMessage.register(SYNC_CHANNEL);
        C2SVerticalFlightMessage.register(SYNC_CHANNEL);
        S2CActivateAttackBoxesMessage.register(SYNC_CHANNEL);
    }
}
