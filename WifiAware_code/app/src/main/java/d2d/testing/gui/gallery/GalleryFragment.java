package d2d.testing.gui.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.crypto.ShortBufferException;

import d2d.testing.R;
import d2d.testing.gui.ViewStreamActivity;


public class GalleryFragment extends Fragment {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private ArrayList<GalleryListData> galleryListData = new ArrayList<>();
    private ArrayList<File> videoFiles;

    private SwipeRefreshLayout swipeContainer;

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
                            itv.remove();
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
        recyclerView = root.findViewById(R.id.listVideos);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2 , GridLayoutManager.VERTICAL,false));

        swipeContainer = root.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                findFiles();

            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_dark,
                android.R.color.holo_blue_light,
                android.R.color.darker_gray);

        findFiles();
        return root;
    }

    public void findFiles(){

        String path = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString();

        File directory = new File(path);
        videoFiles = new ArrayList<>(Arrays.asList(directory.listFiles()));
        videoFiles.sort(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getPath().compareTo(o1.getPath());
            }
        });

        galleryListData = new ArrayList<>(videoFiles.size());
        adapter = new GalleryAdapter(galleryListData, this);
        recyclerView.setAdapter(adapter);

        for (int i = 0; i < videoFiles.size(); i++) {
            galleryListData.add(new GalleryListData(videoFiles.get(i).getName(), null));
        }

        adapter.setListData(galleryListData);
        findBitmaps();
    }

    public void findBitmaps(){

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();

                int size = videoFiles.size();
                for (int i = 0; i < size; i++) {
                    try {
                        mmr.setDataSource(videoFiles.get(i).getPath());
                        Bitmap b = mmr.getFrameAtTime(2000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if(b != null) galleryListData.get(i).setBitmap(rotateBitmap(b));
                        else throw new ShortBufferException("Video demasiado corto...");

                    } catch (Exception e) {
                        e.printStackTrace();
                        //videoFiles.get(i).delete();
                        galleryListData.remove(i);
                        videoFiles.remove(i);
                        size -= 1;
                        i -= 1;
                    }
                }
                mmr.release();

                FragmentActivity fragment = getActivity();
                //A veces cambias de pestaña y el fragmento es null, entoces crashea
                if(fragment != null) fragment.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        });
        t.start();
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
