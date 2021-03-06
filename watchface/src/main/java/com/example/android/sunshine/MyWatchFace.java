/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.example.android.sunshine.WatchDataService.WATCH_DRAWABLE_DATA;
import static com.example.android.sunshine.WatchDataService.WATCH_STATUS_DATA_HIGH;
import static com.example.android.sunshine.WatchDataService.WATCH_STATUS_DATA_LOW;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEEEEE, MMM d yyyy", Locale.US);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint tvDay;
        Paint tvStatus;
        Paint rectanle;
        boolean mAmbient;
        Calendar mCalendar;

        String statusStringLow="";
        String statusStringHigh="";
        int drawableId = 0;
        String dateString = "";

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        final BroadcastReceiver mWatchFaceUpdatesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                statusStringLow = String.valueOf(intent.getLongExtra(WATCH_STATUS_DATA_LOW,0))+"\u00B0";
                statusStringHigh = String.valueOf(intent.getLongExtra(WATCH_STATUS_DATA_HIGH,0))+"\u00B0";
                drawableId =  Utils.getIconResourceForWeatherCondition(intent.getIntExtra(WATCH_DRAWABLE_DATA,-1));
                Log.d("*watchservice ",String.valueOf(drawableId+" "+statusStringHigh));
                invalidate();

            }
        };
        float mXOffset;
        float mYOffset;

        float dateXOffsets;
        float dateYOffsets;

        float statusXOffsets;
        float statusYOffsets;

        float bitmapIconXOffsets;
        float bitmapIconYOffsets;

        Bitmap bmWeatherStatus = null;

        Rect rect;

        Resources resources;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(com.example.android.sunshine.R.dimen.time_y_offsets);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(com.example.android.sunshine.R.color.colorPrimary));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(com.example.android.sunshine.R.color.digital_text));

            tvDay = new TextPaint();
            tvDay = createTextPaint(ContextCompat.getColor(getApplicationContext(), com.example.android.sunshine.R.color.white));

            tvStatus = new TextPaint();
            tvStatus = createTextPaint(ContextCompat.getColor(getApplicationContext(), com.example.android.sunshine.R.color.white));

            rectanle = new Paint();
            rectanle.setStyle(Paint.Style.FILL);
            rectanle.setColor(Color.argb(70,255,255,255));

            rect = new Rect(45,180,275,182);

            dateString = simpleDateFormat.format(Calendar.getInstance().getTime()).toUpperCase();

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);

            IntentFilter watchFaceUpdatesFilter = new IntentFilter(WatchDataService.DATA_KEY);
            MyWatchFace.this.registerReceiver(mWatchFaceUpdatesReceiver, watchFaceUpdatesFilter);

        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            MyWatchFace.this.unregisterReceiver(mWatchFaceUpdatesReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            mXOffset = resources.getDimension(com.example.android.sunshine.R.dimen.time_x_offsets);

            dateXOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.date_x_offsets);
            dateYOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.date_y_offsets);

            statusXOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.status_x_offsets);
            statusYOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.status_y_offsets);

            bitmapIconXOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.icon_x_offsets);
            bitmapIconYOffsets = resources.getDimension(com.example.android.sunshine.R.dimen.icon_y_offsets);

            float textSizeTime = resources.getDimension(isRound
                    ? com.example.android.sunshine.R.dimen.time_size_round : com.example.android.sunshine.R.dimen.time_size);

            float textSizeDay = resources.getDimension(isRound
                    ? com.example.android.sunshine.R.dimen.date_size_round : com.example.android.sunshine.R.dimen.date_size);

            float textSizeStatus = resources.getDimension(isRound
                    ? com.example.android.sunshine.R.dimen.status_size_round : com.example.android.sunshine.R.dimen.status_size);

            float iconSize = resources.getDimension(com.example.android.sunshine.R.dimen.bitmap_icon_size);

            mTextPaint.setTextSize(textSizeTime);

            tvDay.setTextSize(textSizeDay);

            tvStatus.setTextSize(textSizeStatus);

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), com.example.android.sunshine.R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                }


            if (drawableId == 0) {

                //here let it empty

            }else {
                Drawable backgroundDrawable = resources.getDrawable(drawableId, null);
                bmWeatherStatus = ((BitmapDrawable) backgroundDrawable).getBitmap();
                canvas.drawBitmap(bmWeatherStatus,bitmapIconXOffsets,bitmapIconYOffsets,null);

            }


            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String text = String.format("%d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE));
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);

            String demoDate = dateString;
            canvas.drawText(demoDate,dateXOffsets,dateYOffsets,tvDay);

            String demoSatus = statusStringHigh+" "+statusStringLow;
            canvas.drawText(demoSatus,statusXOffsets,statusYOffsets,tvStatus);

            canvas.drawRect(rect,rectanle);

        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
