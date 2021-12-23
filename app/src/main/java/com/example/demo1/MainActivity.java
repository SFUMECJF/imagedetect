
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
    Mat mRGBASave;
    int[] loc = new int[3];
    boolean cameraFlag;
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
        mRGBASave = new Mat();
        loc[1] = 20;
        loc[2] = 60;
        cameraFlag = true;

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
                cameraFlag = !cameraFlag;

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

                // save block
                matSave = mRGBASave.clone();
                // display blockview
                Rect roi = new Rect( 0, mRGBASave.height() / 3, mRGBASave.width(), mRGBASave.height() / 3);
                Mat blockViewMat = new Mat(mRGBASave, roi);

                ivSetMat(blockView, blockViewMat);

//                if (!cameraFlag)
//                {
                    myContours(mRGBASave);
//                }


                // display resultview
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

//                Imgproc.putText(resultBackground,"T",new Point(loc[1],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);
//                Imgproc.putText(resultBackground,"C",new Point(loc[2],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);


                Core.flip(curve, curve, 0);

//                findTC(curve);

                myDottedLine(curve, new Point(0, curve.height() / 4));
                myDottedLine(curve, new Point(0, curve.height() / 4 * 2));
                myDottedLine(curve, new Point(0, curve.height() / 4 * 3));

                myCopy(curve, resultBackground, new Rect(resultView.getHeight() / 20 * 10,10, -1, -1));

                Imgproc.putText(resultBackground,"T",new Point(loc[1],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);
                Imgproc.putText(resultBackground,"C",new Point(loc[2],resultView.getHeight() / 20 * 11),2,2,new Scalar(0,0,0),3);

                ivSetMat(resultView, resultBackground);


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
                }


            }
        }
        if (resRect.x != 0) {
            Imgproc.rectangle(src, new Point(resRect.x, resRect.y), new Point(resRect.x + resRect.width, resRect.y + resRect.height), new Scalar(0, 255, 0, 0), 3);
            block = new Mat(srcOrigin, resRect);
            Log.d(TAG, "find red block and save");
            bigBlock = new Mat(srcOrigin, new Rect(resRect.x, resRect.y - 20, resRect.width, resRect.height + 40));
            mRGBASave = srcOrigin.clone();
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



    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
//                blockView.setImageURI(resultUri);

                try {
                    Bitmap bmp = null;
                    bmp = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(),
                            resultUri);
                    topView.setImageBitmap(bmp);
                    Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(bmp, obj);
                    mRGBASave = obj;
//                    ivSetMat(topView, mRGBASave);
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
//                    topView.setImageURI(selectedImageUri);
                    try {
                        Bitmap bmp = null;
                        bmp = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(),
                                selectedImageUri);
                        Mat obj = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(bmp, obj);
                        mRGBASave = obj;
                        ivSetMat(topView, mRGBASave);
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
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        hierarchy.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        if (!cameraFlag)
            return inputFrame.rgba();
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


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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