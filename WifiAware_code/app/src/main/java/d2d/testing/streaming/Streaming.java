package d2d.testing.streaming;

import java.util.UUID;

import d2d.testing.streaming.sessions.ReceiveSession;

public class Streaming {
    private UUID mUUID;
    private ReceiveSession mReceiveSession;

    public Streaming(UUID id, ReceiveSession receiveSession){
        mUUID = id;
        mReceiveSession = receiveSession;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public ReceiveSession getReceiveSession() {
        return mReceiveSession;
    }

    public void setReceiveSession(ReceiveSession mReceiveSession) {
        this.mReceiveSession = mReceiveSession;
    }
}
