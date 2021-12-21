
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
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.resize;
//import com.example.demo1.CustomOpenCVJavaCameraView;
import org.opencv.features2d.SimpleBlobDetector;
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static String TAG = "MainActivity";
        private Button bt1, bt2, bt3, btSave;
    private ImageView blockView, resultView, topView;//resultView,topView;
    private Mat srcmat1, dstmat, hsvMat, topMat;
    private Bitmap bitmap, contours_bmap;
    private Bitmap bmap;
    JavaCameraView javaCameraView;
    Mat mRGBA, mRGBAT, resizeimage;        //Mat resizeimage = new Mat();
    Mat matSave;
    Mat origin, block, detectRegion, detectRegionLine, detectRegionCurve, resultBackground;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK;// or front
    int SELECT_PICTURE = 200;
    //private List<MatOfPoint> contours;
    private MatOfPoint2f contours2f, approxCurve;
    private int contoursSize;
    private Bitmap resultBitmap;
    //Mat mRgba, mHsv,hierarchy,mHsvMask ,mDilated;
    Mat mRgba, mIntermediateMat, mGray, hierarchy;
    Mat background;
    List<MatOfPoint> contours;
    int[] loc = new int[3];

    Mat bigBlock;


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
        blockView = findViewById(R.id.imageView);
        resultView = findViewById(R.id.imageView3);
   //     resultView = findViewById(R.id.imageView2);
        topView = findViewById(R.id.topView);
        srcmat1 = new Mat();
    //    srcmat2 = new Mat();
        dstmat = new Mat();
        topMat = new Mat();
        matSave = new Mat();
        background = new Mat();
        bigBlock = new Mat();
        loc[1] = 20;
        loc[2] = 60;

        resultBackground = new Mat();
        try {
            detectRegionLine = Utils.loadResource(this, R.drawable.white);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            background = Utils.loadResource(this, R.drawable.white);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            resultBackground = Utils.loadResource(this, R.drawable.white);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        resultView.setImageBitmap();

        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent, 100);
                //startCropActivity();
                //Create the rectangle
                //        Imgproc.rectangle(mRGBA, new Point(w * 12 / 24, h * 11 / 24), new Point(
                //                w * 13 / 24, h * 15/ 24), new Scalar( 0, 255, 0 ), 3);

                Rect roi = new Rect( mRGBA.width()  / 3, 0, mRGBA.width() / 3, mRGBA.height());
//                Rect roi = new Rect( mRGBA.width() * 12 / 24, mRGBA.height() * 10 / 24, mRGBA.width() * 1/ 24, mRGBA.height() * 6/ 24);

//                Imgproc.rectangle(mRGBA, new Point(w * 1 / 3, 0), new Point(
//                        w * 2 / 3, h), new Scalar( 0, 0, 255 ), 5
//                );

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


                Size scaleSize = new Size(blockView.getWidth(),blockView.getHeight());
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
//                blockView.setImageBitmap(contours_bmap);

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
//                blockView.setImageBitmap(resultBitmap);


//                Imgproc.rectangle(srcmat1, new Point(2, 2), new Point(
//                        srcmat1.width() - 2, srcmat1.height() - 2), new Scalar( 255, 0, 0 ), 1
//                );
//


                //Imgproc.rectangle(srcmat1, new Point(0, 0), new Point(srcmat1.width()  , srcmat1.height() ), new Scalar( 0, 0, 255 ), 3);

                matSave = srcmat1.clone();
                bitmap = Bitmap.createBitmap(srcmat1.width(), srcmat1.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcmat1, bitmap);
//                blockView.setImageBitmap(bitmap);
//                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "origin" , "yourDescription");
                blockView.setImageBitmap(bitmap);
            }
        });

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

                resultBackground = background.clone();
                resize(resultBackground, resultBackground, new Size(resultView.getWidth(), resultView.getHeight()) , 0, 0,INTER_AREA  );//INTER_AREA

                Mat blockMat = new Mat();
                Imgproc.cvtColor(block,blockMat,Imgproc.COLOR_BGRA2BGR);// 应该是COLOR_BGRA2BGR， 如果是rga2bgr，颜色都不对了
                resize(blockMat, blockMat, new Size(resultView.getWidth() - 20, resultView.getHeight() / 20 * 9) , 0, 0,INTER_AREA  );//INTER_AREA

                myCopy(blockMat, resultBackground, new Rect(10,10, resultView.getWidth() - 20, resultView.getHeight() / 20 * 9));

                Log.d(TAG, "block mat: " + new String(String.valueOf(blockMat.width())) + "  " + new String(String.valueOf(blockMat.height())));

//                Imgproc.cvtColor(bigBlock,bigBlock,Imgproc.COLOR_BGRA2BGR);
//                myRedBlob(bigBlock);//传的是CV8uC3  BGR  opencv用的bgr

//                Imgproc.putText(resultBackground,"T",new Point(loc[1],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);
//                Imgproc.putText(resultBackground,"C",new Point(loc[2],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);

                Mat curve = myCurveFindTC(blockMat);

                Imgproc.putText(resultBackground,"T",new Point(loc[1],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);
                Imgproc.putText(resultBackground,"C",new Point(loc[2],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);


                Core.flip(curve, curve, 0);

//                findTC(curve);

                myDottedLine(curve, new Point(0, curve.height() / 4));
                myDottedLine(curve, new Point(0, curve.height() / 4 * 2));
                myDottedLine(curve, new Point(0, curve.height() / 4 * 3));

                myCopy(curve, resultBackground, new Rect(resultView.getHeight() / 20 * 12,10, -1, -1));

                ivSetMat(resultView, resultBackground);

//                Mat dst = Mat.ones(new Size(resultView.getWidth(),resultView.getHeight()), CvType.CV_8UC3);
//                Mat imageROI  = new Mat(dst, new Rect(0,0, 80, 80));
//                Mat imageROI  = dst.submat(new Rect(0,0, 80, 80));
////                blockMat.copyTo(imageROI);
//                myCopy(blockMat, dst, new Rect(0,0, 80, 80));
//
//
//
//                Rect blockRect = new Rect(15, 15, resultView.getWidth() - 40, 80);
//                resize(resultBackground, resultBackground, new Size(resultView.getWidth(), resultView.getHeight()) , 0, 0,INTER_AREA  );//INTER_AREA
//                resize(blockMat, blockMat, new Size(resultView.getWidth() -40, 80) , 0, 0,INTER_AREA  );//INTER_AREA
//                Log.d(TAG, "block type is ： " + new String(String.valueOf(blockMat.type())));//block type is ： 24   CV_8UC4
//                Log.d(TAG, "resultBackground type is ： " + new String(String.valueOf(resultBackground.type())));//resultBackground type is ： 16 CV_8UC3
//
////                Mat temp = new Mat();
//                // submat也是copy，不是引用
////                temp = resultBackground.submat(new Rect(0,0, 80, 80));
//                Mat imageROI  = new Mat(resultBackground, new Rect(0,0, 80, 80));
////                blockMat.copyTo(imageROI);
//                Imgproc.GaussianBlur(imageROI, imageROI, new Size(55, 55), 55);
//                ivSetMat(resultView, resultBackground);
//                ivSetMat(resultView, block);
//
//
//              //  Core.bitwise_or(srcmat1, srcmat2, dstmat);
//                //Imgproc.cvtColor(srcmat1, dstmat, Imgproc.COLOR_BGRA2GRAY);
//                srcmat1 = block;
//                Imgproc.medianBlur(srcmat1, srcmat1, 3);
//                Imgproc.GaussianBlur(srcmat1, srcmat1, new Size(11.0, 11.0), 4, 4);
//
//                Mat binaryMat = new Mat();
////                Imgproc.cvtColor(dstmat, binaryMat, Imgproc.COLOR_BGR2GRAY);
//                hsvMat = srcmat1.clone();
////                Mat resultMat = new Mat();
//
//                Imgproc.cvtColor(srcmat1, binaryMat, Imgproc.COLOR_BGR2GRAY);
////                Imgproc.cvtColor(srcmat1, hsvMat, Imgproc.COLOR_BGR2HSV);
////                Core.inRange(hsvMat, new Scalar(110, 0, 0), new Scalar(160, 255, 255), srcmat1);
//
////                dstmat = Mat.ones(binaryMat.size(), CvType.CV_8UC3);
//                Size scaleSize = new Size(blockView.getWidth(),blockView.getHeight());
//                resize(background, dstmat, scaleSize , 0, 0,INTER_AREA  );//INTER_AREA
//
//
//                int ch = dstmat.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
//
//                Log.d(TAG, "channel : " + new String(String.valueOf(ch)));
//
//                for (int i = 0; i < srcmat1.cols(); i++) {
//                    double ration = 0.0;
//                    for (int j = 0; j < srcmat1.rows(); j++) {
//                        double[] data = binaryMat.get(j, i); //Stores element in an array
//                        ration += data[0] / 255;
////                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
////                        {
////                            data[k] = 255; //Pixel modification done here
////                        }
////                        dstmat.put(j, i, data); //Puts element back into matrix
//                    }
//
//                    double[] data = dstmat.get(0, 0); //Stores element in an array
//                    data[0] = 0;
//                    data[1] = 0;
//                    data[2] = 0;
//
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
//
//
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
////                dstmat = binaryMat;
////                Imgproc.cvtColor(binaryMat, dstmat, Imgproc.COLOR_GRAY2BGR);
//                //Imgproc.cvtColor(binaryMat, dstmat, Imgproc.COLOR_RGB2BGR);
//                //Imgproc.cvtColor(hsvMat, dstmat, Imgproc.COLOR_HSV2BGR);
//
////                resultBackground = Mat.ones(resultView.getHeight(), resultView.getWidth(), CvType.CV_8UC3);
//                int backgroundStartX = 0;
//                int backgroundStartY = 0;
//                int backgroundWidth = resultView.getWidth();
//                int backgroundHeight = resultView.getHeight();
//
//                resize(resultBackground, resultBackground, new Size(resultView.getWidth(), resultView.getHeight()));
//                Imgproc.rectangle(resultBackground, new Point(10, 10), new Point(dstmat.width() - 10, dstmat.height() - 10), new Scalar( 255, 255, 255 ), 3);
////                Imgproc.putText(resultBackground,"T",new Point(dstmat.width() - 10 + 2,dstmat.height() - 10 + 2),2,2,new Scalar(255,0,0),3);
//
//                Mat small = resultBackground.submat(new Rect(10, 10, backgroundWidth - 11, 99));
////                Mat temp = small.clone();
////                Mat smallClone = small.clone();
////                smallClone = myContours(smallClone);
//                block.copyTo(small);
//
//                Imgproc.putText(resultBackground,"T",new Point(resultBackground.width() /2,dstmat.height() + 35),2,2,new Scalar(0,0,0),3);
//                Imgproc.putText(resultBackground,"C",new Point(resultBackground.width() /2 + 40,dstmat.height() + 35),2,2,new Scalar(0,0,0),3);
////                Imgproc.line(resultBackground,new Point(resultBackground.width() /2 + 20,0),new Point(resultBackground.width() /2 + 20,dstmat.height()),new Scalar(255,0,0),4);
////                Imgproc.line(resultBackground,new Point(resultBackground.width() /2 + 40 + 20, 0),new Point(resultBackground.width() /2 + 40 + 20,dstmat.height()),new Scalar(255,0,0),4);
//
//                Imgproc.rectangle(resultBackground, new Point(15, dstmat.height() + 60), new Point(15 + dstmat.width() - 10, dstmat.height() + 60 + dstmat.height() - 10), new Scalar( 255, 255, 255 ), 3);
////                Imgproc.putText(resultBackground,"反应强度",new Point(resultBackground.width() /2,dstmat.height() + 60 + dstmat.height() - 10),2,2,new Scalar(0,0,0),3);
//
////                dstmat.copyTo(resultBackground);
//                //                Core.flip(dstmat, dstmat, 0);
////                Imgproc.rectangle(dstmat, new Point(0, 0), new Point(dstmat.width(), dstmat.height()), new Scalar( 0, 0, 255 ), 3);
////                bitmap = Bitmap.createBitmap(dstmat.width(), dstmat.height(), Bitmap.Config.ARGB_8888);
////                Utils.matToBitmap(dstmat, bitmap);
//////                blockView.setImageBitmap(bitmap);
////                resultView.setImageBitmap(bitmap);
//
//                ivSetMat(resultView, resultBackground);


            }
        });

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmap = Bitmap.createBitmap(matSave.width(), matSave.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matSave, bitmap);
//                blockView.setImageBitmap(bitmap);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "origin" , "yourDescription");
//                blockView.setImageBitmap(bitmap);
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



    void ivSetMat(ImageView iv, Mat mat) {
        Mat temp = new Mat();
        Bitmap tempBmap = Bitmap.createBitmap(iv.getWidth(),  iv.getHeight(), Bitmap.Config.ARGB_8888);
        resize(mat, temp, new Size(iv.getWidth(),  iv.getHeight()));
        Utils.matToBitmap(temp, tempBmap);
////                blockView.setImageBitmap(bitmap);
        iv.setImageBitmap(tempBmap);
    }
    // 为什么RETR_EXTERNAL没有反应呢？感觉用这种方法应该就没问题了才对。
    //    RETR_EXTERNAL:表示只检测最外层轮廓，对所有轮廓设置hierarchy[i][2]=hierarchy[i][3]=-1   检测效果不好
//    RETR_LIST:提取所有轮廓，并放置在list中，检测的轮廓不建立等级关系   效果可以
//    RETR_CCOMP:提取所有轮廓，并将轮廓组织成双层结构(two-level hierarchy),顶层为连通域的外围边界，次层位内层边界
//    RETR_TREE:提取所有轮廓并重新建立网状轮廓结构
//    RETR_FLOODFILL：官网没有介绍，应该是洪水填充法
    Mat myContours(Mat src) {
        Mat srcOrigin = src.clone();
        Rect resRect = new Rect(0 , 0, src.width() * 5 / 24, src.height() * 1/ 24);

        contours = new ArrayList<MatOfPoint>();
        hierarchy = new Mat();
        Imgproc.Canny(src, mIntermediateMat, 70, 105); // 20  35的值不错，再小的值，噪音会特别大，处理也很麻烦。 最好能只处理submat
        // 70   105现在不错，能够找到方框, 但是有红色的时候无法识别。
        // canny 这里的阈值调整非常重要。后续能否识别到轮廓，识别到什么轮廓都和这里的设置有关系。
        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        hierarchy.release();

//        ivSetMat(resultView, mIntermediateMat);
//        return mIntermediateMat;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            Rect rect = Imgproc.boundingRect(points);
            double height = rect.height;
            double width = rect.width;

            // src.width() / 2 >= rect.x && src.height() / 2 >= rect.y && src.width() / 2 <= rect.x + width && src.height() / 2 <= rect.y + height &&
            if (height > 30 && width < 400 && width > 100) {// width 横向   height 纵向 竖的  height < 200 &&
                // (abs(src.width() / 2 - rect.x) < 80 && abs(src.height() / 2 - rect.y) < 80) ||
                if ( rect.x < src.width() / 2 - 30 && (abs(src.width() / 2 - rect.x - rect.width) < 80 && abs(src.height() / 2 - rect.y - rect.height) < 80)) {

                    if (resRect.x > rect.x || resRect.x == 0)
                        resRect.x = rect.x;
                    if (resRect.y > rect.y || resRect.y == 0)
                        resRect.y = rect.y;
//                    if (resRect.width < rect.width) {
//                        resRect.width += rect.width;
//                        resRect.width = min(rect.width, src.width() / 20 * 7);
//                    }
//
//                    resRect.height = max(resRect.height, rect.height);
//                    Imgproc.rectangle(src, new Point(resRect.x, resRect.y), new Point(resRect.x + resRect.width, resRect.y + resRect.height), new Scalar(0, 255, 0, 0), 3);
                }

//                Imgproc.putText(src, "contours", rect.tl(), 0, 2, new Scalar(0, 255, 255), 4);

            }
        }
        if (resRect.x != 0) {
            Imgproc.rectangle(src, new Point(resRect.x, resRect.y), new Point(resRect.x + resRect.width, resRect.y + resRect.height), new Scalar(0, 255, 0, 0), 3);
            block = new Mat(srcOrigin, resRect);
            bigBlock = new Mat(srcOrigin, new Rect(resRect.x, resRect.y - 20, resRect.width, resRect.height + 40));
        }

        return src;
    }

    Mat myCopy(Mat src, Mat dst, Rect rect) {
        int ch = src.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
                for (int i = 0; i < src.cols(); i++) {
                    for (int j = 0; j < src.rows(); j++) {
                        double[] data = src.get(j, i); //Stores element in an array
                        dst.put(j + rect.x, i + rect.y, data); //Puts element back into matrix
                    }
                }
                return dst;
    }

    Mat myCurveFindTC(Mat src) {

        Mat binaryMat = new Mat();
        Imgproc.cvtColor(src, binaryMat, Imgproc.COLOR_BGR2GRAY);

        Mat result = new Mat();
        try {
            result = Utils.loadResource(this, R.drawable.white);
        } catch (IOException e) {
            e.printStackTrace();
        }
        resize(result, result, src.size());
        Log.d(TAG, "result channel : " + new String(String.valueOf(result.channels())));


        int ch = binaryMat.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
        double[] rationLoc = new double[binaryMat.cols() + 1];
        double maxRation = 0;

        for (int i = 0; i < binaryMat.cols(); i++) {
            double ration = 0.0;
            for (int j = 0; j < binaryMat.rows(); j++) {
                double[] data = binaryMat.get(j, i); //Stores element in an array
                ration += data[0] / 255;
                rationLoc[i] = ration;
                if (ration > maxRation)
                    maxRation = ration;
//                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
//                        {
//                            data[k] = 255; //Pixel modification done here
//                        }
//                result.put(j, i, data); //Puts element back into matrix

            }
            double[] data = result.get(0, 0); //Stores element in an array
            data[0] = 0;
            data[1] = 0;
            data[2] = 0;

            ration = ration / result.rows();
            //(index, i)
            int index = (int) (result.rows() * (1 - ration));
//                    dstmat.put(index, i, data);
            for (int k = index - 5; k < index + 5; k++) {
                for (int m = i - 5; m < i + 5; m++) {
                    result.put(k, m, data);
                }
            }
        }

        int start = 0, end = 0;
        for (int i = (int) (0.9 * binaryMat.cols()); i > binaryMat.cols() / 2; i--) {
            if (rationLoc[i] < 0.5 * maxRation) {
                end = i;
                break;
            }
        }
        for (int i = end - 15; i > 0; i--) {
            if (rationLoc[i] > 0.5 * maxRation) {
                start = i;
                break;
            }
        }
        loc[2] = start + (end - start)/ 2;

        int endT = 0;
        int startT = 0;
        for (int i = start - 15; i > 0; i--) {
            if (rationLoc[i] < 0.5 * maxRation) {
                endT = i;
                break;
            }
        }
        for (int i = endT - 15; i > 0; i--) {
            if (rationLoc[i] > 0.5 * maxRation) {
                startT = i;
                break;
            }
        }
        loc[1] = startT + (endT - startT) / 2;
        Log.d(TAG, "length   loc  " + new String(String.valueOf(binaryMat.cols())) + "  " + new String(String.valueOf(loc[1])) + "  " + new String(String.valueOf(loc[2])));

        return result;
    }

    void findTC(Mat src) {
        Mat binaryMat = new Mat();
        binaryMat = src.clone();

        double[] rationLoc = new double[binaryMat.cols() + 1];
        double maxRation = 0;

        for (int i = 0; i < binaryMat.cols(); i++) {
            double ration = 0.0;
            for (int j = 0; j < binaryMat.rows(); j++) {
                double[] data = binaryMat.get(j, i); //Stores element in an array
                ration += data[0];

            }
            rationLoc[i] = ration;
            if (ration > maxRation)
                maxRation = ration;

        }

        int start = 0, end = 0;
        int endC = 0;
        for (int i = (int) (0.8 * binaryMat.cols()); i > binaryMat.cols() / 2; i--) {
            if (rationLoc[i] < maxRation / 2) {
                end = i;
                break;
            }
        }
        for (int i = binaryMat.cols() / 2; i < 0.9 * binaryMat.cols(); i++) {
            if (rationLoc[i] < maxRation / 2) {
                start = i;
                break;
            }
        }
        loc[2] = start + end / 2;
        endC = start;
        start = 0;
        end = 0;
        for (int i = (int)(0.05 * binaryMat.cols()); i < binaryMat.cols() / 2; i++) {
            if (rationLoc[i] < 0.7 * maxRation) {
                start = i;
                break;
            }
        }
        for (int i = binaryMat.cols() / 2; i > 0; i--) {
            if (rationLoc[i] < 0.7 * maxRation) {
                end = i;
                break;
            }
        }
        loc[1] = start + end / 2;



        Log.d(TAG, "loc  " + new String(String.valueOf(loc[1])) + "  " + new String(String.valueOf(loc[2])));

    }
    void myDottedLine(Mat src, Point start) {
        for (int i = 0; i < src.width() / 10; i++) {
            if (i %2 == 0) {
                Imgproc.line(src,new Point(start.x + i * 10,start.y),new Point(start.x + i * 10 + 10,start.y),new Scalar(0,0,0),1);
            }
        }
    }
    Mat myRedContours(Mat src) {
        List<MatOfPoint> myRedContours;
        myRedContours = new ArrayList<MatOfPoint>();
        Mat myRedHierarchy = new Mat();
        Mat cannyMat = new Mat();

        Imgproc.Canny(src, cannyMat, 70, 105); // 20  35的值不错，再小的值，噪音会特别大，处理也很麻烦。 最好能只处理submat
        Imgproc.findContours(cannyMat, myRedContours, myRedHierarchy, Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        myRedHierarchy.release();

        for (int contourIdx = 0; contourIdx < myRedContours.size(); contourIdx++) {
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f(myRedContours.get(contourIdx).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            Rect rect = Imgproc.boundingRect(points);
            double height = rect.height;
            double width = rect.width;
            if (height > 0.4 * src.height())
            Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 0), 3);
            }


        return src;
    }
    Mat myRedBlob(Mat src) {
        Mat myhierarchy = new Mat();
        Mat tempMat = new Mat();
        Imgproc.Canny(src, tempMat, 70, 105); // 20  35的值不错，再小的值，噪音会特别大，处理也很麻烦。 最好能只处理submat
        // 70   105现在不错，能够找到方框, 但是有红色的时候无法识别。
        // canny 这里的阈值调整非常重要。后续能否识别到轮廓，识别到什么轮廓都和这里的设置有关系。
        List<MatOfPoint> myRedContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(tempMat, myRedContours, myhierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        myhierarchy.release();
        int contoursLength = 0;
        for (int contourIdx = 0; contourIdx < myRedContours.size(); contourIdx++) {
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f(myRedContours.get(contourIdx).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            Rect rect = Imgproc.boundingRect(points);
            double height = rect.height;
            double width = rect.width;
            double rectArea = width * height;
            double srcArea = src.width() * src.height();
            if (rect.x > 0.1 * src.width() && rect.x < 0.95 * src.width() && rectArea > 0.025 * srcArea && rectArea < 0.5 * srcArea) {
                contoursLength += 1;
                if (contoursLength > 2)
                    loc[2] = (int)(rect.x + width) / 2;
                else
                    loc[contoursLength] = (int)(rect.x + width/ 2) ;
                Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255), 1);
            }

        }
        Log.d(TAG, "find contours size" + new String(String.valueOf(contoursLength)));
        ivSetMat(blockView, src);
        return src;






//        Mat mat = new Mat();
//        Log.d(TAG, "redblob type  " + new String(String.valueOf(src.type())));// 16  CV_8UC3
//
//
//        Mat matsrc = src.clone();
//        Mat mathsv = matsrc.clone();
//        Mat matwhite = matsrc.clone();
////
//        Imgproc.cvtColor(matsrc, mathsv,Imgproc.COLOR_BGR2HSV);
////
////        //red color
//        Core.inRange(mathsv, new Scalar(110, 0, 0), new Scalar(160, 255, 255), matwhite);
////
////        Imgproc.erode(matwhite, matwhite, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
////        Imgproc.dilate( matwhite, matwhite, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
////
////        Imgproc.dilate( matwhite, matwhite, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
////        Imgproc.erode(matwhite, matwhite, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
//
////        Mat matFim = matwhite.clone();
//
//
//
////        Core.bitwise_and(matsrc, matsrc, matFim, matwhite);
////
//////        Imgproc.cvtColor(matFim, matFim, Imgproc.COLOR_GRAY2BGR);
//////        Imgproc.cvtColor(matFim, matFim, Imgproc.COLOR_RGB2RGBA);
////
//////        return matFim;
////        ivSetMat(blockView, matwhite);
////        int startT = 0;
////        int endT = 0;
////        int startC = 0;
////        int endC = 0;
////        for (int i = 0; i < matwhite.cols(); i++) {
////            double[] data = matwhite.get(matwhite.rows() / 2, i); //Stores element in an array
////            Log.d(TAG, "pixel   " + new String(String.valueOf(data[0])) );
////            if (255 - (int)data[0] < 5) {
////                int j = i;
////                for (; j < matwhite.cols(); j++) {
////                    double[] value = matwhite.get(matwhite.rows() / 2, j);
////                    if (255 - (int)value[0] > 5) {
////                        break;
////                    }
////                }
////                if (j - i > 10) {
////                    TLength = (j + i) / 2;
////                    Log.d(TAG, "tlength   " + new String(String.valueOf(TLength)) );
////                }
////
////            }
////        }
////        Log.d(TAG, "tlength   " + new String(String.valueOf(TLength)) + "length  " + new String(String.valueOf(matwhite.cols())));
////        return src;
//
//
//        Mat whiteDilated = new Mat();
////        Imgproc.dilate(matwhite, whiteDilated, new Mat());
////        Imgproc.cvtColor(whiteDilated, whiteDilated, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.cvtColor(src, whiteDilated, Imgproc.COLOR_BGR2GRAY);
//        List<MatOfPoint> myRedContours = new ArrayList<MatOfPoint>();
//        Mat mHierarchy = new Mat();
//        Imgproc.findContours(whiteDilated, myRedContours, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//        for (int contourIdx = 0; contourIdx < myRedContours.size(); contourIdx++) {
//            MatOfPoint2f approxCurve = new MatOfPoint2f();
//            MatOfPoint2f contour2f = new MatOfPoint2f(myRedContours.get(contourIdx).toArray());
//            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
//            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//
//            Rect rect = Imgproc.boundingRect(points);
//            double height = rect.height;
//            double width = rect.width;
//            Log.d(TAG, "find contours");
//            Imgproc.rectangle(whiteDilated, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 255), 3);
//
//        }
//        ivSetMat(blockView, whiteDilated);
//        return src;

//        src = myRedContours(src);


//        contours = new ArrayList<MatOfPoint>();





//                Mat binaryMat = new Mat();
//                Imgproc.cvtColor(rgbImage, binaryMat, Imgproc.COLOR_BGR2GRAY);
//
//
//
//
//        //https://stackoverflow.com/a/40918718/334402
//
////            Pyramid Down - this downsizes the image and looses some resolution
//        //See: http://docs.opencv.org/2.4/doc/tutorials/imgproc/pyramids/pyramids.html
//        //Mat mPyrDownMat = new Mat();
//        //Imgproc.pyrDown(rgbImage, mPyrDownMat);
//        //Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
//
//        //Convert color scheme to HSV - this means that a color can be
//        //identified with a single value, the hue, instead of three values
//        Mat mHsvMat = new Mat();
//        Imgproc.cvtColor(rgbImage, mHsvMat, Imgproc.COLOR_BGR2HSV_FULL);
//
//        //This creates a new image with only the color values that are wihtin
//        //the lower and upper thresholds set in mLowerBound and mUpperBound. These
//        //values were calculated when the method 'setHsvColor' was called with the
//        //color of the object that the user touched on the screen.
//        //So you effectively get an image with just the red or just the blue or whatever
//        //the color of the blob that the user selected was. Note that if there are multiple
//        //blobs or objects with this color you will get them all. You can see this quite easily
//        //with a simple test of the app with a couple of similar colored objects.
//        Scalar mLowerBound = new Scalar(200, 0, 0);
//        Scalar mUpperBound = new Scalar(255, 255, 255);
//        Mat mMask = new Mat();
//        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
//
//        //dilate effectively emphasises the brighter colors, so making them bigger within the image
//        //In this case it should be the chosen color which is emphasised against the
//        //darker (black) background.
//        //See:http://docs.opencv.org/2.4/doc/tutorials/imgproc/erosion_dilatation/erosion_dilatation.html
//        Mat mDilatedMask = new Mat();
//        Imgproc.dilate(mMask, mDilatedMask, new Mat());
//
//        Mat tempRes = new Mat();
////        Imgproc.cvtColor(mDilatedMask, tempRes, Imgproc.COLOR_HSV2BGR);
//        ivSetMat(blockView, mDilatedMask);
////        return rgbImage;
//
//        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//        //Finds the contours which in this case means the edge of the color blobs
//        Mat mHierarchy = new Mat();
//        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        // Find max contour area
//        //This is actually refering to the area enclosed by a contour. For this to work it is important
//        //that the contour be closed, so if this is not the case some objects may be missed here.
////        double maxArea = 0;
////        Iterator<MatOfPoint> each = contours.iterator();
////        while (each.hasNext()) {
////            MatOfPoint wrapper = each.next();
////            double area = Imgproc.contourArea(wrapper);
////            if (area > maxArea)
////                maxArea = area;
////        }
//
//        // Filter contours by area and resize to fit the original image size
//        //Here we are simply discrading any contours that are below the min size that was
//        //set in the method 'setMinContourArea' or the default if it was not set. In other
//        //words discrading any small object detected.
//        List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
//        mContours.clear();
//        Iterator<MatOfPoint> each = contours.iterator();
//        each = contours.iterator();
//        double mMinContourArea = 0;
//        while (each.hasNext()) {
//            MatOfPoint contour = each.next();
//            if (Imgproc.contourArea(contour) > mMinContourArea) {
//                Core.multiply(contour, new Scalar(4,4), contour);
//                mContours.add(contour);
//            }
//        }
//        //Now we return the list of contours - each contour is a closed area that is
//        //colored in whatever color the user selected when they touched the object.
//        //This color, as a reminder, was set by a call to 'setHsvColor'.
////        public List<MatOfPoint> getContours() {
////        return mContours;
////        }
//        Log.d(TAG, "contours size : " + new String(String.valueOf(mContours.size())));
//        for (int contourIdx = 0; contourIdx < mContours.size(); contourIdx++)
//        {
//            MatOfPoint2f approxCurve = new MatOfPoint2f();
//            MatOfPoint2f contour2f = new MatOfPoint2f(mContours.get(contourIdx).toArray());
//            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.01;
//            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//
//            Rect rect = Imgproc.boundingRect(points);
//            double middle = rect.width + rect.x;
//            Log.d(TAG, " x,y width height " + new String(String.valueOf(rect.x)) + "  " + new String(String.valueOf(rect.y)) + "  " +  new String(String.valueOf(rect.width)) + "  " +  new String(String.valueOf(rect.height)) );
//            double height = rect.height;
//            double width = rect.width;
//
//            // src.width() / 2 >= rect.x && src.height() / 2 >= rect.y && src.width() / 2 <= rect.x + width && src.height() / 2 <= rect.y + height &&
////            if (height * width > 50) {// width 横向   height 纵向 竖的  height < 200 &&
//                // (abs(src.width() / 2 - rect.x) < 80 && abs(src.height() / 2 - rect.y) < 80) ||
////                if ( rect.x < src.width() / 2 - 30 && (abs(src.width() / 2 - rect.x - rect.width) < 80 && abs(src.height() / 2 - rect.y - rect.height) < 80)) {
////
////                    if (resRect.x > rect.x || resRect.x == 0)
////                        resRect.x = rect.x;
////                    if (resRect.y > rect.y || resRect.y == 0)
////                        resRect.y = rect.y;
////                    if (resRect.width < rect.width) {
////                        resRect.width += rect.width;
////                        resRect.width = min(rect.width, src.width() / 20 * 7);
////                    }
////
////                    resRect.height = max(resRect.height, rect.height);
//                    Imgproc.rectangle(rgbImage, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 0), 3);
////                }
//
////                Imgproc.putText(src, "contours", rect.tl(), 0, 2, new Scalar(0, 255, 255), 4);
//
////            }
//        }
//        ivSetMat(blockView, rgbImage);
//        ivSetMat(blockView, binaryMat);
//        return rgbImage;
    }


    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                blockView.setImageURI(resultUri);

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
            resultView.setImageBitmap(captureImage);
        }

        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    // update the preview image in the layout
                    blockView.setImageURI(selectedImageUri);
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
        Imgproc.rectangle(mRGBA, new Point(w * 1 / 3, 0), new Point(
                w * 2 / 3, h), new Scalar( 0, 0, 255 ), 5
        );

//        Imgproc.rectangle(mRGBA, new Point(w * 12 / 24, h * 10 / 24), new Point(
//                w * 13 / 24, h * 16/ 24), new Scalar( 0, 0, 255 ), 3);

        mRGBAT = mRGBA.t();
        Core.flip(mRGBA.t(), mRGBAT, 1);
        //return mRGBAT;
        resize(mRGBAT, mRGBAT, mRGBA.size());

        //Size scaleSize = new Size(javaCameraView.getWidth(),javaCameraView.getHeight());
//        Size scaleSize = new Size(200,200);
//        resize(mRGBAT, resizeimage, scaleSize , 0, 0,INTER_CUBIC  );//INTER_AREA
        //resizeimage = mRGBAT;
        //resize(mRGBAT, resizeimage, scaleSize , 0, 0,INTER_LINEAR  );
//         bmap = Bitmap.createBitmap(topView.getWidth() - 1, topView.getHeight() - 1, Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(mRGBAT, bmap);
////                blockView.setImageBitmap(bitmap);
//        topView.setImageBitmap(bmap);


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
//                Mat small = mRGBAT.submat(new Rect(mRGBAT.width() * 1 / 3, mRGBAT.height() / 3, mRGBAT.width() * 1 / 3, mRGBAT.height() / 3));
//                Mat temp = mRGBAT.clone();
//                Mat smallClone = small.clone();
//                smallClone = myContours(smallClone);

//                smallClone.copyTo(small);
                Log.d(TAG, "mRGBAT type  " + new String(String.valueOf(mRGBAT.type())));// 16  CV_8UC3
                Mat temp = myContours(mRGBAT);

                bmap = Bitmap.createBitmap(topView.getWidth(),  topView.getHeight(), Bitmap.Config.ARGB_8888);
                resize(temp, topMat, new Size(topView.getWidth(), topView.getHeight()));
                Utils.matToBitmap(topMat, bmap);
////                blockView.setImageBitmap(bitmap);
                topView.setImageBitmap(bmap);

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
//    private ImageView blockView, resultView;//resultView,topView;
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
//        blockView = findViewById(R.id.imageView);
//        resultView = findViewById(R.id.imageView3);
//   //     resultView = findViewById(R.id.imageView2);
//   //     topView = findViewById(R.id.imageView3);
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
////                blockView.setImageBitmap(bitmap);
//                resultView.setImageBitmap(bitmap);
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
//                blockView.setImageURI(resultUri);
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
//            resultView.setImageBitmap(captureImage);
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
//                    blockView.setImageURI(selectedImageUri);
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