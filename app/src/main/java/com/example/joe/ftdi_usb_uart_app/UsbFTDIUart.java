package com.example.joe.ftdi_usb_uart_app;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by joe on 2015/9/26.
 */
public class UsbFTDIUart {

    private static final String TAG = UsbFTDIUart.class.getSimpleName();
    private Context mContext;
    private  int mBaudRate;

    private static final byte LATENCY_TIMER = 32;

    private final AtomicReference<FT_Device> ftDevRef = new AtomicReference<>();

    public UsbFTDIUart(Context context) {
        mContext = context;
    }

    public boolean isOpened()
    {
        FT_Device ftDev = ftDevRef.get();
        if (ftDev != null && ftDev.isOpen()) {
            //throw new IOException("Device is opened.");
            return true;
        }
        return false;
    }
    private static final int FTDI_DEVICE_VENDOR_ID = 0x0403;
    private static boolean hasFTDIdevice(Context context) {
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
    public void open(int baudRate) throws IOException {
    //public boolean open(int baudRate){
        if (isOpened()) {
            throw new IOException("Device is opened.");
        }
        if( !hasFTDIdevice(mContext) ){
            throw new IOException("NO find FTDI DEVICE in usbDevice");
        }

        mBaudRate = baudRate;
        D2xxManager ftD2xx = null;
        try {
            ftD2xx = D2xxManager.getInstance(mContext);
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG, "open: " + ex.toString());
        }

        if (ftD2xx == null) {
            throw new IOException("Unable to retrieve D2xxManager instance.");
           // Log.e(TAG,"Open: get fd list false");
           // return false;
        }

        int DevCount = ftD2xx.createDeviceInfoList(mContext);
        Log.d(TAG, "Found " + DevCount + " ftdi devices.");
        if (DevCount < 1) {
            throw new IOException("No Devices found");
           // Log.e(TAG,"Open: no device list find");
           // return false;
        }

        FT_Device ftDev = null;
        try {
            // FIXME: The NPE is coming from the library. Investigate if it's
            // possible to fix there.
            ftDev = ftD2xx.openByIndex(mContext, 0);
        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (ftDev == null) {
                throw new IOException("No Devices found");
                //Log.e(TAG,"Open: no device find");
                //return false;
            }
        }

        Log.d(TAG, "Opening using Baud rate " + mBaudRate);
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(mBaudRate);
        ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1,
                D2xxManager.FT_PARITY_NONE);
        ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x00, (byte) 0x00);
        ftDev.setLatencyTimer(LATENCY_TIMER);
        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));

        if (!ftDev.isOpen()) {
            throw new IOException("Unable to open usb device connection.");
            //Log.e(TAG,"Open: Unable to open usb device connection");
           // return false;
        } else {
            Log.d(TAG, "COM open");
        }

        ftDevRef.set(ftDev);
        //return true;
    }

    public int read(byte[] readData) throws IOException {
        final FT_Device ftDev = ftDevRef.get();
        if (ftDev == null || !ftDev.isOpen()) {
            throw new IOException("Device is unavailable.");
        }

        int iavailable = ftDev.getQueueStatus();
        if (iavailable > 0) {
            if (iavailable > 4096)
                iavailable = 4096;
            try {
                ftDev.read(readData, iavailable);

            } catch (NullPointerException e) {
                final String errorMsg = "Error Reading: " + e.getMessage()
                        + "\nAssuming inaccessible USB device.  Closing connection.";
                Log.e(TAG, errorMsg, e);
                throw new IOException(errorMsg, e);
            }
        }

        if (iavailable == 0) {
            iavailable = -1;
        }
        return iavailable;
    }

    public void write(byte[] buffer) {
        final FT_Device ftDev = ftDevRef.get();
        if (ftDev != null && ftDev.isOpen()) {
            try {
                ftDev.write(buffer);
            } catch (Exception e) {
                Log.e(TAG, "Error Sending: " + e.getMessage(), e);
            }
        }
    }

    public void close() throws IOException {
        final FT_Device ftDev = ftDevRef.getAndSet(null);
        if (ftDev != null) {
            try {
                ftDev.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    public String toString() {
        return TAG;
    }
}
