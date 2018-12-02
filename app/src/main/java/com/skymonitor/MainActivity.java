package com.skymonitor;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ClipboardManager;
import android.content.ClipData;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    NodeStatusListAdapter mNodeStatusListAdapter = null;
    ListView mNodeListView = null;
    private static MonitorService mService = new MonitorService();
	String TAG = "SkyMonitor";
    private MyBroadRequestReceiver receiver;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mService = MonitorService.getMonitorService();

        if(mService == null){
        	Log.i("SkyMonitor", "Monitor Service Starts");
        	startService(new Intent(this, MonitorService.class));
        	mService = MonitorService.getMonitorService();
        }

        Log.i("SkyMonitor onCreate",(mService == null)? ("service is null") :("service is not null"));

        mNodeStatusListAdapter = new NodeStatusListAdapter(this, mService.mNodeInfoList);
        mNodeListView  = (ListView) findViewById(R.id.node_list_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mNodeListView.setAdapter(mNodeStatusListAdapter);
        Log.i(TAG,"onCreate");

        IntentFilter filter = new IntentFilter("progressStart");
        IntentFilter filter2 = new IntentFilter("progressEnd");
        receiver = new MyBroadRequestReceiver();
        registerReceiver( receiver, filter);
        registerReceiver( receiver, filter2);
    }

    public void WatchDemo(View V){
    	Intent intent = new Intent(Intent.ACTION_VIEW);
    	intent.setData(Uri.parse("https://www.youtube.com/watch?v=4AeNsOrrinM"));
    	startActivity(intent);
    }
    
    public void StartMonitor(View V){
    	Log.i(TAG,"");
    	String skyKeys = "";
    	Toast toast;
    	URL skyUrl = null;
    	EditText mInputSkykeys = (EditText) findViewById(R.id.keyText);
    	String mIskyKeys = mInputSkykeys.getText().toString();
    	if(mIskyKeys != null && mIskyKeys.compareToIgnoreCase("") > 0)
    	   skyKeys = mInputSkykeys.getText().toString();

    	String mInputInterval = "";
    	int mTimeInterval = 1;
    	EditText mInputTimeInterval = (EditText) findViewById(R.id.timeinterval);
    	mInputInterval = mInputTimeInterval.getText().toString().trim();
    	if(!mInputInterval.equalsIgnoreCase("") && mInputInterval.length() < 2 && !mInputInterval.equalsIgnoreCase("0"))
    		mTimeInterval = Integer.parseInt(mInputInterval);
    	//skyKeys =  mInputSkykeys.getText().toString();
    	if(mService == null)
    		mService = MonitorService.getMonitorService();
    	if(!mService.isOnline()){
    		toast = Toast.makeText(getApplicationContext(), "Please check your network connection and try again ", 3000);
    		toast.setGravity(Gravity.CENTER, 0, 0);
    		toast.show();
    		return;
    	}
    	if(skyKeys == null || skyKeys.equalsIgnoreCase("")){
    		toast = Toast.makeText(getApplicationContext(), "Please enter proper html link as shown in the video. ", 3000);
    		toast.setGravity(Gravity.CENTER, 0, 0);
    		toast.show();
		    return;
    	}
    	try {
			skyUrl = new URL(skyKeys);
            Log.i(TAG, "protocol = " + skyUrl.getProtocol());
            Log.i(TAG, "authority = " + skyUrl.getAuthority());
            Log.i(TAG, "host = " + skyUrl.getHost());
            Log.i(TAG, "path = " + skyUrl.getPath());
            Log.i(TAG, "query = " + skyUrl.getQuery());
        }catch(Exception e){
            toast = Toast.makeText(getApplicationContext(), "Please enter proper html link as shown in the video.. ", 3000);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
    	StringBuilder keys = new StringBuilder(skyUrl.getQuery());
    	keys.replace(0, 9, "");
    	keys.toString().replace("+","");
    	Log.i(TAG+" getNodeInfo =========  ",""+keys);
    	String[] output = keys.toString().split("%2C%0D%0A|\\%2C+");
    	mService.mNodeInfoList.clear();
    	mNodeStatusListAdapter.updateNodeInfoList(mService.mNodeInfoList);
    	if(mService != null){
    	   Log.i(TAG,"calling start monitoring");
    	   Set <String> sKeys = new HashSet<String>(Arrays.asList(output));
    	   try {
			   mService.startMonitoring(sKeys, true, mTimeInterval * 60);
		   }catch (Exception e){
			   toast = Toast.makeText(getApplicationContext(), "Please enter proper html link as shown in the demo video ", 3000);
			   toast.setGravity(Gravity.CENTER, 0, 0);
			   toast.show();
		   }
    	}else
    	   Log.i(TAG,"servie is null");
		mProgressBar.setVisibility(View.VISIBLE);
    }

    public void StopMonitor(View V){
    	Log.i(TAG+" StopMonitor ","in activity");
    	mProgressBar.setVisibility(View.INVISIBLE);
    	if(mService != null)
    	   mService.StopMonitoring();
    	else
    	   Log.i(TAG+" StopMonitor ","service is null ");
    }
    
    Handler handler = new Handler(Looper.getMainLooper());

	final Runnable r = new Runnable() {
		public void run() {
			Log.i(TAG+"Runnable ", " running ");
			if (mNodeStatusListAdapter != null){
				mNodeStatusListAdapter.updateNodeInfoList(mService.mNodeInfoList);
			}
			else
				Log.i(TAG+"Runnable", "adapter is null");

			TextView mLastMonitoredTime = (TextView) findViewById(R.id.lastMonitoredTime);
			
			mService = MonitorService.getMonitorService();
			if(mService != null){
			   mLastMonitoredTime.setText(mService.timeStamp);
			   Log.i(TAG+ " Runnable", mService.timeStamp);
			}else
				Log.i(TAG+ " Runnable", "mService is null");

			handler.postDelayed(this, 6000);
		}
	};
    
    public void BuyCoffee(View V){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Skycoin","ZkRJWndQPAcUfXw2i1LRisBmLN7QV5Dwqs" );
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(getApplicationContext(), "Thank you for support,Skycoin address has been copied to your clip board", 3000);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    protected void onResume()
    {
        super.onResume();
        mService = MonitorService.getMonitorService();

        if(mService == null){
        	Log.i("SkyMonitor onResume", "Monitor Service Starts");
        	startService(new Intent(this, MonitorService.class));
            mService = MonitorService.getMonitorService();
        }
        Log.i(TAG+" onResume",(mService == null)? ("service is null") :("service is not null"));

        if(mNodeStatusListAdapter != null){
    	    mNodeStatusListAdapter.updateNodeInfoList(mService.mNodeInfoList);
        }
    	else
    		Log.i(TAG+" onResume","adapter is null");
        
        handler.post(r);
    }
    
    public class MyBroadRequestReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
            Log.i(TAG+" MyBroadRequestReceiver", intent.getAction());
            String action = intent.getAction();
            if (action.equalsIgnoreCase("progressStart"))
                mProgressBar.setVisibility(View.VISIBLE);
            if (action.equalsIgnoreCase("progressEnd"))
                mProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}