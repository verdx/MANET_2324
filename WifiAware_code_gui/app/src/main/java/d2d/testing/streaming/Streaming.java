package d2d.testing.streaming;

import java.util.UUID;

import d2d.testing.streaming.sessions.ReceiveSession;

public class Streaming {
    private UUID mUUID;
    private String mName;
    private ReceiveSession mReceiveSession;

    public Streaming(UUID id, String name, ReceiveSession receiveSession){
        mUUID = id;
        mReceiveSession = receiveSession;
        mName = name;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public ReceiveSession getReceiveSession() {
        return mReceiveSession;
    }

    public String getName() {
        return mName;
    }

    public void setReceiveSession(ReceiveSession mReceiveSession) {
        this.mReceiveSession = mReceiveSession;
    }
}
