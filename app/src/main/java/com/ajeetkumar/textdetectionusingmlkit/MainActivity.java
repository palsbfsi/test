package com.ajeetkumar.textdetectionusingmlkit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.ajeetkumar.textdetectionusingmlkit.camera.CameraSource;
import com.ajeetkumar.textdetectionusingmlkit.camera.CameraSourcePreview;
import com.ajeetkumar.textdetectionusingmlkit.others.GraphicOverlay;
import com.ajeetkumar.textdetectionusingmlkit.face_detection.FaceRecognitionProcessor;

import com.google.firebase.FirebaseApp;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

	//region ----- Instance Variables -----

	private CameraSource cameraSource = null;
	private CameraSourcePreview preview;
	private GraphicOverlay graphicOverlay;
	private FloatingActionButton fab;
	private static String TAG = MainActivity.class.getSimpleName().toString().trim();

	//endregion

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fab = (FloatingActionButton)findViewById(R.id.fab);

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d(TAG, "CLICK");
				flipCameraSource();
			}
		});


		preview = (CameraSourcePreview) findViewById(R.id.camera_source_preview);
		if (preview == null) {
			Log.d(TAG, "Preview is null");
		}
		graphicOverlay = (GraphicOverlay) findViewById(R.id.graphics_overlay);
		if (graphicOverlay == null) {
			Log.d(TAG, "graphicOverlay is null");
		}

		createCameraSource();
		startCameraSource();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		startCameraSource();
	}

	/** Stops the camera. */
	@Override
	protected void onPause() {
		super.onPause();
		preview.stop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (cameraSource != null) {
			cameraSource.release();
		}
	}

	private void createCameraSource() {
		createCameraSource(false);
	}

	private void createCameraSource(boolean frontFacingCamera) {

		if (cameraSource != null) {
			onPause();
			cameraSource.release();
		}

		cameraSource = new CameraSource(this, graphicOverlay);
		cameraSource.setFacing(frontFacingCamera ? CameraSource.CAMERA_FACING_FRONT : CameraSource.CAMERA_FACING_BACK);

		cameraSource.setMachineLearningFrameProcessor(new FaceRecognitionProcessor(getAssets()));
	}

	private void flipCameraSource() {
		Log.d(TAG, "updating facing");
		createCameraSource(cameraSource.getCameraFacing() != CameraSource.CAMERA_FACING_FRONT);
		startCameraSource();
	}

	private void startCameraSource() {
		if (cameraSource != null) {
			try {
				if (preview == null) {
					Log.d(TAG, "resume: Preview is null");
				}
				if (graphicOverlay == null) {
					Log.d(TAG, "resume: graphOverlay is null");
				}
				preview.start(cameraSource, graphicOverlay);
			} catch (IOException e) {
				Log.e(TAG, "Unable to start camera source.", e);
				cameraSource.release();
				cameraSource = null;
			}
		}
	}
}
