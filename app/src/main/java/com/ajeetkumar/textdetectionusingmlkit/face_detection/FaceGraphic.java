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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap;
import com.ajeetkumar.textdetectionusingmlkit.others.GraphicOverlay;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import java.text.DecimalFormat;
import java.math.RoundingMode;

/**
 * Graphic instance for rendering face bound box
 * overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {

  private static final int FACE_COLOR = Color.YELLOW;
  private static final int TEXT_COLOR = Color.CYAN;
  private static final float STROKE_WIDTH = 7.0f;
  private static final float TEXT_STROKE_WIDTH = 6.0f;

  private final Paint rectPaint;
  private final Paint textPaint;
  private final FirebaseVisionFace face;
  private final Bitmap bitmap;
  private final String gender;
  private final float age;
  private static final float TEXT_SIZE = 40.0f;
  private boolean frontFacingCamera;

  private DecimalFormat df;

  FaceGraphic(GraphicOverlay overlay, FirebaseVisionFace face, Bitmap bitmap, String gender, float age, boolean frontFacingCamera) {
    super(overlay);

    df = new DecimalFormat("#."); // 1 decimal place
    df.setRoundingMode(RoundingMode.CEILING);

    this.gender = gender;
    this.bitmap = bitmap;
    this.face = face;
    this.age = age;
    this.frontFacingCamera = frontFacingCamera;

    rectPaint = new Paint();
    rectPaint.setColor(FACE_COLOR);
    rectPaint.setStyle(Paint.Style.STROKE);
    rectPaint.setStrokeWidth(STROKE_WIDTH);

    textPaint = new Paint();
    textPaint.setColor(TEXT_COLOR);
    textPaint.setTextSize(TEXT_SIZE);
    textPaint.setStrokeWidth(TEXT_STROKE_WIDTH);

    // Redraw the overlay, as this graphic has been added.
    postInvalidate();
  }

  /** Draws the text block annotations for position, size, and raw value on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    if (face == null) {
      throw new IllegalStateException("Attempting to draw a null face.");
    }

    // Draws the bounding box around the face.
    RectF rect = new RectF(face.getBoundingBox());
    rect.left = translateX(rect.left);
    rect.top = translateY(rect.top);
    rect.right = translateX(rect.right);
    rect.bottom = translateY(rect.bottom);
    canvas.drawRect(rect, rectPaint);

    // For debugging
    // canvas.drawBitmap(this.bitmap, this.bitmap.getScaledWidth(canvas), this.bitmap.getScaledHeight(canvas), rectPaint);

    if(frontFacingCamera) {
      canvas.drawText(df.format(age) + " " + gender, rect.right, rect.top, textPaint);
    } else {
      canvas.drawText(df.format(age) + " " + gender, rect.left, rect.top, textPaint);
    }
  }
}
