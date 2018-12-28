// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.ajeetkumar.textdetectionusingmlkit.face_detection;

import android.media.audiofx.DynamicsProcessing;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ajeetkumar.textdetectionusingmlkit.others.FrameMetadata;
import com.ajeetkumar.textdetectionusingmlkit.others.GraphicOverlay;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Color;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.custom.model.*;
import com.google.firebase.ml.custom.*;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import android.content.res.AssetManager;
//import com.ajeetkumar.textdetectionusingmlkit.others.VisionProcessorBase;

/**
 * Processor for the face recognition demo.
 */
public class FaceRecognitionProcessor {

	private static final String TAG = "TextRecProc";

	private final FirebaseVisionFaceDetector detector;
	private FirebaseModelInterpreter firebaseInterpreter = null;
	private FirebaseModelInputOutputOptions inputOutputOptions = null;

	private TensorFlowInferenceInterface genderIinferenceInterface;
	private TensorFlowInferenceInterface ageInferenceInterface;
	private float[] floatValues = new float[64 * 64 * 3];
	private int[] intValues = new int[64*64];

	String detectedGender = "N/A";
	float detectedAge = -1;

	// Whether we should ignore process(). This is usually caused by feeding input data faster than
	// the model can handle.
	private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

	public FaceRecognitionProcessor(AssetManager assetManager) {
		this.genderIinferenceInterface = new TensorFlowInferenceInterface(assetManager, "gender_model.pb");
		this.ageInferenceInterface = new TensorFlowInferenceInterface(assetManager, "age_model.pb");

		detector = FirebaseVision.getInstance().getVisionFaceDetector();
	}

	//region ----- Exposed Methods -----


	public void stop() {
		try {
			detector.close();
		} catch (IOException e) {
			Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
		}
	}


	public void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) throws FirebaseMLException {

		if (shouldThrottle.get()) {
			return;
		}
		FirebaseVisionImageMetadata metadata =
				new FirebaseVisionImageMetadata.Builder()
						.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
						.setWidth(frameMetadata.getWidth())
						.setHeight(frameMetadata.getHeight())
						.setRotation(frameMetadata.getRotation())
						.build();

		detectInVisionImage(FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay, data);
	}

	//endregion

	//region ----- Helper Methods -----

	protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
		return detector.detectInImage(image);
	}

	protected void onSuccess( @NonNull List<FirebaseVisionFace> results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay, @NonNull Bitmap bitmap) {

		graphicOverlay.clear();

		for (FirebaseVisionFace result : results) {
			// crop face bitmap
			Rect rect = result.getBoundingBox();
			// TODO: fix issues when rect is not fully within image
			Bitmap resultBmp = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
			// process bitmap here
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(resultBmp, 64, 64, false);

			scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
			for (int i = 0; i < intValues.length; ++i) {
				final int val = intValues[i];
				floatValues[i * 3 + 0] = (((val >> 16) & 0xFF));
				floatValues[i * 3 + 1] = (((val >> 8) & 0xFF));
				floatValues[i * 3 + 2] = ((val & 0xFF));
			}

			float[] outputs = new float[1];
			String inputName = "input_2";
			String outputName = "pred/mul_33";

			/*** Run Gender Detection Model First ***/

			// Copy the input data into TensorFlow.
			genderIinferenceInterface.feed(inputName, floatValues, 1, 64, 64, 3);

			// Run the inference call.
			genderIinferenceInterface.run(new String[]{outputName});

			// Copy the output Tensor back into the output array.
			genderIinferenceInterface.fetch(outputName, outputs);

			if(outputs[0] < 0.5) {
				detectedGender = "female";
			} else {
				detectedGender = "male";
			}

			/*** Run Age Detection Model ***/
			outputs = new float[1];
			inputName = "input_1";
			outputName = "pred_a/mul_33";

			/*** Run Gender Detection Model First ***/

			// Copy the input data into TensorFlow.
			ageInferenceInterface.feed(inputName, floatValues, 1, 64, 64, 3);

			// Run the inference call.
			ageInferenceInterface.run(new String[]{outputName});

			// Copy the output Tensor back into the output array.
			ageInferenceInterface.fetch(outputName, outputs);

			detectedAge = outputs[0];

			// add graphic overlay
			GraphicOverlay.Graphic faceGraphic = new FaceGraphic(graphicOverlay, result, scaledBitmap, detectedGender, detectedAge);
			graphicOverlay.add(faceGraphic);
		}
	}

	protected void onFailure(@NonNull Exception e) {
		Log.w(TAG, "Face detection failed." + e);
	}

	private void detectInVisionImage( FirebaseVisionImage image, final FrameMetadata metadata, final GraphicOverlay graphicOverlay, final ByteBuffer data) {
		final Bitmap bm = image.getBitmapForDebugging();
		detectInImage(image)
				.addOnSuccessListener(
						new OnSuccessListener<List<FirebaseVisionFace>>() {
							@Override
							public void onSuccess(List<FirebaseVisionFace> results) {
								shouldThrottle.set(false);
								com.ajeetkumar.textdetectionusingmlkit.face_detection.FaceRecognitionProcessor.this.onSuccess(results, metadata, graphicOverlay, bm);
							}
						})
				.addOnFailureListener(
						new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								shouldThrottle.set(false);
								com.ajeetkumar.textdetectionusingmlkit.face_detection.FaceRecognitionProcessor.this.onFailure(e);
							}
						});
		// Begin throttling until this frame of input has been processed, either in onSuccess or
		// onFailure.
		shouldThrottle.set(true);
	}

	//endregion


}
