package com.axioma.wallpapers;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImageViewActivity extends AppCompatActivity {



    private InterstitialAd mInterstitialAd;


    Button wallpaper;

    int position;
    ImageButton shareBtn, downloadBtn, favBtn, rotateBtn;

    Button wallpaperBtn;


    ArrayList<String> imageUrls;
    String imageUrl;
    private SharedPreferences favSharedPreferences;
    private SharedPreferences downloadSharedPreferences;
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        LoadInterstitialAd();

        wallpaper = findViewById(R.id.wallpaper);

        wallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInterstitial();
            }
        });

        // Hide the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        imageUrls = getIntent().getStringArrayListExtra("image_url_list");
        imageUrl = getIntent().getStringExtra("image_url");
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        downloadSharedPreferences = getApplicationContext().getSharedPreferences("image_urls", Context.MODE_PRIVATE);
        favSharedPreferences = getApplicationContext().getSharedPreferences("favorites", Context.MODE_PRIVATE);
        imageView = findViewById(R.id.imageView);
        imageView = findViewById(R.id.imageView);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(this).load(imageUrl).fitCenter().into(imageView);
        shareBtn = findViewById(R.id.shareBtn);
        downloadBtn = findViewById(R.id.download);
        favBtn = findViewById(R.id.favBtn);
        wallpaperBtn = findViewById(R.id.wallpaper);

        favBtn.setOnClickListener(v -> {
            String imageUrl = getIntent().getStringExtra("image_url");
            storeImageUrlFav(imageUrl, v);
            Toast.makeText(getApplicationContext(), "Added as Favourites", Toast.LENGTH_SHORT).show();
        });

        wallpaperBtn.setOnClickListener(v -> {
            String imageUrl = getIntent().getStringExtra("image_url");
            String imageName = getIntent().getStringExtra("image_name");
            String imageExt = getIntent().getStringExtra("image_ext");

            String directoryName = "wallpaperly";
            String fileName = imageName + "." + imageExt;

            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), directoryName);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, fileName);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle(imageName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(file));

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                storeImageUrl(imageUrl, imageName);
                long downloadId = downloadManager.enqueue(request);

                ProgressBar progressBar = findViewById(R.id.progressBar);
                TextView progressText = findViewById(R.id.progressText);
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);

                        Cursor cursor = downloadManager.query(query);
                        if (cursor != null && cursor.moveToFirst()) {
                            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            int downloadedBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                            int totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                            int status = cursor.getInt(statusIndex);
                            long downloadedBytes = cursor.getLong(downloadedBytesIndex);
                            long totalBytes = cursor.getLong(totalBytesIndex);

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                progressBar.setVisibility(View.GONE);
                                progressText.setVisibility(View.GONE);
                                showDialog(file);
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                progressBar.setVisibility(View.GONE);
                                progressText.setVisibility(View.GONE);
                                Toast.makeText(getApplicationContext(), "Failed to download image", Toast.LENGTH_SHORT).show();
                            } else {
                                int progress = (int) ((downloadedBytes * 100L) / totalBytes);
                                progressBar.setProgress(progress);
                                progressText.setText("Downloading ..." + progress + "%");

                                // Check progress again after a delay
                                handler.postDelayed(this, 20);
                            }

                            cursor.close();
                        }
                    }
                }, 20);
            } else {
                Toast.makeText(getApplicationContext(), "Failed to download image", Toast.LENGTH_SHORT).show();
            }
        });

        downloadBtn.setOnClickListener(v -> downloadImage());
        shareBtn.setOnClickListener(v -> {
            String imageUrl = getIntent().getStringExtra("image_url");
            String imageName = getIntent().getStringExtra("image_name");
            String imageExt = getIntent().getStringExtra("image_ext");
            String directoryName = "wallpaperly";
            String fileName = imageName + "." + imageExt;

            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), directoryName);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, fileName);
            System.out.println(file);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle(imageName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(file));

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(getApplicationContext(), "Image Downloaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to download image", Toast.LENGTH_SHORT).show();
            }
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                // Do something after 5s = 5000ms
            }, 5000);
            shareImage(file);
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    private void showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(ImageViewActivity.this);
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
    }

    private void LoadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                                // Log.d(TAG, "Ad was clicked.");
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                // Log.d(TAG, "Ad dismissed fullscreen content.");
                                mInterstitialAd = null;
                                LoadInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e("TAG", "Ad failed to show fullscreen content.");
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                // Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                // Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Log.d("TAG", loadAdError.toString());
                    }
                });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Show the status bar again
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().show(WindowInsets.Type.statusBars());
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void showDialog(File file   ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageViewActivity.this, R.style.CustomAlertDialog);
        // Rest of the dialog code...
        builder.setTitle("Set Wallpaper Options");

        final boolean[] checked = new boolean[]{false, false};

        builder.setMultiChoiceItems(new String[]{"Home Screen", "Lock Screen"}, checked, (dialog, which, isChecked) -> checked[which] = isChecked);

        builder.setPositiveButton("Set Wallpaper", (dialog, which) -> {
            boolean setHomeScreen = checked[0];
            boolean setLockScreen = checked[1];

            // Set the selected image as the wallpaper(s) based on the chosen options
            try {
                if (setHomeScreen && setLockScreen) {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(ImageViewActivity.this);
                    // Set the wallpaper for both home screen and lock screen
                    wallpaperManager.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                    }
                    Toast.makeText(getApplicationContext(), "Wallpaper set for home screen and lock screen.", Toast.LENGTH_SHORT).show();
                } else if (setHomeScreen) {
                    WallpaperManager wallpaperManager1 = WallpaperManager.getInstance(ImageViewActivity.this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager1.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()), null, true, WallpaperManager.FLAG_SYSTEM);
                    }
                    // Set the wallpaper for the home screen only
                    Toast.makeText(getApplicationContext(), "Wallpaper set for home screen.", Toast.LENGTH_SHORT).show();
                } else if (setLockScreen) {
                    // Set the wallpaper for the lock screen only
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        WallpaperManager mm = WallpaperManager.getInstance(getApplicationContext());
                        mm.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()), null, true, WallpaperManager.FLAG_LOCK);
                        Toast.makeText(getApplicationContext(), "Wallpaper set for lock screen.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // No option selected, show a message
                    Toast.makeText(getApplicationContext(), "No option selected. Wallpaper not set.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Cancel button action
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    public void downloadImage() {
        String imageUrl = getIntent().getStringExtra("image_url");
        String imageName = getIntent().getStringExtra("image_name");
        String imageExt = getIntent().getStringExtra("image_ext");
        String directoryName = "wallpaperly";
        String fileName = imageName + "." + imageExt ;

        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
        request.setTitle(imageName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            storeImageUrl(imageUrl, imageName);
            downloadManager.enqueue(request);
            Toast.makeText(getApplicationContext(), "Image Downloaded", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Failed to download image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass the touch event to ScaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void shareImage(File imageFile) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Do something after 5s = 5000ms
        }, 5000);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        // Set the type of the content to "image/png"
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "View The Source Code at https://github.com/TharunDharmaraj/Wallpaperly");
        System.out.println(imageFile);
        // Get the URI of the image file using FileProvider
        Uri imageUri = FileProvider.getUriForFile(
                getApplicationContext() ,
                "com.example.myapplication.fileprovider",
                imageFile
        );
//        List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
//        for (ResolveInfo resolveInfo : resInfoList) {
//            String packageName = resolveInfo.activityInfo.packageName;
//            getApplicationContext().grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        }
        // Add the image URI to the intent as an extra
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

        // Grant temporary permissions to the content URI
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }


    private void goBack() {
        finish();
    }

    private void leftImageSwipe(ArrayList<String> imagrUrls, String imageUrl, int size) {
        position = imagrUrls.indexOf(imageUrl);
        String leftURL = imagrUrls.get((position - 1 < 0) ? size - 1 : position - 1);
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putStringArrayListExtra("image_url_list", imageUrls);
        intent.putExtra("image_url", leftURL);
        intent.putExtra("image_name", leftURL);
        startActivity(intent);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        overridePendingTransition(R.anim.side_in_left, R.anim.slide_out_right);

    }

    private void rightImageSwipe(ArrayList<String> imagrUrls, String imageUrl, int size) {
        position = imagrUrls.indexOf(imageUrl);
        String rightUrl = imagrUrls.get((position + 1) % size);
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putStringArrayListExtra("image_url_list", imageUrls);
        intent.putExtra("image_url", rightUrl);
        intent.putExtra("image_name", rightUrl);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    private void storeImageUrlFav(String imageUrl, View v) {
        String storedUrl = favSharedPreferences.getString(imageUrl, null);

        if (storedUrl == null || !storedUrl.equals(imageUrl)) {
            SharedPreferences.Editor editor = favSharedPreferences.edit();
            editor.putString(imageUrl, imageUrl);
            editor.apply();
            Toast.makeText(v.getContext(), "Added as Favourites", Toast.LENGTH_SHORT).show();
            favBtn.setImageResource(R.drawable.unfavourite);
        } else {
            // Image is already in favorites, remove it
            SharedPreferences.Editor editor = favSharedPreferences.edit();
            editor.remove(imageUrl);
            editor.apply();
            showSnackbarWithUndo(imageUrl, v);
        }
    }

    private void showSnackbarWithUndo(final String imageUrl, View v) {
        Snackbar snackbar = Snackbar.make(v, "Removed from Favorites", Snackbar.LENGTH_SHORT);
        favBtn.setImageResource(R.drawable.baseline_favorite_24);

        snackbar.setAction("Undo", view -> {
            // Remove the image from favorites
            SharedPreferences.Editor editor = favSharedPreferences.edit();
            editor.putString("image_" + imageUrl, imageUrl);
            favBtn.setImageResource(R.drawable.unfavourite);
            editor.apply();
            Toast.makeText(v.getContext(), "Added again from Favorites", Toast.LENGTH_SHORT).show();
        });

        snackbar.show();
        // Notify the fragment to reload
    }

    private void storeImageUrl(String imageUrl, String position) {
        String key = "image_" + position;
        String storedUrl = downloadSharedPreferences.getString(key, null);

        if (storedUrl == null || !storedUrl.equals(imageUrl)) {
            SharedPreferences.Editor editor = downloadSharedPreferences.edit();
            editor.putString(key, imageUrl);
            editor.apply();
        }
    }

    private void rotateImage() {
        float currentRotation = imageView.getRotation();
        float newRotation = currentRotation + 90f;
        imageView.setRotation(newRotation);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Calculate the scale factor
            scaleFactor *= detector.getScaleFactor();

            // Limit the scale factor within a certain range (e.g., 0.5 to 2.0)
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));

            // Apply the scale factor to the ImageView
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
            // Disable the status bar while zooming
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(scaleFactor == 1.0f);
            }
            return true;
        }
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_DISTANCE_THRESHOLD = 300;
        private static final int SWIPE_VELOCITY_THRESHOLD = 300;
        private static final int SWIPE_LEFT_RIGHT_DISTANCE_THRESHOLD = 200;
        private static final int SWIPE_LEFT_RIGHT_VELOCITY_THRESHOLD = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();

            if (Math.abs(distanceX) > Math.abs(distanceY)
                    && Math.abs(distanceX) > SWIPE_LEFT_RIGHT_DISTANCE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_LEFT_RIGHT_VELOCITY_THRESHOLD) {
                if (distanceX > 0) {
                    leftImageSwipe(imageUrls, imageUrl, imageUrls.size());
                    // Right swipe (optional)
                } else {
                    rightImageSwipe(imageUrls, imageUrl, imageUrls.size());
                    // Left swipe (optional)
                }
            } else if (Math.abs(distanceY) > Math.abs(distanceX)
                    && Math.abs(distanceY) > SWIPE_DISTANCE_THRESHOLD
                    && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceY > 0) {
                    // Down swipe
                    goBack();
                } else {
                    // Up swipe (optional)
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }


}