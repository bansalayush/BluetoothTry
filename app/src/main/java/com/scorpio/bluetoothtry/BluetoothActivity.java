package com.scorpio.bluetoothtry;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    /* TODO: NotificationListenerService https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java */

    BluetoothDevice device,pairedDevice;
    Button bEnable, bDisable, bScanDevices;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //private ArrayAdapter mArrayAdapter = new ArrayAdapter(getApplicationContext());
    Spinner scannedDevices;
    TextView blueText;
    ArrayList<String> alScannedDevices = new ArrayList<>();
    String [] sScannedDevices;
    Context mainContext;

    private OutputStream outputStream;
    private InputStream inStream;
    String sendText = "Hello";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        mainContext = getApplicationContext();
        blueText = (TextView) findViewById(R.id.textBlue);
        scannedDevices = (Spinner) findViewById(R.id.scannedDevices);

    }

    /* button function to enable bluetooth*/
    public void enableBluetooth(View v) {
        blueText.setText("Bluetooth is ON");
        connectToBluetooth();
    }

    /* button function to disable bluetooth*/
    public void disableBluetooth(View v) {
        disconnectFromBluetooth();
        blueText.setText("Bluetooth is OFF");
    }

    /* button function to scan devices*/
    public void scanDevices(View v) {
        scanForDevices();
    }

    public  void sendData(View v){
        new ConnectThread(pairedDevice).run();


    }

    /* Enabling bluetooth on the start of app
    @Override
    protected void onStart() {
        super.onStart();
        connectToBluetooth();
    }
    */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }


    /**
     * this receiver is for scanning devices
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d("DISCOVERY_STARTED","Discovery started");
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("DISCOVERY_FINISHED","Discovery finished");
                Toast.makeText(context, "Process finished", Toast.LENGTH_SHORT).show();

                //converting Arraylist to string array //populating spinner
                int size = alScannedDevices.size();
                List<String> lScannedDevices = alScannedDevices.subList(0,size);
                ArrayAdapter<String> aaScannedDevices = new ArrayAdapter<>(mainContext,R.layout.support_simple_spinner_dropdown_item,lScannedDevices);
                scannedDevices.setAdapter(aaScannedDevices);

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                Log.d("ACTION_FOUND","Device found");
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                alScannedDevices.add(device.getName()+" "+device.getAddress());
                Toast.makeText(mainContext, "Found device " + device.getName(), Toast.LENGTH_SHORT).show();
                testCase();
            }
        }
    };

    /**
     * This reciever is for paring devices
     */
    private final BroadcastReceiver pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("Pairing device " + action);

            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if(state==BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(mainContext, "Bonded", Toast.LENGTH_LONG).show();
                    pairedDevice = device;
                }
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(mainContext,"Paired",Toast.LENGTH_SHORT).show();
                }
                //Toast to show unpairing of devices
                /*else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(mainContext,"Paired",Toast.LENGTH_SHORT);
                }*/

            }
        }
    };


    /*private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Log.d("Count of paired devices is--> " + mArrayAdapter.getCount());
            }
        }
    }*/


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                ParcelUuid uuids[] = pairedDevice.getUuids();
                System.out.println("Size of parcelUUID's " + uuids.length);
                System.out.println("UUID is " + uuids[0]+"\n"+uuids[1]+"\n"+uuids[2]+"\n"+uuids[3]+"\n"+uuids[4]+"\n"+uuids[5]);
                tmp = pairedDevice.createRfcommSocketToServiceRecord(uuids[1].getUuid());
            } catch (IOException e) {
                Log.e("Connect Thread", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("Connect Thread", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //*manageMyConnectedSocket(mmSocket);*//*
            Log.d("YIPEEEEEE","Connected");
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Connect Thread", "Could not close the client socket", e);
            }
        }
    }



    //////////////////////////////////////////////////////////////////////////////////

    /***
     * BLUETOOTH FUNCTIONS
     ***/

    private void connectToBluetooth() {

        if (bluetoothAdapter == null) {
            //does not support bluetooth connectivity
        } else {
            if (bluetoothAdapter.isEnabled() == false) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                //make the device discoverable for 300 seconds
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoverableIntent, 2);

            }
        }
    }

    private void scanForDevices() {
        if (bluetoothAdapter.isEnabled() && bluetoothAdapter != null)/*null and connectivity check*/ {
            Log.d("scanForDevices","findng devices");
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
            bluetoothAdapter.startDiscovery();
        }
    }

    public void pairDevice(BluetoothDevice device) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.EXTRA_DEVICE);
        filter.addAction(BluetoothDevice.EXTRA_PAIRING_VARIANT);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //filter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(pReceiver,filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            device.createBond();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("onActivityResult","ResultCode --> " + resultCode);
        if (resultCode == 300) {
            Log.d("onActivityResult","Bluetooth access granted");
            //scanForDevices();
            //getPairedDevices();
        } else {
            Log.d("onActivityResult","bluetooth access not granted");
            this.finish();

        }
    }

    private void disconnectFromBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }

    private void testCase()
    {
        if(device.getName().equals("Scorpio"))
        {
            bluetoothAdapter.cancelDiscovery();
            pairDevice(device);
        }
    }


    /*class BlueSync extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            BluetoothSocket socket = null;
            ParcelUuid [] uuid = pairedDevice.getUuids();
            System.out.println("Length of uuid array"+uuid.length);
            try {
                bluetoothAdapter.cancelDiscovery();
                socket = pairedDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                socket.connect();
                inStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                outputStream.write(sendText.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            ProgressDialog progress = new ProgressDialog(BluetoothActivity.this);
            progress.setMessage("Sending Text");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();

        }


    }*/
}
