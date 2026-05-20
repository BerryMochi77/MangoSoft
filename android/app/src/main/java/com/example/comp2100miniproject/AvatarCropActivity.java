package com.example.comp2100miniproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarCropActivity extends AppCompatActivity {
    public static final String EXTRA_SOURCE_URI = "source_uri";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_CROPPED_URI = "cropped_uri";

    private AvatarCropView cropView;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_avatar_crop);

        cropView = findViewById(R.id.avatarCropView);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String source = getIntent().getStringExtra(EXTRA_SOURCE_URI);
        if (source == null || userId == null) {
            finishWithError();
            return;
        }

        try {
            cropView.setImageUri(Uri.parse(source));
        } catch (IOException | SecurityException ignored) {
            finishWithError();
            return;
        }

        Button buttonCancel = findViewById(R.id.buttonCancelAvatarCrop);
        Button buttonSave = findViewById(R.id.buttonSaveAvatarCrop);
        buttonCancel.setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveCroppedAvatar());
    }

    private void saveCroppedAvatar() {
        Bitmap bitmap = cropView.cropBitmap();
        if (bitmap == null) {
            finishWithError();
            return;
        }

        File dir = new File(getFilesDir(), "avatars");
        if (!dir.exists() && !dir.mkdirs()) {
            finishWithError();
            return;
        }

        File file = new File(dir, "avatar_" + userId + ".png");
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (IOException ignored) {
            finishWithError();
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_CROPPED_URI, Uri.fromFile(file).toString());
        setResult(RESULT_OK, result);
        finish();
    }

    private void finishWithError() {
        Toast.makeText(this, R.string.avatar_update_failed, Toast.LENGTH_SHORT).show();
        setResult(RESULT_CANCELED);
        finish();
    }
}
