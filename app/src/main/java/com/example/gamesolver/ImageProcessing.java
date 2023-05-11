package com.example.gamesolver;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessing {

    private Mat[] values;
    private Mat original;

    public static final int SUDOKU = 0;
    public static final int MAGIC_SQUARE = 1;
    private int mode;
    public ImageProcessing(int game) {
        mode = game;
    }

    // load and convert
    public List<Mat> loadAndConvertImage(Bitmap image) {
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCVLog", "OpenCV Initialized");
        }
        Mat imgOriginal = new Mat();
        Utils.bitmapToMat(image, imgOriginal);
        original = imgOriginal;
        Mat img = imgOriginal.clone();

        //convert in gray scale
        Mat imgGray = new Mat();
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGRA2GRAY);

        //resize image with desired_width and desired heigth
        int desired_width, desired_height, n;
        if(mode == SUDOKU){
            desired_width = 450;
            desired_height = 450;
            n=9;
        }else{
            desired_width = 150;
            desired_height = 150;
            n=3;
        }
        Mat img_resized = resizeImage(imgGray, desired_width, desired_height);

        //extract Sudoku table from image
        Mat[] resultsExtract = extractSudokuFromImage(img_resized, desired_width, desired_height);
        values = resultsExtract.clone();
        Mat warpImageSudoku = resultsExtract[0];

        Bitmap im2 = Bitmap.createBitmap(warpImageSudoku.cols(), warpImageSudoku.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(warpImageSudoku, im2);

        Mat gridRemoved = removeGrid(warpImageSudoku);
        Bitmap im3 = Bitmap.createBitmap(gridRemoved.cols(), gridRemoved.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gridRemoved, im3);

        //split image in 81 cells
        List<Mat> boxes = splitBoxes(gridRemoved, n);

        return boxes;
    }

    public Mat printSolution(int[][] grid) {
        int N = mode == SUDOKU ? 9 : 3;
        Mat warpImage = values[0];
        Mat perspectiveTransform = values[1];

        //create image with solution

        Mat solutionImg = Mat.ones(warpImage.size(), CvType.CV_8UC1);
        Core.multiply(solutionImg, new Scalar(255), solutionImg);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] != 0) {
                    int font = Imgproc.FONT_HERSHEY_DUPLEX;
                    String text = "" + grid[i][j];
                    Size textsize = Imgproc.getTextSize(text, font, 1, 1, null);
                    int textX = (int) (25 + j * warpImage.size().width / N - textsize.width / 2);
                    int textY = (int) (25 + i * warpImage.size().height / N + textsize.height / 2);
                    Imgproc.putText(solutionImg, text, new Point(textX, textY), font, 1, new Scalar(0, 0, 0), 2);
                }
            }
        }

        // warp inverse operation
        Mat unwarpedImg = new Mat();
        Imgproc.warpPerspective(solutionImg, unwarpedImg,
                perspectiveTransform, solutionImg.size(),
                Imgproc.WARP_INVERSE_MAP,
                Core.BORDER_CONSTANT,
                new Scalar(255));

        Mat final_size = resizeImage(unwarpedImg, original.cols(), original.rows());

        //print solution on the original image
        Mat image = original.clone();

        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGRA2BGR);

        double[] val = {0.0, 0.0, 255.0};
        for (int i = 0; i < final_size.rows(); i++) {
            for (int j = 0; j < final_size.cols(); j++) {
                if (final_size.get(i, j)[0] != 255.0) { // se c'è nero che sarebbe pixel del numero
                    image.put(i, j, val);
                }
            }
        }
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2BGRA);

        return image;
    }


    private static Mat resizeImage(Mat image, int width, int height) {
        Mat img_res = new Mat();
        Size dim = new Size(width, height);
        Imgproc.resize(image, img_res, dim, Imgproc.INTER_AREA);
        return img_res;
    }

    private Mat[] extractSudokuFromImage(Mat image, int width, int height) {

        Mat imgDen = new Mat();
        Mat imgThreshAdp = new Mat();

        //remove noise and apply threshold
        Photo.fastNlMeansDenoising(image, imgDen, 7, 7, 21);
        Imgproc.adaptiveThreshold(imgDen, imgThreshAdp, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 7);
        Bitmap im1 = Bitmap.createBitmap(imgThreshAdp.cols(), imgThreshAdp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgThreshAdp, im1);

        // find contours and draw on a new matrix (image)
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = Mat.zeros(image.size(), image.type());
        Imgproc.findContours(imgThreshAdp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // find the edge points of the biggest contour (that is the contour of the sudoku)
        Point[] biggest = biggestContour(contours);
        biggest = reorder(biggest);
        MatOfPoint big = new MatOfPoint(biggest);

        //prepare points and extract sudoku
        Mat pts1 = new MatOfPoint2f(biggest);
        Point[] p = {new Point(0, 0), new Point(width, 0), new Point(0, height), new Point(width, height)};
        Mat pts2 = new MatOfPoint2f(p);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(pts1, pts2);
        Mat final_image = new Mat();
        Imgproc.warpPerspective(image, final_image, perspectiveTransform, new Size(width, height));

        Mat[] results = new Mat[2];
        results[0] = final_image;
        results[1] = perspectiveTransform;
        return results;
    }

    private static Point[] biggestContour(List<MatOfPoint> contours) {
        Point[] biggest = new Point[4];
        double max_area = Double.MIN_VALUE;
        for (MatOfPoint cnt : contours) {
            double area = Imgproc.contourArea(cnt);
            MatOfPoint2f approx = new MatOfPoint2f();
            if (area > 50) {
                MatOfPoint2f m2f = new MatOfPoint2f(cnt.toArray());
                double peri = Imgproc.arcLength(m2f, true);
                Imgproc.approxPolyDP(m2f, approx, 0.02 * peri, true);
            }
            if (area > max_area && approx.size().height == 4) {
                biggest = approx.toArray();
                max_area = area;
            }
        }
        return biggest;
    }

    private static Point[] reorder(Point[] myPoints) {
        Point[] myPointsNew = new Point[4];
        double maxVal = Double.MIN_VALUE;
        double minVal = Double.MAX_VALUE;
        int indexMin = 0, indexMax = 0;

        //max e min value add
        double[] add = new double[4];
        for (int i = 0; i < myPoints.length; i++) {
            add[i] = myPoints[i].x + myPoints[i].y;
            if (maxVal < add[i]) {
                maxVal = add[i];
                indexMax = i;
            }
            if (minVal > add[i]) {
                minVal = add[i];
                indexMin = i;
            }
        }
        myPointsNew[0] = myPoints[indexMin];
        myPointsNew[3] = myPoints[indexMax];

        //max e min value diff
        maxVal = Double.MIN_VALUE;
        minVal = Double.MAX_VALUE;
        double[] diff = new double[4];
        for (int i = 0; i < myPoints.length; i++) {
            diff[i] = myPoints[i].y - myPoints[i].x;
            if (maxVal < diff[i]) {
                maxVal = diff[i];
                indexMax = i;
            }
            if (minVal > diff[i]) {
                minVal = diff[i];
                indexMin = i;
            }
        }
        myPointsNew[1] = myPoints[indexMin];
        myPointsNew[2] = myPoints[indexMax];
        return myPointsNew;
    }

    private static List<Mat> splitBoxes(Mat img, int n) {
        Mat box;
        ArrayList<Mat> boxes = new ArrayList<>();
        ArrayList<Mat> boxesFin = new ArrayList<>();

        int blockSize = img.rows() / n;
        for (int i = 0; i < img.rows(); i += blockSize) {
            for (int j = 0; j < img.cols(); j += blockSize) {
                box = img.rowRange(i, i + blockSize).colRange(j, j + blockSize);
                boxes.add(box);
            }
        }
        for (Mat b : boxes) {
            b = b.submat(3, 47, 3, 47);
            b = resizeImage(b, 28, 28);
            boxesFin.add(b);
        }
        return boxesFin;
    }


    private Mat removeGrid(Mat warp_image) {
        Mat thresh = new Mat();
        Imgproc.adaptiveThreshold(warp_image, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 57, 5);

        // Filter out all numbers and noise to isolate only boxes
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = Mat.zeros(warp_image.size(), warp_image.type());
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        List<MatOfPoint> cnts;

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            cnts = new ArrayList<>();
            cnts.add(c);
            if (area < 1000.0) {
                Imgproc.drawContours(thresh, cnts, -1, new Scalar(0, 0, 0), -1);
            }
        }

        // Fix horizontal and vertical lines
        Mat vertical_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 5));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, vertical_kernel, new Point(-1,-1), 9);
        //Bitmap im5 = Bitmap.createBitmap(thresh.cols(), thresh.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(thresh, im5);
        Mat horizontal_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 1));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, horizontal_kernel, new Point(-1,-1),4);
        //Bitmap im4 = Bitmap.createBitmap(thresh.cols(), thresh.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(thresh, im4);

        //Sort by top to bottom and each row by left to right
        Mat invert = new Mat();
        Core.bitwise_not(thresh, invert);
        Imgproc.findContours(invert, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.adaptiveThreshold(warp_image, thresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 7);
        Mat img_final=new Mat();
        Core.subtract(invert, thresh, img_final);

        Imgproc.medianBlur(img_final, img_final, 3);
        Imgproc.medianBlur(img_final, img_final, 3);
        Imgproc.medianBlur(img_final, img_final, 3);

        return img_final;
    }
}
