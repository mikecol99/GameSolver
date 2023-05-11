package com.example.gamesolver;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.gamesolver.ml.Model;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.List;

public class ModelClass {
    private List<Mat> images;
    private Model model;

    private int N;


    public ModelClass(List<Mat> images, Context context, int mode) throws IOException {
        this.images = images;
        this.model = Model.newInstance(context);
        if(mode == 0){ //sudoku
            N=9;
        }else{
            N=3; //magic_square
        }
    }



    public int[] process(){
        int[] res = new int[N*N];
        int index=0;
        for (Mat image : images) {
            if (Core.countNonZero(image)<30) {
                res[index++] = 0;
                continue;
            }

            Bitmap im3 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, im3);
            image.convertTo(image, CvType.CV_32S);
            int[] rgba = new int[(int)(image.total()*image.channels())];
            image.get(0,0,rgba);
            // Creates inputs for reference.
            TensorBuffer input = TensorBuffer.createFixedSize(new int[]{1, 28, 28, 1}, DataType.FLOAT32);
            input.loadArray(rgba);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(input);
            TensorBuffer outputFeature = outputs.getOutputFeature0AsTensorBuffer();
            float[] results = outputFeature.getFloatArray();
            res[index++]=argMax(results);
        }
        // Releases model resources if no longer used.
        model.close();
        return res;
    }

    private int argMax(float []data){
        float max = -100;
        int argMax = -1;

        for (int i=0; i < data.length; ++i) {
            if (data[i] > max) {
                max = data[i];
                argMax = i;
            }
        }
        if(argMax==-1){
            argMax=0;
        }
        return argMax;
    }
}
