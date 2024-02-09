package gg.playit.control;

import java.net.DatagramSocket;

public class ControlChannel {
    DatagramSocket socket;

    public ControlChannel(DatagramSocket socket) {
        this.socket = socket;
    }

}