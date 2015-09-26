package com.example.joe.ftdi_usb_uart_app;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements View.OnClickListener {
    final private  String TAG=MainActivityFragment.class.getName();
    private EditText console;
    private EditText inputText;
    private Button button;
    private UsbFTDIUart ftdiUart ;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        doSetupUart();
        console = (EditText) this.getActivity().findViewById(R.id.console);
        inputText = (EditText) this.getActivity().findViewById(R.id.inputText);
        button = (Button) this.getActivity().findViewById(R.id.sendbutton);
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
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }
    @Override
    public void onDetach() {
        super.onDetach();
        doReleaseUart();
    }

    void debugMsg(String msg){
        if(inputText!= null){
            inputText.append(msg+"\n");
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
        ftdiUart = new UsbFTDIUart(this.getActivity().getApplicationContext());
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
}
