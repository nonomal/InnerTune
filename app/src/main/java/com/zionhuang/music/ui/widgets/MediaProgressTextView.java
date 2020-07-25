package com.zionhuang.music.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

import static com.zionhuang.music.utils.Utils.makeTimeString;

public class MediaProgressTextView extends AppCompatTextView {
    private MediaControllerCompat mMediaController;
    private ControllerCallback mControllerCallback;
    private ValueAnimator mProgressAnimator;
    private float mDurationScale = Settings.Global.getFloat(getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
    private int duration = 0;

    public MediaProgressTextView(Context context) {
        super(context);
    }

    public MediaProgressTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaProgressTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        if (mediaController != null) {
            mControllerCallback = new ControllerCallback();
            mediaController.registerCallback(mControllerCallback);
            mControllerCallback.onMetadataChanged(mediaController.getMetadata());
            mControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
        } else if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
        }
        mMediaController = mediaController;
    }

    public void disconnectController() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mControllerCallback = null;
            mMediaController = null;
        }
    }

    private class ControllerCallback extends MediaControllerCompat.Callback implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if (state == null) return;
            if (mProgressAnimator != null) {
                mProgressAnimator.cancel();
                mProgressAnimator = null;
            }

            int progress = (int) state.getPosition();

            setText(makeTimeString((long) (progress) / 1000));
            if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                int timeToEnd = (int) ((duration - progress) / state.getPlaybackSpeed());
                if (timeToEnd > 0) {
                    if (mProgressAnimator != null) mProgressAnimator.cancel();
                    mProgressAnimator = ValueAnimator.ofInt(progress, duration)
                            .setDuration((long) (timeToEnd / mDurationScale));
                    mProgressAnimator.setInterpolator(new LinearInterpolator());
                    mProgressAnimator.addUpdateListener(this);
                    mProgressAnimator.start();
                }
            } else {
                setText(makeTimeString((long) (progress) / 1000));
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            if (metadata != null) {
                long max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                duration = (int) max;
            }
            if (mMediaController != null) {
                onPlaybackStateChanged(mMediaController.getPlaybackState());
            }
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int animatedValue = (int) animation.getAnimatedValue();
            setText(makeTimeString((long) (animatedValue / 1000)));
        }
    }
}