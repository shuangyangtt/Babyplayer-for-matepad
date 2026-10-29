package com.pepsi.babyplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Plays videos from a "Baby" folder full screen, repeating the current one.
 * Swipe up = next, swipe down = previous.
 * Every other touch is ignored, so a child grabbing the screen cannot change anything.
 */
public class MainActivity extends Activity {

    private static final String FOLDER = "Baby";
    private static final String[] EXTENSIONS = {
            ".mp4", ".m4v", ".mkv", ".webm", ".3gp", ".mov", ".avi", ".ts", ".flv"
    };
    private static final long SWITCH_COOLDOWN_MS = 800;
    private static final long MAX_SWIPE_MS = 1000;

    private VideoView video;
    private TextView message;
    private final List<File> videos = new ArrayList<>();
    private int index;
    private int resumePosition;
    private int consecutiveErrors;

    private float downX, downY;
    private long downTime;
    private boolean multiTouch;
    private long lastSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // MATCH_PARENT lets VideoView scale the video up to fill the screen, keeping its aspect ratio.
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(24);
        message.setGravity(Gravity.CENTER);
        message.setVisibility(View.GONE);
        root.addView(message, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        // Repeat the current video forever; only a swipe moves to another one.
        video.setOnPreparedListener(mp -> {
            consecutiveErrors = 0;
            mp.setLooping(true);
        });
        video.setOnErrorListener((mp, what, extra) -> {
            // Skip unplayable files, but stop if none of them play.
            if (++consecutiveErrors < videos.size()) {
                play(index + 1, 0);
            } else {
                showMessage("视频无法播放");
            }
            return true;
        });

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
            play(index, 0);
        } else {
            showMessage("需要允许读取存储卡");
        }
    }

    private void loadVideos() {
        videos.clear();
        for (File dir : candidateFolders()) {
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (f.isFile() && isVideo(f.getName())) videos.add(f);
            }
            if (!videos.isEmpty()) break;
        }
        Collections.sort(videos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        if (videos.isEmpty()) showMessage("Baby 文件夹里没有视频");
    }

    /** Baby folder on each storage volume, SD cards first, then internal storage. */
    private List<File> candidateFolders() {
        Set<String> seen = new LinkedHashSet<>();
        List<File> result = new ArrayList<>();
        File[] appDirs = getExternalFilesDirs(null);
        for (int i = appDirs.length - 1; i >= 0; i--) {
            if (appDirs[i] == null) continue;
            String path = appDirs[i].getAbsolutePath();
            int cut = path.indexOf("/Android/");
            if (cut > 0) addFolder(new File(path.substring(0, cut), FOLDER), seen, result);
        }
        addFolder(new File(Environment.getExternalStorageDirectory(), FOLDER), seen, result);
        return result;
    }

    private static void addFolder(File dir, Set<String> seen, List<File> result) {
        if (dir.isDirectory() && seen.add(dir.getAbsolutePath())) result.add(dir);
    }

    private static boolean isVideo(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith(".")) return false;
        for (String ext : EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private void play(int newIndex, int position) {
        if (videos.isEmpty()) return;
        int n = videos.size();
        index = ((newIndex % n) + n) % n;
        message.setVisibility(View.GONE);
        video.setVideoPath(videos.get(index).getAbsolutePath());
        if (position > 0) video.seekTo(position);
        video.start();
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        play(index, resumePosition);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!videos.isEmpty()) {
            resumePosition = video.getCurrentPosition();
            video.stopPlayback();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    /** Swallow every touch; only a clean one-finger vertical swipe switches videos. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                downTime = ev.getEventTime();
                multiTouch = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouch = true;
                break;
            case MotionEvent.ACTION_UP:
                handleSwipe(ev);
                break;
        }
        return true;
    }

    private void handleSwipe(MotionEvent ev) {
        if (multiTouch || ev.getEventTime() - downTime > MAX_SWIPE_MS) return;
        if (ev.getEventTime() - lastSwitch < SWITCH_COOLDOWN_MS) return;
        float dx = ev.getX() - downX;
        float dy = ev.getY() - downY;
        float minDistance = getWindow().getDecorView().getHeight() * 0.2f;
        if (Math.abs(dy) < minDistance || Math.abs(dy) < Math.abs(dx) * 1.5f) return;
        lastSwitch = ev.getEventTime();
        play(dy < 0 ? index + 1 : index - 1, 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Back does nothing so the child cannot leave the player.
    }
}
