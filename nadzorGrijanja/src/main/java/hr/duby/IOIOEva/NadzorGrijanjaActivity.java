package hr.duby.IOIOEva;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;



public class NadzorGrijanjaActivity extends IOIOActivity implements View.OnClickListener, OnItemSelectedListener {
	private Button buttonSR2_;
	private Button buttonSR3_;
	private Button buttonCl_;
	private Button btnCreateTable_;
	private Button btnLoadTable_;
	private Button btnDropTable_;
	private Button btnDeleteTable_;
	private Button btnExportData_;

	private Button btnRTGLayout_;
	private Button btnGDLayout_;
	private CheckBox cbRW_;
	private Spinner spinner;

	private EditText tv;
	private EditText ettempSzad_;
	private EditText ettempPumpaON_;
	private EditText ettempPmaxdop_;
	private EditText ettempZmaxdop_;
	private TextView tvDataBase_;
	private TextView tvS1;
	private TextView tvS2;
	private TextView tvS3;
	private TextView TextView03_;
	private TextView tvloadedTable_;
	private ImageView imgViewBlueTooth_;
	private ImageView ivTitlebarIcon_;
	private ImageView ivTPump_,ivTAlarm_;
	private ImageView ivSWPump_, ivSWAlarm_;
	private ImageView ivRefresh_;
	
	private TwiMaster twi;
	TabHost tabHost_;
	static TabHost tabHostListener;

	int cnt = 0; // Counter
	boolean rwFunctionResult; // rezultat I2C ReadWrite funkcije
	boolean clEvent = false;
	public boolean mainxml = true;
	short bytosh;
	short tbr = 0; // varijabla za Debuging
	int tabtag = 0;

	// *******************************************************************
	// master WRITE variables
	byte address = 42; // 42 IOIO -> 84 PIC
	byte IOIOMAC = 27; // IOIO MAC Address

	byte reqLen = 10; // Request Buffer Length
	byte[] request = new byte[reqLen];

	short tempSzad = 2244; // temp Sobe Zadano
	short tempPumpaON = 4044; // temp vode na kojoj se pali pumpa

	short tempPmaxdop = 6544; // temp Peči max dopušteno
	short tempZmaxdop = 4044; // temp Zaptitna max dopušteno

	byte rwdata = 0;    // Read Write Data to PIC RAM 0->R, 1->W
	short pumpaF = 0;   // Pumpa Flag off/on
	short PUMPOnSW = 0; // Pump Switch State
	short alarmF = 0;   // Alarm Flag off/on
	short ALARMOnSW = 0; // Alarm Switch State
	short PECnt = 0;    // Pump ON Event Counter

	// **********************************************************
	// master READ variables
	byte resLen = 33; // Response Buffer Length (max 32)
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
	String errMsg = "";
	static String TAG = "DTag"; 
	private String loadedTable = "MyTable";  // default is "MyTable"
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.customtitle);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		DefineTabHost();
		BridgeToXML();
		SetAllListener();
		LoadSavedPreferences();

		if (savedInstanceState != null) {
			Integer ti = savedInstanceState.getInt("tabState");
			tabHost_.setCurrentTab(ti);
		}
		
		LoadAllTableFromDatabase("MyDB");
		tvDataBase_.setMovementMethod(new ScrollingMovementMethod());

	} // end of onCreate() method

	// *********************************************************************************
	// *********************************************************************************

	private void LoadSavedPreferences() {
		String value;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// tab 3 ************************************************
		value = sharedPreferences.getString("tempSzad", "22");
		ettempSzad_.setText(value);
		value = sharedPreferences.getString("tempPmaxdop", "70");
		ettempPmaxdop_.setText(value);
		value = sharedPreferences.getString("tempZmaxdop", "22");
		ettempZmaxdop_.setText(value);
		value = sharedPreferences.getString("tempPumpaON", "50");
		ettempPumpaON_.setText(value);

		// tab 1 ************************************************
		value = sharedPreferences.getString("tv1", "20.20");
		tvS1.setText(value);
		value = sharedPreferences.getString("tv2", "44.22");
		tvS2.setText(value);
		value = sharedPreferences.getString("tv3", "19.22");
		tvS3.setText(value);
	}

	private void savePreferences(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	@Override
	protected void onPause() {
		// tab 3 ***********************************************************
		savePreferences("tempSzad", ettempSzad_.getText().toString());
		savePreferences("tempPmaxdop", ettempPmaxdop_.getText().toString());
		savePreferences("tempZmaxdop", ettempZmaxdop_.getText().toString());
		savePreferences("tempPumpaON", ettempPumpaON_.getText().toString());
		// tab 1 ***********************************************************
		savePreferences("tv1", tvS1.getText().toString());
		savePreferences("tv2", tvS2.getText().toString());
		savePreferences("tv3", tvS3.getText().toString());

		super.onPause();
		Log.d("DTag", "onPause");
	}
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	// *****************************************************************************************
	// *****************************************************************************************
	// *****************************************************************************************
	class Looper extends BaseIOIOLooper {

		private DigitalInput DigitalI;

		@Override
		protected void setup() throws ConnectionLostException {
			try {
				DigitalI = ioio_.openDigitalInput(15);
				twi = ioio_.openTwiMaster(2, TwiMaster.Rate.RATE_100KHz, false);
				iWrite("\nBluetooth Connected" + "\n");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						imgViewBlueTooth_
								.setImageResource(R.drawable.bluetooth_connected);
						ivTitlebarIcon_
								.setImageResource(R.drawable.bluetooth_connected);
					}
				});
				// clEvent = true; // run first time when connection is
				// establish
			} catch (ConnectionLostException e) {
				throw e;
			}
		}

		// *********************************************************************************
		// *********************************************************************************
		@Override
		public void loop() throws ConnectionLostException {

			try {
				if (DigitalI.read()) { }  // ? ! -> zbog catch excaptiona

				if (clEvent) {
					iWrite("\nin loop\n");

					// priprema podataka za slanje:
					// *****************************************************
					request[0] = IOIOMAC; // IOIO MAC Address = 27

					String str = ettempSzad_.getText().toString().trim();
					tempSzad = Short.parseShort(str);
					tempSzad = (short) (tempSzad * 100);

					str = ettempPumpaON_.getText().toString().trim();
					tempPumpaON = Short.parseShort(str);
					tempPumpaON = (short) (tempPumpaON * 100);

					str = ettempPmaxdop_.getText().toString().trim();
					tempPmaxdop = Short.parseShort(str);
					tempPmaxdop = (short) (tempPmaxdop * 100);

					str = ettempZmaxdop_.getText().toString().trim();
					tempZmaxdop = Short.parseShort(str);
					tempZmaxdop = (short) (tempZmaxdop * 100);

					switch (rwdata) {
					case 55: // tab 2 buton click
						if (cbRW_.isChecked()) {
							rwdata = 66;
							iWrite("data: R/W\n");
						} else {
							rwdata = 0;
							iWrite("data: R\n");
						}
						break;
					case 66: // tab 3 buton click
						iWrite("data: R/W\n");
						break;
					case 0:
						iWrite("data: R\n");
					}

					request[1] = (rwdata); // 66

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

					if (rwFunctionResult) //
						iWrite("successfully!\n");
					else {
						iWrite("failure: " + String.valueOf(cnt) + "\n");
					}

					if (response[0] != PICMAC) {
						iWrite("NO ACK\n");
					} else { // if(response[0] == PICMAC)
						clEvent = false; // response from PIC
						cnt = 0; // reset contrer of transmition

						pumpaF = response[27];
						PUMPOnSW = response[28];
						alarmF = response[29];
						ALARMOnSW = response[30];
						PECnt  = response[31];

						// Display Tab 1
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								ftemp(response[1], response[2]);
								tvS1.setText(ts);
								ftemp(response[3], response[4]);
								tvS2.setText(ts);
								ftemp(response[5], response[6]);
								tvS3.setText(ts);

								
								//Pump Trigger Status
								if (pumpaF == 1) ivTPump_.setImageResource(R.drawable.triggyess);
								else ivTPump_.setImageResource(R.drawable.triggnos);
								
								
								//Alarm Trigger Status
								if (alarmF == 1) ivTAlarm_.setImageResource(R.drawable.triggyess);
							    else ivTAlarm_.setImageResource(R.drawable.triggnos);
								
								
								//Pump SW Status
								if (PUMPOnSW == 1) ivSWPump_.setImageResource(R.drawable.buttonons);
								else ivSWPump_.setImageResource(R.drawable.buttonoffs); 
								
								//Alarm SW Status
								if (ALARMOnSW == 1) ivSWAlarm_.setImageResource(R.drawable.buttonons);
								else ivSWAlarm_.setImageResource(R.drawable.buttonoffs);
								
								
								if (rwdata == 66) {
									TextView03_
											.setText("Uspješno zapisano u PIC RAM");
								} else {
									TextView03_.setText("Status");
								}

							}
						});

						// Display Tab 2
						// **********************************************************************
						SQLiteDatabase db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
						db.execSQL("CREATE TABLE IF NOT EXISTS "+ loadedTable +" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");
						iWrite("table: "+ loadedTable + "\n");
						
						final Date currentTime = new Date();
						final SimpleDateFormat sdf = new SimpleDateFormat(
								"dd.MM.yyyy HH:mm:ss");
						str = sdf.format(currentTime);

						iWrite(str + "\n");
						float fl;

						// **********************************************
						// **********************************************
						fl = ftemp(response[1], response[2]); // 20.67
						// addPoint(fl); // add point to GraphView
						// addPoint(((fl-20)*10.0f));
						tempStr = (short) (fl * 100); // 2067
						iWrite("tempS: " + ts + "°C\n"); // senzor 1 =
															// 20.67C -> "ts"
															// calculate inside
															// "ftemp"
						// **********************************************

						fl = ftemp(response[3], response[4]);
						tempPtr = (short) (fl * 100);
						iWrite("tempP: " + ts + "°C\n");

						fl = ftemp(response[5], response[6]);
						tempZtr = (short) (fl * 100);
						iWrite("tempZ: " + ts + "°C\n\n");

						db.execSQL("INSERT INTO "+ loadedTable +" VALUES ('" + str
								+ "', " + tempStr + ", " + tempPtr + ", "
								+ tempZtr + ");");
						db.close(); // close DataBase

						ftemp(response[7], response[8]);
						iWrite("tempSmax: " + ts + "°C\n");

						ftemp(response[9], response[10]);
						iWrite("tempSmin: " + ts + "°C\n\n");

						ftemp(response[11], response[12]);
						iWrite("tempPmax: " + ts + "°C\n");

						ftemp(response[13], response[14]);
						iWrite("tempPmin: " + ts + "°C\n\n");

						ftemp(response[15], response[16]);
						iWrite("tempZmax: " + ts + "°C\n");

						ftemp(response[17], response[18]);
						iWrite("tempZmin: " + ts + "°C\n\n");

						ftemp(response[21], response[22]);
						iWrite("tempSzad: " + ts + "°C\n");

						ftemp(response[23], response[24]);
						iWrite("tempPmaxdop: " + ts + "°C\n");

						ftemp(response[25], response[26]);
						iWrite("tempZmaxdop: " + ts + "°C\n");

						ftemp(response[19], response[20]);
						iWrite("tempPumpON: " + ts + "°C\n\n");

						// ********************************************

						if (pumpaF == 1) {
							ts =   "\u2713";  // symbol check mark
						} else {
							ts = "\u2212";    // --
						}
						iWrite("PumpF: " + ts + "\n");

						if (PUMPOnSW == 1) {
							ts =   "ON";  
						} else {
							ts = "OFF";
						}
						iWrite("PumpOn Switch: " + ts + "\n");
						
						if ((PUMPOnSW == 0)&&(pumpaF == 1)){
							iWriteSpan("WARNING!\n Pump_SW is OFF, PumpF is active !\n");
						}

						
						if (alarmF == 1) {
							ts =   "\u2713";  
						} else {
							ts = "\u2212";
						}
						iWrite("AlarmF: " + ts + "\n");

						if (ALARMOnSW == 1) {
							ts = "ON";
						} else {
							ts = "OFF";
						}
						iWrite("AlarmOn Switch: " + ts + "\n");
						if ((ALARMOnSW == 0)&&(alarmF == 1)){
							iWriteSpan("WARNING!\n Alarm_SW is OFF, AlarmF is active !\n");
						}
						
						
						iWrite("Pump ON Event Counter: "+ PECnt+ "\n");
						
						//iWrite("err chk: "+ response[32]+ "\n");	

						response[0] = 0; // reset

						if (rwdata != 0) {
							rwdata = 0; // postavi samo za čitanje - ne i
										// upisivanje na PIC RAM
						}
					} // end else -> if(response[0] == PICMAC)

					cnt++;
					if (cnt > 30) { // 100 times try to get response
						iWrite("NO response, try again\n"); // if not response
															// try another click
						clEvent = false;
						cnt = 0;
					}
				} // end if(clEvent)
			} // end try
			// ******************************************************************
			catch (InterruptedException e) {
				ioio_.disconnect();
				iWrite("Interrupted Exception");
				setDisconnectFlag();
			} catch (ConnectionLostException e) {
				iWrite("Connection Lost Exception");
				setDisconnectFlag();
				// throw e;
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

	private void iWrite(String myval) {
		final String crossValue = myval;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tv.append(crossValue);
			}
		});
	}
	
	private void iWriteSpan(String myspan){
		final String crossValue = myspan;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Spannable wordtoSpan = new SpannableString(crossValue);        						
			    wordtoSpan.setSpan(new ForegroundColorSpan(Color.RED), 0, crossValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			    tv.append(wordtoSpan);
			}
		});	
	}

	public void iWrite2(String myval) {
		final String crossValue = myval;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvDataBase_.append(crossValue);
			}
		});
	}

	public void DefineTabHost() {

		tabHost_ = (TabHost) findViewById(R.id.tabHost);
		tabHost_.setup();

		TabSpec spec = tabHost_.newTabSpec("Tab1");
		spec.setIndicator("", getResources().getDrawable(R.drawable.one));
		spec.setContent(R.id.tab1);
		tabHost_.addTab(spec);

		spec = tabHost_.newTabSpec("Tab2");
		spec.setIndicator("", getResources().getDrawable(R.drawable.two));
		spec.setContent(R.id.tab2);
		tabHost_.addTab(spec);

		spec = tabHost_.newTabSpec("Tab3");
		spec.setIndicator("", getResources().getDrawable(R.drawable.three));
		spec.setContent(R.id.tab3);
		tabHost_.addTab(spec);

		spec = tabHost_.newTabSpec("Tab4");
		spec.setIndicator("", getResources().getDrawable(R.drawable.four));
		spec.setContent(R.id.tab4);
		tabHost_.addTab(spec);

		spec = tabHost_.newTabSpec("Tab5");
		spec.setIndicator("", getResources().getDrawable(R.drawable.five));
		spec.setContent(R.id.tab5);
		tabHost_.addTab(spec);

	}

	private void BridgeToXML() {
		tv = (EditText) findViewById(R.id.title);
		ettempSzad_ = (EditText) findViewById(R.id.ettempSzad);
		ettempPumpaON_ = (EditText) findViewById(R.id.ettempPumpaON);
		ettempPmaxdop_ = (EditText) findViewById(R.id.ettempPmaxdop);
		ettempZmaxdop_ = (EditText) findViewById(R.id.ettempZmaxdop);

		tvDataBase_ = (TextView) findViewById(R.id.tvDataBase);
		tvS1 = (TextView) findViewById(R.id.tv1B);
		tvS2 = (TextView) findViewById(R.id.tv2B);
		tvS3 = (TextView) findViewById(R.id.tv3B);
		TextView03_ = (TextView) findViewById(R.id.TextView03);
		tvloadedTable_ = (TextView) findViewById(R.id.tvloadedTable);

		buttonSR2_ = (Button) findViewById(R.id.buttonSR2);
		buttonSR3_ = (Button) findViewById(R.id.buttonSR3);
		buttonCl_ = (Button) findViewById(R.id.buttonCL);
		btnCreateTable_ = (Button) findViewById(R.id.btnCreateTable);
		btnLoadTable_ = (Button) findViewById(R.id.btnLoadTable);
		btnDropTable_ = (Button) findViewById(R.id.btnDropTable);
		btnDeleteTable_ = (Button) findViewById(R.id.btnDeleteTable);
		btnExportData_ = (Button) findViewById(R.id.btnExportData);
		btnRTGLayout_ = (Button) findViewById(R.id.btnRTGLayout);
		btnGDLayout_ = (Button) findViewById(R.id.btnGDLayout);

		cbRW_ = (CheckBox) findViewById(R.id.cbRW);

		imgViewBlueTooth_ = (ImageView) findViewById(R.id.imgViewBlueTooth);
		imgViewBlueTooth_.setImageResource(R.drawable.bluetooth_disconnected2);

		ivTitlebarIcon_ = (ImageView) findViewById(R.id.ivTitlebarIcon);
		ivTPump_  = (ImageView) findViewById(R.id.ivTPump);
		ivTAlarm_ = (ImageView) findViewById(R.id.ivTAlarm);
		ivSWPump_ = (ImageView) findViewById(R.id.ivSWPump);
		ivSWAlarm_= (ImageView) findViewById(R.id.ivSWAlarm);
		
		ivRefresh_ = (ImageView) findViewById(R.id.ivRefresh);
		
		spinner = (Spinner) findViewById(R.id.spinnerLoad);
	}

	private void setDisconnectFlag() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				imgViewBlueTooth_.setImageResource(R.drawable.bluetooth_disconnected2);
				ivTitlebarIcon_.setImageResource(R.drawable.bluetooth_disconnected2);
			}
		});
	}

	private void SetAllListener() {
		buttonSR2_.setOnClickListener(this);
		buttonSR3_.setOnClickListener(this);
		buttonCl_.setOnClickListener(this);
		btnCreateTable_.setOnClickListener(this);
		btnLoadTable_.setOnClickListener(this);
		btnDropTable_.setOnClickListener(this);
		btnDeleteTable_.setOnClickListener(this);
		btnExportData_.setOnClickListener(this);
		btnRTGLayout_.setOnClickListener(this);
		btnGDLayout_.setOnClickListener(this);
		spinner.setOnItemSelectedListener(this);
		
		ivRefresh_.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {

		SQLiteDatabase db;
		String s;
		Intent myIntent;

		switch (v.getId()) {
		case R.id.ivRefresh: // čitanje sa PIC RAMa Tab1
			rwdata = 0;
			clEvent = true;

			break;
		case R.id.buttonSR2: // čitanje sa PIC RAMa Tab2
			rwdata = 55;
			clEvent = true;
			break;
		case R.id.buttonSR3: // čitanje i zapisivanje u PIC RAM
								// ("Uppload & Save button")
			rwdata = 66;
			clEvent = true;
			break;
		case R.id.buttonCL: // Clear display
			tv.setText("");
			break;

		case R.id.btnCreateTable:
			// **************************
			db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
			db.execSQL("CREATE TABLE IF NOT EXISTS "+ loadedTable +" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");

			final Date currentTime = new Date();
			final SimpleDateFormat sdf = new SimpleDateFormat(
					"dd.MM.yyyy HH:mm:ss");
			s = sdf.format(currentTime);

			db.execSQL("INSERT INTO "+ loadedTable +" VALUES ('" + s
					+ "', 1111,2222,3333);");

			db.close();
			tvDataBase_.setText("");
			iWrite2("table created!\n");
			break;

		case R.id.btnLoadTable:
			// **************************
			loadTableData();
			break;
		case R.id.btnDropTable:
			// *************************
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Are you sure to DROP table: "+ loadedTable +"?");

			// Set up the buttons
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
					tvDataBase_.setText("");
					SQLiteDatabase db2;
					db2 = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
					db2.execSQL("DROP TABLE IF EXISTS '" + loadedTable + "'");
					db2.close();
					
					iWrite2("all data in table: "+ loadedTable +" is DROPED");
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
			break;
		case R.id.btnDeleteTable:
			// *************************
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("Are you sure to DELETE all data in table: "+ loadedTable +"?");

			// Set up the buttons
			builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
					tvDataBase_.setText("");
					SQLiteDatabase db2;
					db2 = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
					db2.delete(loadedTable, null, null);
					db2.close();
					
					iWrite2("All data in table: "+ loadedTable +" is DELETED");
					LoadAllTableFromDatabase("MyDB"); //refresh spinner list
			    }
			});
			builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        dialog.cancel();
			    }
			});

			builder2.show();
			break;
			
		case R.id.btnExportData:			
			if (exportDataTable2Excel(this, loadedTable +".xls")) {
				Toast.makeText(this,  errMsg, Toast.LENGTH_LONG).show();
			}else Toast.makeText(this,  errMsg, Toast.LENGTH_LONG).show();
			break;

		case R.id.btnRTGLayout:
			// **************************
			myIntent = new Intent(this, RealTimeGraph.class);
			startActivity(myIntent);
			break;

		case R.id.btnGDLayout:
			// **************************
			myIntent = new Intent(this, GraphData.class);
			myIntent.putExtra("loadedTable_", loadedTable);
			startActivity(myIntent);
			break;


		}

	}

	private void loadTableData() {
		tvDataBase_.setText("");
		String s;
		SQLiteDatabase db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
		db.execSQL("CREATE TABLE IF NOT EXISTS "+ loadedTable +" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");

		Cursor cur = db.rawQuery("SELECT * FROM "+ loadedTable, null);   
		if (cur != null && cur.getCount() > 0) {
			iWrite2("Loaded table: "+ loadedTable + "\n");
			cur.moveToFirst();
			do {
				s = cur.getString(cur.getColumnIndex("Datum"));
				iWrite2(s + "\n");
				s = cur.getString(cur.getColumnIndex("Temp1S")) + "\n";
				iWrite2(s);
				s = cur.getString(cur.getColumnIndex("Temp2S")) + "\n";
				iWrite2(s);
				s = cur.getString(cur.getColumnIndex("Temp3S")) + "\n";
				iWrite2(s);
			} while (cur.moveToNext());
		} else {
			iWrite2("No data in table: "+ loadedTable +"\n");
		}

		db.close();
		
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
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(NadzorGrijanjaActivity.this, R.layout.spinner_item, myDataTableList);  //android.R.layout.simple_spinner_item
		spinner.setAdapter(adapter);		    
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
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
					tvloadedTable_.setText(loadedTable);
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
			tvloadedTable_.setText(loadedTable);
			loadTableData();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}
	
	private boolean exportDataTable2Excel(Context context, String fileName) { 
        // exportDataTable2Excel(this,"myNCGData.xls");
		
		// check if available and not read only 
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) { 
        	errMsg = "Storage not available or read only";
            return false; 
        } 
 
        boolean success = false; 
        //New Workbook
        Workbook wb = new HSSFWorkbook();
        Cell c = null;      
        //New Sheet
        Sheet sheet1 = null;
        sheet1 = wb.createSheet("tempData");
        
        //load Database table 
        SQLiteDatabase db;
        db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
		db.execSQL("CREATE TABLE IF NOT EXISTS "+ loadedTable +" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");

		Cursor cur = db.rawQuery("SELECT * FROM "+ loadedTable, null);
		if (cur != null && cur.getCount() > 0) {
			cur.moveToFirst();
			int i = 0;
			String s;
			Row row = sheet1.createRow(0);
			c = row.createCell(0);
	        c.setCellValue("Datum");
	        c = row.createCell(1);
	        c.setCellValue("tempS");
	        c = row.createCell(2);
	        c.setCellValue("tempP");
	        c = row.createCell(3);
	        c.setCellValue("tempZ");
			
			do {
				i++;
				row = sheet1.createRow(i);
				
				s = cur.getString(cur.getColumnIndex("Datum"));
				c = row.createCell(0);
		        c.setCellValue(s);
		        
				s = cur.getString(cur.getColumnIndex("Temp1S")) + "\n";
				c = row.createCell(1);
		        c.setCellValue(s);
				
				s = cur.getString(cur.getColumnIndex("Temp2S")) + "\n";
				c = row.createCell(2);
		        c.setCellValue(s);
				
				s = cur.getString(cur.getColumnIndex("Temp3S")) + "\n";
				c = row.createCell(3);
		        c.setCellValue(s);
				
			} while (cur.moveToNext());
		} else {
			errMsg = "No data in table!";
		}

		db.close();  //close dataBase


        sheet1.setColumnWidth(0, (4000));
        sheet1.setColumnWidth(1, (1600));
        sheet1.setColumnWidth(2, (1600));
        sheet1.setColumnWidth(3, (1600));
 
        // Create a path where we will place our List of objects on external storage 
        File file = new File(context.getExternalFilesDir(null), fileName); 
        FileOutputStream os = null; 
 
        try { 
            os = new FileOutputStream(file);
            wb.write(os);
            errMsg = "Successfully Exported!";
            success = true; 
        } catch (IOException e) { 
            errMsg = "Error writing " + file;
        } catch (Exception e) { 
            errMsg ="Failed to save file"; 
        } finally { 
            try { 
                if (os != null) 
                    os.close(); 
            } catch (Exception ex) { 
            } 
        } 
        return success; 
    } 
	
	
	public static boolean isExternalStorageReadOnly() { 
	        String extStorageState = Environment.getExternalStorageState(); 
	        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) { 
	            return true; 
	        } 
	        return false; 
	 } 
	 
	 public static boolean isExternalStorageAvailable() { 
	        String extStorageState = Environment.getExternalStorageState(); 
	        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) { 
	            return true; 
	        } 
	        return false; 
	 } 
	   
	 
	public void onBackPresed() { // Kill Application on Exit
			// twi.close(); // free TWI module and pins
			android.os.Process.killProcess(android.os.Process.myPid());
			

	}


	
}

/* TO - DO LIST:
 * **************************************
 *	posložiti tab 1 / pumpa i alarm imaju 4 moguća stanja
 *	spriječiti "screen saver" tj. izlaz iz programa nakon dugog vrmena nekorištenja   >> done
 *	ubaciti mogućnost odabira tablice za unos podataka								  >> done
 *	ubaciti export podataka u excel ili sl.											  >> done
 * 	ubaciti odabir senzora kod prikaza grafa
 * 	upozorenje kada je PumpF ili AlarmF = 1 a SW su onemogućeni                       >> done
 *  potrošnja energije: dtemp = tempP - tempZ ; protok
 */
 
