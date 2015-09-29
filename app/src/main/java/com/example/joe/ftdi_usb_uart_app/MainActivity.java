package com.example.joe.ftdi_usb_uart_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;


public class MainActivity extends Activity {
    private EditText console;
    private EditText inputText;
    private Button button;
    private UsbFTDIUart ftdiUart ;
    private UsbConnection mUsbConnection;
    private Thread sendThread;
    private Thread listenThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doSetupUsb();
        console = (EditText) this.findViewById(R.id.console);
        inputText = (EditText) this.findViewById(R.id.inputText);
        button = (Button) this.findViewById(R.id.sendbutton);
        if( button != null ) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if( mUsbConnection != null && mUsbConnection.isOpened()) {
                        String msg=inputText.getText().toString()+"\r\n";
                        mUsbConnection.write(msg.getBytes());
                    }else{
                        debugMsg("reinit usb");
                        if( mUsbConnection == null) doSetupUsb();
                        mUsbConnection.open(115200);
                    }

                }
            });
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUsbConnection.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }












    void debugMsg(String msg){
        if(console!= null){
            console.append(msg+"\n");
        }
    }

    void doSetupUsb()
    {
        mUsbConnection = new UsbConnection(this.getApplicationContext(),mHandler);
    }
    private Handler mHandler = new Handler(){
        byte[] readData;
        int len;
        public void handleMessage(Message msg) {
            if (console == null) {
                return;
            }
            if (msg.what == mUsbConnection.MSG_DATA_AVAILABLE_ID) {
                readData = msg.getData().getByteArray("data");
                len = msg.getData().getInt("size");
                String msgd="";
                String str1="";
                for( int i =0 ;i < len; i++) {
                    msgd = msgd + Integer.toString(readData[i]) + ",";
                    str1 = str1+ Character.toString((char)readData[i]);
                }
                console.append("readData =" + str1);
            } else if (msg.what == mUsbConnection.MSG_DEBUG_ID){
                console.append(msg.getData().getString("data"));
            }else if( msg.what == mUsbConnection.MSG_CONNECT_BROKEN_ID){
                console.append(msg.getData().getString("data"));
            }

            super.handleMessage(msg);
        }
    };

}
