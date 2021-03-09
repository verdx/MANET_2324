package d2d.testing.gui;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import java.util.ArrayList;

import d2d.testing.MainActivity;
import d2d.testing.R;


public class FragmentStreams extends Fragment {
    View view;

    private ArrayList<StreamDetail> streamList = new ArrayList();
    private StreamListAdapter arrayAdapter;
    private ListView streamsListView;
    private MainActivity mainActivity;

    public FragmentStreams(){

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.streams_fragment,container,false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        streamsListView = view.findViewById(R.id.streamListView);
        arrayAdapter = new StreamListAdapter(getContext(), streamList);
        streamsListView.setAdapter(arrayAdapter);
        execListener();
    }

    public void setMainActivity(MainActivity activity){
        mainActivity = activity;
    }

    public void updateList(boolean on_off, String ip, String name){
        if(!ip.equals("0.0.0.0")) {
            StreamDetail detail = new StreamDetail(name,ip);
            if (on_off) {
                if (!streamList.contains(detail))
                    streamList.add(detail);
            } else {
                if (streamList.contains(detail))
                    streamList.remove(detail);
            }

            arrayAdapter.notifyDataSetChanged();
        }
    }

    private void execListener() {

        streamsListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               mainActivity.openViewStreamActivity(getActivity(),streamList.get(position).getIp());
            }
        });
    }

    public ArrayList<StreamDetail> getStreamList(){
        return this.streamList;
    }
}
