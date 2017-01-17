package com.example.android.sunshine;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by OFFICE on 1/15/2017.
 */

public class WatchDataService extends WearableListenerService {

    public static final String DATA_KEY = "android.vikas.com.watchface";
    public static final String WATCH_STATUS_DATA_HIGH = "watchStatusDataHigh";
    public static final String WATCH_STATUS_DATA_LOW = "watchStatusDataLow";
    public static final String WATCH_DRAWABLE_DATA = "watchDrawableData";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
        for (DataEvent dataEvent:dataEventBuffer){

            if (dataEvent.getType()==DataEvent.TYPE_CHANGED){

            //here check for the data changes
                DataItem item = dataEvent.getDataItem();
                if (item.getUri().getPath().compareTo("/watchData") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateData(dataMap);
                }

            }
        }
        Log.d("*watchservice","");
    }

    private void updateData(DataMap dataMap) {
        //here send the data to the main watch service
        Long statusHigh = dataMap.getLong(WATCH_STATUS_DATA_HIGH);
        Long statusLow = dataMap.getLong(WATCH_STATUS_DATA_LOW);
        int drawable = dataMap.getInt(WATCH_DRAWABLE_DATA);

        Intent watchFaceUpdate = new Intent();
        watchFaceUpdate.setAction(DATA_KEY);
        watchFaceUpdate.putExtra(WATCH_STATUS_DATA_HIGH, statusHigh);
        watchFaceUpdate.putExtra(WATCH_STATUS_DATA_LOW, statusLow);
        watchFaceUpdate.putExtra(WATCH_DRAWABLE_DATA, drawable);
        sendBroadcast(watchFaceUpdate);
        Log.d("*watchservice",String.valueOf(statusHigh));

    }
}
