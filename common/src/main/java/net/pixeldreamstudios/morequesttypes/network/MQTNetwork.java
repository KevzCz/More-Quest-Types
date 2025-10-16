package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;

public class MQTNetwork {
    private MQTNetwork() {}

    public static void init() {
        NetworkManager.registerReceiver(
                dev.architectury.networking.NetworkManager.Side.C2S,
                MQTStructuresRequest.TYPE,
                MQTStructuresRequest.STREAM_CODEC,
                MQTStructuresRequest::handle
        );
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTStructuresResponse.TYPE,
                MQTStructuresResponse.STREAM_CODEC,
                MQTStructuresResponse::handle
        );

        NetworkManager.registerReceiver(
                dev.architectury.networking.NetworkManager.Side.C2S,
                MQTWorldsRequest.TYPE,
                MQTWorldsRequest.STREAM_CODEC,
                MQTWorldsRequest::handle
        );

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTWorldsResponse.TYPE,
                MQTWorldsResponse.STREAM_CODEC,
                MQTWorldsResponse::handle
        );
        NetworkManager.registerReceiver(
                dev.architectury.networking.NetworkManager.Side.C2S,
                MQTBiomesRequest.TYPE,
                MQTBiomesRequest.STREAM_CODEC,
                MQTBiomesRequest::handle
        );

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTBiomesResponse.TYPE,
                MQTBiomesResponse.STREAM_CODEC,
                MQTBiomesResponse::handle
        );
    }
}
