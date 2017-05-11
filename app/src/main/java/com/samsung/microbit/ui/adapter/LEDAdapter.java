package com.samsung.microbit.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.samsung.microbit.R;

/**
 * Represents an adapter for a pattern grid.
 * Provides methods to create and manage grid cells.
 */
public class LEDAdapter extends BaseAdapter {
    private Context mContext;
    private int mDeviceCodeArray[];

    public LEDAdapter(Context c, int deviceCodeArray[]) {
        mContext = c;
        mDeviceCodeArray = deviceCodeArray;
    }

    @Override
    public int getCount() {
        return 25;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Creates a new ImageView for each item referenced by the Adapter.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if(convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            final float scale = mContext.getResources().getDisplayMetrics().density;
            int led_size = mContext.getResources().getInteger(R.integer.led_size);
            int pixels = (int) (led_size * scale + 0.5f);
            imageView.setLayoutParams(new GridView.LayoutParams(pixels, pixels));
        } else {
            imageView = (ImageView) convertView;
        }

        if(mDeviceCodeArray[position] == 1) {
            imageView.setBackground(mContext.getResources().getDrawable(R.drawable.red_white_led_btn));
        } else {
            imageView.setBackground(mContext.getResources().getDrawable(R.drawable.white_red_led_btn));

            int startIndex = position - 5;
            while(startIndex >= 0) {
                if(mDeviceCodeArray[startIndex] == 1) {
                    imageView.setBackground(mContext.getResources().getDrawable(R.drawable.red_white_led_btn));
                }
                startIndex -= 5;
            }
        }

        int pos = calculateLEDPosition(position);
        imageView.setTag(R.id.position, pos);
        imageView.setTag(R.id.ledState, 0);
        imageView.setSelected(false);
        imageView.setContentDescription("" + pos + getLEDStatus(position));

        return imageView;
    }

    /**
     * Increments by 1 position in the array to correctly read out the LED position.
     *
     * @param position Position of the cell to increment.
     * @return New incremented position.
     */
    private int calculateLEDPosition(int position) {
        return ++position;
    }

    /**
     * To read out the status of the currently selected LED at a given position.
     *
     * @param position Position of the led.
     * @return Status of the led at given position.
     */
    private String getLEDStatus(int position) {
        return mDeviceCodeArray[position] == 1 ? "on" : "off";
    }
}