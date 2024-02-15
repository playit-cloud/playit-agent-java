package gg.playit.control;

public interface ControlMessageListener {
    default void handlePong(long requestId, ControlMessages.Pong msg) {}

    default void handleError(long requestId, ControlMessages.ResponseErrorCode code) {}

    default void handleAgentRegistered(long requestId, ControlMessages.AgentRegistered msg) {}

    default void handleAgentPortMapping(long requestId, ControlMessages.AgentPortMapping msg) {}

    default void handleUdpChannelDetails(long requestId, ControlMessages.UdpChannelDetails msg) {}

    default void handleNewClient(ControlMessages.NewClient msg) {}
}
