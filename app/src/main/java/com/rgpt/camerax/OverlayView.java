package com.rgpt.camerax;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    public enum Status {
        STUDYING_OK,
        NO_FACE,
        FACE_OBSTRUCTED
    }

    private final List<Face> faces = new ArrayList<>();
    private final List<RectF> objectBoundingBoxes = new ArrayList<>();
    
    private int imageWidth = 480;
    private int imageHeight = 640;
    private boolean isFrontLens = true;
    private Status currentStatus = Status.NO_FACE;

    private final Paint facePaintOk;
    private final Paint facePaintWarning;
    private final Paint objectPaint;
    private final Paint textPaint;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        facePaintOk = new Paint();
        facePaintOk.setColor(Color.GREEN);
        facePaintOk.setStyle(Paint.Style.STROKE);
        facePaintOk.setStrokeWidth(8f);
        facePaintOk.setAntiAlias(true);

        facePaintWarning = new Paint();
        facePaintWarning.setColor(Color.RED);
        facePaintWarning.setStyle(Paint.Style.STROKE);
        facePaintWarning.setStrokeWidth(10f);
        facePaintWarning.setAntiAlias(true);

        objectPaint = new Paint();
        objectPaint.setColor(Color.YELLOW);
        objectPaint.setStyle(Paint.Style.STROKE);
        objectPaint.setStrokeWidth(6f);
        objectPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(4f, 0f, 0f, Color.BLACK);
    }

    public void updateResults(List<Face> newFaces, List<RectF> newObjects, int imgWidth, int imgHeight, boolean isFront, Status status) {
        this.faces.clear();
        if (newFaces != null) {
            this.faces.addAll(newFaces);
        }

        this.objectBoundingBoxes.clear();
        if (newObjects != null) {
            this.objectBoundingBoxes.addAll(newObjects);
        }

        if (imgWidth > 0 && imgHeight > 0) {
            this.imageWidth = imgWidth;
            this.imageHeight = imgHeight;
        }

        this.isFrontLens = isFront;
        this.currentStatus = status;

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth == 0 || viewHeight == 0 || imageWidth == 0 || imageHeight == 0) {
            return;
        }

        float scaleX = (float) viewWidth / imageWidth;
        float scaleY = (float) viewHeight / imageHeight;

        // Draw face bounding boxes
        Paint facePaint = (currentStatus == Status.STUDYING_OK) ? facePaintOk : facePaintWarning;

        for (Face face : faces) {
            Rect boundingBox = face.getBoundingBox();
            RectF mappedRect = translateRect(boundingBox, scaleX, scaleY, viewWidth);
            canvas.drawRoundRect(mappedRect, 16f, 16f, facePaint);

            String label = (currentStatus == Status.STUDYING_OK) ? "Studying (Face OK)" : "Obstructed / Warning";
            canvas.drawText(label, mappedRect.left, Math.max(mappedRect.top - 12f, 40f), textPaint);
        }

        // Draw detected objects (e.g. books / obstructions)
        for (RectF objRect : objectBoundingBoxes) {
            RectF mappedRect = translateRectF(objRect, scaleX, scaleY, viewWidth);
            canvas.drawRoundRect(mappedRect, 12f, 12f, objectPaint);
            canvas.drawText("Book / Obstruction", mappedRect.left, Math.max(mappedRect.top - 10f, 40f), textPaint);
        }
    }

    private RectF translateRect(Rect rect, float scaleX, float scaleY, int viewWidth) {
        float left = rect.left * scaleX;
        float right = rect.right * scaleX;
        float top = rect.top * scaleY;
        float bottom = rect.bottom * scaleY;

        if (isFrontLens) {
            // Mirror horizontally for front-facing camera
            float mirroredLeft = viewWidth - right;
            float mirroredRight = viewWidth - left;
            return new RectF(mirroredLeft, top, mirroredRight, bottom);
        } else {
            return new RectF(left, top, right, bottom);
        }
    }

    private RectF translateRectF(RectF rect, float scaleX, float scaleY, int viewWidth) {
        float left = rect.left * scaleX;
        float right = rect.right * scaleX;
        float top = rect.top * scaleY;
        float bottom = rect.bottom * scaleY;

        if (isFrontLens) {
            float mirroredLeft = viewWidth - right;
            float mirroredRight = viewWidth - left;
            return new RectF(mirroredLeft, top, mirroredRight, bottom);
        } else {
            return new RectF(left, top, right, bottom);
        }
    }
}
