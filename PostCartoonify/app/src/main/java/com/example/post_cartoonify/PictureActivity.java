package com.example.post_cartoonify;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.*;


public class PictureActivity extends AppCompatActivity {


    private static final String TAG = "OpenCV:PictureActivity";
    private Mat procMat;

    private ImageView imageView;
    private static final String IMAGE_DIRECTORY = "/CustomImage";


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    procMat=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        imageView = findViewById(R.id.img);

        imageView.setImageBitmap(cartoonImage(MainActivity.bitmap));
        saveImage(cartoonImage(MainActivity.bitmap));
    }

    public Bitmap cartoonImage(Bitmap img){
        procMat = new Mat();
        Bitmap bmp32 = img.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, procMat);

        Mat orig = new Mat();
        Mat gray = new Mat();
        Mat edges = new Mat();
        Mat color = new Mat();
        Mat cartoon = new Mat();

        Mat mRGBaF = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC4);
        Mat mRGBaT = new Mat(img.getWidth(), img.getHeight(), CvType.CV_8UC4);


        Imgproc.cvtColor(procMat, orig, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(procMat, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(gray, gray, 5);

        Mat kernel_sharpening = new Mat(3, 3, CvType.CV_8SC1);
        kernel_sharpening.put(0, 0, -1,-1,-1,-1,9,-1,-1,-1,-1);

        Imgproc.filter2D(gray, gray, gray.depth(), kernel_sharpening);

        Imgproc.medianBlur(gray, gray, 9);
        Size s = new Size(7, 7);
        Imgproc.GaussianBlur(gray, gray, s, 0);

        Imgproc.adaptiveThreshold(gray, edges, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 6);
        Imgproc.bilateralFilter(orig, color, 1, 700, 700);
        Core.bitwise_and(color, color, cartoon, edges);

        orig.release();
        gray.release();
        edges.release();
        color.release();
        kernel_sharpening.release();

        Core.flip(cartoon, cartoon, 1);
        Mat rotated = new Mat(cartoon.cols(), cartoon.rows(), CvType.CV_8UC4);

        if(MainActivity.cameraFront){
//            Core.transpose(cartoon, mRGBaT);
////            Imgproc.resize(mRGBaT, mRGBaF, mRGBaF.size(), 0,0,0);
////            Core.flip(mRGBaF, cartoon, 1);
            Core.rotate(cartoon, rotated, Core.ROTATE_90_CLOCKWISE);

        }
        else{
//            Core.transpose(cartoon, mRGBaT);
//            Imgproc.resize(mRGBaT, mRGBaF, mRGBaF.size(), 0,0,0);
            Core.flip(cartoon, cartoon, 0);

            Core.rotate(cartoon, rotated, Core.ROTATE_90_COUNTERCLOCKWISE);
        }

        img = Bitmap.createBitmap(img.getHeight(), img.getWidth(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated, img);

        return img;
    }



    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();   //give read write permission
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

    }

}