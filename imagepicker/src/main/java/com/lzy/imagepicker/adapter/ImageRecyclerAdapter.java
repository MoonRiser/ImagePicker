package com.lzy.imagepicker.adapter;

import android.Manifest;
import android.app.Activity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.R;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageBaseActivity;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.imagepicker.util.InnerToaster;
import com.lzy.imagepicker.util.Utils;
import com.lzy.imagepicker.view.SuperCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import static com.lzy.imagepicker.ImagePicker.LOAD_COUNT_ONCE;


public class ImageRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {


    private static final int ITEM_TYPE_CAMERA = 0;
    private static final int ITEM_TYPE_NORMAL = 1;
    private ImagePicker imagePicker;
    private Activity mActivity;
    private LinkedList<ImageItem> images = new LinkedList<>();
    private ArrayList<ImageItem> mSelectedImages;
    private boolean isShowCamera;
    private int mImageSize;
    private LayoutInflater mInflater;
    private OnImageItemClickListener listener;

    public void setOnImageItemClickListener(OnImageItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnImageItemClickListener {
        void onImageItemClick(View view, ImageItem imageItem, int position);
    }

    public void refreshData(List<ImageItem> images) {
        if (images == null) return;
        this.images.clear();
        this.images.addAll(images);
        notifyDataSetChanged();
    }

    public void insertData(List<ImageItem> images, boolean isUpdate) {
        int originSize = this.images.size();
        if (!isUpdate) {
            this.images.addAll(images);
        } else {
            this.images.addAll(0, images);
        }
        if (isShowCamera && originSize == 0) {
            originSize++;
        }
        notifyItemRangeInserted(originSize, images.size());
        Log.i("xres", "run once" + originSize + "#  " + System.currentTimeMillis());

    }


    public ImageRecyclerAdapter(Activity activity) {
        this.mActivity = activity;

        mImageSize = Utils.getImageItemWidth(mActivity);
        imagePicker = ImagePicker.getInstance();
        isShowCamera = imagePicker.isShowCamera();
        mSelectedImages = imagePicker.getSelectedImages();
        mInflater = LayoutInflater.from(activity);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_CAMERA) {
            return new CameraViewHolder(mInflater.inflate(R.layout.adapter_camera_item, parent, false));
        }
        final ImageViewHolder imageViewHolder = new ImageViewHolder(mInflater.inflate(R.layout.adapter_image_list_item, parent, false));
        imageViewHolder.ivThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageItem imageItem = getItem(imageViewHolder.getBindingAdapterPosition());
                final int position = imageViewHolder.getBindingAdapterPosition();
                if (listener != null)
                    listener.onImageItemClick(imageViewHolder.itemView, imageItem, position);
            }
        });
        imageViewHolder.checkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageItem imageItem = getItem(imageViewHolder.getBindingAdapterPosition());
                final int position = imageViewHolder.getBindingAdapterPosition();
                imageViewHolder.cbCheck.setChecked(!imageViewHolder.cbCheck.isChecked());
                int selectLimit = imagePicker.getSelectLimit();
                if (imageViewHolder.cbCheck.isChecked() && mSelectedImages.size() >= selectLimit) {
                    InnerToaster.obj(mActivity).show(mActivity.getString(R.string.ip_select_limit, selectLimit));
                    imageViewHolder.cbCheck.setChecked(false);
                    imageViewHolder.mask.setVisibility(View.GONE);
                } else {
                    imagePicker.addSelectedImageItem(position, imageItem, imageViewHolder.cbCheck.isChecked());
                    imageViewHolder.mask.setVisibility(View.VISIBLE);
                }
            }
        });
        return imageViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder instanceof CameraViewHolder) {
            ((CameraViewHolder) holder).bindCamera();
        } else if (holder instanceof ImageViewHolder) {
            Log.i("xres", "onBindViewHolder run once" + "#  " + System.currentTimeMillis());

            ((ImageViewHolder) holder).bind(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isShowCamera) return position == 0 ? ITEM_TYPE_CAMERA : ITEM_TYPE_NORMAL;
        return ITEM_TYPE_NORMAL;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return isShowCamera ? images.size() + 1 : images.size();
    }

    public ImageItem getItem(int position) {
        if (isShowCamera) {
            if (position == 0) return null;
            return images.get(position - 1);
        } else {
            return images.get(position);
        }
    }

    private class ImageViewHolder extends ViewHolder {

        ImageView ivThumb;
        View mask;
        View checkView;
        SuperCheckBox cbCheck;
        TextView tvDuration;


        ImageViewHolder(View itemView) {
            super(itemView);
            ivThumb = (ImageView) itemView.findViewById(R.id.iv_thumb);
            mask = itemView.findViewById(R.id.mask);
            checkView = itemView.findViewById(R.id.checkView);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            cbCheck = (SuperCheckBox) itemView.findViewById(R.id.cb_check);
            itemView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize));
        }

        void bind(final int position) {
            final ImageItem imageItem = getItem(position);
            if (imagePicker.isMultiMode()) {
                cbCheck.setVisibility(View.VISIBLE);
                boolean checked = mSelectedImages.contains(imageItem);
                if (checked) {
                    mask.setVisibility(View.VISIBLE);
                    cbCheck.setChecked(true);
                } else {
                    mask.setVisibility(View.GONE);
                    cbCheck.setChecked(false);
                }
            } else {
                cbCheck.setVisibility(View.GONE);
            }
            // TODO: 1/3/21  xres
            if (imageItem.isVideo()) {
                tvDuration.setVisibility(View.VISIBLE);
                tvDuration.setText(DateUtils.formatElapsedTime(imageItem.duration / 1000));
            }
            if (imageItem.isImage()) {
                tvDuration.setVisibility(View.GONE);
            }

            imagePicker.getImageLoader().displayImage(mActivity, imageItem.uri, ivThumb, mImageSize, mImageSize);
        }

    }

    private class CameraViewHolder extends ViewHolder {

        View mItemView;

        CameraViewHolder(View itemView) {
            super(itemView);
            mItemView = itemView;
        }

        void bindCamera() {
            mItemView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mImageSize));
            mItemView.setTag(null);
            mItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!((ImageBaseActivity) mActivity).checkPermission(Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, ImageGridActivity.REQUEST_PERMISSION_CAMERA);
                    } else {
                        imagePicker.takePicture(mActivity, ImagePicker.REQUEST_CODE_TAKE);
                    }
                }
            });
        }
    }
}
