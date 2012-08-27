package com.seeedstudio.android.beecon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.seeedstudio.android.ir.IRparser;
import com.seeedstudio.android.ir.Utility;
import com.seeedstudio.bluetooth.BluetoothChatService;
import com.seeedstudio.bluetooth.DeviceListActivity;

public class MainActivity extends Activity {

    // debugging
    private static final boolean D = Utility.DEBUG;
    private static final String TAG = "MainActivity";

    // **************************************************//
    // static
    // **************************************************//
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private static final long VIBRATION_LENGTH = 45;

    // frequency. normally, is 36~40 mHz, eq 0x24~0x28
    // 38 ---> 0x26 ...
    private static byte[] FREQUENCY36 = new byte[] { 0x24 };
    private static byte[] FREQUENCY37 = new byte[] { 0x25 };
    private static byte[] FREQUENCY38 = new byte[] { 0x26 };
    private static byte[] FREQUENCY39 = new byte[] { 0x27 };
    private static byte[] FREQUENCY40 = new byte[] { 0x28 };

    // **************************************************//
    // field number
    // **************************************************//

    private Button bt, study, send, homePage, irLight;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    // IR protocol parser
    private IRparser parser = null;

    private static byte[] temp = null;

    private Vibrator mVibrator;
    private boolean mDoVibrate = true;

    private int frequencyFlag = 2;

    // **************************************************//
    // activity lifecycle and setup UI
    // **************************************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (D)
            Utility.logging(TAG, "onCreate");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (D)
            Utility.logging(TAG, "onStart");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null)
                setupUI();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (D)
            Utility.logging(TAG, "onResume");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity
        // returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop the Bluetooth chat services
        if (mChatService != null)
            mChatService.stop();
        if (D)
            Utility.logging(TAG, "onDestroy");
    }

    private void setupUI() {
        bt = (Button) findViewById(R.id.connect_bluetooth);
        study = (Button) findViewById(R.id.study_button);
        send = (Button) findViewById(R.id.send_button);
        homePage = (Button) findViewById(R.id.seeed_home_button);
        irLight = (Button) findViewById(R.id.ir_light);

        // initialize vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
        // init parser
        parser = new IRparser();

        bt.setOnClickListener(new ClickEvent());
        study.setOnClickListener(new ClickEvent());
        // send.setOnClickListener(new ClickEvent());
        send.setOnTouchListener(new TouchEvent());
        homePage.setOnClickListener(new ClickEvent());
        irLight.setOnTouchListener(new TouchEvent());

    }

    private class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.connect_bluetooth:
                // reading to connect
                Intent serverIntent = new Intent(MainActivity.this,
                        DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
            case R.id.study_button:
                // send study command and waiting
                sendMessage(IRparser.getHEADER());
                sendMessage(IRparser.getSEND_TRAIL());
                Utility.toastShort(getApplicationContext(),
                        getString(R.string.study));
                break;
            case R.id.send_button:
                // TODO
                sendCommand();
                break;
            case R.id.seeed_home_button:
                // opem the seeed studio home page
                String url = "http://www.seeedstudio.com";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                break;
            default:
                break;
            }
        }

    }

    private class TouchEvent implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                // Virbrate
                if (mVibrator.hasVibrator()) {
                    if (D)
                        Utility.logging(TAG, "Virbrating");
                    mVibrator.vibrate(VIBRATION_LENGTH);
                }

                switch (v.getId()) {
                case R.id.send_button:
                    if (temp != null)
                        // irLight.setBackgroundResource(R.drawable.beeconir_light);
                        setIRLightBackground(frequencyFlag, true);
                    break;

                default:
                    break;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {

                switch (v.getId()) {
                case R.id.connect_bluetooth:
                    // reading to connect
                    Intent serverIntent = new Intent(MainActivity.this,
                            DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    break;
                case R.id.study_button:
                    // send study command and waiting
                    sendMessage(IRparser.getHEADER());
                    sendMessage(IRparser.getSEND_TRAIL());

                    Utility.toastShort(getApplicationContext(),
                            getString(R.string.study));
                    break;
                case R.id.send_button:
                    // TODO
                    sendCommand();
                    // irLight.setBackgroundResource(R.drawable.beeconir);
                    setIRLightBackground(frequencyFlag, false);
                    v.playSoundEffect(AudioManager.FX_KEY_CLICK);
                    break;
                case R.id.seeed_home_button:
                    // opem the seeed studio home page
                    String url = "http://www.seeedstudio.com";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    break;
                case R.id.ir_light:
                    changeFrequency();
                    break;
                default:
                    break;
                }
            }
            return false;
        }

    }

    private void changeFrequency() {
        frequencyFlag++;
        if (5 == frequencyFlag) {
            frequencyFlag = 0;
        }

        switch (frequencyFlag) {
        case 0:
            irLight.setBackgroundResource(R.drawable.dark39);
            parser.setFRQ(FREQUENCY36);
            Utility.toastShort(getApplicationContext(),
                    getString(R.string.change_to_36));
            break;
        case 1:
            irLight.setBackgroundResource(R.drawable.dark40);
            parser.setFRQ(FREQUENCY37);
            Utility.toastShort(getApplicationContext(),
                    getString(R.string.change_to_37));
            break;
        case 2:
            irLight.setBackgroundResource(R.drawable.beeconir);
            parser.setFRQ(FREQUENCY38);
            Utility.toastShort(getApplicationContext(),
                    getString(R.string.change_to_38));
            break;
        case 3:
            irLight.setBackgroundResource(R.drawable.dark39);
            parser.setFRQ(FREQUENCY39);
            Utility.toastShort(getApplicationContext(),
                    getString(R.string.change_to_39));
            break;
        case 4:
            irLight.setBackgroundResource(R.drawable.dark40);
            parser.setFRQ(FREQUENCY40);
            Utility.toastShort(getApplicationContext(),
                    getString(R.string.change_to_40));
            break;
        default:
            break;
        }

        // setIRLightBackground(frequencyFlag, false);
    }

    private void setIRLightBackground(int flag, boolean light) {
        // if (parser == null) {
        // Utility.toastShort(getApplicationContext(),
        // getString(R.string.change_to_38));
        // }

        if (light) {
            switch (flag) {
            case 0:
                irLight.setBackgroundResource(R.drawable.light39);
                break;
            case 1:
                irLight.setBackgroundResource(R.drawable.light40);
                break;
            case 2:
                irLight.setBackgroundResource(R.drawable.beeconir_light);
                break;
            case 3:
                irLight.setBackgroundResource(R.drawable.light39);
                break;
            case 4:
                irLight.setBackgroundResource(R.drawable.light40);
                break;
            default:
                break;
            }
        } else {
            switch (flag) {
            case 0:
                irLight.setBackgroundResource(R.drawable.dark39);
                break;
            case 1:
                irLight.setBackgroundResource(R.drawable.dark40);
                break;
            case 2:
                irLight.setBackgroundResource(R.drawable.beeconir);
                break;
            case 3:
                irLight.setBackgroundResource(R.drawable.dark39);
                break;
            case 4:
                irLight.setBackgroundResource(R.drawable.dark40);
                break;
            default:
                break;
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // **************************************************//
    // handle the connect
    // **************************************************//

    /**
     * Sends a message.
     * 
     * @param message
     *            A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    /**
     * Sends a message.
     * 
     * @param message
     *            A byte array of text to send.
     */
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            mChatService.write(message);
        }
    }

    private void sendCommand() {
        if (D)
            Utility.logging(TAG, "sendCommand()");

        if (temp != null) {
            sendMessage(parser.encoder(parser.getSaveData()));
        }

        byte[] data = null;
        if (parser.toParser(parser.toReady())) {
            if (D)
                Utility.logging(TAG, "sendCommand() to parser");

            data = parser.encoder(parser.getSaveData());
            temp = data;
            sendMessage(parser.encoder(temp));
        }
    }

    private void checkCommand() {
        if (parser.isAvailable()) {
            temp = parser.encoder(parser.getSaveData());
            send.setBackgroundResource(R.drawable.send_sel);
            Utility.toastShort(getApplicationContext(), "Got it");
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if (D)
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    Utility.toastShort(getApplicationContext(),
                            getString(R.string.connected)
                                    + mConnectedDeviceName);
                    bt.setBackgroundResource(R.drawable.bt_pressed);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    Utility.toastShort(getApplicationContext(),
                            getString(R.string.connecting));
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // TODO change the IR light
                if (D)
                    Utility.logging(TAG, "send data: " + writeBuf.toString());
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // TODO get data and Toast
                parser.add(readBuf);
                checkCommand();
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Utility.toastShort(getApplicationContext(),
                        getString(R.string.connected) + mConnectedDeviceName);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(),
                        msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                        .show();
                break;
            }
        }

    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(
                        DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter
                        .getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupUI();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
