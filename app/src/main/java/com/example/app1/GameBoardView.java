package com.example.app1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class GameBoardView extends View {

    // --- Dữ liệu từ Controller ---
    private int[][] board;
    private Bitmap[] pokemonImages;
    private Point selectedTile;
    private List<Point> path;

    // --- Các đối tượng vẽ ---
    private Paint selectionPaint;
    private Paint linePaint;
    private float tileWidth;
    private float tileHeight;
    private Paint tileBackgroundPaint;
    private Paint borderPaint;

    // --- Listener để giao tiếp với Activity ---
    private OnTileClickListener onTileClickListener;

    public interface OnTileClickListener {
        void onTileClick(int row, int col);
    }

    public void setOnTileClickListener(OnTileClickListener listener) {
        this.onTileClickListener = listener;
    }

    // --- Các hàm khởi tạo ---
    public GameBoardView(Context context) { super(context); init(null); }
    public GameBoardView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(attrs); }
    public GameBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(attrs); }

    private void init(@Nullable AttributeSet set) {
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.RED);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(5);

        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(8);
        tileBackgroundPaint = new Paint();
        tileBackgroundPaint.setColor(Color.parseColor("#FFFBEB")); // Màu be nhạt
        tileBackgroundPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#4CAF50")); // Màu xanh lá cây
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
    }

    // --- Các hàm setter để nhận dữ liệu ---
    public void setBoard(int[][] board) { this.board = board; }
    public void setPokemonImages(Bitmap[] images) { this.pokemonImages = images; }
    public void setSelectedTile(Point tile) { this.selectedTile = tile; invalidate(); }
    public void drawPath(List<Point> path) { this.path = path; invalidate(); }

    public void clearPathAndSelection() {
        this.path = null;
        this.selectedTile = null;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (board != null) {
            // Trừ 2 vì board có viền ảo
            tileWidth = (float) w / (board[0].length - 2);
            tileHeight = (float) h / (board.length - 2);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (board == null || pokemonImages == null) return;

        // Vẽ các ô Pokemon
        for (int i = 1; i < board.length - 1; i++) {
            for (int j = 1; j < board[0].length - 1; j++) {
                int pokemonId = board[i][j];

                // CHỈ VẼ NỀN, VIỀN VÀ HÌNH ẢNH NẾU Ô ĐÓ CÓ POKEMON
                if (pokemonId != 0) {
                    float left = (j - 1) * tileWidth;
                    float top = (i - 1) * tileHeight;
                    @SuppressLint("DrawAllocation") RectF tileRect = new RectF(left, top, left + tileWidth, top + tileHeight);

                    // 1. Vẽ nền và viền
                    float cornerRadius = 8f;
                    canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, tileBackgroundPaint);
                    canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint);

                    // 2. Vẽ hình ảnh Pokemon với một chút padding
                    float padding = 8f;
                    @SuppressLint("DrawAllocation") RectF bitmapRect = new RectF(left + padding, top + padding, left + tileWidth - padding, top + tileHeight - padding);
                    canvas.drawBitmap(pokemonImages[pokemonId], null, bitmapRect, null);
                }
            }
        }

        // Vẽ ô được chọn
        if (selectedTile != null) {
            float left = (selectedTile.x - 1) * tileWidth;
            float top = (selectedTile.y - 1) * tileHeight;
            canvas.drawRect(left, top, left + tileWidth, top + tileHeight, selectionPaint);
        }

        // Vẽ đường nối
        if (path != null && path.size() > 1) {
            // ... (code vẽ đường nối giữ nguyên)
            for (int i = 0; i < path.size() - 1; i++) {
                Point p1 = path.get(i);
                Point p2 = path.get(i + 1);
                float startX = (p1.x - 1) * tileWidth + tileWidth / 2;
                float startY = (p1.y - 1) * tileHeight + tileHeight / 2;
                float endX = (p2.x - 1) * tileWidth + tileWidth / 2;
                float endY = (p2.y - 1) * tileHeight + tileHeight / 2;
                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && onTileClickListener != null) {
            int col = (int) (event.getX() / tileWidth) + 1;
            int row = (int) (event.getY() / tileHeight) + 1;

            // Đảm bảo không click ra ngoài biên
            if (row < board.length - 1 && col < board[0].length - 1) {
                onTileClickListener.onTileClick(row, col);
            }
        }
        return true;
    }
}