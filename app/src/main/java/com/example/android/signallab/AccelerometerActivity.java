package com.example.android.signallab;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    GraphView graph;
    TextView xVal, yVal, zVal;
    SensorManager sensorManager;
    Sensor accelerometer;
    LineGraphSeries<DataPoint> seriesX, seriesY, seriesZ;
    Button startButton;
    boolean collectValues = false;
    int counter; // X-axis


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);

        // Graph X-axis counter initialization so that the graph starts at zero
        counter = 0;

        // Initialize button and set a listener to activate measurement upon clicking on the button
        startButton = findViewById(R.id.startButton);
        // Simply sets a boolean to true, to start collect values from sensor.
        // The sensor always listens, but won't do anything unless collectValues = true in our case
        startButton.setOnClickListener(view -> {
            if(!collectValues) {
                collectValues = true;
                startButton.setText("Stop measuring");
            } else{
                collectValues = false;
                startButton.setText("Start measuring");
            }
        });

        // Views mapping, connecting variables to the layout
        graph = findViewById(R.id.graph);
        xVal = findViewById(R.id.xValueView);
        yVal = findViewById(R.id.yValueView);
        zVal = findViewById(R.id.zValueView);

        // Initializing the sensor manager with an accelerometer, and registering a listener.
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);


        // Setting up initialized data points for each series of data
        // (x-values, y-values and z-values) with their respective colors
        seriesX = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesX.setColor(Color.RED);
        graph.addSeries(seriesX);

        seriesY = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesY.setColor(Color.GREEN);
        graph.addSeries(seriesY);

        seriesZ = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesZ.setColor(Color.BLUE);
        graph.addSeries(seriesZ);

    }
    double[] grativity = {0, 0, 0};
    double[] linear_acceleration = {0, 0, 0};

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(collectValues) {
            double x, y, z;

            // Move along X-axis
            counter++;

            /*TODO do stuff with x,y,z values*/
            final double alpha = 0.9;
            grativity[0] = alpha * grativity[0] + (1 - alpha) * event.values[0];
            grativity[1] = alpha * grativity[1] + (1 - alpha) * event.values[1];
            grativity[2] = alpha * grativity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - grativity[0];
            linear_acceleration[1] = event.values[1] - grativity[1];
            linear_acceleration[2] = event.values[2] - grativity[2];

            // Get sensor data
            x = linear_acceleration[0];
            y = linear_acceleration[1];
            z = linear_acceleration[2];

            // Update the text view with sensor data
            xVal.setText(String.valueOf(x));
            yVal.setText(String.valueOf(y));
            zVal.setText(String.valueOf(z));

            // Add x,y and z data to series
            seriesX.appendData(new DataPoint(counter, x), true, 30, false);
            seriesY.appendData(new DataPoint(counter, y), true, 30, false);
            seriesZ.appendData(new DataPoint(counter, z), true, 30, false);

            // Add series to graph for display of data
            if (counter % 10 == 0){
             graph.addSeries(seriesX);
             graph.addSeries(seriesY);
             graph.addSeries(seriesZ);}

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Auto-generated method.
    }
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    protected void onResume() {
        // register listener again
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }
}