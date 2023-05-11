package com.example.gamesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    final int SUDOKU = 0;
    final int MAGIC_SQUARE=1;
    ImageView bgImage;
    Button galleryButton;
    Button cameraButton;

    RadioButton sudokuOption;
    int game;
    byte[] pictureByteArray;

    Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgImage = (ImageView) findViewById(R.id.bg_image);

        galleryButton = (Button) findViewById(R.id.gallery);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickAImage(view);

            }
        });

        cameraButton = (Button) findViewById(R.id.camera);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
                myIntent.putExtra("game", game);
                MainActivity.this.startActivity(myIntent);
            }
        });

        //default radio button selected
        sudokuOption = (RadioButton) findViewById(R.id.sudoku);
        sudokuOption.setChecked(true);
        game=SUDOKU;


    }
    public void pickAImage(View view)
    {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        someActivityResultLauncher.launch(photoPickerIntent);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri selectedImage = Objects.requireNonNull(data).getData();
                    InputStream imageStream = null;
                    image = BitmapFactory.decodeStream(imageStream);
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    image = BitmapFactory.decodeStream(imageStream);
                    int bytes = image.getByteCount();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                    pictureByteArray = stream.toByteArray();

                }
                Intent myIntent = new Intent(MainActivity.this, ImageGallery.class);
                myIntent.putExtra("picture", pictureByteArray);
                myIntent.putExtra("game", game);
                MainActivity.this.startActivity(myIntent);
            });

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.sudoku:
                if (checked)
                    game = SUDOKU;
                    break;
            case R.id.magic_square:
                if (checked)
                    game = MAGIC_SQUARE;
                    break;
        }
    }
    }