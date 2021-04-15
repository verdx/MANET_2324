package d2d.testing.gui.gallery;

import android.graphics.Bitmap;

public class GalleryListData{

    private String path;
    private Bitmap thumbail;

    public GalleryListData(String path, Bitmap thumbail) {
        this.path = path;
        this.thumbail = thumbail;
    }

    public Bitmap getThumbail() {
        return thumbail;
    }

    public void setThumbail(Bitmap thumbail) {
        this.thumbail = thumbail;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}