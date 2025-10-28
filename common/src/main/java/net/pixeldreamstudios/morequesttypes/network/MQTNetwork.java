package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;

public class MQTNetwork {
    private MQTNetwork() {}

    public static void init() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
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
                NetworkManager.Side.C2S,
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
                NetworkManager.Side.C2S,
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

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                MQTSoundsRequest.TYPE,
                MQTSoundsRequest.STREAM_CODEC,
                MQTSoundsRequest::handle
        );
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTSoundsResponse.TYPE,
                MQTSoundsResponse.STREAM_CODEC,
                MQTSoundsResponse::handle
        );

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                MQTNearestEntityRequest.TYPE,
                MQTNearestEntityRequest.STREAM_CODEC,
                MQTNearestEntityRequest::handle
        );
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTNearestEntityResponse.TYPE,
                MQTNearestEntityResponse.STREAM_CODEC,
                MQTNearestEntityResponse::handle
        );
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                MQTLoottablesRequest.TYPE,
                MQTLoottablesRequest.STREAM_CODEC,
                MQTLoottablesRequest::handle
        );
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                MQTLoottablesResponse.TYPE,
                MQTLoottablesResponse.STREAM_CODEC,
                MQTLoottablesResponse::handle
        );

    }
}
