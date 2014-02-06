package com.stfalcon.imageSorter;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.haarman.listviewanimations.ArrayAdapter;

/**
 * Created by alexandr on 05.02.14.
 */
class GoogleCardsAdapter extends ArrayAdapter<Integer> {

    private Context mContext;
    private LruCache<Integer, Bitmap> mMemoryCache;

    public GoogleCardsAdapter(Context context) {
        mContext = context;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory;
        mMemoryCache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.activity_googlecards_card, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) view.findViewById(R.id.activity_googlecards_card_textview);
            view.setTag(viewHolder);

            viewHolder.imageView = (ImageView) view.findViewById(R.id.activity_googlecards_card_imageview);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.textView.setText("This is card " + (getItem(position) + 1));
        setImageView(viewHolder, position);

        return view;
    }

    private void setImageView(ViewHolder viewHolder, int position) {

        int id = getItem(position);
        Bitmap bitmap = getBitmapFromMemCache(id);
        if (bitmap == null) {
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(),
                    id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            addBitmapToMemoryCache(id, bitmap);
        }
        viewHolder.imageView.setImageBitmap(bitmap);
    }

    private void addBitmapToMemoryCache(int key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(int key) {
        return mMemoryCache.get(key);
    }

    private static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
