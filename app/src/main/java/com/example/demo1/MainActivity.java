
package com.example.demo1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.resize;
//import com.example.demo1.CustomOpenCVJavaCameraView;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static String TAG = "MainActivity";
        private Button bt1, bt2, bt3, btSave;
    private ImageView iv1, iv2, iv3;//iv2,iv3;
    private Mat srcmat1, dstmat, hsvMat, topMat;
    private Bitmap bitmap, contours_bmap;
    private Bitmap bmap;
    JavaCameraView javaCameraView;
    Mat mRGBA, mRGBAT, resizeimage;        //Mat resizeimage = new Mat();
    Mat matSave;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK;// or front
    int SELECT_PICTURE = 200;
    //private List<MatOfPoint> contours;
    private MatOfPoint2f contours2f, approxCurve;
    private int contoursSize;
    private Bitmap resultBitmap;
    //Mat mRgba, mHsv,hierarchy,mHsvMask ,mDilated;
    Mat mRgba, mIntermediateMat, mGray, hierarchy;
    List<MatOfPoint> contours;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status)
        {
            if (status == BaseLoaderCallback.SUCCESS) {
                javaCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    static
    {
        if (OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV is Configured or Connected successfully.");
        }
        else
        {
            Log.d(TAG, "OpenCV not Working or Loaded.");
        }
    }

    // callback to be executed after the user has givenapproval or rejection via system prompt
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // camera can be turned on
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                initializeCamera(javaCameraView, activeCamera);
            } else {
                // camera will stay off
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera(JavaCameraView javaCameraView, int activeCamera){
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(activeCamera);
        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = (JavaCameraView) findViewById(R.id.cameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(MainActivity.this);
        javaCameraView.setMaxFrameSize(2560,1440);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // checking if the permission has already been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
            initializeCamera(javaCameraView, activeCamera);
        } else {
            // prompt system dialog
            Log.d(TAG, "Permission prompt");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }


        iniLoadOpenCV();

        bt1 = findViewById(R.id.button);
        bt2 = findViewById(R.id.button2);
        bt3 = findViewById(R.id.button3);
        btSave = findViewById(R.id.button4);
        iv1 = findViewById(R.id.imageView);
        iv2 = findViewById(R.id.imageView3);
   //     iv2 = findViewById(R.id.imageView2);

        iv3 = findViewById(R.id.topView);

        srcmat1 = new Mat();
    //    srcmat2 = new Mat();
        dstmat = new Mat();
        topMat = new Mat();
        matSave = new Mat();


        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent, 100);
                //startCropActivity();
                //Create the rectangle
                //        Imgproc.rectangle(mRGBA, new Point(w * 12 / 24, h * 11 / 24), new Point(
                //                w * 13 / 24, h * 15/ 24), new Scalar( 0, 255, 0 ), 3);
//                Rect roi = new Rect( mRGBA.width()  / 3, 0, mRGBA.width() / 3, mRGBA.height());
                Rect roi = new Rect( mRGBA.width() * 12 / 24, mRGBA.height() * 10 / 24, mRGBA.width() * 1/ 24, mRGBA.height() * 6/ 24);

                //Create the cv::Mat with the ROI you need, where "image" is the cv::Mat you want to extract the ROI from
                srcmat1 = (new Mat(mRGBA, roi)).t();
                Core.flip(srcmat1, srcmat1, 1);

//                mRgba =srcmat1;
//                mHsv = new Mat();
//                Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_RGB2HSV, 3);
//                hierarchy.release();
//                Scalar lowerThreshold = new Scalar ( 0, 0, 0 );
//                Scalar upperThreshold = new Scalar ( 255, 255, 255 );
//                Core.inRange ( mHsv, lowerThreshold , upperThreshold, mHsvMask );
//                Imgproc.dilate ( mHsvMask, mDilated, new Mat() );
//                try {
//                    Imgproc.findContours(mDilated, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//                } catch(java.lang.IllegalArgumentException e) {
//                    Log.d(TAG, "contuous error !!!!!!!!!!!!!!");
//                }
//                try {
//                    for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ )
//                    {
//                        if(Imgproc.contourArea(contours.get(contourIdx))>100) {
//                            Imgproc.drawContours(mRgba, contours, contourIdx, new Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255), 1, 8, hierarchy, 0, new Point());
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "contuous error 2   !!!!!!!!!!!!!!");
//                }
//
//                resize(mRgba, mRgba, mRGBA.size());
//                srcmat1  = mRgba;


                Size scaleSize = new Size(iv1.getWidth(),iv1.getHeight());
                resize(srcmat1, srcmat1, scaleSize , 0, 0,INTER_AREA  );//INTER_AREA

//                Mat resultMat = srcmat1.clone();
//                hsvMat = new Mat();
//                Imgproc.cvtColor(srcmat1, hsvMat, Imgproc.COLOR_BGR2HSV);
//                Core.inRange(hsvMat, new Scalar(0, 0, 221), new Scalar(180, 30, 255), hsvMat);
//                Core.bitwise_not(hsvMat, hsvMat);
//                contours_bmap = Bitmap.createBitmap(hsvMat.width(), hsvMat.height(), Bitmap.Config.ARGB_8888);
//                Mat outMat = new Mat();
//                Imgproc.findContours(hsvMat, contours, outMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//                contoursSize = contours.size();
//                double epsilon;
//                Imgproc.drawContours(resultMat, contours, -1, new Scalar(0, 0, 0), 10);
//
//                Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2BGR);
//                Utils.matToBitmap(resultMat, contours_bmap);
//                iv1.setImageBitmap(contours_bmap);

//                contours = new ArrayList<>();
//                hsvMat = new Mat();
//                Mat resultMat = srcmat1.clone();
//                Mat binaryMat = new Mat();
//                Imgproc.cvtColor(srcmat1, hsvMat, Imgproc.COLOR_BGR2HSV);
//                Core.inRange(hsvMat, new Scalar(0, 0, 221), new Scalar(180, 30, 255), binaryMat);
//                Core.bitwise_not(binaryMat, binaryMat);
//                resultBitmap = Bitmap.createBitmap(binaryMat.width(), binaryMat.height(), Bitmap.Config.ARGB_8888);
//                Mat outMat = new Mat();
//                Imgproc.findContours(binaryMat, contours, outMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//                contoursSize = contours.size();
//                Imgproc.drawContours(resultMat, contours, -1, new Scalar(0, 0, 0), 10);
//
//                Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2BGR);
//                Utils.matToBitmap(resultMat, resultBitmap);
//                iv1.setImageBitmap(resultBitmap);


//                Imgproc.rectangle(srcmat1, new Point(2, 2), new Point(
//                        srcmat1.width() - 2, srcmat1.height() - 2), new Scalar( 255, 0, 0 ), 1
//                );
//


                //Imgproc.rectangle(srcmat1, new Point(0, 0), new Point(srcmat1.width()  , srcmat1.height() ), new Scalar( 0, 0, 255 ), 3);

                matSave = srcmat1.clone();
                bitmap = Bitmap.createBitmap(srcmat1.width(), srcmat1.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcmat1, bitmap);
//                iv1.setImageBitmap(bitmap);
//                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "origin" , "yourDescription");
                iv1.setImageBitmap(bitmap);
            }
        });
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                bmap = Bitmap.createBitmap(mRGBAT.width(), mRGBAT.height(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(mRGBAT, bmap);
//////                iv1.setImageBitmap(bitmap);
//                iv3.setImageBitmap(bmap);
//
//            }
//        });

        // handle the Choose Image button to trigger
        // the image chooser function
        bt2.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                imageChooser();
                                            }
        });

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              //  Core.bitwise_or(srcmat1, srcmat2, dstmat);
                //Imgproc.cvtColor(srcmat1, dstmat, Imgproc.COLOR_BGRA2GRAY);
                Imgproc.medianBlur(srcmat1, srcmat1, 3);
                Imgproc.GaussianBlur(srcmat1, srcmat1, new Size(11.0, 11.0), 4, 4);

                Mat binaryMat = new Mat();
//                Imgproc.cvtColor(dstmat, binaryMat, Imgproc.COLOR_BGR2GRAY);
                hsvMat = srcmat1.clone();
//                Mat resultMat = new Mat();

                Imgproc.cvtColor(srcmat1, binaryMat, Imgproc.COLOR_BGR2GRAY);
//                Imgproc.cvtColor(srcmat1, hsvMat, Imgproc.COLOR_BGR2HSV);
//                Core.inRange(hsvMat, new Scalar(110, 0, 0), new Scalar(160, 255, 255), srcmat1);

                dstmat = Mat.ones(binaryMat.size(), CvType.CV_8UC1);
                int ch = dstmat.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
                if (ch == 1)
                    Log.d(TAG, "channel : " );
                for (int i = 0; i < dstmat.cols(); i++) {
                    double ration = 0.0;
                    for (int j = 0; j < dstmat.rows(); j++) {
                        double[] data = binaryMat.get(j, i); //Stores element in an array
                        ration += data[0] / 255;
                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
                        {
                            data[k] = 255; //Pixel modification done here
                        }
                        dstmat.put(j, i, data); //Puts element back into matrix
                    }
                    double[] data = binaryMat.get(0, 0); //Stores element in an array
                    data[0] = 0;
                    ration = ration / dstmat.rows();
                    //(index, i)
                    int index = (int) (dstmat.rows() * (1 - ration));
//                    dstmat.put(index, i, data);
                    for (int k = index - 5; k < index + 5; k++) {
                        for (int m = i - 5; m < i + 5; m++) {
                            dstmat.put(k, m, data);
                        }
                    }
                }
                //Imgproc.threshold(dstmat,dstmat,125,255,Imgproc.THRESH_BINARY);
                //Imgproc.adaptiveThreshold(dstmat,dstmat,255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 13, 5);
//                Imgproc.line(srcmat1,new Point(0,dstmat.height()),new Point(dstmat.width(),0),new Scalar(255,0,0),4);
//                Imgproc.putText(srcmat1,"I`m a cat",new Point(srcmat1.width() /2,srcmat1.height()/3),2,2,new Scalar(0,255,0),3);
////                for (int i = 0; i < dstmat.width(); i++) {
////
////                    for (int j = 0; j < dstmat.height(); j++) {
////                        ration += dstmat.get(i, j);
////                    }
////                }
//
//
//
//                dstmat = binaryMat;
//                Imgproc.cvtColor(binaryMat, dstmat, Imgproc.COLOR_GRAY2BGR);
                //Imgproc.cvtColor(binaryMat, dstmat, Imgproc.COLOR_RGB2BGR);
                //Imgproc.cvtColor(hsvMat, dstmat, Imgproc.COLOR_HSV2BGR);
                Core.flip(dstmat, dstmat, 0);
                Imgproc.rectangle(dstmat, new Point(0, 0), new Point(dstmat.width(), dstmat.height()), new Scalar( 0, 0, 255 ), 3);
                bitmap = Bitmap.createBitmap(dstmat.width(), dstmat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dstmat, bitmap);
//                iv1.setImageBitmap(bitmap);
                iv2.setImageBitmap(bitmap);

            }
        });

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmap = Bitmap.createBitmap(matSave.width(), matSave.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matSave, bitmap);
//                iv1.setImageBitmap(bitmap);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "origin" , "yourDescription");
//                iv1.setImageBitmap(bitmap);
            }
        });

    }

    // this function is triggered when
    // the Select Image Button is clicked
    void imageChooser() {

        // create an instance of the
        // intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                iv1.setImageURI(resultUri);

                try {
                    Bitmap bmp = null;
                    bmp = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(),
                            resultUri);
                    Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(bmp, obj);
                    srcmat1 = obj;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == 100) {
            Bitmap captureImage = (Bitmap) data.getExtras().get("data");
            iv2.setImageBitmap(captureImage);
        }

        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    // update the preview image in the layout
                    iv1.setImageURI(selectedImageUri);
                    try {
                        Bitmap bmp = null;
                        bmp = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(),
                                selectedImageUri);
                        Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(bmp, obj);
                        srcmat1 = obj;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }


    }


    @Override
    public void onCameraViewStarted(int width, int height)
    {
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mHsv = new Mat(height,width,CvType.CV_8UC3);
//        hierarchy = new Mat();
//        mHsvMask = new Mat();
//        mDilated = new Mat(height, width, CvType.CV_8UC4);

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        hierarchy = new Mat();

        mRGBA = new Mat(height, width, CvType.CV_8UC4);
//        resizeimage = new Mat(javaCameraView.getHeight(), javaCameraView.getWidth(), CvType.CV_8UC4);
        //resizeimage = new Mat(200, 200, CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped()
    {
        mRGBA.release();
//        mRgba.release();
//        mHsv.release();
//        mHsvMask.release();
//        mDilated.release();
//        hierarchy.release();
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        hierarchy.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRGBA = inputFrame.rgba();

        int w = mRGBA.width();
        int h = mRGBA.height();
//        Imgproc.rectangle(mRGBA, new Point(w * 1 / 3, 0), new Point(
//                w * 2 / 3, h), new Scalar( 0, 0, 255 ), 5
//        );

        Imgproc.rectangle(mRGBA, new Point(w * 12 / 24, h * 10 / 24), new Point(
                w * 13 / 24, h * 16/ 24), new Scalar( 0, 0, 255 ), 3);

        mRGBAT = mRGBA.t();
        Core.flip(mRGBA.t(), mRGBAT, 1);
        //return mRGBAT;
        resize(mRGBAT, mRGBAT, mRGBA.size());

        //Size scaleSize = new Size(javaCameraView.getWidth(),javaCameraView.getHeight());
//        Size scaleSize = new Size(200,200);
//        resize(mRGBAT, resizeimage, scaleSize , 0, 0,INTER_CUBIC  );//INTER_AREA
        //resizeimage = mRGBAT;
        //resize(mRGBAT, resizeimage, scaleSize , 0, 0,INTER_LINEAR  );
//         bmap = Bitmap.createBitmap(mRGBAT.width(), mRGBAT.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(mRGBAT, bmap);
//////                iv1.setImageBitmap(bitmap);
//        iv3.setImageBitmap(bmap);


//        Rect rect_rect = new Rect();
//        mRgba = mRGBAT;
//        contours = new ArrayList<MatOfPoint>();
//        hierarchy = new Mat();
//        Imgproc.Canny(mRgba, mIntermediateMat, 80, 100);
//        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
//
//        hierarchy.release();
//
//        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
//            MatOfPoint2f approxCurve = new MatOfPoint2f();
//            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
//            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
//            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//
//            Rect rect = Imgproc.boundingRect(points);
//            double height = rect.height;
//            double width = rect.width;
//
//            if (height > 10 && height < 30 && width > 3 && width < 30) {
//                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 0), 3);
//                Imgproc.putText(mRgba, "contours", rect.tl(), 0, 2, new Scalar(0, 255, 255), 4);
//            }
//        }
//
//        return mRgba;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                bmap = Bitmap.createBitmap(iv3.getWidth(),  iv3.getHeight(), Bitmap.Config.ARGB_8888);
                resize(mRGBAT, topMat, new Size(iv3.getWidth(), iv3.getHeight()));
                Utils.matToBitmap(topMat, bmap);
////                iv1.setImageBitmap(bitmap);
                iv3.setImageBitmap(bmap);

            }
        });
        return mRGBAT;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        srcmat1.release();
//        srcmat2.release();
//        dstmat.release();
        topMat.release();

        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (javaCameraView != null)
        {
            javaCameraView.disableView();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV is Configured or Connected successfully.");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        else
        {
            Log.d(TAG, "OpenCV not Working or Loaded.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
    private void iniLoadOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        if(success) {
            Toast.makeText(this.getApplicationContext(), "Loading OpenCV Libraries...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
        }
    }
        private void startCropActivity () {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);

    }
}

//package com.example.demo1;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.JavaCameraView;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.android.Utils;
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.Point;
//import org.opencv.core.Scalar;
//import org.opencv.imgproc.Imgproc;
//
//import java.io.IOException;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//
//import com.theartofdev.edmodo.cropper.CropImage;
//import com.theartofdev.edmodo.cropper.CropImageView;
//
//public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
//    private static String TAG = "MainActivity";
//
//    private Button bt1, bt2, bt3;
//    private ImageView iv1, iv2;//iv2,iv3;
//    private Mat srcmat1,srcmat2,dstmat;
//    private Bitmap bitmap;
//    JavaCameraView javaCameraView;
//    Mat mRGBA, mRGBAT;
//
//    // constant to compare
//    // the activity result code
//    int SELECT_PICTURE = 200;
//    private static final int MY_CAMERA_REQUEST_CODE = 110;
//    int activeCamera = CameraBridgeViewBase.CAMERA_ID_FRONT;
//
//
//    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case BaseLoaderCallback.SUCCESS: {
//                    javaCameraView.enableView();
//                    break;
//                }
//                default: {
//                    super.onManagerConnected(status);
//                    break;
//                }
//            }
//        }
//    };
////    static {
//////        if (OpenCVLoader.initDebug()) {
//////            Log.d(TAG, "opencv is configured successfully.");
//////        }
//////        else {
//////            Log.d(TAG, "opencv not working.");
//////        }
////    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        srcmat1.release();
//        srcmat2.release();
//        dstmat.release();
//        if (javaCameraView != null) {
//            javaCameraView.disableView();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (javaCameraView != null) {
//            javaCameraView.disableView();
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (OpenCVLoader.initDebug()) {
//            Log.d(TAG, "opencv is configured successfully.");
//            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
//        }
//        else {
//            Log.d(TAG, "opencv not working.");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        iniLoadOpenCV();
//
//        bt1 = findViewById(R.id.button);
//        bt2 = findViewById(R.id.button2);
//        bt3 = findViewById(R.id.button3);
//        iv1 = findViewById(R.id.imageView);
//        iv2 = findViewById(R.id.imageView3);
//   //     iv2 = findViewById(R.id.imageView2);
//   //     iv3 = findViewById(R.id.imageView3);
//        srcmat1 = new Mat();
//    //    srcmat2 = new Mat();
//        dstmat = new Mat();
//        javaCameraView = (JavaCameraView) findViewById(R.id.cameraView);
//        javaCameraView.setVisibility(SurfaceView.VISIBLE);
//        javaCameraView.setCvCameraViewListener(MainActivity.this);
//
//        try {
//            //srcmat1 = Utils.loadResource(this, R.drawable.d03);
//            srcmat2 = Utils.loadResource(this, R.drawable.d02);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // request permissions
//        if (ContextCompat.checkSelfPermission(MainActivity.this,
//                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[] {
//                        Manifest.permission.CAMERA
//                    },
//                    100);
//        }
//        // another camera permission
//        setContentView(R.layout.activity_main);
//        // checking if the permission has already been granted
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "Permissions granted");
//            initializeCamera(javaCameraView, activeCamera);
//        } else {
//            // prompt system dialog
//            Log.d(TAG, "Permission prompt");
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
//        }
//
//        bt3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
////                startActivityForResult(intent, 100);
//                startCropActivity();
//            }
//        });
//
//        // handle the Choose Image button to trigger
//        // the image chooser function
//        bt2.setOnClickListener(new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View v) {
//                                                imageChooser();
//                                            }
//        });
//
//        bt1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//              //  Core.bitwise_or(srcmat1, srcmat2, dstmat);
//                //Imgproc.cvtColor(srcmat1, dstmat, Imgproc.COLOR_BGRA2GRAY);
//                dstmat = Mat.ones(srcmat1.size(), CvType.CV_8UC1);
//                int ch = dstmat.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
//                for (int i = 0; i < dstmat.cols(); i++) {
//                    double ration = 0.0;
//                    for (int j = 0; j < dstmat.rows(); j++) {
//                        double[] data = srcmat1.get(j, i); //Stores element in an array
//                        ration += data[0] / 255;
//                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
//                        {
//                            data[k] = 255; //Pixel modification done here
//                        }
//                        dstmat.put(j, i, data); //Puts element back into matrix
//                    }
//                    double[] data = srcmat1.get(0, 0); //Stores element in an array
//                    data[0] = 0;
//                    ration = ration / dstmat.rows();
//                    //(index, i)
//                    int index = (int) (dstmat.rows() * (1 - ration));
////                    dstmat.put(index, i, data);
//                    for (int k = index - 5; k < index + 5; k++) {
//                        for (int m = i - 5; m < i + 5; m++) {
//                            dstmat.put(k, m, data);
//                        }
//                    }
//                }
//                //Imgproc.threshold(dstmat,dstmat,125,255,Imgproc.THRESH_BINARY);
//                //Imgproc.adaptiveThreshold(dstmat,dstmat,255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 13, 5);
////                Imgproc.line(srcmat1,new Point(0,dstmat.height()),new Point(dstmat.width(),0),new Scalar(255,0,0),4);
////                Imgproc.putText(srcmat1,"I`m a cat",new Point(srcmat1.width() /2,srcmat1.height()/3),2,2,new Scalar(0,255,0),3);
//////                for (int i = 0; i < dstmat.width(); i++) {
//////
//////                    for (int j = 0; j < dstmat.height(); j++) {
//////                        ration += dstmat.get(i, j);
//////                    }
//////                }
////
////
////
//                bitmap = Bitmap.createBitmap(dstmat.width(), dstmat.height(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(dstmat, bitmap);
////                iv1.setImageBitmap(bitmap);
//                iv2.setImageBitmap(bitmap);
//
//            }
//        });
//
//
//    }
//
//    // this function is triggered when
//    // the Select Image Button is clicked
//    void imageChooser() {
//
//        // create an instance of the
//        // intent of the type image
//        Intent i = new Intent();
//        i.setType("image/*");
//        i.setAction(Intent.ACTION_GET_CONTENT);
//
//        // pass the constant to compare it
//        // with the returned requestCode
//        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
//    }
//
//    // this function is triggered when user
//    // selects the image from the imageChooser
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            if (resultCode == RESULT_OK) {
//                Uri resultUri = result.getUri();
//                iv1.setImageURI(resultUri);
//
//                try {
//                    Bitmap bmp = null;
//                    bmp = MediaStore.Images.Media.getBitmap(
//                            this.getContentResolver(),
//                            resultUri);
//                    Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
//                    Utils.bitmapToMat(bmp, obj);
//                    srcmat1 = obj;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//                Exception error = result.getError();
//            }
//        }
//        if (requestCode == 100) {
//            Bitmap captureImage = (Bitmap) data.getExtras().get("data");
//            iv2.setImageBitmap(captureImage);
//        }
//
//        if (resultCode == RESULT_OK) {
//
//            // compare the resultCode with the
//            // SELECT_PICTURE constant
//            if (requestCode == SELECT_PICTURE) {
//                // Get the url of the image from data
//                Uri selectedImageUri = data.getData();
//                if (null != selectedImageUri) {
//                    // update the preview image in the layout
//                    iv1.setImageURI(selectedImageUri);
//                    try {
//                        Bitmap bmp = null;
//                        bmp = MediaStore.Images.Media.getBitmap(
//                                this.getContentResolver(),
//                                selectedImageUri);
//                        Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
//                        Utils.bitmapToMat(bmp, obj);
//                        srcmat1 = obj;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            }
//        }
//    }
//
//    private void iniLoadOpenCV() {
//        boolean success = OpenCVLoader.initDebug();
//        if(success) {
//            Toast.makeText(this.getApplicationContext(), "Loading OpenCV Libraries...", Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
//        }
//    }
//
//    private void startCropActivity () {
//        // start picker to get image for cropping and then use the image in cropping activity
//        CropImage.activity()
//                .setGuidelines(CropImageView.Guidelines.ON)
//                .start(this);
//
//    }
//
//    @Override
//    public void onCameraViewStarted(int width, int height) {
//         mRGBA = new Mat(height, width, CvType.CV_8UC4);
//    }
//
//    @Override
//    public void onCameraViewStopped() {
//         mRGBA.release();
//    }
//
//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        mRGBA = inputFrame.rgba();
//        mRGBAT = mRGBA.t();
//        Core.flip(mRGBA.t(), mRGBAT,1);
//        Imgproc.resize(mRGBAT, mRGBAT, mRGBA.size());
//        return mRGBAT;
//    }
//
//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//
//    }
//
//    // callback to be executed after the user has givenapproval or rejection via system prompt
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == MY_CAMERA_REQUEST_CODE) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // camera can be turned on
//                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
//                initializeCamera(javaCameraView, activeCamera);
//            } else {
//                // camera will stay off
//                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    private void initializeCamera(JavaCameraView javaCameraView, int activeCamera){
//        javaCameraView.setCameraPermissionGranted();
//        javaCameraView.setCameraIndex(activeCamera);
//        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
//        javaCameraView.setCvCameraViewListener(this);
//    }
//}