package com.example.android.signallab;

import static android.media.AudioFormat.*;

import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.os.Bundle;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.widget.Button;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Environment;

/*
* AudioManager provides access to the volume and ringer
* AudioRecord "pulls" or reads data into an array with the read() method.
* AudioTrack manages and plays a single audio resource for Java applications.
* It allows streaming of PCM audio buffers to the audio sink for playback.
* This is achieved by "pushing" the data to the AudioTrack object using one of the write() methods
 */
public class RecorderActivity extends AppCompatActivity {
    private boolean isRecording = false;
    private AudioManager am = null;
    private AudioRecord record = null;
    private AudioTrack track = null;

    byte[] inAudioBuffer;
    byte[] outAudioBuffer;
    private byte[] workBuffer;
    private static final String DataFile = "BufferData128.txt"; //Name of the file to which the data is exported
    private static final int bufferSize = 1024;
    private static final int workBufferSize = bufferSize*10;
    private int bufferPosition;

    private int sign = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workBuffer = new byte[workBufferSize];
        bufferPosition = 0;
        setContentView(R.layout.activity_recorder);
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
        /* Initializing AudioRecord and AudioTrack */
        initializeRecordAndTrack();

        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(true);


        /*
        * Start a new thread to run recordAndPlay to not interfere with UI-thread.
        */
        (new Thread(this::recordAndPlay)).start();

        Button startRecording = findViewById(R.id.record);
        startRecording.setOnClickListener(view -> {
            if (!isRecording)
                startRecordingAndPlaying();
        });
        Button stopRecording = findViewById(R.id.stop);
        stopRecording.setOnClickListener(view -> {
            if (isRecording)
            {
                stopRecordingAndPlaying();
            }
        });

    }
    /*
    * Records audio with read(), into inAudioBuffer, with a maximum length of 1024 byte-values.
    * Then plays back the audio with write, from audioBuffer with a length of how many bytes
    * were read into the array.
    * num = the number of bytes read into the audioBuffer, zero, or an error code
     */
    private void recordAndPlay()
    {
        inAudioBuffer = new byte[bufferSize];
        outAudioBuffer= new byte [bufferSize];
        int num;

        AudioProcessingThread audioProcessingThread = null;
        double coeff = 1;

        boolean runAudioProcessing = false;
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        while (true)
        {
            if (isRecording)
            {
                /*Read from microphone and put in buffer*/
                num = record.read(inAudioBuffer, 0,bufferSize);

                /*Change volume by scaling audio sample values*/
                double expectVolume = 1;
               double volume=expectVolume/coeff;
                for (int i = 0; i < inAudioBuffer.length; i+=2) {
                    /*Read one audio sample from the in buffer*/
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.put(inAudioBuffer[i]);
                    bb.put(inAudioBuffer[i+1]);
                    short audioSample = bb.getShort(0);

            	    /*TODO do stuff with audio sample values */
                    /*e.g. scale sample value*/
                    if (sign == 0){
                        audioSample = (short) (audioSample * volume);
                        sign = 1;
                    } else{
                        audioSample = (short) (audioSample * volume * (1));
                        sign = 0;
                    }

                    /*Put scaled audio sample in out buffer*/
                    bb.putShort(0,audioSample);
                    outAudioBuffer[i]=bb.get(0);
                    outAudioBuffer[i + 1] = (byte) (bb.get(1));
                }
                /*To bypass - Copy audio data from inAudioBuffer to outAudioBuffer*/
                 //  outAudioBuffer = inAudioBuffer;

                /*Write from out buffer to speaker*/
                track.write(outAudioBuffer, 0, num);

                if (audioProcessingThread == null)//if audio processing is not running then...
                {
                    //copy data to workBuffer
                    //check that workBuffer is not overrun
                    if (bufferPosition + num > workBuffer.length - 1) {
                        java.lang.System.arraycopy(inAudioBuffer, 0, workBuffer, bufferPosition, workBuffer.length - bufferPosition - 1);
                        //start the audio processing now!!
                        runAudioProcessing = true;
                    } else {
                        java.lang.System.arraycopy(inAudioBuffer, 0, workBuffer, bufferPosition, num);
                        //check if the buffer is full
                        if (bufferPosition + num >= workBuffer.length - 1) {
                            //start the audio processing now!!
                            runAudioProcessing = true;
                        }

                    }
                    //should not update buffer position if the buffer was reset to zero
                    if (runAudioProcessing) {
                        bufferPosition = 0;
                        audioProcessingThread = new AudioProcessingThread(workBuffer, DataFile);
                        audioProcessingThread.start();

                    } else {
                        bufferPosition += num;
                    }

                    runAudioProcessing = false;

                } else if (!audioProcessingThread.isAlive()) {
                    //Audio processing is completed
                    coeff = audioProcessingThread.getCoeff();

                    audioProcessingThread = null;
                }
            }
        }
    }
    /*
    * Starts recording audio and plays the the audio that has been written to the AudioTrack
     */
    private void startRecordingAndPlaying()
    {
        record.startRecording();
        track.play();
        isRecording = true;

    }
    /*
    * Stops recording and pauses the audio being played
     */
    private void stopRecordingAndPlaying()
    {
        record.stop();
        track.pause();
        isRecording = false;
    }

    /*
    * Initializes the AudioRecord and AudioTrack.
    * Uses the sampleRate specified before, Format in Mono. Encoding in 16 bits.
    * getMinBufferSize returns the minimum buffer size required for the successful creation of an
    * AudioRecord object, in byte units.
     */
    private void initializeRecordAndTrack(){

        int sampleRateHz = 8000;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateHz,
                CHANNEL_IN_MONO, ENCODING_PCM_16BIT);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateHz,
                CHANNEL_IN_MONO, ENCODING_PCM_16BIT, minBufferSize);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRateHz)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();

        int maxJitter = AudioTrack.getMinBufferSize(sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        track = new AudioTrack(attributes, format, maxJitter,
                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }



    }

    class AudioProcessingThread extends Thread {
        double coeff;
       private final byte[] workBuffer;
       private final String DataFile;

       public AudioProcessingThread(byte[] workBuffer, String DataFile) {
           this.workBuffer = workBuffer;
           this.DataFile = DataFile;
            coeff = 0;
        }
        @Override
        public void run() {
            // Do some calculations on audio samples in workBuffer
            // coeff can be set based on these calculations
            int sum = 0;

            for (int i = 0; i < workBuffer.length; i+=2) {
                /*Read one audio sample from the in buffer*/
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(workBuffer[i]);
                bb.put(workBuffer[i+1]);
                short sample = bb.getShort(0);

                sum += sample;
            }
            coeff = (double) 2 * sum / workBuffer.length;

            saveToFile(DataFile, workBuffer);
        }
        private void saveToFile(String fileName, byte[] data) {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                return;
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), fileName);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public double getCoeff() {
            return coeff;
        }
    }

