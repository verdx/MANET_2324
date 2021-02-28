package d2d.testing.net.packets;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;

import org.videolan.libvlc.util.Dumper;

import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import d2d.testing.streaming.sessions.ReceiveSession;

public class StreamingList {

    //Añadir la clase que implemente el listener para notificarla cuando la lista cambie.
    private final Map<UUID, ReceiveSession> mList = new HashMap<>();

    public void addIdReceiveSession(UUID id, ReceiveSession receiveSession){
        mList.put(id, receiveSession);
    }

    public ReceiveSession getReceiveSession (UUID id){
        return mList.get(id);
    }

    public ReceiveSession removeReceiveSession(UUID id){
        return mList.remove(id);
    }

}
