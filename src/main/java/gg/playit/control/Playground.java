package gg.playit.control;

import java.io.IOException;

public class Playground {
    public static void main(String[] args) throws IOException {
        var channel = PlayitControlChannel.setup("<your secret key>");

        while (true) {
            var feed = channel.update();
            if (feed.isPresent()) {
                var value = feed.get();
                System.out.println("Got msg: " + value);
            }
        }
    }
}
