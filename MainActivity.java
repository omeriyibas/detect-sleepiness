package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CascadeClassifier faceDetector;
    CascadeClassifier eyeDetector;
    CascadeClassifier mouthDetector;
    TextView blink_tv, yawn_tv, rub_tv;

    Mat rgba, gray, display,tresh_eye;
    CameraBridgeViewBase cameraBridgeViewBase;

    boolean notify_blink = false, notify_yawn = false, notify_rub = false;


    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            super.onManagerConnected(status);

            switch (status) {

                case BaseLoaderCallback.SUCCESS:

                    try {


                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);  // creating a folder


                        //face cascade

                        InputStream is_face = getResources().openRawResource(R.raw.frontalface);
                        File faceCascadeFile = new File(cascadeDir, "frontalface.xml"); // creating file on that folder
                        FileOutputStream os = new FileOutputStream(faceCascadeFile);
                        byte[] face_buffer = new byte[4096];
                        int face_byteRead;
                        // writing that file from raw folder
                        while ((face_byteRead = is_face.read(face_buffer)) != -1) {
                            os.write(face_buffer, 0, face_byteRead);
                        }
                        is_face.close();
                        os.close();

                        faceDetector = new CascadeClassifier(faceCascadeFile.getAbsolutePath());

                        //eye cascade

                        InputStream is_eye = getResources().openRawResource(R.raw.eye);
                        File eye_cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File eyeCascadeFile = new File(eye_cascadeDir, "eye.xml");
                        FileOutputStream fos_eye = new FileOutputStream(eyeCascadeFile);
                        byte[] buffer_eye = new byte[4096];
                        int bytesRead_eye;

                        while ((bytesRead_eye = is_eye.read(buffer_eye)) != -1) {
                            fos_eye.write(buffer_eye, 0, bytesRead_eye);
                        }
                        is_eye.close();
                        fos_eye.close();

                        eyeDetector = new CascadeClassifier(eyeCascadeFile.getAbsolutePath());

                        //mouth cascade

                        InputStream is_mouth = getResources().openRawResource(R.raw.mouth);
                        File mouth_cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mouthCascadeFile = new File(mouth_cascadeDir, "mouth.xml");
                        FileOutputStream fos_mouth = new FileOutputStream(mouthCascadeFile);
                        byte[] buffer_mouth = new byte[4096];
                        int bytesRead_mouth;

                        while ((bytesRead_mouth = is_mouth.read(buffer_mouth)) != -1) {
                            fos_mouth.write(buffer_mouth, 0, bytesRead_mouth);
                        }
                        is_mouth.close();
                        fos_mouth.close();

                        mouthDetector = new CascadeClassifier(mouthCascadeFile.getAbsolutePath());

                    } catch (IOException e) {
//                        Log.i(TAG, "Cascade file not found");
                    }


                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }


        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blink_tv = (TextView) findViewById(R.id.blink_tv);
        yawn_tv = (TextView) findViewById(R.id.yawn_tv);
        rub_tv = (TextView) findViewById(R.id.rub_tv);


        Dexter.withContext(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Toast.makeText(MainActivity.this, "granted", Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "denied", Toast.LENGTH_SHORT);

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                }).check();

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        blink_tv.setVisibility(View.INVISIBLE);
        yawn_tv.setVisibility(View.INVISIBLE);
        rub_tv.setVisibility(View.INVISIBLE);



    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        rgba = inputFrame.rgba();
        Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_BGRA2GRAY);

        int height = rgba.height();
        int absoluteFaceSize = (int) (height * 0.1); //max boyut 10da biri
        int mouth_height;


        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(rgba, faces, 1.2, 5, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());

        notify_blink = false;
        notify_rub = false;
        notify_yawn = false;


        for (Rect face : faces.toArray()) {

//            Imgproc.rectangle(rgba, new Point(face.x, face.y), new Point(face.x + face.width, face.y + face.height), new Scalar(0, 0, 255), 2);
            Rect roi_up = new Rect(new Point(face.x, face.y), new Point(face.x + face.width, face.y + (face.height / 2)));


//            Imgproc.rectangle(rgba, new Point(roi_up.x, roi_up.y), new Point(roi_up.x + roi_up.width, roi_up.y + roi_up.height), new Scalar(255, 0, 0), 2);


            Mat cropped_up = new Mat(rgba, roi_up);
            Mat cropped_up_gray = new Mat(gray, roi_up);

            MatOfRect eyes = new MatOfRect();


            eyeDetector.detectMultiScale(cropped_up, eyes,1.2, 5, 2, new Size(30, 30), new Size());

            for (Rect eye : eyes.toArray()) {
//                Imgproc.rectangle(cropped_up, new Point(eye.x, eye.y), new Point(eye.x + eye.width, eye.y + eye.height), new Scalar(0, 255, 0), 2);
                Imgproc.circle(cropped_up, new Point(eye.x+(eye.width/2), eye.y+(eye.height/2)),(int)eye.height/2, new Scalar(0, 255, 0), 2);
                Rect roi_eye=new Rect(new Point(eye.x, eye.y), new Point(eye.x + eye.width, eye.y + eye.height));
                Mat eye_roi=new Mat(gray,roi_eye);

                final List<MatOfPoint> counturs = new ArrayList<>();
                final Mat hierarchy = new Mat();

                Imgproc.findContours(eye_roi,counturs,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
                int area= (int) Imgproc.contourArea(counturs.get(0));
                if (area<1800){
                    notify_blink = true;
                }
                System.out.println(area);
            }
//
//            if (eyes.toArray().length == 0) {
//                notify_blink = true;
//            }
            if (eyes.toArray().length < 2) {
                notify_rub = true;
            }


            Rect roi_down = new Rect(new Point(face.x, face.y + (face.height / 2)), new Point(face.x + face.width, face.y + face.height));
//            Imgproc.rectangle(rgba, new Point(roi_down.x, roi_down.y), new Point(roi_down.x + roi_down.width, roi_down.y + roi_down.height), new Scalar(0, 255, 0), 2);
            Mat cropped_down = new Mat(rgba, roi_down);
            MatOfRect mouths = new MatOfRect();
            mouthDetector.detectMultiScale(cropped_down, mouths, 1.2, 5, 2, new Size(50, 50), new Size());
            for (Rect mouth : mouths.toArray()) {
                Imgproc.rectangle(cropped_down, new Point(mouth.x, mouth.y), new Point(mouth.x + mouth.width, mouth.y + mouth.height), new Scalar(0, 255, 0), 2);

                Rect roi_mouth=new Rect(new Point(mouth.x, mouth.y), new Point(mouth.x + mouth.width, mouth.y + mouth.height));
                Mat mouth_roi=new Mat(gray,roi_mouth);

                final List<MatOfPoint> counturs2 = new ArrayList<>();
                final Mat hierarchy2 = new Mat();

                Imgproc.findContours(mouth_roi,counturs2,hierarchy2,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
                int area2= (int) Imgproc.contourArea(counturs2.get(0));
//
                if (area2>12000){
                    notify_yawn = true;
                }
                System.out.println(area2);



//                mouth_height = (int) mouth.size().height;

//                System.out.println(mouth_height);
//                if (mouth_height > 80) {
//                    notify_yawn = true;
//                }

            }
            if (mouths.toArray().length==0){
                notify_yawn = true;
            }


        }

        Core.flip(rgba, rgba, 1);
        Imgproc.cvtColor(rgba, display, Imgproc.COLOR_RGBA2BGR);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (notify_blink) {
                    blink_tv.setVisibility(View.VISIBLE);
                } else {
                    blink_tv.setVisibility(View.INVISIBLE);
                }
                if (notify_rub){
                    rub_tv.setVisibility(View.VISIBLE);
                }else{
                    rub_tv.setVisibility(View.INVISIBLE);
                }
                if (notify_yawn){
                    yawn_tv.setVisibility(View.VISIBLE);
                }else{
                    yawn_tv.setVisibility(View.INVISIBLE);

                }

            }
        });

        return display;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat();
        gray = new Mat();
        display = new Mat();
    }


    @Override
    public void onCameraViewStopped() {
        rgba.release();
        gray.release();
        display.release();

    }


    @Override
    protected void onResume() {
        super.onResume();


        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "opencv cant start", Toast.LENGTH_SHORT).show();
        } else {
            try {
                baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }
}
