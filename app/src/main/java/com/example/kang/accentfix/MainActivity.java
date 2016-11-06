package com.example.kang.accentfix;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private AudioTrack audioTrack = null;
    private Thread recordingThread = null;
    private Thread playbackThread = null;
    private static String fileName = null;
    private boolean isRecording = false;
    int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    byte[] sData = new byte[bufferSize / 2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setButtonHandlers();
        enableButtons(false);
    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                saveAudioToBuffer();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
        playbackThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile(sData);
            }
        }, "Playback Thread");
        playbackThread.start();

    }

    private void playAudio(byte[] sData) {
        int mBufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (mBufferSize == AudioTrack.ERROR || mBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            // For some readon we couldn't obtain a buffer size
            mBufferSize = RECORDER_SAMPLERATE  * 2;
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize, AudioTrack.MODE_STREAM);
        if (audioTrack!=null) {
            audioTrack.play();
            audioTrack.write(sData, 100, sData.length);
            //audioTrack.flush();
            //audioTrack.stop();
            //audioTrack.release();
        }
        else{
            Log.d("Audio","playback failed");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveAudioToBuffer() {
        // Write the output audio in byte
        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, sData.length);
            //audioTrack.write(sData, 0, bufferSize);
        }
    }

    private void writeAudioDataToFile(byte[] sData) {
        // Write the output audio in byte

        String filePath = getExternalCacheDir()+"voice8K16bit.txt";
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
            os.write(sData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
