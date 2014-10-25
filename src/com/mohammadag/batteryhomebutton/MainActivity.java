package com.mohammadag.batteryhomebutton;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private BatteryDrawable mDrawable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDrawable = new BatteryDrawable(((ImageView) findViewById(R.id.imageView1)));
		((ImageView) findViewById(R.id.imageView1)).setImageDrawable(mDrawable);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			mDrawable.setBatteryLevel(level);
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter iF = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(receiver, iF);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}
}
