package gg.playit.control;

import gg.playit.api.models.PortType;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ControlMessagesTest {
    @Test
    public void testPing() throws Exception {
        ControlMessages.Ping ping = new ControlMessages.Ping();
        ping.now = 32;
        ping.currentPing = 100;
        ping.sessionId = new ControlMessages.AgentSessionId();
        ping.sessionId.sessionId = 132;
        ping.sessionId.accountId = 44;
        ping.sessionId.agentId = 56;

        testSerialize(ping);
        checkOutput(ping, "00000000000000200100000064010000000000000084000000000000002c0000000000000038");
    }

    @Test
    public void testAgentRegister() throws Exception {
        ControlMessages.AgentRegister msg = new ControlMessages.AgentRegister();

        msg.accountId = 44;
        msg.agentId = 66;
        msg.agentVersion = 321;
        msg.timestamp = 32141251;
        msg.tunnelAddr = new InetSocketAddress("209.25.140.1", 5533);
        msg.clientAddr = new InetSocketAddress("33.22.11.55", 45231);
        msg.signature = Hex.decodeHex("b428e389b63c02fa21fbc518b6bb4f85f022ff405d2c0f7de5aede9d1a8ba071");

        testSerialize(msg);
        checkOutput(msg, "000000000000002c000000000000004200000000000001410000000001ea6fc30421160b37b0af04d1198c01159db428e389b63c02fa21fbc518b6bb4f85f022ff405d2c0f7de5aede9d1a8ba071");
    }

    @Test
    public void testAgentKeepAlive() throws Exception {
        ControlMessages.AgentKeepAlive msg = new ControlMessages.AgentKeepAlive();

        msg.sessionId = new ControlMessages.AgentSessionId();
        msg.sessionId.sessionId = 132;
        msg.sessionId.accountId = 44;
        msg.sessionId.agentId = 56;

        testSerialize(msg);
        checkOutput(msg, "0000000000000084000000000000002c0000000000000038");
    }

    @Test
    public void testSetupUdpChannel()  throws Exception {
        ControlMessages.SetupUdpChannel msg = new ControlMessages.SetupUdpChannel();

        msg.sessionId = new ControlMessages.AgentSessionId();
        msg.sessionId.sessionId = 132;
        msg.sessionId.accountId = 44;
        msg.sessionId.agentId = 56;

        testSerialize(msg);
        checkOutput(msg, "0000000000000084000000000000002c0000000000000038");
    }

    @Test
    public void testAgentCheckPortMapping()  throws Exception {
        ControlMessages.AgentCheckPortMapping msg = new ControlMessages.AgentCheckPortMapping();

        msg.sessionId = new ControlMessages.AgentSessionId();
        msg.sessionId.sessionId = 132;
        msg.sessionId.accountId = 44;
        msg.sessionId.agentId = 56;

        msg.portRange = new ControlMessages.PortRange();
        msg.portRange.ip = InetAddress.getByName("209.25.140.14");
        msg.portRange.portStart = 20000;
        msg.portRange.portEnd = (short)60000;
        msg.portRange.proto = new ControlMessages.PortProto();
        msg.portRange.proto.proto = PortType.UDP;

        testSerialize(msg);
        checkOutput(msg, "0000000000000084000000000000002c000000000000003804d1198c0e4e20ea6002");
    }

    static void checkOutput(ControlMessages.Message msg, String hex) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        msg.writeTo(buffer);

        int size = buffer.position();
        buffer.flip();

        byte[] bytes = new byte[size];
        buffer.get(bytes);

        String reference = Hex.encodeHexString(bytes);
        Assert.assertEquals(hex, reference);
    }

    static void testSerialize(ControlMessages.Message msg) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        msg.writeTo(buffer);

        buffer.flip();

        ControlMessages.Message newInstance;
        try {
            newInstance = msg.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        newInstance.readFrom(buffer);
        Assert.assertEquals(msg, newInstance);
    }
}