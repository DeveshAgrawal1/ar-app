package com.example.practice4;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

	private Mat mRgba;
	private Mat mGray;
	private boolean facefound = false;
	Bitmap maskk1;
	private CameraBridgeViewBase openCvCameraView;
	private CascadeClassifier cascadeClassifier;
	private CascadeClassifier cascadeClassifiereye;

	private int mCameraId=0;
	private boolean detectface = false;
	private boolean isplaying = false;
	private MediaPlayer mediaplayer;
	private int mAbsoluteFaceSize = 0;
	private float mRelativeFaceSize = 0.2f;

	private int checker = 5;
	private Handler handler = new Handler();
	private double timeElapsed = 0, finalTime = 0;

	public TextView position;
	private double currentpos;
	private double thispos;
	private Mat grayscaleImage;
	private int absoluteFaceSize;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				initializeOpenCVDependencies();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	private void initializeOpenCVDependencies() {

		try {
			// Copy the resource into a temp file so OpenCV can load it
			InputStream is = getResources().openRawResource(
					R.raw.haarcascade_frontalface_alt);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			File mCascadeFile = new File(cascadeDir,
					"haarcascade_frontalface_alt.xml");
			FileOutputStream os = new FileOutputStream(mCascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			InputStream ise = getResources().openRawResource(
					R.raw.haarcascade_eye_tree_eyeglasses);
			File cascadeDirE = getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFileE = new File(cascadeDirE,
					"haarcascade_eye_tree_eyeglasses.xml");
			FileOutputStream ose = new FileOutputStream(cascadeFileE);

			byte[] bufferER = new byte[4096];
			int bytesReadER;
			while ((bytesReadER = ise.read(bufferER)) != -1) {
				ose.write(bufferER, 0, bytesReadER);
			}
			ise.close();
			ose.close();
			// Load the cascade classifier
			cascadeClassifier = new CascadeClassifier(
					mCascadeFile.getAbsolutePath());
			cascadeClassifiereye = new CascadeClassifier(
					cascadeFileE.getAbsolutePath());
			if (cascadeClassifier.empty() || cascadeClassifiereye.empty()) {
				Log.e(TAG, "Failed to load cascade classifier");
				cascadeClassifiereye = null;
				cascadeClassifier = null;
			} else
				Log.i(TAG,
						"Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

		} catch (Exception e) {
			Log.e("OpenCVActivity", "Error loading cascade", e);
		}

		// And we are ready to go
		openCvCameraView.enableView();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_main);

		Button button1 = (Button) findViewById(R.id.button1);
		Button button2 = (Button) findViewById(R.id.button2);
		Button button3 = (Button) findViewById(R.id.button3);
		Button button4 = (Button) findViewById(R.id.button4);
		
		position = (TextView) findViewById(R.id.textView1);
		mediaplayer = MediaPlayer.create(this, R.drawable.sach);
		maskk1 = BitmapFactory.decodeResource(getResources(), R.drawable.s8);
		finalTime = mediaplayer.getDuration();
		openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		openCvCameraView.setCvCameraViewListener(this);
		button1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mediaplayer.start();

				handler.postDelayed(updateit, 100);
			}
		});
		button2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mediaplayer.pause();
				isplaying = false;
			}
		});
		button3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mediaplayer.stop();
				isplaying = false;
			}
		});

		button4.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				swapCamera();
			}
		});
	}
	private void swapCamera() {
	    mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
	    openCvCameraView.disableView();
	    openCvCameraView.setCameraIndex(mCameraId);
	    openCvCameraView.enableView();
	}

	private Runnable updateit = new Runnable() {
		public void run() {
			timeElapsed = mediaplayer.getCurrentPosition();
			double timeRemaining = finalTime - timeElapsed;
			double minutesdone = TimeUnit.MILLISECONDS
					.toMinutes((long) timeElapsed);
			double secondsdone = TimeUnit.MILLISECONDS
					.toSeconds((long) timeElapsed)
					- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
							.toMinutes((long) timeElapsed));
			if (minutesdone == 0 && secondsdone >= checker
					&& secondsdone <= checker + 10) {
				isplaying = true;
			} else
				isplaying = false;
			position.setText(String.format(
					"%d min, %d sec",

					TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed),
					TimeUnit.MILLISECONDS.toSeconds((long) timeElapsed)
							- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
									.toMinutes((long) timeElapsed))));
			handler.postDelayed(this, 100);
		}
	};

	public void onPause() {
		super.onPause();
		if (openCvCameraView != null)
			openCvCameraView.disableView();
	}

	public void onDestroy() {
		super.onDestroy();
		openCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// Create a grayscale image
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}

		if (isplaying) {
			currentpos = mediaplayer.getCurrentPosition();
			TimeUnit.MILLISECONDS.toSeconds((int) currentpos);

			Mat doit, result, dst, tempd;
			result = new Mat();
			tempd = new Mat();
			Rect roi;
			doit = new Mat(maskk1.getHeight(), maskk1.getWidth(),
					CvType.CV_8UC3);
			Utils.bitmapToMat(maskk1, tempd);


			doit = tempd.clone();

			MatOfRect faces = new MatOfRect();

			// Use the classifier to detect faces
			if (cascadeClassifier != null) {
				cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2,
						new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
						new Size());
			}

			Rect[] facesArray = faces.toArray();
			if (facesArray.length > 0) {
				facefound = true;
			}

			if (facefound == true) {

				for (int i = 0; i < facesArray.length; i++) {
					doit = tempd.clone();
					Core.rectangle(mRgba, facesArray[i].tl(),
							facesArray[i].br(), FACE_RECT_COLOR, 3);

					roi = new Rect((int) facesArray[i].tl().x,
							(int) facesArray[i].tl().y,
							(int) facesArray[i].width,
							(int) facesArray[i].height);
					result = mRgba.submat(roi);
					Imgproc.resize(doit, doit, roi.size());

					dst = doit.clone();
					Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2HSV);
					Core.inRange(dst, new Scalar(0, 0, 0), new Scalar(0, 0, 0),
							dst);
					Imgproc.cvtColor(dst, doit, Imgproc.COLOR_GRAY2BGRA);

					Core.bitwise_and(result, doit, result);

					mRgba.copyTo(result);

				}
			}
		}
		facefound = false;
		return mRgba;
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
				mLoaderCallback);
	}

}