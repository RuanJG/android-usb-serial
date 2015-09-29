package com.example.joe.ftdi_usb_uart_app;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;

/**
 * Created by joe on 2015/9/26.
 */
public class UsbConnection {

    private static final String TAG = UsbConnection.class.getSimpleName();
    private Context mContext;
    private Handler mHandler;
    private final AtomicReference<Handler> mHandlerLock = new AtomicReference<>();
    public final int MSG_DEBUG_ID =0;
    public final int MSG_DATA_AVAILABLE_ID =1;
    public final int MSG_CONNECT_BROKEN_ID =2;
    private static final int FTDI_DEVICE_VENDOR_ID = 0x0403;
    private   int mBaudRate;
    private UsbConnectionImpl mUsbConnection;
    private final LinkedBlockingQueue<byte[]> mPacketsToSend = new LinkedBlockingQueue<>();
    private Thread mMainThread;
    private AtomicBoolean mThreadConnected = new AtomicBoolean(false);;
    private static final int READ_BUFFER_SIZE = 4096;



    public UsbConnection(Context parentContext,Handler handler ) {
        mContext = parentContext;
        mHandler = handler;
        mHandlerLock.set(mHandler);
        mBaudRate = 115200;
        mThreadConnected.set(false);
    }





    protected void closeThread()
    {
        mThreadConnected.set(false);
        if (mMainThread != null && mMainThread.isAlive() && !mMainThread.isInterrupted()) {
            mMainThread.interrupt();
        }
    }

    protected void closeConnection() throws IOException {
        if (mUsbConnection != null ) {
            mUsbConnection.closeUsbConnection();
        }
    }

    protected void openConnection() throws IOException {
        if (mUsbConnection != null) {
            try {
                mUsbConnection.openUsbConnection();
                debugMsg(TAG + "Reusing previous usb connection.");
                return;
            } catch (IOException e) {
                debugMsg(TAG + "Previous usb connection is not usable:" + e.getMessage());
                mUsbConnection = null;
                mThreadConnected.set(false);
            }
        }

        if (isFTDIdevice(mContext)) {
            final UsbConnectionImpl tmp = new UsbFTDIConnection(mContext, this, mBaudRate);
            try {
                tmp.openUsbConnection();

                // If the call above is successful, 'mUsbConnection' will be set.
                mUsbConnection = tmp;
                debugMsg(TAG + "Using FTDI usb connection.");
            } catch (IOException e) {
                debugMsg(TAG+ "Unable to open a ftdi usb connection. Falling back to the open "
                        + "usb-library."+ e.getMessage());
                mThreadConnected.set(false);
            }
        }

        // Fallback
        if (mUsbConnection == null) {
            final UsbConnectionImpl tmp = new UsbCDCConnection(mContext, this, mBaudRate);

            // If an error happens here, let it propagate up the call chain since this is the fallback.
            tmp.openUsbConnection();
            mUsbConnection = tmp;
            debugMsg(TAG+ "Using open-source usb connection.");
        }
    }

    private static boolean isFTDIdevice(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList == null || deviceList.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, UsbDevice> device : deviceList.entrySet()) {
            if (device.getValue().getVendorId() == FTDI_DEVICE_VENDOR_ID) {
                return true;
            }
        }
        return false;
    }

    protected int readDataBlock(byte[] buffer) throws IOException {
        if (mUsbConnection == null) {
            throw new IOException("Uninitialized usb connection.");
        }

        return mUsbConnection.readDataBlock(buffer);
    }

    protected void sendBuffer(byte[] buffer) throws IOException {
        if (mUsbConnection == null) {
            throw new IOException("Uninitialized usb connection.");
        }

        mUsbConnection.sendBuffer(buffer);
    }

    protected void onConnectionOpened() {
        /*
        if (mConnectionStatus.compareAndSet(MAVLINK_CONNECTING, MAVLINK_CONNECTED)) {
            mLogger.logInfo(TAG, "Starting manager thread.");
            mTaskThread = new Thread(mManagerTask, "MavLinkConnection-Manager Thread");
            mTaskThread.start();
        }*/
        mThreadConnected.set(true);
        mMainThread = new Thread(mMainTask,"usbConnection-Manager Thread");
        mMainThread.start();
        debugMsg(TAG+ " establish connection " );
    }

    protected void onConnectionFailed(String errMsg) {
        /*
        mLogger.logInfo(TAG, "Unable to establish connection: " + errMsg);
        reportComError(errMsg);
        disconnect();
        */
        closeThread();
        mUsbConnection = null;
        reportConnectBroken(TAG + " Unable to establish connection: " + errMsg);
        //debugMsg(TAG+ " Unable to establish connection: " + errMsg);
    }

    /*
    @Override
    public int getConnectionType() {
        return MavLinkConnectionTypes.MAVLINK_CONNECTION_USB;
    }*/

    @Override
    public String toString() {
        if (mUsbConnection == null) {
            return TAG;
        }

        return mUsbConnection.toString();
    }

    static abstract class UsbConnectionImpl {
        protected final int mBaudRate;
        protected final Context mContext;
        private final UsbConnection parentConnection;
        //protected final Logger mLogger = AndroidLogger.getLogger();

        protected UsbConnectionImpl(Context context, UsbConnection parentConn, int baudRate) {
            mContext = context;
            this.parentConnection = parentConn;
            mBaudRate = baudRate;
        }

        protected void onUsbConnectionOpened(){
            parentConnection.onConnectionOpened();
        }

        protected void onUsbConnectionFailed(String errMsg){
            parentConnection.onConnectionFailed(errMsg);
        }

        protected abstract void closeUsbConnection() throws IOException;

        protected abstract void openUsbConnection() throws IOException;

        protected abstract int readDataBlock(byte[] readData) throws IOException;

        protected abstract void sendBuffer(byte[] buffer);
    }







    public boolean isOpened()
    {
        if( mUsbConnection != null && mThreadConnected.get())
            return true;
        return false;
    }
    public void write(byte []buffer)
    {
        if (!mPacketsToSend.offer(buffer)) {
            debugMsg(TAG + "Unable to send mavlink packet. Packet queue is full!");
        }
    }
    public boolean open(int baudRate)
    {
        mBaudRate = baudRate;
        try {
            openConnection();
            return isOpened();
        }catch (IOException e ){
            debugMsg("Openusb failed:" + e.getMessage());
        }
        return false;
    }
    public void close(){
        //close thread
        closeThread();
        //close usb
        try{
            closeConnection();
        }catch (IOException e ){
            debugMsg("close usb failed:" + e.getMessage());
        }
    }
    private void debugMsg(String msg)
    {
        Handler handler = mHandlerLock.get();
        Message mesg = new Message();
        Bundle bun = new Bundle();
        bun.putString("data",msg+"\n");
        mesg.setData(bun);
        mesg.what = MSG_DEBUG_ID;
        handler.sendMessage(mesg);
    }
    private void reportDataRecived(byte[] readData, int bufferSize)
    {
        Handler handler = mHandlerLock.get();
        Message mesg = new Message();
        Bundle bun = new Bundle();
        bun.putByteArray("data",readData);
        bun.putInt("size", bufferSize);
        mesg.setData(bun);
        mesg.what = MSG_DATA_AVAILABLE_ID;
        handler.sendMessage(mesg);
    }
    private void reportConnectBroken(String msg)
    {
        Handler handler = mHandlerLock.get();
        Message mesg = new Message();
        Bundle bun = new Bundle();
        bun.putString("data", msg + "\n");
        mesg.setData(bun);
        mesg.what = MSG_CONNECT_BROKEN_ID;
        handler.sendMessage(mesg);
    }

    private final Runnable mSendingTask = new Runnable() {
        @Override
        public void run() {
        try {
            while (mThreadConnected.get()) {
                byte[] buffer = mPacketsToSend.take();
                debugMsg("get a send package");
                try {
                    sendBuffer(buffer);
                } catch (IOException e) {
                    debugMsg(TAG + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            debugMsg(TAG + e.getMessage());
        } finally {
            debugMsg(TAG + "send thread quit");
        }
    }
    };

    private final Runnable mMainTask = new Runnable() {

    @Override
    public void run() {
        Thread sendingThread = null;
        //Thread loggingThread = null;
        try {
            //final long connectionTime = System.currentTimeMillis();
            //mConnectionTime.set(connectionTime);
            //reportConnect(connectionTime);

            // Launch the 'Sending' thread
            debugMsg(TAG+ "Starting sender thread.");
            sendingThread = new Thread(mSendingTask, "usbConnection-Sending Thread");
            sendingThread.start();

            //Launch the 'Logging' thread
            //mLogger.logInfo(TAG, "Starting logging thread.");
            //loggingThread = new Thread(mLoggingTask, "MavLinkConnection-Logging Thread");
            //loggingThread.start();

            final Parser parser = new Parser();
            parser.stats.mavlinkResetStats();

            final byte[] readBuffer = new byte[READ_BUFFER_SIZE];

            while (mThreadConnected.get()) {
                int bufferSize = readDataBlock(readBuffer);
                handleData(parser, bufferSize, readBuffer);
            }
        } catch (IOException e) {
            // Ignore errors while shutting down
            if (mThreadConnected.get()) {
                debugMsg(TAG+ e.getMessage());
            }
        } finally {
            if (sendingThread != null && sendingThread.isAlive()) {
                sendingThread.interrupt();
            }
            // if (loggingThread != null && loggingThread.isAlive()) {
            //   loggingThread.interrupt();
            // }

            debugMsg(TAG + "Exiting main thread.");
        }
    }



    private void handleData(Parser parser, int bufferSize, byte[] buffer) {
        if (bufferSize < 1) {
            return;
        }
        reportDataRecived(buffer,bufferSize);
            /*
            for (int i = 0; i < bufferSize; i++) {
                MAVLinkPacket receivedPacket = parser.mavlink_parse_char(buffer[i] & 0x00ff);
                if (receivedPacket != null) {
                    queueToLog(receivedPacket);
                    reportReceivedPacket(receivedPacket);
                }
            }
            */
    }
};












}



