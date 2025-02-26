package com.example.android.signallab;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Obstacle {
    private float x, y, width, height;
    private static final Paint paint = new Paint();

    public Obstacle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void move(float speed) {
        x -= speed;  // 让障碍物从右往左移动
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(x, y, x + width, y + height, paint);
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }
}
