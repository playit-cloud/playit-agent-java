package gg.playit.control;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ControlRpcMessage<M extends ControlMessages.ControlRequest> implements ControlMessages.Message {
    public long requestId;
    public M content;

    public ControlRpcMessage(long id, M msg) {
        this.requestId = id;
        this.content = msg;
    }

    @Override
    public void writeTo(ByteBuffer buffer) throws IOException {
        buffer.putLong(requestId);
        this.content.writeId(buffer);
        this.content.writeTo(buffer);
    }

    @Override
    public void readFrom(ByteBuffer buffer) throws IOException {
        throw new IOException("failed to read message");
    }
}
