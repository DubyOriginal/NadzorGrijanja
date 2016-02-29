package hr.duby.IOIOEva;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

public class GraphData extends Activity implements OnTouchListener {

	GFXSurface mGfxsurface;
	FrameLayout frm;
	int[] dataArrayS = null;
	int[] dataArrayP = null;
	int[] dataArrayZ = null;
	private Context mContext; 
	boolean isRunning = false;
	boolean isTouched = false;
	private int startpos; // start Counter za ScroolBar
	private int startposmax;
	private int mXs; // X div [px]
	private boolean isdataLoaded = false;
	private float text_size_A;
	private float text_size_B;
	private float text_size_C;

	// we can be in one of these 3 states
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;
	private int mode = NONE;
	// remember some things for zooming
	private PointF start = new PointF();
	private float oldDist = 1f;

	private String selectedTable = "MyTable";  // default is "MyTable"
	//private String loadedSensor = "tempS";     // default is "tempS"
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		DLog("onCreate");

		text_size_A = getResources().getDimension(R.dimen.text_size_A);
		text_size_B = getResources().getDimension(R.dimen.text_size_B);
		text_size_C = getResources().getDimension(R.dimen.text_size_C);
		DLog("text_size_A: " + text_size_A + " " + "text_size_B: " + text_size_B + " " + "text_size_C: " + text_size_C);

		mGfxsurface = new GFXSurface(this);
		setContentView(R.layout.graphdata);

		frm = (FrameLayout) findViewById(R.id.frameLayout);
		frm.addView(mGfxsurface);

		mGfxsurface.setOnTouchListener(this);

		startpos = 1; // od 1 do -> da.length-nod
		mXs = 25;
		
		//selectedTable = loadedTable -> from. NadzorGrijanjaActivity
		Intent intent = getIntent();
		selectedTable = intent.getExtras().getString("loadedTable_");
		
		 mContext = this; // to use all around this class

	}

	@Override
	protected void onPause() {
		super.onPause();
		mGfxsurface.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGfxsurface.resume();
	}
	


	// ****************************************************************
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			start.set(event.getX(), event.getY());
			mode = DRAG;
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event); // dijagonala između 2 prsta
			if (oldDist > 10f)
				mode = ZOOM;
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;

		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				float dx = start.x - event.getX();
				dx /= 50; // scale X
				startpos += dx;
				if (startpos > startposmax)
					startpos = startposmax;
				else if (startpos < 1)
					startpos = 1;

			} else if (mode == ZOOM) {
				float newDist = spacing(event);
				float scale = 0.0f;

				if (newDist > 10f) {
					scale = newDist / oldDist;
					mXs = (int) (mXs * scale);
					if (mXs > 100)
						mXs = 100;
					else if (mXs < 1)
						mXs = 1;
				}
			}
			break;
		}

		return true; // false znači za jedan klik i gotovo
	}

	// Determine the space between the first two fingers
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		DLog("GraphData onBackPressed");
		isRunning = false;
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		GraphData.this.finish();
	}


	// ******************************************************************************
	// ******************************************************************************
	public class GFXSurface extends SurfaceView implements Runnable {
		SurfaceHolder ourHolder;
		public Thread ourThread = null;
		private Paint mPaint;
		private Canvas mCanvas;

		private float maxValue;
		private float mScale;
		private float mYoff;
		private float mW, mH, mLb, mTb, mBb, mRb;
		private String statusBarText = "";

		public GFXSurface(Context context) {
			super(context);
			ourHolder = getHolder();
			ourThread = new Thread(this);
			ourThread.start();
		}

		public void pause() {
			isRunning = false;
			while (true) {
				try {
					ourThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
			ourThread = null;
			DLog("GFXSurface onPause");
		}

		public void resume() {
			isRunning = true;
			ourThread = new Thread(this);
			ourThread.start();
			DLog("GFXSurface onResume");
		}

		private void loadData(boolean isloadchk, int daarray[]) {
			SQLiteDatabase db;
			int i;
			DLog("loadData Method");

			db = openOrCreateDatabase("MyDB", MODE_PRIVATE, null);
			db.execSQL("CREATE TABLE IF NOT EXISTS "+selectedTable+" (Datum TEXT(40), Temp1S INT(4),Temp2S INT(4),Temp3S INT(4));");

//			String ts;         
//			if (loadedSensor.equals("tempZ")) ts = "Temp3S";         // tempZ
//			else if (loadedSensor.equals("tempP")) ts = "Temp2S";    // tempP
//			else ts = "Temp1S";                                      // tempS
				
			Cursor cur = db.rawQuery("SELECT * FROM "+selectedTable, null);
			if (cur != null && cur.getCount() > 0) {
				i = 0;
				dataArrayS = new int[cur.getCount()];   //initialize array
				dataArrayP = new int[cur.getCount()];   //initialize array
				dataArrayZ = new int[cur.getCount()];   //initialize array
				
				cur.moveToFirst();
				do {
					dataArrayS[i] = cur.getInt(1);    // load data for sensor tempS
					dataArrayP[i] = cur.getInt(2);    // load data for sensor tempP
					dataArrayZ[i] = cur.getInt(3);    // load data for sensor tempZ
					//dataArrayZ[i] = cur.getInt(cur.getColumnIndex("Temp3S"));    // load data for sensor tempZ
					i++;
				} while (cur.moveToNext());
				isdataLoaded = true;
			} else {
				isdataLoaded = false; // there is no data
			}

			db.close();
		}

		@Override
		public void run() {
			while (isRunning) {
				if (!ourHolder.getSurface().isValid())
					continue;
				mCanvas = ourHolder.lockCanvas();

				if (isdataLoaded) {
					statusBarText = "table: " + selectedTable;
					if (dataArrayS != null) // just in case Check
						DrawSurface();
				} else {
					loadData(isdataLoaded, dataArrayS); // try to load data
					if (isdataLoaded) {
						statusBarText = "table: " + selectedTable;
						if (dataArrayS != null) // just in case Check
							DrawSurface();
					} else {
						statusBarText = "There is no data";
					}

				}

				ourHolder.unlockCanvasAndPost(mCanvas);
				// isRunning = false;
			}
		}

		public void setMaxValue(int max) {
			maxValue = max;
			mScale = -(mYoff * (1.0f / maxValue));
		}

		public int[] getMinMax(int[] array) {
			int min = Integer.MAX_VALUE; // or -1
			int max = Integer.MIN_VALUE; // or -1
			for (int i : array) { // if you need to know the index, use int
									// (i=0;i<array.length;i++) instead
				if (i < min)
					min = i; // or if(min == -1 || array[i] < array[min]) min =
								// i;
				if (i > max)
					max = i; // or if(max == -1 || array[i] > array[max]) max =
								// i;
			}
			return new int[] { min, max };
		}

		public void drawGrid() {
			// final Paint paint = mPaint;
			int gYColor = Color.argb(128, 128, 128, 128); // grid color
			int gXColor = Color.argb(200, 0, 0, 240); // grid color
			mPaint.setAntiAlias(true);
			int gY;

			// X os Line
			mPaint.setColor(gXColor);
			mPaint.setStrokeWidth(3);
			mCanvas.drawLine(mLb, mYoff, mW + mLb - mRb, mYoff, mPaint);

			// Y os Line
			mPaint.setColor(gXColor);
			mPaint.setStrokeWidth(3);
			mCanvas.drawLine(mLb, mYoff, mLb, mTb, mPaint);

			// Y lines
			mPaint.setColor(gYColor);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeWidth(1);
			mPaint.setTextSize(text_size_A);

			float xsc = maxValue / 700;
			int xsci = (int) xsc;
			float divx = mYoff / 7;
			for (int i = 0; i < 8; i++) {
				gY = (int) (mYoff - i * divx);
				mPaint.setColor(gYColor);
				mCanvas.drawLine(mLb - 10, gY, mW + mLb - mRb, gY, mPaint);
				mPaint.setColor(0xFF000000);
				mCanvas.drawText("" + i * xsci, 10, gY, mPaint);
			}
		}

		private void init() {
			mLb = getResources().getDimension(R.dimen.border_left); 	// 50 left Border
			mTb = getResources().getDimension(R.dimen.border_top); 		// 10 top Border
			mBb = getResources().getDimension(R.dimen.border_bottom); 	// 55 Bottom Border
			mRb = getResources().getDimension(R.dimen.border_right); 	// 5 Right Border

			mW = mCanvas.getWidth() - mLb - mRb; // graph draw area - Width
			mH = mCanvas.getHeight() - mTb - mBb; // graph draw area - Height
			mYoff = mH;

//			if (loadedSensor.equals("tempP")) maxValue = 14000;
//			else maxValue = 3500; 
			
			maxValue = 8400;     //@@@ to-do - set
			
			mScale = (mYoff * (1.0f / maxValue));
		}

		public void DrawScrool(int arraySize, int numberofdots) {
			float pAx, pAy, pBx, pBy;
			float scrlRatio;
			float handleW;
			float scroolW;
			float rX;

			pAx = mLb;
			pAy = mH + 5;
			pBx = mLb + mW - 5;
			pBy = mH + 50;
			//DLog("pAx: " + pAx + " pAy: " + pAy + " pBx: " + pBx + " pBy: " + pBy );
			scrlRatio = (float) numberofdots / arraySize;

			// ScroolBar Area
			scroolW = pBx - pAx;
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setColor(Color.GRAY);
			mCanvas.drawRect(pAx, pAy, pBx, pBy, mPaint);

			// ScroolBar - Handle Area
			if (scrlRatio > 1)
				scrlRatio = 1;
			pBx = mLb + (mW * scrlRatio) - 5 - 2;
			pBy = pBy - 2;
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setColor(Color.LTGRAY);
			pAx = pAx - 2;
			handleW = pBx - pAx;
			rX = scroolW - handleW;
			pAx = pAx + (rX * startpos / startposmax);
			pBx = pAx + handleW;

			mCanvas.drawRect(pAx, pAy + 2, pBx, pBy, mPaint);

		}

		// ***********************************************************************************
		// ***********************************************************************************
		public void DrawSurface() {
			float oldX, oldY, newX, newY;
			int dps; // dots per screen
			int nod; // number of dots
			int endpos;
			int[] minmaxtemp = new int[2];
			int arr_len;

			int tempS_color = 0xFFFF4040; 
			int tempP_color = 0xFF1E90FF;
			int tempZ_color = 0xFF66CD00;

			arr_len = dataArrayS.length;
			
			// get min/max temp. of data array
			minmaxtemp = getMinMax(dataArrayP);

			init(); // initialization: mLb,mTb,mBb,mRb mW,mH
					// mYoff,maxValue,mScale

			newX = mLb;
			newY = 0;

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setTextSize(text_size_A); // default size

			mCanvas.drawColor(0xFFE0E0E0); // Background Color FFF0F0F0

			mPaint.setStyle(Paint.Style.FILL); // Graph Area
			mPaint.setColor(Color.WHITE);
			mCanvas.drawRect(mLb, mTb, mW + mLb - mRb, mH, mPaint);

			drawGrid();

			// OnTouch Event controls: startpos, mXs

			dps = (int) mW / mXs; // dots per screen
			// default startpos = 1 od 1 do -> da.length-nod
			nod = dps + 1; // sa scroolenjem
			if (arr_len <= dps) { // bez scroolanja
				nod = arr_len;
				startpos = 1; // 1-1 = 0
			}
			DrawScrool(arr_len, dps);

			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setStrokeWidth(3);
			
			//********************************************
			// Graph from sensor: tempS
			//********************************************
			startposmax = arr_len - dps;
			oldX = mLb;
			if (((startpos - 1) > arr_len) || (startpos < 1)) {
				DLog("Error by Duby - startpos-1=" + (startpos - 1));
				startpos = 1;
			}
			newX = mLb;
			newY = 0;
			
			oldY = mYoff - (dataArrayS[startpos - 1] * mScale);
			endpos = nod + startpos - 1;
			if (endpos > arr_len) {
				DLog("Error by Duby - endpos=" + endpos);
				endpos = 10;
			}
			for (int i = startpos; i < endpos; i++) {
				newX = newX + mXs;
				newY = mYoff - (dataArrayS[i] * mScale);
				mPaint.setColor(tempS_color); // graph line color
				mCanvas.drawLine(oldX, oldY, newX, newY, mPaint);
				mPaint.setColor(0xFF090909); // dots color
				mCanvas.drawCircle(newX, newY, 3, mPaint);
				oldX = newX;
				oldY = newY;
			}
			//********************************************
			// Graph from sensor: tempP
			//********************************************
			startposmax = arr_len - dps;
			oldX = mLb;
			if (((startpos - 1) > arr_len) || (startpos < 1)) {
				DLog("Error by Duby - startpos-1=" + (startpos - 1));
				startpos = 1;
			}
			newX = mLb;
			newY = 0;
			
			oldY = mYoff - (dataArrayP[startpos - 1] * mScale);
			endpos = nod + startpos - 1;
			if (endpos > arr_len) {
				DLog("Error by Duby - endpos=" + endpos);
				endpos = 10;
			}
			for (int i = startpos; i < endpos; i++) {
				newX = newX + mXs;
				newY = mYoff - (dataArrayP[i] * mScale);
				mPaint.setColor(tempP_color); // graph line color
				mCanvas.drawLine(oldX, oldY, newX, newY, mPaint);
				mPaint.setColor(0xFF090909); // dots color
				mCanvas.drawCircle(newX, newY, 3, mPaint);
				oldX = newX;
				oldY = newY;
			}
			//********************************************
			// Graph from sensor: tempZ
			//********************************************
			startposmax = arr_len - dps;
			oldX = mLb;
			if (((startpos - 1) > arr_len) || (startpos < 1)) {
				DLog("Error by Duby - startpos-1=" + (startpos - 1));
				startpos = 1;
			}
			newX = mLb;
			newY = 0;
			
			oldY = mYoff - (dataArrayZ[startpos - 1] * mScale);
			endpos = nod + startpos - 1;
			if (endpos > arr_len) {
				DLog("Error by Duby - endpos=" + endpos);
				endpos = 10;
			}
			for (int i = startpos; i < endpos; i++) {
				newX = newX + mXs;
				newY = mYoff - (dataArrayZ[i] * mScale);
				mPaint.setColor(tempZ_color); // graph line color
				mCanvas.drawLine(oldX, oldY, newX, newY, mPaint);
				mPaint.setColor(0xFF090909); // dots color
				mCanvas.drawCircle(newX, newY, 3, mPaint);
				oldX = newX;
				oldY = newY;
			}

			// status bar text
			// ****************************************************************************
			int yOff = mCanvas.getHeight() - 20;
			mPaint.setColor(0xFF000000);

			int letterSize = 30;
			int cursorPos = 20;
			int space = 20;
			String text = "X div= " + mXs + "px";
			int textPx = text.length() * letterSize;
			mCanvas.drawText(text, cursorPos, yOff, mPaint);
			cursorPos = cursorPos + textPx + space;

			text = "min= " + minmaxtemp[0] / 100;
			textPx = text.length() * letterSize;
			mCanvas.drawText(text, cursorPos, yOff, mPaint);  //300
			cursorPos = cursorPos + textPx + space;

			text = "max= " + minmaxtemp[1] / 100;
			textPx = text.length() * letterSize;
			mCanvas.drawText(text, cursorPos, yOff, mPaint);  //500
			cursorPos = cursorPos + textPx + space + 10;

			text = "data count= " + arr_len;
			textPx = text.length() * letterSize;
			mCanvas.drawText(text, cursorPos, yOff, mPaint);       //700
			cursorPos = cursorPos + textPx + space + 30;

			mPaint.setColor(0xFF0000F0); // status text color
			textPx = statusBarText.length() * letterSize;
			mCanvas.drawText(statusBarText, cursorPos, yOff, mPaint);    // loaded Table
			cursorPos = cursorPos + textPx + space + 120;

			mPaint.setTextSize(text_size_B); // default size
			mPaint.setColor(tempS_color); // sensor tempS color
			mCanvas.drawText("S", cursorPos, yOff, mPaint); // 'S' with matching color
			mPaint.setColor(tempP_color); // sensor tempS color
			mCanvas.drawText("P", cursorPos + 50, yOff, mPaint); // 'P' with matching color
			mPaint.setColor(tempZ_color); // sensor tempS color
			mCanvas.drawText("Z", cursorPos + 100, yOff, mPaint); // 'Z' with matching color

			// title bar text
			mPaint.setTextSize(text_size_C);
			mPaint.setColor(0xFFFF0F00); // text color
			mCanvas.drawText("Graph from data", 900, 80, mPaint);

		}

	} // end of GFXSurface class


	//*******************************************************************************************************
	private void DLog(String msg) {
		String className = this.getClass().getSimpleName();
		Log.d("DTag", className + ": " + msg);    //className + " " + hashCode() + ":  " + msg
	}
}
