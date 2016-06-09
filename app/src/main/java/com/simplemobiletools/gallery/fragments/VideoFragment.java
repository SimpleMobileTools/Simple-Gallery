package com.simplemobiletools.gallery.fragments;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.activities.ViewPagerActivity;
import com.simplemobiletools.gallery.models.Medium;

import java.io.IOException;
import java.util.Locale;

public class VideoFragment extends ViewPagerFragment
        implements View.OnClickListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private static final String MEDIUM = "medium";
    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;

    private ImageView playOutline;
    private TextView currTimeView;
    private TextView durationView;
    private Handler timerHandler;
    private SeekBar seekBar;
    private Medium medium;
    private boolean isPlaying;
    private boolean isDragged;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_video_item, container, false);

        medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (medium == null)
            return view;

        setupPlayer(view);
        view.setOnClickListener(this);

        return view;
    }

    private void setupPlayer(View view) {
        if (getActivity() == null)
            return;

        playOutline = (ImageView) view.findViewById(R.id.video_play_outline);
        playOutline.setOnClickListener(this);

        final SurfaceView surfaceView = (SurfaceView) view.findViewById(R.id.video_surface);
        surfaceView.setOnClickListener(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        initTimeHolder(view);
    }

    public void itemDragged() {
        pauseVideo();
    }

    private void initTimeHolder(View view) {
        RelativeLayout timeHolder = (RelativeLayout) view.findViewById(R.id.video_time_holder);
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
                    int currPos = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(currPos);
                    currTimeView.setText(getTimeString(currPos));
                }

                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(MEDIUM, medium);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play_outline:
                togglePlayPause();
                break;
            default:
                ((ViewPagerActivity) getActivity()).fragmentClicked();
        }
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

            seekBar.setProgress(0);
            currTimeView.setText(getTimeString(0));
        } catch (IOException e) {
            Log.e(TAG, "init media player " + e.getMessage());
        }
    }

    private void addPreviewImage() {
        mediaPlayer.start();
        mediaPlayer.pause();
    }

    @Override
    public void onPause() {
        super.onPause();
        cleanup();
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
        pauseVideo();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        surfaceHolder.setFixedSize(width, height);
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
            mediaPlayer.seekTo(progress * 1000);
            seekBar.setProgress(progress);
            currTimeView.setText(getTimeString(progress));
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
