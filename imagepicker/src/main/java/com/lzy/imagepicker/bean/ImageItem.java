package com.lzy.imagepicker.bean;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.lzy.imagepicker.util.Utils;

import java.io.Serializable;

public class ImageItem implements Serializable, Parcelable {

    public String name;
    public String path;
    public long size;
    public int width;
    public int height;
    public String mimeType;
    public long addTime;
    public Uri uri;
    public long duration = -1L; // only for video, in ms


    @Override
    public boolean equals(Object o) {
        if (o instanceof ImageItem) {
            ImageItem item = (ImageItem) o;
            return this.path.equalsIgnoreCase(item.path) && this.addTime == item.addTime;
        }

        return super.equals(o);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeLong(this.size);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeString(this.mimeType);
        dest.writeLong(this.addTime);
        dest.writeParcelable(this.uri, 0);
        dest.writeLong(duration);
    }

    public ImageItem() {
    }

    protected ImageItem(Parcel in) {
        this.name = in.readString();
        this.path = in.readString();
        this.size = in.readLong();
        this.width = in.readInt();
        this.height = in.readInt();
        this.mimeType = in.readString();
        this.addTime = in.readLong();
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.duration = in.readLong();
    }

    public static final Parcelable.Creator<ImageItem> CREATOR = new Parcelable.Creator<ImageItem>() {
        @Override
        public ImageItem createFromParcel(Parcel source) {
            return new ImageItem(source);
        }

        @Override
        public ImageItem[] newArray(int size) {
            return new ImageItem[size];
        }
    };

    public boolean isImage() {
        return Utils.isImage(mimeType);
    }

    public boolean isVideo() {
        return Utils.isVideo(mimeType);
    }


}
