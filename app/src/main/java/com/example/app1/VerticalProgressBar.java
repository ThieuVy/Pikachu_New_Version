package com.example.app1;

import android.annotation.SuppressLint; // QUAN TRỌNG: Phải có dòng import này
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

public class VerticalProgressBar extends ProgressBar {

    // --- Các hàm khởi tạo (không đổi) ---
    public VerticalProgressBar(Context context) {
        super(context);
        init();
    }

    public VerticalProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public VerticalProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    /**
     * Dòng @SuppressLint này sẽ TẮT CẢNH BÁO MÀU VÀNG bạn thấy ở dòng 48.
     * Nó báo cho Android Studio biết rằng việc tráo đổi tham số bên dưới
     * là hành động có chủ đích.
     */
    @SuppressLint("SuspiciousNameCombination")
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Dòng 48 bạn hỏi đây. Nó đang làm đúng nhiệm vụ là tráo đổi
        // height và width để ProgressBar có thể hiển thị dọc.
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);

        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected synchronized void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }
}