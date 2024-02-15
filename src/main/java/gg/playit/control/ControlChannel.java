package gg.playit.control;

import gg.playit.Constants;
import gg.playit.api.models.PlayitAgentVersion;
import gg.playit.api.models.ReqProtoRegister;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UnusedReturnValue")
public class ControlChannel implements Closeable {
    public PlayitAgentVersion version;

    DatagramSocket socket;
    InetSocketAddress controlAddress;

    volatile ControlMessages.AgentSessionId sessionId;
    volatile InetSocketAddress localClientAddr;

    AtomicLong nextId;
    AtomicLong serverTimeOffset;
    AtomicInteger currentPing;
    AtomicLong expiresAt;

    public ControlChannel(InetSocketAddress controlAddress) throws SocketException {
        this.version = Constants.version;

        this.socket = new DatagramSocket();
        this.controlAddress = controlAddress;
        this.nextId = new AtomicLong(0);
        this.serverTimeOffset = new AtomicLong(0);
        this.currentPing = new AtomicInteger(0);
        this.expiresAt = new AtomicLong(0);
    }

    public long expiresAt() {
        return this.expiresAt.get();
    }

    public void setTimeoutMilli(int milli) throws SocketException {
        this.socket.setSoTimeout(milli);
    }

    public InetSocketAddress getControlAddress() {
        return this.controlAddress;
    }

    public ReqProtoRegister registerRequest() throws ChannelError {
        InetSocketAddress localAddress = this.localClientAddr;
        if (localAddress == null) {
            throw new ChannelError(ChannelErrorCode.ClientAddrUnknown);
        }

        ReqProtoRegister req = new ReqProtoRegister();
        req.setAgentVersion(this.version);
        req.setClientAddr(addrString(localAddress));
        req.setTunnelAddr(addrString(this.controlAddress));

        return req;
    }

    public long keepAlive() throws IOException, ChannelError {
        ControlMessages.AgentSessionId sessionId = this.sessionId;
        if (sessionId == null) {
            throw new ChannelError(ChannelErrorCode.SessionNotSet);
        }

        ControlMessages.AgentKeepAlive msg = new ControlMessages.AgentKeepAlive();
        msg.sessionId = sessionId;
        return send(new ControlRpcMessage<>(id(), msg));
    }

    public long ping() throws IOException {
        ControlMessages.Ping msg = new ControlMessages.Ping();
        msg.now = epoch();
        msg.sessionId = this.sessionId;
        msg.currentPing = this.currentPing.get();
        return send(new ControlRpcMessage<>(id(), msg));
    }

    public long setupUdpChannel() throws IOException, ChannelError {
        ControlMessages.AgentSessionId sessionId = this.sessionId;
        if (sessionId == null) {
            throw new ChannelError(ChannelErrorCode.SessionNotSet);
        }

        ControlMessages.SetupUdpChannel msg = new ControlMessages.SetupUdpChannel();
        msg.sessionId = sessionId;
        return send(new ControlRpcMessage<>(id(), msg));
    }

    public long register(String encodedData) throws IOException {
        WrappedAgentRegisterBytes data = new WrappedAgentRegisterBytes();

        try {
            data.bytes = Hex.decodeHex(encodedData);
        } catch (DecoderException e) {
            throw new IOException("failed to parse encoded register data: " + encodedData);
        }
        return send(new ControlRpcMessage<>(id(), data));
    }

    public <C extends ControlMessageListener> void readNext(C handle) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        this.socket.receive(packet);

        if (!packet.getAddress().equals(this.controlAddress.getAddress())) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(
                packet.getData(),
                packet.getOffset(),
                packet.getLength()
        );

        int feedType = buffer.getInt();

        /* response */
        if (feedType == 1) {
            long requestId = buffer.getLong();
            int messageType = buffer.getInt();

            if (messageType == 1) {
                ControlMessages.Pong pong = new ControlMessages.Pong();
                pong.readFrom(buffer);

                this.localClientAddr = pong.clientAddr;

                long latency = epoch() - pong.requestNow;
                this.serverTimeOffset.set(pong.serverNow - (pong.requestNow + latency / 2));
                this.currentPing.set((int) latency);

                handle.handlePong(requestId, pong);
            }
            else if (messageType == 2) {
                handle.handleError(requestId, ControlMessages.ResponseErrorCode.InvalidSignature);
            }
            else if (messageType == 3) {
                handle.handleError(requestId, ControlMessages.ResponseErrorCode.Unauthorized);
            }
            else if (messageType == 4) {
                handle.handleError(requestId, ControlMessages.ResponseErrorCode.RequestQueued);
            }
            else if (messageType == 5) {
                handle.handleError(requestId, ControlMessages.ResponseErrorCode.TryAgainLater);
            }
            else if (messageType == 6) {
                ControlMessages.AgentRegistered registered = new ControlMessages.AgentRegistered();
                registered.readFrom(buffer);

                this.sessionId = registered.sessionId.clone();
                this.expiresAt.set(registered.expiresAt - this.serverTimeOffset.get());

                handle.handleAgentRegistered(requestId, registered);
            }
            else if (messageType == 7) {
                ControlMessages.AgentPortMapping portMapping = new ControlMessages.AgentPortMapping();
                portMapping.readFrom(buffer);

                handle.handleAgentPortMapping(requestId, portMapping);
            }
            else if (messageType == 8) {
                ControlMessages.UdpChannelDetails udpChannelDetails = new ControlMessages.UdpChannelDetails();
                udpChannelDetails.readFrom(buffer);

                handle.handleUdpChannelDetails(requestId, udpChannelDetails);
            }
            else {
                throw new IOException("Invalid message type: " + messageType);
            }
        }
        /* new client */
        else if (feedType == 2) {
            ControlMessages.NewClient newClient = new ControlMessages.NewClient();
            newClient.readFrom(buffer);
            handle.handleNewClient(newClient);
        }
        else {
            throw new IOException("Invalid message type from control server: " + feedType);
        }
    }

    long id() {
        return this.nextId.getAndAdd(1);
    }

    long epoch() {
        return Instant.now().toEpochMilli();
    }

    long send(ControlRpcMessage<ControlMessages.ControlRequest> msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        msg.writeTo(buffer);

        DatagramPacket packet = new DatagramPacket(buffer.array(), 0, buffer.position());
        packet.setSocketAddress(this.controlAddress);

        this.socket.send(packet);
        return msg.requestId;
    }

    String addrString(InetSocketAddress addr) {
        if (addr.getAddress().getAddress().length == 4) {
            return addr.toString();
        } else {
            return "[" + addr.getAddress().toString().substring(1) + "]:" + addr.getPort();
        }
    }

    public boolean hasAgentSession() {
        return this.sessionId != null;
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public boolean hasClientAddr() {
        return this.localClientAddr != null;
    }

    public enum ChannelErrorCode {
        SessionNotSet,
        ClientAddrUnknown,
    }

    public static class ChannelError extends Exception {
        public ChannelErrorCode code;

        private ChannelError(ChannelErrorCode code) {
            this.code = code;
        }
    }

    static class WrappedAgentRegisterBytes implements ControlMessages.ControlRequest {
        byte[] bytes;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            buffer.put(bytes);
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            throw new IOException("read not supported for WrappedBytes");
        }
    }
}
