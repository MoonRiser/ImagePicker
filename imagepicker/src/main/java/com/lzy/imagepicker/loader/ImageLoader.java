package com.lzy.imagepicker.loader;

import android.app.Activity;
import android.net.Uri;
import android.widget.ImageView;

import com.lzy.imagepicker.bean.ImageItem;

import java.io.Serializable;


public interface ImageLoader extends Serializable {

    void displayImage(Activity activity,Uri uri, ImageView imageView, int width, int height);

    void displayImagePreview(Activity activity, Uri uri, ImageView imageView, int width, int height);

    void clearMemoryCache();
}
