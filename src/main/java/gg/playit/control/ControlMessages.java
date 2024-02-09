package gg.playit.control;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

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
        public long currentPing;
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
                    buffer.putLong(currentPing);
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
                    this.currentPing = buffer.getLong();
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
    }

    public static class PortRange implements Message {
        public InetAddress ip;
        public short portStart;
        public short portEnd;

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

                this.ip = InetAddress.getByAddress(ipData);
                this.portStart = buffer.getShort();
                this.portEnd = buffer.getShort();
            } catch (BufferUnderflowException e) {
                throw new IOException("ran out of data", e);
            }
        }
    }

    public enum PortProto implements Message {
        Both,
        Tcp,
        Udp;

        @Override
        public void writeTo(ByteBuffer buffer) throws IOException {
            try {
                if (this == PortProto.Tcp) {
                    buffer.put((byte) 1);
                } else if (this == PortProto.Udp) {
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
            throw new IOException("cannot read enum constant");
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
            short portNumber = buffer.getShort();

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
            }
            else {
                buffer.put((byte) 6);
            }

            buffer.put(addr.getAddress());
            buffer.putShort((short)net.getPort());
        } catch (BufferOverflowException e) {
            throw new IOException("ran out of data", e);
        }
    }
}
