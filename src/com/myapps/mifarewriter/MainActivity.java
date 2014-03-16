package com.myapps.mifarewriter;

import java.io.IOException;
import java.math.BigInteger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{

	private static final String TAG = null;
	EditText mSector;
	EditText mKeya;
	EditText mBlock;
	EditText mData;
	EditText mNewkeya;
	EditText mNewkeyb;
	Button mWriteTagButton;
	TextView mResult;
	CheckBox mDefault;
	ScrollView scrollview;
	WebView webview;
	
	private NfcAdapter mAdapter;
	private boolean mInWriteMode;
    
    MifareClassic mfc;
    private boolean default_key;
    private boolean store_data;
    private boolean modify_keys;
    
    boolean auth;
    boolean flag;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
			
		scrollview = (ScrollView) this.findViewById(R.id.counter_pat_scrollview);
		
		mSector = (EditText)findViewById(R.id.sec_no);
		mKeya = (EditText)findViewById(R.id.keya);
		mBlock = (EditText)findViewById(R.id.block_no);
		mData = (EditText)findViewById(R.id.data);
		mNewkeya = (EditText)findViewById(R.id.new_keya);
		mNewkeyb = (EditText)findViewById(R.id.new_keyb);
		mResult = (TextView)findViewById(R.id.result);
		
		mWriteTagButton = (Button)findViewById(R.id.write_tag_button);
        mWriteTagButton.setOnClickListener(this);
        webview=(WebView)findViewById(R.id.web);
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        
        addListenerOnmDefault();
        addListenerOnmStoreData();
        addListenerOnmModifyKeys();
        
        default_key=false;
        store_data=false;
        modify_keys=false;
        
        auth = false;
        flag=true;
	}
	
	public void onClick(View v) {
		if(v.getId() == R.id.write_tag_button) {
			
			int sec_no = getSector();
			
			if(sec_no>15)
            {	flag = false;
            	displayMessage("ERROR : Sector no. should be between 2 and 15");
            }
			if(getKeya().length()!=0 && getKeya().length()!=6)
			{	flag = false;
        		displayMessage("ERROR : KeyA should be of length 6");
			}
			/*
			int block_no = getBlock();
			if(block_no>63 || block_no==1 || block_no==3)
			{	flag = false;
				displayMessage("ERROR : Block value should be less than 63 (except 1 and 3)");
			}
			*/
			if((getnkeya().length()!=0 && getnkeya().length()!=6) || (getnkeyb().length()!=0 && getnkeyb().length()!=6))
			{	flag = false;
				displayMessage("ERROR : New Keys should be of length 6");
			}
			
			if(store_data==false && modify_keys==false)
			{	flag = false;
				displayMessage("ERROR : Choose one");
			}
			
			if(flag)
			{
			displayMessage("Touch and hold tag against phone to write.");
			enableWriteMode();
			}
			
			sendScroll();
		}
	}
	
	@Override
    public void onNewIntent(Intent intent) {
		if(mInWriteMode) {
			mInWriteMode = false;
			
			// write to newly scanned tag
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			try {
				writeTag(tag);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void enableWriteMode() {
		mInWriteMode = true;
		
		// set up a PendingIntent to open the app when a tag is scanned
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[] { tagDetected };
        
		mAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
	}
	
	private void disableWriteMode() {
		mAdapter.disableForegroundDispatch(this);
	}
	
	private void writeTag(Tag tag) throws IOException {
		
            MifareClassic mfc = MifareClassic.get(tag);
         
            try {
				mfc.connect();
			
            byte keya[] = { (byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF,(byte) 0xFF};

            if(!default_key){
           
            	String key = getKeya();
            	
            	key = toHex(key).toUpperCase();
            	int len = key.length();
            	key = key.substring(28,40);
            	keya = new BigInteger(key, 16).toByteArray();
            	
            	//Toast.makeText(getApplicationContext(), ""+keya, Toast.LENGTH_LONG).show();
            }
       
           	auth = mfc.authenticateSectorWithKeyA(getSector(), keya);
            
            if(auth)
            {
            	Toast.makeText(getApplicationContext(), "Authenticated with KEY A successfully", Toast.LENGTH_LONG).show();
            	
            	if(store_data)
            	{
            	int block_no = getBlock();
            	String data = getData();
            	
            	String s = data;
            	s = toHex(s);
             	BigInteger bi = new BigInteger(s, 16);
             	byte[] a1 = bi.toByteArray();
             	byte[] a2 = new byte[16];
             	System.arraycopy(a1, 0, a2, 16 - a1.length, a1.length);
             	mfc.writeBlock(block_no,a2);
             	
             	if(data.length()>16)
             		displayMessage("Tag written successfully \n WARNING : Only 16 characters of data stored");
             	else
             		displayMessage("Tag written successfully");
            	}
            	
            	else if(modify_keys)
            	{
            		String nkeya = getnkeya();
            		nkeya = toHex(nkeya).toUpperCase();
            		nkeya = nkeya.substring(28,40);
            		
            		String nkeyb = getnkeyb();
            		nkeyb = toHex(nkeyb).toUpperCase();
            		nkeyb = nkeyb.substring(28,40);
            		
            		String nkey = nkeya+"FF078069"+nkeyb;
                	int len = nkey.length();
                	
                	byte[] nkeyab = new BigInteger(nkey, 16).toByteArray();
                	
                	mfc.writeBlock((getSector()*4)+3,nkeyab);
                	displayMessage("Tag written successfully");
                	webview.loadUrl("http://satyamhospital.org/put_applog.php?sector=\""+getSector()+"\"&pk=\""+getKeya()+"\"&nka=\""+getnkeya()+"\"&nkb=\""+getnkeyb()+"\"");
                			
            	}
            	
            	else
            	{
            		displayMessage("ERROR : Please select one of 3A or 3B");
            	}
            }
             else
            	 displayMessage("Authentication failed with KeyA");
            
            	sendScroll();
            	
            }catch (IOException e) { 
                Log.e(TAG, e.getLocalizedMessage());
                //showAlert(3);
            }
            
	    }
	
	public int getSector() {
		 String s = mSector.getText().toString();
		 return Integer.parseInt(s);
	}
	
	
	private int getBlock() {
		 String s = mBlock.getText().toString();
		 return Integer.parseInt(s);
	}
	
	private String getKeya() {
		 return mKeya.getText().toString();
	}
	
	private String getData() {
		 return mData.getText().toString();
	}
	
	private String getnkeya() {
		 return mNewkeya.getText().toString();
	}
	
	private String getnkeyb() {
		 return mNewkeyb.getText().toString();
	}
	
	public String toHex(String arg) {
	    return String.format("%040x", new BigInteger(arg.getBytes()));
	}
	
	

	public void displayMessage(String s)
	{
		String a = mResult.getText().toString();
		mResult.setText(a+"\n"+s);
	}
	
	public void addListenerOnmDefault() { 
		mDefault = (CheckBox) findViewById(R.id.chkDefault);
		mDefault.setOnClickListener(new OnClickListener() {
	 
		  public void onClick(View v) {
			if (((CheckBox) v).isChecked()) {
				default_key=true;
			} 
		  }
		});
	  }

	public void addListenerOnmStoreData() { 
		mDefault = (CheckBox) findViewById(R.id.chkStoreData);
		mDefault.setOnClickListener(new OnClickListener() {
	 
		  public void onClick(View v) {
			if (((CheckBox) v).isChecked()) {
				store_data=true;
			} 
		  }
		});
	  }
	
	public void addListenerOnmModifyKeys() { 
		mDefault = (CheckBox) findViewById(R.id.chkModifyKeys);
		mDefault.setOnClickListener(new OnClickListener() {
	 
		  public void onClick(View v) {
			if (((CheckBox) v).isChecked()) {
				modify_keys=true;
			} 
		  }
		});
	  }
	
	private void sendScroll(){
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {Thread.sleep(100);} catch (InterruptedException e) {}
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollview.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }).start();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
