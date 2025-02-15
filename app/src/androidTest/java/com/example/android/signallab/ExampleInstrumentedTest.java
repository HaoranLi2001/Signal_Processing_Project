package com.example.android.signallab;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;
import android.content.Context;
import static org.junit.Assert.*;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Use ApplicationProvider to get the application context
        Context appContext = ApplicationProvider.getApplicationContext();


        assertEquals("com.example.android.signallab", appContext.getPackageName());
    }
}
