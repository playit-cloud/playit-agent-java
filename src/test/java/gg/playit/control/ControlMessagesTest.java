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

    @Test
    public void testPong() throws Exception {
        ControlMessages.Pong msg = new ControlMessages.Pong();
        msg.requestNow = 321;
        msg.serverNow = 5342;
        msg.serverId = 654;
        msg.dataCenterId = 564;
        msg.clientAddr = new InetSocketAddress("209.25.140.1", 5533);
        msg.tunnelAddr = new InetSocketAddress("33.22.11.55", 45231);
        msg.sessionExpireAt = 4653;

        testSerialize(msg);
        checkOutput(msg, "000000000000014100000000000014de000000000000028e0000023404d1198c01159d0421160b37b0af01000000000000122d");
    }

    @Test
    public void testAgentRegistered() throws Exception {
        ControlMessages.AgentRegistered msg = new ControlMessages.AgentRegistered();
        msg.sessionId = new ControlMessages.AgentSessionId();
        msg.sessionId.sessionId = 132;
        msg.sessionId.accountId = 44;
        msg.sessionId.agentId = 56;
        msg.expiresAt = 42131;

        testSerialize(msg);
        checkOutput(msg, "0000000000000084000000000000002c0000000000000038000000000000a493");
    }

    @Test
    public void testAgentPortMapping() throws Exception {
        ControlMessages.AgentPortMapping msg = new ControlMessages.AgentPortMapping();
        msg.portRange = new ControlMessages.PortRange();
        msg.portRange.ip = InetAddress.getByName("209.25.140.133");
        msg.portRange.portStart = (short)3214;
        msg.portRange.portEnd = (short)3216;
        msg.portRange.proto = new ControlMessages.PortProto();
        msg.portRange.proto.proto = PortType.BOTH;

        msg.target = new ControlMessages.AgentSessionId();
        msg.target.sessionId = 132;
        msg.target.accountId = 44;
        msg.target.agentId = 56;

        testSerialize(msg);
        checkOutput(msg, "04d1198c850c8e0c900301000000010000000000000084000000000000002c0000000000000038");
    }

    @Test
    public void testUdpChannelDetails() throws Exception {
        ControlMessages.UdpChannelDetails msg = new ControlMessages.UdpChannelDetails();
        msg.tunnelAddress = new InetSocketAddress("2602:fbaf:808::2", 4213);
        msg.token = Hex.decodeHex("dbce811e5fb75185ede16db49a291b4dd1aca59207ce8c534baff4d53c95aa587696540790e0e4055f0dc5ff41cef19cf11ad9b53142fe1f25e51ea126c15dc7");

        testSerialize(msg);
        checkOutput(msg, "062602fbaf08080000000000000000000210750000000000000040dbce811e5fb75185ede16db49a291b4dd1aca59207ce8c534baff4d53c95aa587696540790e0e4055f0dc5ff41cef19cf11ad9b53142fe1f25e51ea126c15dc7");
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