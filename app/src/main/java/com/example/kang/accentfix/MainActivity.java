package com.example.kang.accentfix;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kang.server.TcpClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private AudioTrack audioTrack = null;
    private Thread recordingThread = null;
    private Thread playbackThread = null;
    private Thread webThread = null;
    private static String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.txt";
    FileOutputStream os = null;
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
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
            intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
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
        try {
            os = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                saveAudioToBuffer(os);
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
                playAudio();
            }
        }, "Playback Thread");
        playbackThread.start();
        sendData();
    }

    private void playAudio() {
        byte[] audioData = null;

        try {
            InputStream inputStream = new FileInputStream(fileName);

            int minBufferSize = AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            audioData = new byte[minBufferSize];

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,RECORDER_SAMPLERATE,AudioFormat.CHANNEL_OUT_MONO,RECORDER_AUDIO_ENCODING,minBufferSize,AudioTrack.MODE_STREAM);
            audioTrack.play();
            int i=0;

            while((i = inputStream.read(audioData)) != -1) {
                audioTrack.write(audioData,0,i);
            }
        } catch(FileNotFoundException fe) {
            Log.e("pb","File not found");
        } catch(IOException io) {
            Log.e("pb","IO Exception");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void saveAudioToBuffer(FileOutputStream os) {
        // Write the output audio in byte
        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, sData.length);
            try {
                os.write(sData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendData(){
        try {
            TcpClient tcpClient = new TcpClient(sData);
            tcpClient.connectandSend(getApplicationContext(),"192.168.1.153",8888);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
