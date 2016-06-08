package com.simplemobiletools.gallery;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.simplemobiletools.gallery.activities.ViewPagerActivity;

import java.io.IOException;

public class ViewPagerFragment extends Fragment
        implements View.OnClickListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener {
    private static final String TAG = ViewPagerFragment.class.getSimpleName();
    private static final String MEDIUM = "medium";
    private Media medium;
    private static SurfaceHolder surfaceHolder;
    private static ImageView playOutline;
    private static boolean isPlaying;
    private static MediaPlayer mediaPlayer;

    public void setMedium(Media medium) {
        this.medium = medium;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;

        if (medium == null && savedInstanceState != null) {
            medium = (Media) savedInstanceState.getSerializable(MEDIUM);
        }

        if (medium != null) {
            if (medium.getIsVideo()) {
                view = inflater.inflate(R.layout.pager_video_item, container, false);
                setupPlayer(view);
            } else {
                view = inflater.inflate(R.layout.pager_photo_item, container, false);
                final SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view.findViewById(R.id.photo_view);
                imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
                imageView.setImage(ImageSource.uri(medium.getPath()));
                imageView.setMaxScale(5f);
            }
        } else {
            view = inflater.inflate(R.layout.pager_photo_item, container, false);
        }

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
    }

    public void itemDragged() {
        pauseVideo();
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
                ((ViewPagerActivity) getActivity()).photoClicked();
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

        if (mediaPlayer == null)
            initMediaPlayer();

        isPlaying = !isPlaying;
        if (isPlaying) {
            mediaPlayer.start();
            playOutline.setImageDrawable(null);
        } else {
            mediaPlayer.pause();
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
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        pauseVideo();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
}
