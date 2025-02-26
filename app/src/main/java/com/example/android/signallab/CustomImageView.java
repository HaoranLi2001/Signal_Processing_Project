package com.example.android.signallab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class CustomImageView extends ImageView implements Runnable {
    private Paint paint;
    private List<Obstacle> obstacles;
    private boolean isRunning;
    private Thread gameThread;
    private Bitmap bitmap;
    private Canvas canvas;
    private float obstacleSpeed = 15f; // 移动速度
    private float screenWidth, screenHeight;
    private float obstacleWidth = 300;
    private float obstacleSpacing = 600; // 每隔100像素生成一个障碍物
    private long startTime;
    private int obstacleIndex = 0; // 控制 sin() 变化的索引

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        obstacles = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 当视图大小变化时，初始化 bitmap 和 canvas
        screenWidth = w;
        screenHeight = h;

        // 延迟初始化，确保尺寸已知
        if (bitmap == null || canvas == null) {
            bitmap = Bitmap.createBitmap((int) screenWidth, (int) screenHeight, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            setImageBitmap(bitmap); // 将 Bitmap 设置为 ImageView 的内容
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        initObstacles();
        isRunning = true;
        startTime = System.currentTimeMillis();
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void initObstacles() {
        obstacles.clear();
        float baseHeight = screenHeight / 2 - 500;  // 初始障碍物高度
        obstacleIndex = 0;  // 重置障碍物索引

        // 根据正弦函数生成障碍物
        for (float x = screenWidth; x < screenWidth + 500; x += obstacleSpacing) {
            // 使用正弦函数动态调整障碍物高度
            float heightOffset = (float) (300 * Math.sin(obstacleIndex * Math.PI / 5)); // 变化量
            float newHeight = baseHeight + heightOffset;  // 计算新的障碍物高度

            // 上半部分障碍物的生成，Y 坐标为 0，紧贴屏幕顶部
            obstacles.add(new Obstacle(x, 0, obstacleWidth, newHeight));

            // 下半部分障碍物的生成，Y 坐标为屏幕底部，紧贴屏幕底部
            obstacles.add(new Obstacle(x, screenHeight - newHeight, obstacleWidth, newHeight));

            obstacleIndex++;  // 增加索引，确保每次变化
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            updateObstacles();
            drawObstacles();
            try {
                Thread.sleep(30); // 控制帧率
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.x -= obstacleSpeed;

            // 超出左侧屏幕，移除并在右侧生成新障碍物
            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(i);
                addNewObstacle();
            }
        }
    }

    private void addNewObstacle() {
        obstacles.clear();
        float baseHeight = screenHeight / 2 - 500;  // 初始障碍物高度
        obstacleIndex = 0;  // 重置障碍物索引

        // 根据正弦函数生成障碍物
        for (float x = screenWidth; x < screenWidth + 500; x += obstacleSpacing) {
            // 使用正弦函数动态调整障碍物高度
            float heightOffset = (float) (300 * Math.sin(obstacleIndex * Math.PI / 4)); // 变化量
            float newHeight = baseHeight + heightOffset;  // 计算新的障碍物高度

            // 上半部分障碍物的生成，Y 坐标为 0，紧贴屏幕顶部
            obstacles.add(new Obstacle(x, 0, obstacleWidth, newHeight));

            // 下半部分障碍物的生成，Y 坐标为屏幕底部，紧贴屏幕底部
            obstacles.add(new Obstacle(x, screenHeight - newHeight, obstacleWidth, newHeight));

            obstacleIndex++;  // 增加索引，确保每次变化
        }
    }

    private void drawObstacles() {
        // 确保 bitmap 和 canvas 已初始化
        if (canvas != null) {
            // 清除画布内容，设置为透明背景
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // 绘制障碍物
            for (Obstacle obstacle : obstacles) {
                canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);
            }

            // 更新 ImageView 显示内容
            postInvalidate(); // 刷新 ImageView
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 定义障碍物类
    private static class Obstacle {
        float x, y, width, height;

        public Obstacle(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
