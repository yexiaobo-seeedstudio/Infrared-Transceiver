/**
 * 
 */
package com.seeedstudio.android.ir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Log;

/**
 * @author Seeed Studio
 * 
 */
public class IRparser {
    // debugging
    private final static boolean D = Utility.DEBUG;
    private final static String TAG = "IRparser";

    // **************************************************** //
    // static
    // **************************************************** //

    // from terminal to phone
    // 'S10'
    private static byte[] GET_HEADER = new byte[] { 0x53, 0x31, 0x30 };
    // '_E'
    private static byte[] GET_TRAIL = new byte[] { 0x5f, 0x45 };

    // phone to terminal
    // 'S11'
    private static byte[] SEND_HEADER = new byte[] { 0x53, 0x31, 0x31 };
    // '/E'
    private static byte[] SEND_TRAIL = new byte[] { 0x2f, 0x45 };

    // frequency. normally, is 36~40 mHz, eq 0x24~0x28
    // 38 ---> 0x26
    private static byte[] FREQUENCY = new byte[] { 0x26 };

    // **************************************************** //
    // field number
    // **************************************************** //

    // temp to save the be parsed byte array
    private byte[] saveData = null;
    // alt for static
    public static byte[] tempData = null;

    // for deal with data which be added
    private ArrayList<byte[]> byteArrayList = new ArrayList<byte[]>();
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // **************************************************** //
    // Constructor and setter, getter method
    // **************************************************** //

    /**
     * 
     * Constructor for IR Parser
     * 
     */
    public IRparser() {
    }

    public static byte[] getHEADER() {
        return GET_HEADER;
    }

    public static void setHEADER(byte[] hEADER) {
        GET_HEADER = hEADER;
    }

    public static byte[] getTRAIL() {
        return GET_TRAIL;
    }

    public static void setTRAIL(byte[] tRAIL) {
        GET_TRAIL = tRAIL;
    }

    public static byte[] getSEND_HEADER() {
        return SEND_HEADER;
    }

    public static void setSEND_HEADER(byte[] sEND_HEADER) {
        SEND_HEADER = sEND_HEADER;
    }

    public static byte[] getSEND_TRAIL() {
        return SEND_TRAIL;
    }

    public static void setSEND_TRAIL(byte[] sEND_TRAIL) {
        SEND_TRAIL = sEND_TRAIL;
    }

    public byte[] getSaveData() {
        return saveData;
    }

    public void setSaveData(byte[] saveData) {
        this.saveData = saveData;
        tempData = saveData;
        if (D)
            Log.d(TAG, "save data length: " + saveData.length);
    }

    public byte[] getFRQ() {
        return FREQUENCY;
    }

    public void setFRQ(byte[] fRQ) {
        FREQUENCY = fRQ;
    }

    // **************************************************** //
    // add data to parser and ready to encoding
    // **************************************************** //
    
    /**
     * Add the data to parser
     * 
     * @param data
     *            Data will be parser, byte[]
     */
    public void add(byte[] data) {
        if (byteArrayList != null) {
            byteArrayList.add(data);
        }
    }

    /**
     * Add the data to parser
     * 
     * @param data
     *            Data will be parser, ArrayList<byte[]>
     */
    public void add(ArrayList<byte[]> data) {
        if (byteArrayList != null) {
            byteArrayList = data;
        }
    }

    /**
     * Deal with all data to be ready, whatever byte[] or ArrayList<byte[]>
     * 
     * @return
     */
    public byte[] toReady() {
        if (byteArrayList != null) {
            try {
                if (D)
                    Log.d(TAG, "reading ++++> byte array list: "
                            + byteArrayList.size());

                byte[] back = composeData(byteArrayList);
                byteArrayList.clear();

                if (D)
                    Log.d(TAG, "reading ++++> " + back.length);

                return back;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Compose all the data byte array together
     * 
     * @param dataList
     *            byte Array Data List
     * @return
     * @throws IOException
     */
    private byte[] composeData(ArrayList<byte[]> dataList) throws IOException {
        if (D)
            Log.d(TAG, "compose data ++++");

        byte[] data;
        if (dataList == null) {
            return null;
        }

        for (int i = 0; i < dataList.size(); i++) {
            baos.write(dataList.get(i));
        }

        baos.flush();
        data = baos.toByteArray();
        baos.reset();

        if (D)
            Log.d(TAG, "compose data length ++++> " + data.length);

        return data;
    }

    // **************************************************** //
    // decoder
    // **************************************************** //

    /**
     * To parser the data, split the Header and Trail
     * 
     * @param data
     * @return if ok, back true, else return false
     */
    public boolean toParser(byte[] data) {
        if (D)
            Log.d(TAG, "toParser() ++++");

        int sign = 0;
        byte[] temp = null;
//        int headerLength = GET_HEADER.length;
//        int trailLength = GET_TRAIL.length;

        // get the header and cut it down
        for (int i = 0; i < data.length - 2; i++) {
            if (data[i] == GET_HEADER[0]) {
                sign++;

                if (D)
                    Log.d(TAG, "toParser() ++++> get the first header data: "
                            + data[i]);

                if (data[i + 1] == GET_HEADER[1]
                        && data[i + 2] == GET_HEADER[2]) {
                    sign++;
                    // cut down the header
                    temp = cutHeader(data, data.length - i - 3);

                    if (D)
                        Log.d(TAG, "toParser() ++++> temp array length: "
                                + temp.length);

                    if (D)
                        Log.d(TAG, "toParser() ++++> cut down the header: "
                                + data[i] + data[i + 1] + data[i + 2]);
                }
            }
        }

        // not get the header
        if (temp == null) {
            if (D)
                Log.d(TAG, "toParser() ++++> not get the header");
            return false;
        }

        // get the trail and cut it down
        for (int i = 1; i < temp.length; i++) {
            if (D)
                Log.d(TAG, "toParser() ++++> reading to cut trail");

            if (temp[i - 1] == GET_TRAIL[0]) {
                sign++;

                if (D)
                    Log.d(TAG, "toParser() ++++> get the last trail data");

                if (temp[i] == GET_TRAIL[1]) {
                    sign++;
                    // temp = cutTrail(temp, temp.length - i - 2);
                    temp = cutTrail(temp, i - 1);

                    if (D)
                        Log.d(TAG, "toParser() ++++> cut down the trail data");
                    break;
                }
            }
        }

        // it complete to parser the package
        if (sign == 4) {
            setSaveData(temp);
            return true;
        }

        return false;
    }

    private byte[] cutHeader(byte[] data, int nBytes) {
        byte[] back;
        ByteArrayBuffer bab;

        bab = new ByteArrayBuffer(nBytes);
        bab.append(data, data.length - nBytes, nBytes);
        back = bab.toByteArray();

        return back;
    }

    private byte[] cutTrail(byte[] temp, int nBytes) {
        byte[] back;
        ByteArrayBuffer bab;

        bab = new ByteArrayBuffer(nBytes);
        bab.append(temp, 0, nBytes);
        back = bab.toByteArray();

        return back;
    }

    // **************************************************** //
    // encoder
    // **************************************************** //

    /**
     * Encoder the commander with package header and trail
     * 
     * @param data
     *            IR commander
     * @return endcoding data, byte[]
     */
    public byte[] encoder(byte[] data) {
        if (data == null) {
            return null;
        }

        byte[] back;

        ByteArrayBuffer bab = new ByteArrayBuffer(SEND_HEADER.length
                + data.length + SEND_TRAIL.length);
        bab.append(SEND_HEADER, 0, SEND_HEADER.length); // header
        bab.append(FREQUENCY, 0, FREQUENCY.length); // frequency
        bab.append(data, 0, data.length); // data
        bab.append(SEND_TRAIL, 0, SEND_TRAIL.length); // trail

        back = bab.toByteArray();
        if (D)
            Log.d(TAG, "encoder data length: " + back.length);

        for (int i = 0; i < back.length; i++) {
            if (D)
                Log.d(TAG, "encoder data[" + i + "]: " + back[i]);
        }

        return back;
    }
}
