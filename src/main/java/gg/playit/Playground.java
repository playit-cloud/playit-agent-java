package gg.playit;

import gg.playit.api.DefaultPlayitAPI;
import gg.playit.api.models.AgentRouting;
import gg.playit.api.models.ReqAgentsRoutingGet;
import gg.playit.api.models.ReqProtoRegister;
import gg.playit.api.models.SignedAgentKey;
import gg.playit.control.ControlChannel;
import gg.playit.control.ControlMessageListenerDefaultError;
import gg.playit.control.ControlMessages;
import gg.playit.runner.TunnelRunner;

import java.net.InetSocketAddress;

public class Playground {
    public static void main(String[] args) throws Exception {
        TunnelRunner runner = new TunnelRunner("");
        runner.update();

//        DefaultPlayitAPI api = new DefaultPlayitAPI();
//        api.getApiClient().setAgentKey("5c5a943ff5a884f31c4bc31780d1b3db210d12757be84c965c2d256d101ca6d0");
//
//        AgentRouting routing = api.agentsRoutingGetPost(new ReqAgentsRoutingGet()).getData();
//
//        ControlChannel channel = new ControlChannel(new InetSocketAddress(routing.getTargets6().get(0), 5525));
//
//        channel.ping();
//
//        channel.readNext(new ControlMessageListenerDefaultError() {
//            @Override
//            public void handlePong(long requestId, ControlMessages.Pong msg) {
//                System.out.println("Got pong " + msg.clientAddr);
//            }
//        });
//
//        ReqProtoRegister register = channel.registerRequest();
//        System.out.println("Register:\n" + register);
//
//        while (!channel.hasAgentSession()) {
//            SignedAgentKey signed = api.protoRegisterPost(register).getData();
//            channel.register(signed.getKey());
//
//            channel.readNext(new ControlMessageListenerDefaultError() {
//                @Override
//                public void handleAgentRegistered(long requestId, ControlMessages.AgentRegistered msg) {
//                    System.out.println("Registered: " + msg.sessionId.sessionId);
//                }
//
//                @Override
//                public void handleError(long requestId, ControlMessages.ResponseErrorCode code) {
//                    long wait;
//                    if (code == ControlMessages.ResponseErrorCode.RequestQueued) {
//                        wait = 1_000;
//                    } else if (code == ControlMessages.ResponseErrorCode.TryAgainLater) {
//                        wait = 5_000;
//                    } else {
//                        throw new RuntimeException("got error: " + code);
//                    }
//
//                    try {
//                        Thread.sleep(wait);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//        }
//
//        System.out.println("Session authenticated");
//
//        while (true) {
//
//        }
    }
}
