package d2d.testing.streaming;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import d2d.testing.gui.SaveStream;
import d2d.testing.streaming.sessions.SessionBuilder;


public class StreamingRecord {

    static private StreamingRecord INSTANCE = null;

    private static class Record{
        private Streaming mStreaming;
        private boolean mAllowDispatch;
        private SaveStream mSaveStream;

        public Record(Streaming streaming, boolean allowDispatch, SaveStream saveStream){
            mStreaming = streaming;
            mAllowDispatch = allowDispatch;
            mSaveStream =  saveStream;
        }
    }

    private final Map<UUID, Record> mRecords;
    private SessionBuilder mLocalStreamingBuilder;
    private UUID mLocalStreamingUUID;
    private String mLocalStreamingName;

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
        Record record = new Record(streaming, allowDispatch, null);
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

    public synchronized void changeStreamingDownload(UUID id, boolean isDownload){
        Record rec = mRecords.get(id);
        if(rec != null){
            rec.mStreaming.setDownload(isDownload);
        }
        for(StreamingRecordObserver ob : mObservers){
            ob.streamingUpdate(rec.mStreaming, isDownload);
        }
    }

    public synchronized void startDownload(final Context c, final UUID id){
        Record record = mRecords.get(id);
        SaveStream saveStream = new SaveStream(c, id.toString());
        record.mSaveStream = saveStream;
        saveStream.startDownload();
    }

    public synchronized void stopDownload(UUID id){
        Record record = mRecords.get(id);
        record.mSaveStream.stopDownload();
    }

    public synchronized void addLocalStreaming(UUID id, String name, SessionBuilder sessionBuilder){
        mLocalStreamingUUID = id;
        mLocalStreamingName = name;
        mLocalStreamingBuilder = sessionBuilder;
        for(StreamingRecordObserver ob : mObservers){
            ob.localStreamingAvailable(id, name ,sessionBuilder);
        }
    }

    public synchronized void removeLocalStreaming(){
        mLocalStreamingUUID = null;
        mLocalStreamingName = null;
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
            ob.localStreamingAvailable(mLocalStreamingUUID, mLocalStreamingName, mLocalStreamingBuilder);
        }
        for(Record rec : mRecords.values()){
            ob.streamingAvailable(rec.mStreaming, rec.mAllowDispatch);
        }
    }

    public synchronized void removeObserver(StreamingRecordObserver ob){
        mObservers.remove(ob);
    }

    public SessionBuilder getLocalStreamingBuilder() {
        return mLocalStreamingBuilder;
    }

    public UUID getLocalStreamingUUID() {
        return mLocalStreamingUUID;
    }

    public String getLocalStreamingName() {
        return mLocalStreamingName;
    }

}
