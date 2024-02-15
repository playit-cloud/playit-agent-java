package gg.playit.control;

import gg.playit.api.models.PortType;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class ControlMessages {
    public interface Message {
        void writeTo(ByteBuffer buffer) throws IOException;

        void readFrom(ByteBuffer buffer) throws IOException;
    }

    public interface ControlRequest extends Message {
        void writeId(ByteBuffer buffer) throws IOException;
    }

    public static class Ping implements ControlRequest {
        public long now;
        public int currentPing;
        public AgentSessionId sessionId;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
            try {
                buffer.putInt(6);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                buffer.putLong(this.now);
                if (this.currentPing == 0) {
                    buffer.put((byte) 0);
                } else {
                    buffer.put((byte) 1);
                    buffer.putInt(currentPing);
                }
                if (this.sessionId == null) {
                    buffer.put((byte) 0);
                } else {
                    buffer.put((byte) 1);
                    sessionId.writeTo(buffer);
                }
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.now = buffer.getLong();

                if (buffer.get() == (byte) 0) {
                    this.currentPing = 0;
                } else {
                    this.currentPing = buffer.getInt();
                }

                if (buffer.get() == (byte) 0) {
                    this.sessionId = null;
                } else {
                    this.sessionId = new AgentSessionId();
                    this.sessionId.readFrom(buffer);
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Ping)) {
                return false;
            }

            Ping other = (Ping) o;

            return this.now == other.now
                    && this.currentPing == other.currentPing
                    && this.sessionId.equals(other.sessionId);
        }
    }

    public static class AgentRegister implements ControlRequest {
        public long accountId;
        public long agentId;
        public long agentVersion;
        public long timestamp;
        public InetSocketAddress clientAddr;
        public InetSocketAddress tunnelAddr;
        public byte[] signature;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
            try {
                buffer.putInt(2);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                buffer.putLong(this.accountId);
                buffer.putLong(this.agentId);
                buffer.putLong(this.agentVersion);
                buffer.putLong(this.timestamp);
                writeInet(buffer, this.clientAddr);
                writeInet(buffer, this.tunnelAddr);
                buffer.put(this.signature);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.accountId = buffer.getLong();
                this.agentId = buffer.getLong();
                this.agentVersion = buffer.getLong();
                this.timestamp = buffer.getLong();
                this.clientAddr = readInet(buffer);
                this.tunnelAddr = readInet(buffer);

                this.signature = new byte[32];
                buffer.get(this.signature);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentRegister that = (AgentRegister) o;

            return accountId == that.accountId
                    && agentId == that.agentId
                    && agentVersion == that.agentVersion
                    && timestamp == that.timestamp
                    && Objects.equals(clientAddr, that.clientAddr)
                    && Objects.equals(tunnelAddr, that.tunnelAddr)
                    && Arrays.equals(signature, that.signature);
        }
    }

    public static class AgentKeepAlive implements ControlRequest {
        public AgentSessionId sessionId;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
            try {
                buffer.putInt(3);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId.writeTo(buffer);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId = new AgentSessionId();
                this.sessionId.readFrom(buffer);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentKeepAlive that = (AgentKeepAlive) o;
            return Objects.equals(sessionId, that.sessionId);
        }
    }

    public static class SetupUdpChannel implements ControlRequest {
        public AgentSessionId sessionId;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
            try {
                buffer.putInt(4);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId.writeTo(buffer);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId = new AgentSessionId();
                this.sessionId.readFrom(buffer);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SetupUdpChannel that = (SetupUdpChannel) o;
            return Objects.equals(sessionId, that.sessionId);
        }
    }

    public static class AgentCheckPortMapping implements ControlRequest {
        public AgentSessionId sessionId;
        public PortRange portRange;

        @Override
        public void writeId(ByteBuffer buffer) throws IOException {
            try {
                buffer.putInt(4);
                this.sessionId.writeTo(buffer);
                this.portRange.writeTo(buffer);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId.writeTo(buffer);
                this.portRange.writeTo(buffer);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId = new AgentSessionId();
                this.sessionId.readFrom(buffer);

                this.portRange = new PortRange();
                this.portRange.readFrom(buffer);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentCheckPortMapping that = (AgentCheckPortMapping) o;
            return Objects.equals(sessionId, that.sessionId)
                    && Objects.equals(portRange, that.portRange);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, portRange);
        }
    }

    public static class PortRange implements Message {
        public InetAddress ip;
        public short portStart;
        public short portEnd;
        public PortProto proto;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                if (ip instanceof Inet4Address) {
                    buffer.put((byte) 4);
                } else {
                    buffer.put((byte) 6);
                }
                buffer.put(ip.getAddress());

                buffer.putShort(this.portStart);
                buffer.putShort(this.portEnd);

                this.proto.writeTo(buffer);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                int ipVersion = buffer.get();

                byte[] ipData;
                if (ipVersion == 4) {
                    ipData = new byte[4];
                } else if (ipVersion == 6) {
                    ipData = new byte[16];
                } else {
                    throw new IOException("Invalid IP protocol version: " + ipVersion);
                }

                buffer.get(ipData);
                this.ip = InetAddress.getByAddress(ipData);
                this.portStart = buffer.getShort();
                this.portEnd = buffer.getShort();
                this.proto = new PortProto();
                this.proto.readFrom(buffer);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortRange portRange = (PortRange) o;
            return portStart == portRange.portStart
                    && portEnd == portRange.portEnd
                    && Objects.equals(ip, portRange.ip)
                    && Objects.equals(proto, portRange.proto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, portStart, portEnd, proto);
        }
    }

    public static class PortProto implements Message {
        public PortType proto;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                if (this.proto == PortType.TCP) {
                    buffer.put((byte) 1);
                } else if (this.proto == PortType.UDP) {
                    buffer.put((byte) 2);
                } else {
                    buffer.put((byte) 3);
                }
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                int value = buffer.get();
                if (value == 1) {
                    this.proto = PortType.TCP;
                } else if (value == 2) {
                    this.proto = PortType.UDP;
                } else if (value == 3) {
                    this.proto = PortType.BOTH;
                } else {
                    throw new IOException("invalid port type variant: " + value);
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortProto portProto = (PortProto) o;
            return proto == portProto.proto;
        }

        @Override
        public int hashCode() {
            return Objects.hash(proto);
        }
    }

    public static class AgentSessionId implements Message {
        public long sessionId;
        public long accountId;
        public long agentId;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                buffer.putLong(this.sessionId);
                buffer.putLong(this.accountId);
                buffer.putLong(this.agentId);
            } catch (BufferOverflowException e) {
                throw new IOException("ran out of space", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId = buffer.getLong();
                this.accountId = buffer.getLong();
                this.agentId = buffer.getLong();
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AgentSessionId)) {
                return false;
            }

            AgentSessionId other = (AgentSessionId) o;

            return this.sessionId == other.sessionId
                    && this.accountId == other.accountId
                    && this.agentId == other.agentId;
        }

        @Override
        public AgentSessionId clone() {
            AgentSessionId id = new AgentSessionId();
            id.sessionId = this.sessionId;
            id.accountId = this.accountId;
            id.agentId = this.agentId;
            return id;
        }
    }

    public static class Pong implements Message {
        public long requestNow;
        public long serverNow;
        public long serverId;
        public int dataCenterId;
        public InetSocketAddress clientAddr;
        public InetSocketAddress tunnelAddr;
        public long sessionExpireAt;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                buffer.putLong(this.requestNow);
                buffer.putLong(this.serverNow);
                buffer.putLong(this.serverId);
                buffer.putInt(this.dataCenterId);
                writeInet(buffer, this.clientAddr);
                writeInet(buffer, this.tunnelAddr);

                if (this.sessionExpireAt == 0) {
                    buffer.put((byte)0);
                } else {
                    buffer.put((byte)1);
                    buffer.putLong(this.sessionExpireAt);
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.requestNow = buffer.getLong();
                this.serverNow = buffer.getLong();
                this.serverId = buffer.getLong();
                this.dataCenterId = buffer.getInt();
                this.clientAddr = readInet(buffer);
                this.tunnelAddr = readInet(buffer);

                if (buffer.get() == 0) {
                    this.sessionExpireAt = 0;
                } else {
                    this.sessionExpireAt = buffer.getLong();
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pong pong = (Pong) o;
            return requestNow == pong.requestNow && serverNow == pong.serverNow && serverId == pong.serverId && dataCenterId == pong.dataCenterId && sessionExpireAt == pong.sessionExpireAt && Objects.equals(clientAddr, pong.clientAddr) && Objects.equals(tunnelAddr, pong.tunnelAddr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(requestNow, serverNow, serverId, dataCenterId, clientAddr, tunnelAddr, sessionExpireAt);
        }
    }

    public enum ResponseErrorCode {
        InvalidSignature,
        Unauthorized,
        RequestQueued,
        TryAgainLater,
    }

    public static class AgentRegistered implements Message {
        public AgentSessionId sessionId;
        public long expiresAt;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId.writeTo(buffer);
                buffer.putLong(this.expiresAt);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.sessionId = new AgentSessionId();
                this.sessionId.readFrom(buffer);

                this.expiresAt = buffer.getLong();
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentRegistered that = (AgentRegistered) o;
            return expiresAt == that.expiresAt && Objects.equals(sessionId, that.sessionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, expiresAt);
        }
    }

    public static class AgentPortMapping implements Message {
        public PortRange portRange;
        public AgentSessionId target;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                this.portRange.writeTo(buffer);
                if (this.target == null) {
                    buffer.put((byte) 0);
                } else {
                    buffer.put((byte) 1);
                    buffer.putInt(1);
                    this.target.writeTo(buffer);
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.portRange = new PortRange();
                this.portRange.readFrom(buffer);

                if (buffer.get() == 0) {
                    this.target = null;
                } else {
                    int variant = buffer.getInt();
                    if (variant != 1) {
                        throw new IOException("unknown port mapping variant: " + variant);
                    }

                    this.target = new AgentSessionId();
                    this.target.readFrom(buffer);
                }
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgentPortMapping that = (AgentPortMapping) o;
            return Objects.equals(portRange, that.portRange) && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portRange, target);
        }
    }

    public static class UdpChannelDetails implements Message {
        public InetSocketAddress tunnelAddress;
        public byte[] token;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                writeInet(buffer, this.tunnelAddress);
                buffer.putLong(this.token.length);
                buffer.put(this.token);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.tunnelAddress = readInet(buffer);
                long length = buffer.getLong();
                if (length != (int)length) {
                    throw new IOException("token length too long");
                }
                this.token = new byte[(int)length];
                buffer.get(this.token);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UdpChannelDetails that = (UdpChannelDetails) o;
            return Objects.equals(tunnelAddress, that.tunnelAddress) && Arrays.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(tunnelAddress);
            result = 31 * result + Arrays.hashCode(token);
            return result;
        }
    }

    public static class NewClient implements Message {
        public InetSocketAddress connectAddr;
        public InetSocketAddress peerAddr;
        public ClaimInstructions claimInstructions;
        public long tunnelServerId;
        public int dataCenterId;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                writeInet(buffer, this.connectAddr);
                writeInet(buffer, this.peerAddr);
                this.claimInstructions.writeTo(buffer);
                buffer.putLong(this.tunnelServerId);
                buffer.putLong(this.dataCenterId);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.connectAddr = readInet(buffer);
                this.peerAddr = readInet(buffer);
                this.claimInstructions = new ClaimInstructions();
                this.claimInstructions.readFrom(buffer);
                this.tunnelServerId = buffer.getLong();
                this.dataCenterId = buffer.getInt();
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NewClient newClient = (NewClient) o;
            return tunnelServerId == newClient.tunnelServerId && dataCenterId == newClient.dataCenterId && Objects.equals(connectAddr, newClient.connectAddr) && Objects.equals(peerAddr, newClient.peerAddr) && Objects.equals(claimInstructions, newClient.claimInstructions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(connectAddr, peerAddr, claimInstructions, tunnelServerId, dataCenterId);
        }
    }

    public static class ClaimInstructions implements Message {
        public InetSocketAddress address;
        public byte[] token;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                writeInet(buffer, this.address);
                buffer.putLong(this.token.length);
                buffer.put(this.token);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public void readFrom(ByteBuffer buffer) throws IOException {
            try {
                this.address = readInet(buffer);
                long len = buffer.getLong();
                if (len != (int)len) {
                    throw new IOException("claim token too long: " + len);
                }

                this.token = new byte[(int)len];
                buffer.get(this.token);
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClaimInstructions that = (ClaimInstructions) o;
            return Objects.equals(address, that.address) && Arrays.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(address);
            result = 31 * result + Arrays.hashCode(token);
            return result;
        }
    }

    public static InetSocketAddress readInet(ByteBuffer buffer) throws IOException {
        try {
            int version = buffer.get();
            byte[] ipData;

            if (version == 4) {
                ipData = new byte[4];
            } else if (version == 6) {
                ipData = new byte[16];
            } else {
                throw new IOException("invalid ip proto version: " + version);
            }

            buffer.get(ipData);

            InetAddress ip = InetAddress.getByAddress(ipData);
            int portNumber = Short.toUnsignedInt(buffer.getShort());

            return new InetSocketAddress(ip, portNumber);
        } catch (BufferUnderflowException e) {
            throw new IOException("ran out of data", e);
        }
    }

    public static void writeInet(ByteBuffer buffer, InetSocketAddress net) throws IOException {
        try {
            InetAddress addr = net.getAddress();

            if (addr instanceof Inet4Address) {
                buffer.put((byte) 4);
            } else {
                buffer.put((byte) 6);
            }

            buffer.put(addr.getAddress());
            buffer.putShort((short) net.getPort());
        } catch (BufferOverflowException e) {
            throw new IOException("ran out of data", e);
        }
    }

}
