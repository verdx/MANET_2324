package d2d.testing.gui.gallery;

import android.graphics.Bitmap;

public class GalleryListData{

    private String path;
    private Bitmap bitmap;

    public GalleryListData(String path, Bitmap thumbail) {
        this.path = path;
        this.bitmap = thumbail;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}