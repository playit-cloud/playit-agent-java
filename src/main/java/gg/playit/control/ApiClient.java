package gg.playit.control;

import gg.playit.messages.SocketAddr;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;

public class ApiClient implements Closeable {
    private static final String API_URL = "https://api.playit.cloud/agent";

    private final CloseableHttpClient client;
    private final String secret;

    public ApiClient(String secret) {
        client = HttpClients.createDefault();
        this.secret = secret;
    }

    public byte[] getSignedAgentRegisterData(long version, SocketAddr clientAddr, SocketAddr tunnelAddr) throws IOException, DecoderException {
        var post = new HttpPost(API_URL);

        String requestBody = String.join("\n",
                "{",
                "\"type\": \"sign-agent-register\",",
                "\"agent_version\": " + Long.toUnsignedString(version) + ",",
                "\"client_addr\": \"" + clientAddr.toString() + "\",",
                "\"tunnel_addr\": \"" + tunnelAddr.toString() + "\"",
                "}"
        );
        post.setEntity(new StringEntity(requestBody));
        post.addHeader("Authorization", String.format("agent-key %s", this.secret));
        post.addHeader("Content-Type", "application/json");

        var response = client.execute(post);
        var responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Got API error: " + response.getStatusLine().getStatusCode() + ", " + responseBody);
        }

        System.out.println("Data: " + responseBody);

        var pos = responseBody.indexOf("\"data\":");
        if (pos == -1) {
            throw new Error("could not find \"data\": in response, " + responseBody);
        }

        var dataStart = responseBody.indexOf("\"", pos + "\"data\":".length());
        var dataEnd = responseBody.indexOf("\"", dataStart + 1);

        var hexData = responseBody.substring(dataStart + 1, dataEnd);
        return Hex.decodeHex(hexData);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
