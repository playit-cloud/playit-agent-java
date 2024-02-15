package gg.playit.runner;

import gg.playit.ApiException;
import gg.playit.api.DefaultPlayitAPI;
import gg.playit.api.models.AgentRouting;
import gg.playit.api.models.ReqAgentsRoutingGet;
import gg.playit.api.models.ReqProtoRegister;
import gg.playit.api.models.SignedAgentKey;
import gg.playit.control.ControlChannel;
import gg.playit.control.ControlMessageListener;
import gg.playit.control.ControlMessageListenerDefaultError;
import gg.playit.control.ControlMessages;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Instant;

public class TunnelRunner {
    String secretKey;
    DefaultPlayitAPI api;

    volatile ControlChannel control;
    final Object controlLock;

    final MessageListener listener;

    volatile DatagramSocket udpSocket;
    final Object udpSocketLock;

    public TunnelRunner(String secretKey) {
        this.secretKey = secretKey;
        this.api = new DefaultPlayitAPI();
        this.api.getApiClient().setAgentKey(this.secretKey);

        this.controlLock = new Object();

        UpdaterThread target = new UpdaterThread();
        target.parent = this;

        this.listener = new MessageListener();
        listener.runner = this;

        this.udpSocketLock = new Object();

        new Thread(target);
    }

    static class UpdaterThread implements Runnable {
        TunnelRunner parent;

        @Override
        public void run() {
        }
    }

    public static class Error extends Exception {
        public ErrorCode code;

        Error(ErrorCode code) {
            this.code = code;
        }
    }

    public enum ErrorCode {
        FailedToSetupControlChannel,
        AuthenticationWithTunnelServerFailed,
    }

    long lastAddressUpdateTest;
    long lastRegisterRequest;
    long lastKeepAlive;
    long lastPing;
    long lastUdpSetup = Long.MIN_VALUE;

    boolean expired;

    public void setupUdp() throws IOException, ControlChannel.ChannelError {
        synchronized (this.udpSocketLock) {
            if (this.udpSocket == null) {
                this.udpSocket = new DatagramSocket();
            }

            this.control.setupUdpChannel();
            lastUdpSetup = Instant.now().toEpochMilli();
        }
    }

    public void update() throws IOException, ApiException, Error, ControlChannel.ChannelError {
        if (this.control == null || this.lastAddressUpdateTest + 15_000 < Instant.now().toEpochMilli()) {
            this.updateControl();
            this.lastAddressUpdateTest = Instant.now().toEpochMilli();
        }

        this.control.setTimeoutMilli(250);

        /* refresh session if expiring in 10 seconds */
        boolean sessionRefreshed = false;
        {
            long now = Instant.now().toEpochMilli();

            if ((now - this.control.expiresAt() < 10_000 || this.expired) && this.lastRegisterRequest + 1_000 < now) {
                sessionRefreshed = true;
                lastRegisterRequest = now;

                String registerData = this.api.protoRegisterPost(this.control.registerRequest()).getData().getKey();
                this.control.register(registerData);
            }
        }

        /* send keep alive every 10 seconds */
        {
            long now = Instant.now().toEpochMilli();

            if (!sessionRefreshed && this.lastKeepAlive + 10_000 < now) {
                this.lastKeepAlive = now;
                this.control.keepAlive();
            }
        }

        /* send ping */
        {
            long now = Instant.now().toEpochMilli();

            if (lastPing + 5_000 < now) {
                lastPing = now;
                this.control.ping();
            }
        }

        long startEpoch = Instant.now().toEpochMilli();

        this.control.setTimeoutMilli(100);
        while (this.control != null && Instant.now().toEpochMilli() - startEpoch < 1_000) {
            this.control.readNext(this.listener);
        }
    }

    static class MessageListener implements ControlMessageListener {
        TunnelRunner runner;

        @Override
        public void handleAgentRegistered(long requestId, ControlMessages.AgentRegistered msg) {
        }

        @Override
        public void handleError(long requestId, ControlMessages.ResponseErrorCode code) {
        }
    }

    private void updateControl() throws IOException, ApiException, Error, ControlChannel.ChannelError {
        synchronized (this.controlLock) {
            ControlChannel updatedControl = connectToControl();

            boolean updateControl;
            if (this.control == null) {
                if (updatedControl == null) {
                    throw new Error(ErrorCode.FailedToSetupControlChannel);
                }

                updateControl = true;
            } else {
                updateControl = this.control.getControlAddress() != updatedControl.getControlAddress();
            }

            if (updateControl) {
                ReqProtoRegister register = updatedControl.registerRequest();

                for (int i = 0; i < 10; i++) {
                    SignedAgentKey key = this.api.protoRegisterPost(register).getData();
                    updatedControl.register(key.getKey());

                    updatedControl.setTimeoutMilli(500);
                    RegisterListen listen = new RegisterListen();

                    try {
                        updatedControl.readNext(listen);
                    } catch (IOException e) {
                    }

                    if (listen.registered != null) {
                        if (this.control != null) {
                            this.control.close();
                        }

                        this.control = updatedControl;
                        this.expireAt = listen.registered.expiresAt;

                        break;
                    }

                    if (listen.error == ControlMessages.ResponseErrorCode.InvalidSignature || listen.error == ControlMessages.ResponseErrorCode.Unauthorized) {
                        throw new Error(ErrorCode.AuthenticationWithTunnelServerFailed);
                    }
                }
            }
        }
    }

    static class RegisterListen implements ControlMessageListener {
        ControlMessages.AgentRegistered registered;
        ControlMessages.ResponseErrorCode error;

        @Override
        public void handleAgentRegistered(long requestId, ControlMessages.AgentRegistered msg) {
            this.registered = msg;
        }

        @Override
        public void handleError(long requestId, ControlMessages.ResponseErrorCode code) {
            this.error = code;
        }
    }

    ControlChannel connectToControl() throws ApiException, IOException {
        AgentRouting data = api.agentsRoutingGetPost(new ReqAgentsRoutingGet()).getData();

        for (String controlAddr : data.getTargets6()) {
            InetSocketAddress addr = new InetSocketAddress(controlAddr, 5525);
            ControlChannel channel = new ControlChannel(addr);

            channel.setTimeoutMilli(50);

            for (int i = 0; i < 5; i++) {
                try {
                    channel.ping();

                    channel.readNext(new ControlMessageListenerDefaultError() {
                        @Override
                        public void handlePong(long requestId, ControlMessages.Pong msg) {
                        }
                    });
                } catch (SocketException e) {
                }

                if (channel.hasClientAddr()) {
                    return channel;
                }
            }

            break;
        }

        for (String controlAddr : data.getTargets4()) {
            InetSocketAddress addr = new InetSocketAddress(controlAddr, 5525);
            ControlChannel channel = new ControlChannel(addr);

            channel.setTimeoutMilli(50);

            for (int i = 0; i < 5; i++) {
                try {
                    channel.ping();

                    channel.readNext(new ControlMessageListenerDefaultError() {
                        @Override
                        public void handlePong(long requestId, ControlMessages.Pong msg) {
                        }
                    });
                } catch (SocketException e) {
                }

                if (channel.hasClientAddr()) {
                    return channel;
                }
            }
        }

        return null;
    }
}
