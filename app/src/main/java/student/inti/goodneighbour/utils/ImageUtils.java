package student.inti.goodneighbour.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.os.Handler;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 800; // pixels
    private static final int COMPRESSION_QUALITY = 70; // 70% quality to reduce size
    private static final String DB_URL = "https://goodneighbour-b8fad-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static FirebaseDatabase getDatabase() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReferenceFromUrl(DB_URL);
            return ref.getDatabase();
        } catch (Exception e) {
            Log.e(TAG, "Error getting database instance", e);
            throw e;
        }
    }

    public static void uploadImage(Context context, Uri imageUri, String path, String userId,
                                   OnCompleteListener<String> listener) {
        try {
            Log.d(TAG, "Starting image upload process");
            // Compress and convert image to base64
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Scale down image if too large
            if (bitmap.getWidth() > MAX_IMAGE_SIZE || bitmap.getHeight() > MAX_IMAGE_SIZE) {
                float scale = Math.min(
                        (float) MAX_IMAGE_SIZE / bitmap.getWidth(),
                        (float) MAX_IMAGE_SIZE / bitmap.getHeight()
                );
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                Log.d(TAG, "Image scaled to " + newWidth + "x" + newHeight);
            }

            // Convert to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            Log.d(TAG, "Image converted to base64, size: " + base64Image.length());

            // Save to Realtime Database
            DatabaseReference dbRef = getDatabase().getReference();
            String imagePath = path + "/" + userId;
            Log.d(TAG, "Uploading to path: " + imagePath);

            // Add timeout handler
            Handler handler = new Handler();
            Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    UIUtils.hideLoading();
                    listener.onFailure("Upload timed out after 30 seconds");
                }
            };
            handler.postDelayed(timeoutRunnable, 30000); // 30 second timeout

            dbRef.child(imagePath).setValue(base64Image)
                    .addOnSuccessListener(aVoid -> {
                        handler.removeCallbacks(timeoutRunnable);
                        Log.d(TAG, "Upload successful");
                        UIUtils.hideLoading();
                        listener.onSuccess(imagePath);
                    })
                    .addOnFailureListener(e -> {
                        handler.removeCallbacks(timeoutRunnable);
                        Log.e(TAG, "Upload failed", e);
                        UIUtils.hideLoading();
                        listener.onFailure(e.getMessage());
                    });

            bitmap.recycle();
            baos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            UIUtils.hideLoading();
            listener.onFailure(e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            UIUtils.hideLoading();
            listener.onFailure("Unexpected error: " + e.getMessage());
        }
    }

    public static void loadImage(String imagePath, OnCompleteListener<Bitmap> listener) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "Attempted to load image with null or empty path");
            listener.onFailure("No image path provided");
            return;
        }

        Log.d(TAG, "Loading image from path: " + imagePath);
        DatabaseReference dbRef = getDatabase().getReference();
        dbRef.child(imagePath).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        try {
                            String base64Image = dataSnapshot.getValue(String.class);
                            if (base64Image != null) {
                                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (bitmap != null) {
                                    Log.d(TAG, "Image loaded successfully");
                                    listener.onSuccess(bitmap);
                                } else {
                                    Log.e(TAG, "Failed to decode bitmap");
                                    listener.onFailure("Failed to decode image");
                                }
                            } else {
                                Log.e(TAG, "Invalid image data from database");
                                listener.onFailure("Invalid image data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing loaded image", e);
                            listener.onFailure("Error processing image: " + e.getMessage());
                        }
                    } else {
                        Log.w(TAG, "Image not found at path: " + imagePath);
                        listener.onFailure("Image not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load image", e);
                    listener.onFailure(e.getMessage());
                });
    }

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }
}