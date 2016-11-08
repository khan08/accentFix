package com.example.kang.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class TcpClient  {

    private static final String TAG = TcpClient.class.getSimpleName();

    private Socket socket;
    private DataOutputStream out;
    private byte[] data;

    public TcpClient(byte[] data)
    {
        this.data = data;
        socket = null;
        out = null;
    }


    public void connectandSend(Context context, String host, int port)
    {
        if (socket==null || !socket.isConnected()) {
            new ConnectTask(context).execute(host, String.valueOf(port));
        }
    }

    public class ConnectTask extends AsyncTask<String, Void, Void> {

        private Context context;

        public ConnectTask(Context context) {
            this.context = context;
        }
        @Override


        protected void onPreExecute() {
            showToast(context, "Connecting..");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (socket.isConnected()) {
                showToast(context, "Connection successfull");
            }
            send(data);
            send("quit");
            super.onPostExecute(result);
        }

        private String host;
        private int port;

        @Override
        protected Void doInBackground(String... params) {
            try {
                String host = params[0];
                int port = Integer.parseInt(params[1]);
                socket = new Socket(host, port);
                out = new DataOutputStream(socket.getOutputStream());
            } catch (UnknownHostException e) {
                showToast(context, "Don't know about host: " + host + ":" + port);
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                showToast(context, "Couldn't get I/O for the connection to: " + host + ":" + port);
                Log.e(TAG, e.getMessage());
            }
            return null;
        }


    }

    public void disconnect(Context context)
    {
        if ( socket.isConnected() )
        {
            try {
                out.close();
                socket.close();
            } catch (IOException e) {
                showToast(context, "Couldn't get I/O for the connection");
                Log.e(TAG, e.getMessage());
            }
        }
    }


    /**
     * Send command to a Pure Data audio engine.
     */
    public void send(byte[] data)
    {
        if ( socket.isConnected() ) try {
            //out.writeInt(data.length);
            out.write(data,0,data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String command)
    {
        try {
            byte[] byteCommand=command.getBytes("UTF-8");
            if ( socket.isConnected() ) {
                out.write(byteCommand);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(final Context context, final String message) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
