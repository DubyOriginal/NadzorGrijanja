package hr.duby.IOIOEva;

import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class RealTimeGraph extends IOIOActivity implements View.OnClickListener, OnItemSelectedListener {

	private Button btnStart_;
	private Button btnStop_;
	private Button btnBackToMain_;
	private EditText etSampInt_;
	private EditText etXspeed_;
	private GraphView mGraphView;
	private ImageView ivTitlebarIcon_;
	private TextView tvDataTable_, tvSensor_;
	private Spinner spinner, spSensor_;

	public static boolean sampling = false;
	boolean closeIOIO = false;
	boolean rwFunctionResult; // rezultat I2C ReadWrite funkcije
	boolean clEvent = false;
	boolean isConnected = false;
	short bytosh;
	short tbr = 0; // varijabla za Debuging
	int tabtag = 0;
	int cnt = 0; // Counter
	int samplingInterval = 2;

	// **********************************************************
	// master WRITE variables
	byte address = 42; // 42 IOIO -> 84 PIC
	byte IOIOMAC = 27; // IOIO MAC Address

	byte reqLen = 10; // Request Buffer Length
	byte[] request = new byte[reqLen];

	short tempSzad = 2244; // temp Sobe Zadano
	short tempPumpaON = 4044; // temp vode na kojoj se pali pumpa

	short tempPmaxdop = 6544; // temp Peči max dopušteno
	short tempZmaxdop = 4044; // temp Zaštitna max dopušteno

	byte rwdata = 0; // Read Write Data to PIC RAM 0->R, 1->W
	short pumpaF = 0; // Pumpa Flag off/on
	short PUMPOnSW = 0; // Pump Switch State
	short alarmF = 0; // Alarm Flag off/on
	short ALARMOnSW = 0; // Alarm Switch State

	// **********************************************************
	// master READ variables
	byte resLen = 33; // Response Buffer Length (32 = max)
	byte[] response = new byte[resLen];
	byte PICMAC = 65; // PIC MAC Address
	short tempStr = 0; // temp sobe trenutna
	short tempSmin = 0; // temp sobe min
	short tempSmax = 0; // temp sobe max
	short tempPtr = 0; // temp Peči Trenutna
	short tempPmin = 0; // temp Peči min
	short tempPmax = 0; // temp Peči max
	short tempZtr = 0; // temp Zaštite Trenutna
	short tempZmin = 0; // temp Zaštite min
	short tempZmax = 0; // temp Zaštite max

	String ts = ""; // temporary string
	String str = "";
	String loadedTable = "MyTable";  // default is "MyTable"
	String loadedSensor;

	final Context context = this;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.realtimegraph);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.customtitle);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mGraphView = (GraphView) findViewById(R.id.graphRT);

		btnStart_ = (Button) findViewById(R.id.btnStart);
		btnStop_ = (Button) findViewById(R.id.btnStop);
		btnBackToMain_ = (Button) findViewById(R.id.btnBackToMain);

		
		btnStart_.setOnClickListener(this);
		btnStop_.setOnClickListener(this);
		btnBackToMain_.setOnClickListener(this);

		etSampInt_ = (EditText) findViewById(R.id.etSampInt);
		etXspeed_ = (EditText) findViewById(R.id.etXspeed);

		tvDataTable_ = (TextView) findViewById(R.id.tvDataTable);
		tvDataTable_.setText(loadedTable);
		
		tvSensor_ = (TextView) findViewById(R.id.tvSensor);

		ivTitlebarIcon_ = (ImageView) findViewById(R.id.ivTitlebarIcon);

		float xs = Float.valueOf(etXspeed_.getText().toString());
		mGraphView.setSpeed(xs);


		samplingInterval = Integer.valueOf(etSampInt_.getText().toString());

		LoadSavedPreferences();
		
		spinner = (Spinner) findViewById(R.id.spinnerLoad);
		LoadAllTableFromDatabase("MyDB");
		spinner.setOnItemSelectedListener(this);
		
		spSensor_ = (Spinner) findViewById(R.id.spSensor);
		fillSpinnerSemsor();
		spSensor_.setOnItemSelectedListener(this);
		loadedSensor = "tempS";   // default Sensor
		mGraphView.setMaxValue(35);
	}



	private void LoadSavedPreferences() {
		String value;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		value = sharedPreferences.getString("SampInt", "2");
		etSampInt_.setText(value);

		value = sharedPreferences.getString("Xspeed", "13");
		etXspeed_.setText(value);

	}

	private void savePreferences(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	private void LoadAllTableFromDatabase(String dbName) {
		ArrayList<String> myDataTableList = new ArrayList<String>();
		SQLiteDatabase db;
		db = openOrCreateDatabase(dbName, MODE_PRIVATE, null);
		Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
		String s;
		if (c.moveToFirst()) {
			while ( !c.isAfterLast() ) {
				s = c.getString( c.getColumnIndex("name"));
				if (s.equals("android_metadata")){
					c.moveToNext();
					continue;
				}
				myDataTableList.add(s);
		        c.moveToNext();
		    }
		}
		db.close();
		myDataTableList.add("Create new table");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(RealTimeGraph.this, R.layout.spinner_item, myDataTableList);  //android.R.layout.simple_spinner_item
		spinner.setAdapter(adapter);		    
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		switch (arg0.getId()) {
		case R.id.spinnerLoad:
			String s;
			s = spinner.getSelectedItem().toString();
			
			if (s.equals("Create new table")){
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Enter table name");

				// Set up the input
				final EditText etInput = new EditText(this);
				// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
				etInput.setInputType(InputType.TYPE_CLASS_TEXT);
				builder.setView(etInput);

				// Set up the buttons
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	loadedTable = etInput.getText().toString();
						SQLiteDatabase db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
						db.execSQL("CREATE TABLE IF NOT EXISTS "+ loadedTable +" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");
						db.close();
						tvDataTable_.setText(loadedTable);
						LoadAllTableFromDatabase("MyDB"); //refresh spinner list
				    }
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        dialog.cancel();
				    }
				});

				builder.show();
				
			}else{
				loadedTable = s;
				tvDataTable_.setText(loadedTable);
			}
			break;

		case R.id.spSensor:
			sampling = false;      //stop sampling
			loadedSensor = spSensor_.getSelectedItem().toString();
			tvSensor_.setText(loadedSensor);
			if (loadedSensor.equals("tempP")){ 
				mGraphView.setMaxValue(140);
				mGraphView.drawGrid();
			}
			else { 
				mGraphView.setMaxValue(35);
				mGraphView.drawGrid();
			}
			break;
		}	
		mGraphView.clearCanvas();     //clear traces from dialog
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		Log.d("DTag", "createIOIOLooper");
		return new Looper();
	}
	
	private void fillSpinnerSemsor() {
		String[] sensorlist = {"tempS", "tempP","tempZ"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,  R.layout.spinner_item, sensorlist);
		spSensor_.setAdapter(adapter);
	}

	// *************************************************************************
	// *************************************************************************

	class Looper extends BaseIOIOLooper {
		TwiMaster twi;

		@Override
		protected void setup() throws ConnectionLostException {
			try {
				twi = ioio_.openTwiMaster(2, TwiMaster.Rate.RATE_100KHz, false);

				isConnected = true;
				Log.d("DTag", "isConnected = " + isConnected);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ivTitlebarIcon_
								.setImageResource(R.drawable.bluetooth_connected);
					}
				});

			} catch (ConnectionLostException e) {
				Log.d("DTag", "Not Connected");
				throw e;
			}
		}

		// *********************************************************************************
		// *********************************************************************************
		@Override
		public void loop() throws ConnectionLostException {

			if (closeIOIO) {
				ioio_.disconnect();
				clEvent = false;
				sampling = false;
				SystemClock.sleep(100);
				Log.d("DTag", "close IOIO");
			}

			try {
				// kontrola za AUTOMATSKO prikupljanje Podataka
				if (sampling) {
					clEvent = true;
					samplingInterval = Integer.valueOf(etSampInt_.getText().toString());
					float xSpd = Float.valueOf(etXspeed_.getText().toString());
					mGraphView.setSpeed(xSpd);
					SystemClock.sleep(samplingInterval * 1000);
				}

				if (clEvent) {
					// u RealTimeGraph -> podaci se ne šalju več samo primaju
					request[1] = (rwdata); // rwdata = 0 -> only Read

					request[2] = (byte) (tempSzad >> 8);
					request[3] = (byte) (tempSzad);

					request[4] = (byte) (tempPumpaON >> 8);
					request[5] = (byte) (tempPumpaON);

					request[6] = (byte) (tempPmaxdop >> 8);
					request[7] = (byte) (tempPmaxdop);

					request[8] = (byte) (tempZmaxdop >> 8);
					request[9] = (byte) (tempZmaxdop);

					// *****************************************************
					// *****************************************************

					rwFunctionResult = twi.writeRead(address, false, request,
							reqLen, response, resLen);

					if (response[0] != PICMAC) {
						Log.d("DTag", "NO ACK");
					} else { // if(response[0] == PICMAC)
						clEvent = false; // response from PIC
						cnt = 0; // reset contrer of transmition

						pumpaF = response[27];
						alarmF = response[28];

						// Display Tab 2
						// **********************************************************************
						SQLiteDatabase db = openOrCreateDatabase("MyDB",
								MODE_PRIVATE, null);
						db.execSQL("CREATE TABLE IF NOT EXISTS "
								+ loadedTable
								+ " (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");

						final Date currentTime = new Date();
						final SimpleDateFormat sdf = new SimpleDateFormat(
								"dd.MM.yyyy HH:mm:ss");
						str = sdf.format(currentTime);

						float fl;

						// **********************************************
						// **********************************************
						fl = ftemp(response[1], response[2]);            // fl = 20.67
						if (loadedSensor.equals("tempS")) addPoint(fl);  // add point "tempS" to GraphView
						tempStr = (short) (fl * 100);                    // 2067
						// **********************************************

						fl = ftemp(response[3], response[4]);
						if (loadedSensor.equals("tempP")) addPoint(fl);  // add point "tempP" to GraphView
						tempPtr = (short) (fl * 100);

						fl = ftemp(response[5], response[6]);
						if (loadedSensor.equals("tempZ")) addPoint(fl);  // add point "tempZ" to GraphView
						tempZtr = (short) (fl * 100);

						db.execSQL("INSERT INTO "+ loadedTable +" VALUES ('" + str
								+ "', " + tempStr + ", " + tempPtr + ", "
								+ tempZtr + ");");
						// spremi u bazu
						// *********************************************************************************************************

						db.close(); // close DataBase
						// ********************************************

						response[0] = 0; // reset

						if (rwdata != 0) {
							rwdata = 0; // postavi samo za čitanje - ne i
										// upisivanje na PIC RAM
						}
					} // end else -> if(response[0] == PICMAC)

					cnt++;
					if (cnt > 30) { // 100 times try to get response
						Log.d("DTag", "NO response, try again"); // if not
																	// response
																	// try
																	// another
																	// click
						clEvent = false;
						cnt = 0;
					}
				} // end if(clEvent)
			} // end try
			// ******************************************************************
			catch (InterruptedException e) {
				ioio_.disconnect();
				isConnected = false;
				ivTitlebarIcon_.setImageResource(R.drawable.bluetooth_disconnected2);
				//Log.d("DTag", "Interrupted Exception");
			} catch (ConnectionLostException e) {
				//Log.d("DTag", "Connection Lost Exception");
				isConnected = false;
				ivTitlebarIcon_.setImageResource(R.drawable.bluetooth_disconnected2);
			}

		} // end of "loop()"

	} // end of "looper()"

	private float ftemp(short a, short b) { // Format temp from response
		a = (short) (a & 0xFF); // HighByte
		b = (short) (b & 0xFF); // LowByte

		float bytofl = (((a << 8) & 0xFF00) | ((b & 0xFF)));
		bytofl = bytofl / 100;

		ts = String.valueOf(bytofl);
		return bytofl;
	}

	private void addPoint(final float point) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mGraphView.addDataPoint(point);
			}
		});
	}

	@Override
	public void onClick(View arg0) {

		switch (arg0.getId()) {
		case R.id.btnStart:
			sampling = true;
			mGraphView.clearCanvas();
			Log.d("DTag", "Sampling = TRUE");
			break;
			
		case R.id.btnStop:
			sampling = false;
			Log.d("DTag", "Sampling = FALSE");
			break;

		case R.id.btnBackToMain:
			finish();
			break;

		}

	}

	@Override
	protected void onPause() {
		savePreferences("SampInt", etSampInt_.getText().toString());
		savePreferences("Xspeed", etXspeed_.getText().toString());
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		closeIOIO = true;
		Log.d("DTag", ".RealTimeGraph onBackPressed");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finish();
	}

	@Override
	protected void onResume() {
		mGraphView.clearCanvas();
		Log.d("DTag", "onResume");
		super.onResume();
	}



	

}
