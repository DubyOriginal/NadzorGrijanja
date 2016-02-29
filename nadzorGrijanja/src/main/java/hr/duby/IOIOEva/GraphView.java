package hr.duby.IOIOEva;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GraphView extends View {

	private Bitmap mBitmap;
	private Paint mPaint = new Paint();
	private Canvas mCanvas = new Canvas();

	private float mSpeed;
	private float mLastX;
	private float mScale;
	private float mLastValue = 300;
	private float mYOffset;
	private float mWidth;
	private float maxValue = 1024f;

	public static float newX;

	int gW, gH;

	public GraphView(Context context) {
		super(context);
		init();
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeWidth(3);
		// paint.setAntiAlias(true);

		gW = getWidth();
		gH = getHeight();
	}

	public void drawGrid() {
		// final Paint paint = mPaint;
		int gYColor = Color.argb(128, 128, 128, 128); // grid color
		int gXColor = Color.argb(150, 0, 0, 240); // grid color
		mPaint.setAntiAlias(true);
		float gY;

		// X os Line
		mPaint.setColor(gXColor);
		mPaint.setStrokeWidth(3);
		mCanvas.drawLine(4, 4, 4, mYOffset - 2, mPaint);

		// Y os Line
		mPaint.setColor(gXColor);
		mPaint.setStrokeWidth(3);
		mCanvas.drawLine(4, mYOffset - 4, mWidth - 4, mYOffset - 4, mPaint);

		// Y lines
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeWidth(1);
		mPaint.setTextSize(17);

		float xsc = maxValue / 7;
		float divx = mYOffset / 7;
		for (int i = 1; i < 8; i++) {
			gY = (mYOffset - i * divx);
			mPaint.setColor(gYColor);
			mCanvas.drawLine(0, gY, mWidth, gY, mPaint);
			mPaint.setColor(0xFF000000);
			mCanvas.drawText("" + i * xsc, 10, gY + 1, mPaint);
		}

		init(); // setup paint properties

		// invalidate();
	}

	public void setMaxValue(int max) {
		maxValue = max;
		mScale = -(mYOffset * (1.0f / maxValue));
	}

	public void setSpeed(float speed) {
		mSpeed = speed;
	}

	public void clearCanvas() {
		mCanvas.drawColor(0xFFF0F0F0);
		mLastX = 0;
		invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		mCanvas.setBitmap(mBitmap);
		mCanvas.drawColor(0xFFFFFFFF);
		mYOffset = h;
		mScale = -(mYOffset * (1.0f / maxValue));
		mWidth = w;
		mLastX = mWidth;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void addDataPoint(float value) {
		final Paint paint = mPaint;
		newX = mLastX + mSpeed;
		final float v = mYOffset + (value * mScale);

		paint.setColor(0xFFFF34B3);
		mCanvas.drawLine(mLastX, mLastValue, newX, v, paint);
		mPaint.setColor(0xFF000000);
		mCanvas.drawCircle(newX, v, 3, mPaint);

		// status bar text
		// ****************************************************************************
		mPaint.setColor(0xFFF0F0F0); // redraw old text
		mCanvas.drawRect(10, mCanvas.getHeight() - 35, mCanvas.getWidth() - 10,
				mCanvas.getHeight() - 8, mPaint);

		mPaint.setTextSize(20);
		String ts = "";
		if (RealTimeGraph.sampling) {
			ts = "running...";
		} else
			ts = "stoped";
		mPaint.setColor(0xFFFF1010);
		mCanvas.drawText(ts, 20, mCanvas.getHeight() - 13, mPaint);

		mPaint.setTextSize(17);
		int tsx = 500; // text start X position
		mPaint.setColor(0xFF000000);
		ts = String.format("%.0f", newX);
		mCanvas.drawText("X= " + ts, tsx, mCanvas.getHeight() - 10, mPaint);
		ts = String.format("%.2f", value);
		mCanvas.drawText("Y= " + ts, tsx + 60, mCanvas.getHeight() - 10, mPaint);

		mLastValue = v;
		mLastX += mSpeed;

		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		synchronized (this) {
			if (mBitmap != null) {
				if (mLastX >= mWidth) {
					mLastX = 0;
					final Canvas cavas = mCanvas;
					cavas.drawColor(0xFFFFFFFF);
					mPaint.setColor(0xFF777777);
					cavas.drawLine(0, mYOffset, mWidth, mYOffset, mPaint);
				}
				if (mLastX == 0) {
					drawGrid();
				}
				canvas.drawBitmap(mBitmap, 0, 0, null);
			}
		}
	}
}
