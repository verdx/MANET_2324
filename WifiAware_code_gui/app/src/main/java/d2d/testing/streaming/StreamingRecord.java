package d2d.testing.streaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import d2d.testing.streaming.sessions.SessionBuilder;


public class StreamingRecord {

    static private StreamingRecord INSTANCE = null;

    private static class Record{
        private Streaming mStreaming;
        private boolean mAllowDispatch;

        public Record(Streaming streaming, boolean allowDispatch){
            mStreaming = streaming;
            mAllowDispatch = allowDispatch;
        }
    }

    private final Map<UUID, Record> mRecords;
    private SessionBuilder mLocalStreamingBuilder;
    private UUID mLocalStreamingUUID;

    private List<StreamingRecordObserver> mObservers;

    public static synchronized StreamingRecord getInstance(){
        if(INSTANCE == null) {
            INSTANCE = new StreamingRecord();
        }
        return INSTANCE;
    }

    private StreamingRecord(){
        mRecords = new HashMap<>();
        mObservers = new ArrayList<>();
        mLocalStreamingUUID = null;
    }

    public synchronized void addStreaming(Streaming streaming, boolean allowDispatch){
        Record record = new Record(streaming, allowDispatch);
        mRecords.put(streaming.getUUID(), record);
        for(StreamingRecordObserver ob : mObservers){
            ob.streamingAvailable(streaming, allowDispatch);
        }
    }

    public synchronized void changeStreamingDispatchable(UUID id, boolean allowDispatch){
        Record rec = mRecords.get(id);
        if(rec != null){
            rec.mAllowDispatch = allowDispatch;
        }
        for(StreamingRecordObserver ob : mObservers){
            ob.streamingAvailable(rec.mStreaming, allowDispatch);
        }
    }

    public synchronized void addLocalStreaming(UUID id, SessionBuilder sessionBuilder){
        mLocalStreamingUUID = id;
        mLocalStreamingBuilder = sessionBuilder;
        for(StreamingRecordObserver ob : mObservers){
            ob.localStreamingAvailable(id, sessionBuilder);
        }
    }

    public synchronized void removeLocalStreaming(){
        mLocalStreamingUUID = null;
        mLocalStreamingBuilder = null;
        for(StreamingRecordObserver ob : mObservers){
            ob.localStreamingUnavailable();
        }
    }

    public synchronized Streaming getStreaming(UUID id){
        Record rec = mRecords.get(id);
        if(rec != null) return rec.mStreaming;
        return null;
    }

    public synchronized boolean streamingExist(UUID id){
        if(mLocalStreamingUUID != null && mLocalStreamingUUID.equals(id)) return true;
        Record rec = mRecords.get(id);
        if(rec != null) return true;
        return false;
    }

    public synchronized List<Streaming> getStreamings(){
        List<Streaming> list = new ArrayList<>();
        for(Record rec : mRecords.values()){
            list.add(rec.mStreaming);
        }
        return list;
    }

    public synchronized Streaming removeStreaming(UUID id){
        Record rec =  mRecords.remove(id);
        if(rec != null){
            for(StreamingRecordObserver ob : mObservers){
                ob.streamingUnavailable(rec.mStreaming);
            }
            return rec.mStreaming;
        }
        return null;
    }

    public synchronized void addObserver(StreamingRecordObserver ob){
        mObservers.add(ob);
        if(mLocalStreamingUUID != null){
            ob.localStreamingAvailable(mLocalStreamingUUID, mLocalStreamingBuilder);
        }
        for(Record rec : mRecords.values()){
            ob.streamingAvailable(rec.mStreaming, rec.mAllowDispatch);
        }
    }

    public synchronized void removeObserver(StreamingRecordObserver ob){
        mObservers.remove(ob);
    }

}
