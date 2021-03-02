package d2d.testing.streaming;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import d2d.testing.streaming.sessions.ReceiveSession;
import d2d.testing.streaming.sessions.SessionBuilder;


public class StreamingList {

    static private StreamingList INSTANCE = null;

    //Añadir la clase que implemente el listener para notificarla cuando la lista cambie.
    private final Map<UUID, ReceiveSession> mList;
    private SessionBuilder mLocalStreamingBuilder;
    private UUID mLocalStreamingUUID;

    public static StreamingList getInstance(){
        if(INSTANCE == null) {
            INSTANCE = new StreamingList();
        }
        return INSTANCE;
    }

    private StreamingList(){
        mList = new HashMap<>();
        mLocalStreamingUUID = null;
    }

    public synchronized void addReceiveStreaming(UUID id, ReceiveSession receiveSession){
        mList.put(id, receiveSession);
    }

    public synchronized void addLocalStreaming(UUID id, SessionBuilder sessionBuilder){
        mLocalStreamingUUID = id;
        mLocalStreamingBuilder = sessionBuilder;
    }



    public synchronized ReceiveSession getStreaming(UUID id){
        return mList.get(id);
    }

    public synchronized ReceiveSession removeStreaming(UUID id){
        return mList.remove(id);
    }

}
