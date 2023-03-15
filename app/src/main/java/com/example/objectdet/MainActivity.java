package com.example.objectdet;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.birdclassification.R;
import com.example.birdclassification.ml.BirdsClassification;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView output;

    private Button button;
    private ActivityResultLauncher<String> mSelectedImageLauncher;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        output = findViewById(R.id.output_text);
        button = findViewById(R.id.button_choose);


        mSelectedImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        Bitmap bitmap;
                        if (result != null) {
                            try {
                                imageView.setImageURI(result);
                                bitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), result);
                                analyseImage(bitmap);

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

        button.setOnClickListener(view -> {
            requestPermission();
        });

        imageView.setOnClickListener(view -> {
            if(!output.getText().toString().isEmpty()){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="+output.getText().toString()));
                startActivity(intent);
            }
        });
    }


    private void analyseImage(Bitmap bitmap){
        try {
            BirdsClassification model = BirdsClassification.newInstance(this);

            // Creates inputs for reference.
            TensorImage image = TensorImage.fromBitmap(bitmap);

            // Runs model inference and gets result.
            BirdsClassification.Outputs outputs = model.process(image);
            List<Category> probability = outputs.getProbabilityAsCategoryList();

            float max = probability.get(0).getScore();
            int index=0;
            for(int i=0; i<probability.size(); i++){
                if(max<probability.get(i).getScore()){
                    index =i;
                    max = probability.get(i).getScore();
                }
            }

            //show the result
            output.setText(probability.get(index).getLabel());

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }



    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            // Permission has already been granted
            mSelectedImageLauncher.launch("image/*");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mSelectedImageLauncher.launch("image/*");

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }
}