package com.example.gamesolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.List;

public class ImageGallery extends AppCompatActivity {

    ImageView imageView;
    Button solveButton;
    Button retryButton;

    int game;

    Solver solver;
    int[] grid;

    final int SUDOKU = 0;
    final int MAGIC_SQUARE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_gallery);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray("picture");
        game = extras.getInt("game");
        Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        imageView = (ImageView) findViewById(R.id.display_image);
        imageView.setImageBitmap(image);


        retryButton = (Button) findViewById(R.id.retry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(ImageGallery.this, MainActivity.class);
                ImageGallery.this.startActivity(myIntent);
                imageView.setImageBitmap(null);
            }
        });

        solveButton = (Button) findViewById(R.id.solve);
        solveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageProcessing imageProcessing = new ImageProcessing(game);
                List<Mat> m = imageProcessing.loadAndConvertImage(image);

                try {
                    ModelClass model = new ModelClass(m, ImageGallery.this, game);
                    grid = model.process();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                solver = new Solver(game);
                int[][] solved = solver.solve(grid);
                if(solved != null) {
                    Mat result = imageProcessing.printSolution(solved);
                    Bitmap bitmapSudoku = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(result, bitmapSudoku);
                    imageView.setImageBitmap(bitmapSudoku);
                } else {
                    Toast.makeText(getApplicationContext(), "I can't find the solution", Toast.LENGTH_SHORT).show();
                }
                solveButton.setEnabled(false);
            }
        });

    }
}