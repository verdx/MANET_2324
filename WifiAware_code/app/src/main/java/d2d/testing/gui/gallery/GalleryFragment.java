package d2d.testing.gui.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import d2d.testing.R;
import d2d.testing.gui.ViewStreamActivity;
import wseemann.media.FFmpegMediaMetadataRetriever;


public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private ArrayList<GalleryListData> galleryListData;
    private ArrayList<File> videoFiles;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_gallery, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {

            for (Iterator<GalleryListData> itg = galleryListData.iterator(); itg.hasNext(); ) {
                GalleryListData value = itg.next();
                if (value.isSelected()) {
                    for (Iterator<File> itv = videoFiles.iterator(); itv.hasNext(); ) {
                        File file = itv.next();
                        if(file.getName().equals(value.getPath())){
                            file.delete();
                            break;
                        }
                    }
                    itg.remove();
                }
            }
            adapter.setListData(galleryListData);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        String path = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString();

        File directory = new File(path);
        videoFiles = new ArrayList<>(Arrays.asList(directory.listFiles()));

        galleryListData = new ArrayList<>(videoFiles.size());

        for (int i = 0; i < videoFiles.size(); i++) {
            galleryListData.add(new GalleryListData(videoFiles.get(i).getName(), null));
        }

        recyclerView = root.findViewById(R.id.listVideos);
        adapter = new GalleryAdapter(galleryListData, this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2 , GridLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(adapter);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();

                int size = videoFiles.size();
                for (int i = 0; i < size; i++) {
                    try {
                        mmr.setDataSource(videoFiles.get(i).getPath());
                        Bitmap b = mmr.getFrameAtTime(100000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                        if(b != null) galleryListData.get(i).setBitmap(rotateBitmap(b));
                        else throw new Exception("Video demasiado corto...");

                    }catch (Exception e){
                        e.printStackTrace();
                        galleryListData.remove(i);
                        videoFiles.remove(i);
                        size -= 1;
                        i -= 1;
                    }
                }
                mmr.release();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
        t.start();

        adapter.notifyDataSetChanged();

        return root;
    }

    private Bitmap rotateBitmap(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    }


    public void startVideo(int position){
        Intent intent = new Intent(getContext(), ViewStreamActivity.class);
        intent.putExtra("isFromGallery", true);
        intent.putExtra("path", videoFiles.get(position).getAbsolutePath());
        getActivity().startActivity(intent);
    }
}
