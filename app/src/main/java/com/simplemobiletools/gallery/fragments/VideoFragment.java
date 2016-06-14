package com.simplemobiletools.gallery.fragments;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private static final String PROGRESS = "progress";
    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private ImageView playOutline;
    private TextView currTimeView;
    private TextView durationView;
    private Handler timerHandler;
    private SeekBar seekBar;
    private Medium medium;
    private View timeHolder;
    private boolean isPlaying;
    private boolean isDragged;
    private boolean isFullscreen;
    private int currTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_video_item, container, false);

        medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (savedInstanceState != null) {
            currTime = savedInstanceState.getInt(PROGRESS);
        }

        isFullscreen = (getActivity().getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) ==
                View.SYSTEM_UI_FLAG_FULLSCREEN;
        setupPlayer(view);
        view.setOnClickListener(this);

        return view;
    }

    private void setupPlayer(View view) {
        if (getActivity() == null)
            return;

        playOutline = (ImageView) view.findViewById(R.id.video_play_outline);
        playOutline.setOnClickListener(this);

        surfaceView = (SurfaceView) view.findViewById(R.id.video_surface);
        surfaceView.setOnClickListener(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        initTimeHolder(view);
    }

    public void itemDragged() {
        pauseVideo();
    }

    @Override
    public void systemUiVisibilityChanged(boolean toFullscreen) {
        if (isFullscreen != toFullscreen) {
            isFullscreen = toFullscreen;
            checkFullscreen();
        }
    }

    private void initTimeHolder(View view) {
        timeHolder = view.findViewById(R.id.video_time_holder);
        final Resources res = getResources();
        final int height = Utils.getNavBarHeight(res);
        final int left = timeHolder.getPaddingLeft();
        final int top = timeHolder.getPaddingTop();
        final int right = timeHolder.getPaddingRight();
        final int bottom = timeHolder.getPaddingBottom();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            timeHolder.setPadding(left, top, right, bottom + height);
        } else {
            timeHolder.setPadding(left, top, right + height, bottom);
        }

        currTimeView = (TextView) view.findViewById(R.id.video_curr_time);
        durationView = (TextView) view.findViewById(R.id.video_duration);
        seekBar = (SeekBar) view.findViewById(R.id.video_seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        if (isFullscreen)
            timeHolder.setVisibility(View.INVISIBLE);
    }

    private void setupTimeHolder() {
        final int duration = mediaPlayer.getDuration() / 1000;
        seekBar.setMax(duration);
        durationView.setText(getTimeString(duration));
        timerHandler = new Handler();
        setupTimer();
    }

    private void setupTimer() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !isDragged && isPlaying) {
                    currTime = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(currTime);
                    currTimeView.setText(getTimeString(currTime));
                }

                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PROGRESS, currTime);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play_outline:
                togglePlayPause();
                break;
            default:
                isFullscreen = !isFullscreen;
                checkFullscreen();

                if (listener == null)
                    listener = (FragmentClickListener) getActivity();
                listener.fragmentClicked();
                break;
        }
    }

    private void checkFullscreen() {
        int anim = R.anim.fade_in;
        if (isFullscreen) {
            anim = R.anim.fade_out;
            seekBar.setOnSeekBarChangeListener(null);
        } else {
            seekBar.setOnSeekBarChangeListener(this);
        }

        final Animation animation = AnimationUtils.loadAnimation(getContext(), anim);
        timeHolder.startAnimation(animation);
    }

    private void pauseVideo() {
        if (isPlaying) {
            togglePlayPause();
        }
    }

    private void togglePlayPause() {
        if (getActivity() == null)
            return;

        isPlaying = !isPlaying;
        if (isPlaying) {
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }

            playOutline.setImageDrawable(null);
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }

            playOutline.setImageDrawable(getResources().getDrawable(R.mipmap.play_outline_big));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getContext(), Uri.parse(medium.getPath()));
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepare();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            addPreviewImage();
            setupTimeHolder();
            setProgress(currTime);
        } catch (IOException e) {
            Log.e(TAG, "init media player " + e.getMessage());
        }
    }

    private void setProgress(int seconds) {
        mediaPlayer.seekTo(seconds * 1000);
        seekBar.setProgress(seconds);
        currTimeView.setText(getTimeString(seconds));
    }

    private void addPreviewImage() {
        mediaPlayer.start();
        mediaPlayer.pause();
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

        if (currTimeView != null)
            currTimeView.setText(getTimeString(0));

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (seekBar != null)
            seekBar.setProgress(0);

        if (timerHandler != null)
            timerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        seekBar.setProgress(seekBar.getMax());
        final int duration = mediaPlayer.getDuration() / 1000;
        currTimeView.setText(getTimeString(duration));

        pauseVideo();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        setVideoSize(width, height);
    }

    private void setVideoSize(int videoWidth, int videoHeight) {
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

        final android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        surfaceView.setLayoutParams(lp);
    }

    private String getTimeString(int duration) {
        final StringBuilder sb = new StringBuilder(8);
        final int hours = duration / (60 * 60);
        final int minutes = (duration % (60 * 60)) / 60;
        final int seconds = ((duration % (60 * 60)) % 60);

        if (mediaPlayer != null && mediaPlayer.getDuration() > 3600000) {
            sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":");
        }

        sb.append(String.format(Locale.getDefault(), "%02d", minutes));
        sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds));

        return sb.toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mediaPlayer != null && fromUser) {
            setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer == null)
            initMediaPlayer();

        mediaPlayer.pause();
        isDragged = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!isPlaying) {
            togglePlayPause();
        } else {
            mediaPlayer.start();
        }

        isDragged = false;
    }
}
