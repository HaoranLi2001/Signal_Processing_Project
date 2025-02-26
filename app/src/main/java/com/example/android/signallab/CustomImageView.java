package com.example.android.signallab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class CustomImageView extends ImageView implements Runnable {
    private Paint paint;
    private List<Obstacle> obstacles;
    private boolean isRunning;
    private boolean gameOver; // New flag to track if the game is over
    private Thread gameThread;
    private Bitmap bitmap;
    private Canvas canvas;
    private float obstacleSpeed = 15f; // 移动速度
    private float screenWidth, screenHeight;
    private float obstacleWidth = 300;
    private float obstacleSpacing = 600; // 每隔100像素生成一个障碍物
    private long startTime;
    private int obstacleIndex = 0; // 控制 sin() 变化的索引
    private VideoActivity videoActivity;

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        obstacles = new ArrayList<>();
        gameOver = false; // Initialize gameOver flag as false
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

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

        for (float x = screenWidth; x < screenWidth + 500; x += obstacleSpacing) {
            float heightOffset = (float) (300 * Math.sin(obstacleIndex * Math.PI / 5)); // 变化量
            float newHeight = baseHeight + heightOffset;  // 计算新的障碍物高度

            obstacles.add(new Obstacle(x, 0, obstacleWidth, newHeight));
            obstacles.add(new Obstacle(x, screenHeight - newHeight, obstacleWidth, newHeight));

            obstacleIndex++;  // 增加索引，确保每次变化
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!gameOver) { // Only update the game if it's not over
                updateObstacles();
                drawObstacles();
            }
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

            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(i);
                addNewObstacle();
            }
        }
    }

    private void addNewObstacle() {
        obstacles.clear();
        float baseHeight = screenHeight / 2 - 500;  // 初始障碍物高度
        obstacleIndex = 0;

        for (float x = screenWidth; x < screenWidth + 500; x += obstacleSpacing) {
            float heightOffset = (float) (300 * Math.sin(obstacleIndex * Math.PI / 4));
            float newHeight = baseHeight + heightOffset;

            obstacles.add(new Obstacle(x, 0, obstacleWidth, newHeight));
            obstacles.add(new Obstacle(x, screenHeight - newHeight, obstacleWidth, newHeight));

            obstacleIndex++;
        }
    }

    public List<Obstacle> getVisibleObstacles() {
        List<Obstacle> visibleObstacles = new ArrayList<>();
        for (Obstacle obstacle : obstacles) {
            if (obstacle.x + obstacle.width > 0 && obstacle.x < screenWidth &&
                    obstacle.y + obstacle.height > 0 && obstacle.y < screenHeight) {
                visibleObstacles.add(obstacle);
            }
        }
        return visibleObstacles;
    }

    public boolean detectCollision(List<Obstacle> visibleObstacles) {
        float middleX = 0;
        float middleY = 0;
        PointF leftEye = videoActivity.getLeftEyePos();
        PointF rightEye = videoActivity.getRightEyePos();

        if (leftEye != null && rightEye != null) {
            middleX = (leftEye.x + rightEye.x) / 2;
            middleY = (leftEye.y + rightEye.y) / 2;
        }

        for (Obstacle obstacle : visibleObstacles) {
            if (middleX >= obstacle.x && middleX <= obstacle.x + obstacle.width &&
                    middleY >= obstacle.y && middleY <= obstacle.y + obstacle.height) {
                gameOver = true; // Set game over flag when collision occurs
                return true;
            }
        }
        return false;
    }

    private void drawObstacles() {
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (Obstacle obstacle : obstacles) {
                canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);
            }
            postInvalidate(); // 刷新 ImageView
        }
    }

    public void restartGame() {
        gameOver = false; // Reset game over flag
        obstacles.clear(); // Clear obstacles
        initObstacles(); // Re-initialize obstacles
        startTime = System.currentTimeMillis(); // Reset game time
        gameThread = new Thread(this); // Restart game thread
        gameThread.start();
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
