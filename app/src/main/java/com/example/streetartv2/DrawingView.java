package com.example.streetartv2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.DiscretePathEffect;
// --- NOWY IMPORT DLA LINII PRZERYWANEJ ---
import android.graphics.DashPathEffect;

import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DrawingView extends View {

    // --- ZAKTUALIZOWANY ENUM: Usunięte 3D, dodane NEON i DASHED ---
    public enum BrushType {
        NORMAL,
        MARKER,
        SPRAY,
        CRAYON,
        NEON,
        DASHED
    }

    // --- ZAKTUALIZOWANA KLASA STROKE: Dodane pole na pędzel poświaty ---
    private static class Stroke {
        Path path;
        Paint paint;
        Paint glowPaint; // Opcjonalny pędzel do poświaty neonu

        Stroke(Path path, Paint paint, @Nullable Paint glowPaint) {
            this.path = path;
            this.paint = paint;
            this.glowPaint = glowPaint;
        }
    }

    private Paint currentBrush;
    private ArrayList<Stroke> completedStrokes = new ArrayList<>();
    private Path currentPath;
    private Bitmap backgroundBitmap;
    private BrushType currentBrushType = BrushType.NORMAL;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        currentBrush = new Paint();
        currentBrush.setAntiAlias(true);
        currentBrush.setColor(Color.RED);
        currentBrush.setStyle(Paint.Style.STROKE);
        currentBrush.setStrokeJoin(Paint.Join.ROUND);
        currentBrush.setStrokeWidth(15f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);

                Paint newPaint = new Paint(currentBrush);
                Paint newGlowPaint = null; // Domyślnie brak poświaty

                // Resetujemy wszystkie efekty przed zastosowaniem nowych
                newPaint.setMaskFilter(null);
                newPaint.setPathEffect(null);

                switch (currentBrushType) {
                    case MARKER:
                        newPaint.setAlpha(120);
                        break;
                    case SPRAY:
                        newPaint.setMaskFilter(new BlurMaskFilter(newPaint.getStrokeWidth(), BlurMaskFilter.Blur.NORMAL));
                        break;
                    case CRAYON:
                        newPaint.setPathEffect(new DiscretePathEffect(10.0f, 4.0f));
                        break;
                    case NEON:
                        // Dla neonu tworzymy dwa pędzle: zewnętrzną poświatę i wewnętrzny rdzeń
                        newGlowPaint = new Paint(currentBrush); // Kopiujemy ustawienia do pędzla poświaty
                        newGlowPaint.setMaskFilter(new BlurMaskFilter(currentBrush.getStrokeWidth(), BlurMaskFilter.Blur.NORMAL));

                        // Wewnętrzny rdzeń jest cieńszy i jaśniejszy
                        newPaint.setStrokeWidth(currentBrush.getStrokeWidth() / 3);
                        newPaint.setColor(Color.WHITE); // Rdzeń neonu jest zazwyczaj biały
                        break;
                    case DASHED:
                        // 20 pikseli kreski, 20 pikseli przerwy
                        newPaint.setPathEffect(new DashPathEffect(new float[]{20, 20}, 0));
                        break;
                    case NORMAL:
                    default:
                        break;
                }

                completedStrokes.add(new Stroke(currentPath, newPaint, newGlowPaint));
                return true;

            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                break;

            default:
                return false;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        // --- ZAKTUALIZOWANA PĘTLA RYSUJĄCA ---
        for (Stroke stroke : completedStrokes) {
            // Jeśli istnieje pędzel poświaty (dla neonu), narysuj ją pierwszą (na spodzie)
            if (stroke.glowPaint != null) {
                canvas.drawPath(stroke.path, stroke.glowPaint);
            }
            // Zawsze rysuj główną linię
            canvas.drawPath(stroke.path, stroke.paint);
        }
    }

    public void setBrushType(BrushType type) {
        currentBrushType = type;
    }

    // ... reszta metod bez zmian ...
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (backgroundBitmap != null && w > 0 && h > 0) {
            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, w, h, false);
        }
    }
    public void setBitmap(Bitmap bitmap) {
        if (getWidth() > 0 && getHeight() > 0) {
            this.backgroundBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), false);
        } else {
            this.backgroundBitmap = bitmap;
        }
        invalidate();
    }
    public void clearDrawing() {
        completedStrokes.clear();
        invalidate();
    }
    public void setBrushColor(int newColor) {
        currentBrush.setColor(newColor);
    }
    public void setBrushSize(float newSize) {
        currentBrush.setStrokeWidth(newSize);
    }
    public void undo() {
        if (!completedStrokes.isEmpty()) {
            completedStrokes.remove(completedStrokes.size() - 1);
            invalidate();
        }
    }

    public Bitmap getCanvasBitmap() {
        // Tworzymy nową, pustą bitmapę o wymiarach naszego widoku
        Bitmap outputBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        // Tworzymy płótno (Canvas), które będzie rysować na tej nowej bitmapie
        Canvas canvas = new Canvas(outputBitmap);
        // Rysujemy na tym płótnie wszystko to, co jest aktualnie widoczne (tło i linie)
        draw(canvas);
        return outputBitmap;
    }
}