package gg.playit.control;

public interface ControlMessageListenerDefaultError extends ControlMessageListener {
    default void handlePong(long requestId, ControlMessages.Pong msg) {
        throw new RuntimeException("unexpected pong (" + requestId + ")");
    }

    default void handleError(long requestId, ControlMessages.ResponseErrorCode code) {
        throw new RuntimeException("unexpected error (" + requestId + "): " + code);
    }

    default void handleAgentRegistered(long requestId, ControlMessages.AgentRegistered msg) {
        throw new RuntimeException("unexpected agent registered (" + requestId + ")");
    }

    default void handleAgentPortMapping(long requestId, ControlMessages.AgentPortMapping msg) {
        throw new RuntimeException("unexpected agent port mapping (" + requestId + ")");
    }

    default void handleUdpChannelDetails(long requestId, ControlMessages.UdpChannelDetails msg) {
        throw new RuntimeException("unexpected udp channel details (" + requestId + ")");
    }

    default void handleNewClient(ControlMessages.NewClient msg) {
        throw new RuntimeException("unexpected new client (" + msg.peerAddr + ")");
    }
}
