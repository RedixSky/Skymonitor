package com.skymonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MonitorService extends Service {

    private static MonitorService mService = null;
    private ServiceHandler mServiceHandler = null;
    private static int offlineCount = 0;
    public static String timeStamp = " : : ";
    int TIME_TO_REPEAT_CHECK = 60 * 1000;
    SharedPreferences sPref = null;
    SharedPreferences.Editor ed = null;
    public static ArrayList<NodeInfo> mNodeInfoList = new ArrayList<NodeInfo>();
    LinkedHashSet<String> mNodeKeyHashSet = new LinkedHashSet<String>();
    private View mView;
    final private int STARTMONITOR = 1,MONITORINDIVIDUALKEY = 2;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;
	String TAG = "SkyMonitor Service";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.i("MonitorService", "onStartCommand");
    	//onTaskRemoved(intent); not needed yet
    	sPref = getApplicationContext().getSharedPreferences("com.skymonitor", Context.MODE_PRIVATE);
        ed = sPref.edit();
        mService = this;
        
        Set<String> skykeys = sPref.getStringSet("skykeys", null);
        int interval = sPref.getInt("interval", 1);
        //uncomment below once everything is fixed
        if(skykeys != null && skykeys.size() > 0){
           startMonitoring(skykeys, false, interval);
           Log.i(TAG+"onStartCommand ", skykeys.toString());
        }
    	return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
    	Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
    	restartServiceIntent.setPackage(getPackageName());
    	startService(restartServiceIntent);
    	super.onTaskRemoved(rootIntent);
    }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static MonitorService getMonitorService() {
		return mService;
	}

	public void onCreate(){
		Log.i(TAG, "onCreate");
		super.onCreate();
		mService = this;
		HandlerThread handlerThread = new HandlerThread("MonitorThread");
        handlerThread.start();
        mServiceHandler = new ServiceHandler(handlerThread.getLooper());
	}

	public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        Intent broadcastIntent = new Intent("com.skymonitor.MonitorService");
        sendBroadcast(broadcastIntent);
    }

	public class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
			// TODO Auto-generated constructor stub
        	super(looper);
		}

        @Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();

			switch (msg.what) {
			case STARTMONITOR:
				// String skykeys = data.getString("skykeys");
				timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()).toString();
				int interval = data.getInt("interval", 60);
				boolean isFromUser = data.getBoolean("isFromUser", false);
				if(!isOnline()){
				   showPopUp("Network connection not available : Not able to perform Skymonitoring : "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()), "networkfail");
				   mServiceHandler.removeMessages(1);
			       Message message = mServiceHandler.obtainMessage(1);
			       Bundle data1 = new Bundle();
			       data1.putBoolean("isFromUser", false);
			       data.putInt("interval", interval);
			       message.setData(data1);
			       mServiceHandler.sendMessageDelayed(message, TIME_TO_REPEAT_CHECK * interval);
				   break;
				}
				mNodeInfoList.clear();
				mNodeKeyHashSet.clear();
				Set<String> skykeys = sPref.getStringSet("skykeys",new HashSet<String>());
				int totalkeys = skykeys.size();
				Log.i(TAG+" startMonitoring", "inHandler " + skykeys +" interval "+interval);
				Iterator<String> skykeyItirator = skykeys.iterator();
				Intent broadcastIntent = new Intent();
		        broadcastIntent.setAction("progressStart");
		        sendBroadcast(broadcastIntent);
		        int numberofthiskey = 0;
				while (skykeyItirator.hasNext()) {
					String skykey = skykeyItirator.next();
					startMonitoringIndividualKey(skykey, isFromUser, totalkeys, ++numberofthiskey);
					Log.i(TAG+"startMonitoring", "inHandler " + skykey);
				}
				broadcastIntent.setAction("progressEnd");
		        sendBroadcast(broadcastIntent);
				mServiceHandler.removeMessages(1);
		        Message message = mServiceHandler.obtainMessage(1);
		        Bundle data1 = new Bundle();
		        data1.putBoolean("isFromUser", false);
		        data.putInt("interval", interval);
		        message.setData(data1);
		        mServiceHandler.sendMessageDelayed(message, TIME_TO_REPEAT_CHECK * interval);
				break;
			case MONITORINDIVIDUALKEY:
				String skykey = data.getString("skykey");
                int keytotal = data.getInt("totalkeys",0);
                int keyNumber = data.getInt("keyNumber",0);
				boolean mIsFromUser = data.getBoolean("isFromUser", false);
				Log.i(TAG+" startMonitoringIndividualkey", "inHandler " + skykey+" isFromUser "+mIsFromUser);
				DefaultHttpClient httpClient2 = new DefaultHttpClient();
				// HttpGet httpGet = new HttpGet("http://www.skycoin.net");

				String result = "";
				try {
                    HttpGet httpGet2 = new HttpGet("https://skywirenc.com/?key_list=" + skykey);
                    ResponseHandler<String> resHandler2 = new BasicResponseHandler();
					result = httpClient2.execute(httpGet2, resHandler2);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				}catch (Exception e){
                    e.printStackTrace();
                    showPopUp("Invalid Key entered : " + skykey+" : "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                            +" Number of key in the row : "+mNodeInfoList.size(), "invalidKey");
                    clearSavedKeys();
                    mServiceHandler.removeMessages(1);
                    mServiceHandler.removeMessages(2);
                    Toast.makeText(getApplicationContext(), "Please enter proper URL as shown in demo video", 3000).show();
                    return;
                }
				Log.i(TAG+" NetworkOperation =======>> ", result.toString());
				// saveHtmlFile(page);//Only write status to file

				org.jsoup.nodes.Document document2 = Jsoup.parse(result);

				for (Element elementSuccess : document2.select("span[class=badge badge-success]")) {
                    Log.i( TAG+" badge-success ", elementSuccess.toString());
                    if (elementSuccess.text().equalsIgnoreCase("Online")) {
                        if (!mNodeKeyHashSet.contains(skykey)) {
                            NodeInfo mNodeInfo = new NodeInfo(skykey, 1);
                            mNodeInfoList.add(mNodeInfo);
                            mNodeKeyHashSet.add(skykey);

                        }
                        break;
                    }
                }
                for (Element elementDanger : document2.select("span[class=badge badge-danger]")) {
                        Log.i(TAG+ " badge-danger ",elementDanger.toString());
                        if (elementDanger.text().equalsIgnoreCase("Offline")) {
                            offlineCount ++;
                            if (!mNodeKeyHashSet.contains(skykey)) {
                                NodeInfo mNodeInfo = new NodeInfo(skykey, 0);
                                mNodeInfoList.add(mNodeInfo);
                                mNodeKeyHashSet.add(skykey);
                            }
                            break;
                        }
				}
                Log.i("NetworkOperation ", ""+mNodeInfoList.size() +" offlineCount:: "+offlineCount);
                Log.i("NetworkOperationDone "," offlineCount:: "+offlineCount+"keytotal:: "+keytotal+"keyNumber:: "+keyNumber);
                if((keytotal == keyNumber)) {
                    if(offlineCount > 0)
                       showPopUp(offlineCount + " node/nodes are Offline :" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()),"offlinenode");
                    else
                        offlineCount = 0;
                }
				Elements elements = document2.select("span[class=badge badge-success]");
				if(!mIsFromUser && (elements == null || elements.size() == 0)){
				   showPopUp("Invalid Key entered : " + skykey+" : "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
						   +" Number of key in the row : "+mNodeInfoList.size(),"invalidkey");
				   clearSavedKeys();
				   mServiceHandler.removeMessages(1);
				   mServiceHandler.removeMessages(2);
				   Toast.makeText(getApplicationContext(), "Please enter proper URL as shown in demo video", 3000).show();
				}
				break;
			default:
				break;
			}
		}
	}

	private void clearSavedKeys(){
		mNodeInfoList.clear();
		ed.clear();
		ed.commit();
	}
	
	public void startMonitoring(Set<String> skykeys, boolean isFromUser,int interval){
		Log.i("startMonitoring", skykeys.toString()+" interval "+interval+" isFromUser "+isFromUser);
		mNodeInfoList.clear();
		mNodeKeyHashSet.clear();
		ed.clear();
		ed.putStringSet("skykeys", skykeys);
		ed.putInt("interval", interval);
		ed.apply();
		ed.commit();

		mServiceHandler.removeMessages(1);
		mServiceHandler.removeMessages(2);
        Message msg = mServiceHandler.obtainMessage(1);
        Bundle data = new Bundle();
        data.putBoolean("isFromUser", isFromUser);
        data.putInt("interval", interval);
        msg.setData(data);
        mServiceHandler.sendMessageDelayed(msg, /*1000*60*60*/5000);
        timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()).toString();
        Toast.makeText(this, "Sky Monitoring started", 3000).show();
	}
	
	public void StopMonitoring(){
		Log.i("stopMonitoring","in Service");
		mNodeInfoList.clear();
		mNodeKeyHashSet.clear();
		ed.clear();
		ed.apply();
		ed.commit();
		mServiceHandler.removeMessages(1);
		mServiceHandler.removeMessages(2);
		Toast.makeText(this, "Sky Monitoring STOPPED", 3000).show();
	}
	
	public void startMonitoringIndividualKey(String skykey,boolean isfromUser, int totalkeys,int keyNumber) {
		Log.i("startMonitoringIndividualKey", skykey.toString());
			Message msg = mServiceHandler.obtainMessage(2);
			Bundle data = new Bundle();
			data.putString("skykey", skykey);
			data.putBoolean("isfromUser", isfromUser);
			data.putInt("totalkeys",totalkeys);
			data.putInt("keyNumber",keyNumber);
			msg.setData(data);
			mServiceHandler.sendMessageDelayed(msg, 3000);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private void saveHtmlFile(String htmlFileString) {

        String path = Environment.getExternalStorageDirectory().getPath();
        String fileName = DateFormat.format("dd_MM_yyyy_hh_mm_ss", System.currentTimeMillis()).toString();
        fileName = fileName+ "SkyMonitor" + ".html";
        File file = new File(path, fileName);
        String html = null;
        html = htmlFileString;

        try {
        	org.jsoup.nodes.Document document = Jsoup.parse(html);
        	
        	List<String> subtitles = new ArrayList<String>();

        	for( Element element : document.select("span[class=badge badge-success]") )
        	{
        		if(element.text().equalsIgnoreCase("Online"))
        	      subtitles.add(element.text());
        		else if( element.text().equalsIgnoreCase("Offline")) {
                    //offlineCount ++;
                }
        	}

        	Log.e("saveHtmlFile", "File Save : " + file.getPath()+"   "+subtitles.get(0)+" size "+subtitles.size());
        	for(int i = 0; i < subtitles.size() ; i++)
        		Log.e("saveHtmlFile", "i = "+i+"  "+ subtitles.get(i));
        	
        	
        	
            FileOutputStream out = new FileOutputStream(file);
            byte[] data = html.getBytes();
            out.write(data);
            out.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
	
    private void showPopUp(String message,String from){
    	Intent intent;
        intent = new Intent(this, SkyAlertPopup.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Message", message);
        intent.putExtra("from",from);
        startActivity(intent);
    }
    
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
