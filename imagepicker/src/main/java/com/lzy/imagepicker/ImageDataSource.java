package com.lzy.imagepicker;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.lzy.imagepicker.bean.ImageFolder;
import com.lzy.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;


public class ImageDataSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;
    public static final int LOADER_CATEGORY = 1;
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");

    private final String[] MEDIA_PROJECTION = {
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.Files.FileColumns._ID,
            "duration"
    };

    // TODO: 1/3/21  xres
    private static final String SELECTION_ALL =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };

    private static final String SELECTION_ONLY_IMAGES =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
    private static final String[] SELECTION_IMAGES_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
    };


    private final FragmentActivity activity;
    private final OnImagesLoadedListener loadedListener;
    private final ArrayList<ImageFolder> imageFolders = new ArrayList<>();
    private int mLoadedCount;


    public ImageDataSource(FragmentActivity activity, String path, OnImagesLoadedListener loadedListener) {
        this.activity = activity;
        this.loadedListener = loadedListener;
        mLoadedCount = 0;

        LoaderManager loaderManager = LoaderManager.getInstance(activity);
        if (path == null) {
            loaderManager.initLoader(LOADER_ALL, null, this);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("path", path);
            loaderManager.initLoader(LOADER_CATEGORY, bundle, this);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if (id == LOADER_ALL) {
            if (ImagePicker.getInstance().isWithVideo()) {
                cursorLoader = new CursorLoader(activity, QUERY_URI, MEDIA_PROJECTION, SELECTION_ALL, SELECTION_ALL_ARGS, MEDIA_PROJECTION[6] + " DESC");

            } else {
                cursorLoader = new CursorLoader(activity, QUERY_URI, MEDIA_PROJECTION, SELECTION_ONLY_IMAGES, SELECTION_IMAGES_ARGS, MEDIA_PROJECTION[6] + " DESC");
            }
        }
        if (id == LOADER_CATEGORY)
            cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION, MEDIA_PROJECTION[1] + " like '%" + args.getString("path") + "%'", null, MEDIA_PROJECTION[6] + " DESC");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {
            return;
        }
        if (mLoadedCount == data.getCount()) {
            return;
        }
        imageFolders.clear();
        mLoadedCount = data.getCount();
        ArrayList<ImageItem> allImages = new ArrayList<>();
        while (data.moveToNext()) {
            String imageName = data.getString(data.getColumnIndexOrThrow(MEDIA_PROJECTION[0]));
            String imagePath = data.getString(data.getColumnIndexOrThrow(MEDIA_PROJECTION[1]));

            File file = new File(imagePath);
            if (!file.exists() || file.length() <= 0) {
                continue;
            }
            // TODO: 1/3/21  xres
            long duration = data.getLong(data.getColumnIndex("duration"));
            long id = data.getLong(data.getColumnIndex(MediaStore.Files.FileColumns._ID));
            long imageSize = data.getLong(data.getColumnIndexOrThrow(MEDIA_PROJECTION[2]));
            int imageWidth = data.getInt(data.getColumnIndexOrThrow(MEDIA_PROJECTION[3]));
            int imageHeight = data.getInt(data.getColumnIndexOrThrow(MEDIA_PROJECTION[4]));
            String imageMimeType = data.getString(data.getColumnIndexOrThrow(MEDIA_PROJECTION[5]));
            long imageAddTime = data.getLong(data.getColumnIndexOrThrow(MEDIA_PROJECTION[6]));
            ImageItem imageItem = new ImageItem();
            imageItem.name = imageName;
            imageItem.path = imagePath;
            imageItem.size = imageSize;
            imageItem.width = imageWidth;
            imageItem.height = imageHeight;
            imageItem.mimeType = imageMimeType;
            imageItem.addTime = imageAddTime;
            // TODO: 1/3/21  xres
            Uri contentUri;
            if (imageItem.isImage()) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (imageItem.isVideo()) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                imageItem.duration = duration;
            } else {
                // ?
                contentUri = MediaStore.Files.getContentUri("external");
            }
            imageItem.uri = ContentUris.withAppendedId(contentUri, id);

            allImages.add(imageItem);
            File imageFile = new File(imagePath);
            File imageParentFile = imageFile.getParentFile();
            ImageFolder imageFolder = new ImageFolder();
            imageFolder.name = imageParentFile.getName();
            imageFolder.path = imageParentFile.getAbsolutePath();

            if (!imageFolders.contains(imageFolder)) {
                ArrayList<ImageItem> images = new ArrayList<>();
                images.add(imageItem);
                imageFolder.cover = imageItem;
                imageFolder.images = images;
                imageFolders.add(imageFolder);
            } else {
                imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
            }
        }
        if (data.getCount() > 0 && allImages.size() > 0) {
            ImageFolder allImagesFolder = new ImageFolder();
            allImagesFolder.name = activity.getResources().getString(R.string.ip_all_images);
            allImagesFolder.path = "/";
            allImagesFolder.cover = allImages.get(0);
            allImagesFolder.images = allImages;
            imageFolders.add(0, allImagesFolder);
        }
        ImagePicker.getInstance().setImageFolders(imageFolders);
        loadedListener.onImagesLoaded(imageFolders);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        System.out.println("--------");
    }

    public interface OnImagesLoadedListener {
        void onImagesLoaded(List<ImageFolder> imageFolders);
    }
}
