package com.simplemobiletools.gallery.fragments;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.models.Medium;

import java.io.IOException;
import java.util.Locale;

public class VideoFragment extends ViewPagerFragment
        implements View.OnClickListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener,
        SeekBar.OnSeekBarChangeListener, OnPreparedListener {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private static final String PROGRESS = "progress";

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ImageView mPlayOutline;
    private TextView mCurrTimeView;
    private TextView mDurationView;
    private Handler mTimerHandler;
    private SeekBar mSeekBar;
    private Medium mMedium;
    private View mTimeHolder;

    private boolean mIsPlaying;
    private boolean mIsDragged;
    private boolean mIsFullscreen;
    private int mCurrTime;
    private int mDuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_video_item, container, false);

        mMedium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (savedInstanceState != null) {
            mCurrTime = savedInstanceState.getInt(PROGRESS);
        }

        mIsFullscreen = (getActivity().getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) ==
                View.SYSTEM_UI_FLAG_FULLSCREEN;
        setupPlayer(view);
        view.setOnClickListener(this);

        return view;
    }

    private void setupPlayer(View view) {
        if (getActivity() == null)
            return;

        mPlayOutline = (ImageView) view.findViewById(R.id.video_play_outline);
        mPlayOutline.setOnClickListener(this);

        mSurfaceView = (SurfaceView) view.findViewById(R.id.video_surface);
        mSurfaceView.setOnClickListener(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        initTimeHolder(view);
    }

    public void itemDragged() {
        pauseVideo();
    }

    @Override
    public void systemUiVisibilityChanged(boolean toFullscreen) {
        if (mIsFullscreen != toFullscreen) {
            mIsFullscreen = toFullscreen;
            checkFullscreen();
        }
    }

    private void initTimeHolder(View view) {
        mTimeHolder = view.findViewById(R.id.video_time_holder);
        final Resources res = getResources();
        final int height = Utils.getNavBarHeight(res);
        final int left = mTimeHolder.getPaddingLeft();
        final int top = mTimeHolder.getPaddingTop();
        int right = mTimeHolder.getPaddingRight();
        int bottom = mTimeHolder.getPaddingBottom();

        if (Utils.hasNavBar(getActivity())) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += height;
            } else {
                right += height;
            }
            mTimeHolder.setPadding(left, top, right, bottom);
        }

        mCurrTimeView = (TextView) view.findViewById(R.id.video_curr_time);
        mDurationView = (TextView) view.findViewById(R.id.video_duration);
        mSeekBar = (SeekBar) view.findViewById(R.id.video_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);

        if (mIsFullscreen)
            mTimeHolder.setVisibility(View.INVISIBLE);
    }

    private void setupTimeHolder() {
        mSeekBar.setMax(mDuration);
        mDurationView.setText(getTimeString(mDuration));
        mTimerHandler = new Handler();
        setupTimer();
    }

    private void setupTimer() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = mMediaPlayer.getCurrentPosition() / 1000;
                    mSeekBar.setProgress(mCurrTime);
                    mCurrTimeView.setText(getTimeString(mCurrTime));
                }

                mTimerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PROGRESS, mCurrTime);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play_outline:
                togglePlayPause();
                break;
            default:
                mIsFullscreen = !mIsFullscreen;
                checkFullscreen();

                if (mListener == null)
                    mListener = (FragmentClickListener) getActivity();
                mListener.fragmentClicked();
                break;
        }
    }

    private void checkFullscreen() {
        int anim = R.anim.fade_in;
        if (mIsFullscreen) {
            anim = R.anim.fade_out;
            mSeekBar.setOnSeekBarChangeListener(null);
        } else {
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        final Animation animation = AnimationUtils.loadAnimation(getContext(), anim);
        mTimeHolder.startAnimation(animation);
    }

    private void pauseVideo() {
        if (mIsPlaying) {
            togglePlayPause();
        }
    }

    private void togglePlayPause() {
        if (getActivity() == null)
            return;

        mIsPlaying = !mIsPlaying;
        if (mIsPlaying) {
            if (mMediaPlayer != null) {
                mMediaPlayer.start();
            }

            mPlayOutline.setImageDrawable(null);
        } else {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
            }

            mPlayOutline.setImageDrawable(getResources().getDrawable(R.mipmap.play_outline_big));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        if (mMediaPlayer != null)
            return;

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getContext(), Uri.parse(mMedium.getPath()));
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "init media player " + e.getMessage());
        }
    }

    private void setProgress(int seconds) {
        mMediaPlayer.seekTo(seconds * 1000);
        mSeekBar.setProgress(seconds);
        mCurrTimeView.setText(getTimeString(seconds));
    }

    private void addPreviewImage() {
        mMediaPlayer.start();
        mMediaPlayer.pause();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseVideo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            cleanup();
        }
    }

    private void cleanup() {
        pauseVideo();

        if (mCurrTimeView != null)
            mCurrTimeView.setText(getTimeString(0));

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mSeekBar != null)
            mSeekBar.setProgress(0);

        if (mTimerHandler != null)
            mTimerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mSeekBar.setProgress(mSeekBar.getMax());
        mCurrTimeView.setText(getTimeString(mDuration));
        pauseVideo();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        setVideoSize(width, height);
    }

    private void setVideoSize(int videoWidth, int videoHeight) {
        if (getActivity() == null)
            return;

        final float videoProportion = (float) videoWidth / (float) videoHeight;
        final Display display = getActivity().getWindowManager().getDefaultDisplay();
        int screenWidth;
        int screenHeight;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            screenWidth = realMetrics.widthPixels;
            screenHeight = realMetrics.heightPixels;
        } else {
            screenWidth = display.getWidth();
            screenHeight = display.getHeight();
        }

        final float screenProportion = (float) screenWidth / (float) screenHeight;

        final android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        mSurfaceView.setLayoutParams(lp);
    }

    private String getTimeString(int duration) {
        final StringBuilder sb = new StringBuilder(8);
        final int hours = duration / (60 * 60);
        final int minutes = (duration % (60 * 60)) / 60;
        final int seconds = ((duration % (60 * 60)) % 60);

        if (duration > 3600) {
            sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":");
        }

        sb.append(String.format(Locale.getDefault(), "%02d", minutes));
        sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds));

        return sb.toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mMediaPlayer != null && fromUser) {
            setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mMediaPlayer == null)
            initMediaPlayer();

        mMediaPlayer.pause();
        mIsDragged = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!mIsPlaying) {
            togglePlayPause();
        } else {
            mMediaPlayer.start();
        }

        mIsDragged = false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mDuration = mp.getDuration() / 1000;
        addPreviewImage();
        setupTimeHolder();
        setProgress(mCurrTime);
    }
}
