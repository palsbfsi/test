package com.ajeetkumar.textdetectionusingmlkit;

import android.Manifest;
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

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

	//region ----- Instance Variables -----

	private CameraSource cameraSource = null;
	private CameraSourcePreview preview;
	private GraphicOverlay graphicOverlay;
	private FloatingActionButton fab;
	private static String TAG = MainActivity.class.getSimpleName().toString().trim();
	private final int MY_PERMISSIONS_REQUEST_CAMERA = 100;

	//endregion

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fab = (FloatingActionButton)findViewById(R.id.fab);

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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

		// request permissions
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					MY_PERMISSIONS_REQUEST_CAMERA);
		} else {
			createCameraSource();
			startCameraSource();
		}
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

		cameraSource.setMachineLearningFrameProcessor(new FaceRecognitionProcessor(getAssets(), frontFacingCamera));
	}

	private void flipCameraSource() {
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

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_CAMERA: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// permission was granted, yay!

					createCameraSource();
					startCameraSource();

				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request.
		}
	}
}
