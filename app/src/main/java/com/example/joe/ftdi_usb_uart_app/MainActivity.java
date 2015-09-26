package com.example.joe.ftdi_usb_uart_app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doSetupUart();
        console = (EditText) this.findViewById(R.id.console);
        inputText = (EditText) this.findViewById(R.id.inputText);
        button = (Button) this.findViewById(R.id.sendbutton);
        if( button != null ) {
            //button.setOnClickListener(this);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    debugMsg("onclick");

                    if (ftdiUart != null && !ftdiUart.isOpened())
                        doOpenUart();
                    else
                        debugMsg("ftdiuart not init");
                    if (ftdiUart.isOpened())
                        doSendUartString();
                    else
                        debugMsg("ftdiuart not open ok");
                }
            });
        }
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

    void doReleaseUart()
    {
        try {
            if (ftdiUart.isOpened()) {
                doStopListenUart();
                ftdiUart.close();
            }
        }catch (IOException e) {
            debugMsg("OnDetach Close uart error!!");
            e.printStackTrace();
        }
        ftdiUart = null;
    }
    void doSetupUart()
    {
        ftdiUart = new UsbFTDIUart(this.getApplicationContext());
    }
    boolean doOpenUart()
    {
        boolean success = false;
        try{
            ftdiUart.open(115200);
            debugMsg("open uart ok");
            success = true;
        }catch (IOException e){
            success = false;
            debugMsg( "OnAttach open uart error!!");
            debugMsg(e.toString());
            e.printStackTrace();
        }
        if( success )
            doListenUart();
        return success;
    }

    public  void doReadUart()
    {
        byte[] readData=new byte[4096];
        try{
            ftdiUart.read(readData);
        }catch (IOException e){
            debugMsg("Read uart error!!");
            e.printStackTrace();
        }
        if( readData.length == 0)
            return;
        String msg = readData.toString();
        Message mesg = new Message();
        Bundle bun = new Bundle();
        bun.putString("data",msg);
        mesg.setData(bun);
        mHandler.sendMessage(mesg);
    }

    Runnable uartReciveRunner = new Runnable() {
        @Override
        public void run() {
            while (listenThreadStart){
                doReadUart();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            debugMsg("tcpreciveRunner exit");
        }
    };
    Runnable uartSendRunner = new Runnable() {
        @Override
        public void run() {
            String msg=inputText.getText().toString()+"\r\n";
            if( !ftdiUart.isOpened())
                return;
            ftdiUart.write(msg.getBytes());
            debugMsg("uartsendRunner exit");
        }
    };

    private void doSendUartString()
    {
        new Thread(uartSendRunner).start();
    }
    boolean listenThreadStart = false;
    private void doListenUart()
    {
        listenThreadStart = true;
        new Thread(uartReciveRunner).start();
    }
    private void doStopListenUart()
    {
        listenThreadStart = false;
    }


    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            if( console == null) {
                return;
            }

            console.append(msg.getData().getString("data"));

            super.handleMessage(msg);
        }
    };
/*
    @Override
    public void onClick(View view) {
        debugMsg("onclick");

        if (ftdiUart != null && !ftdiUart.isOpened())
            doOpenUart();
        else
            debugMsg("ftdiuart not init");
        if (ftdiUart.isOpened())
            doSendUartString();
        else
            debugMsg("ftdiuart not open ok");
    }*/
}
