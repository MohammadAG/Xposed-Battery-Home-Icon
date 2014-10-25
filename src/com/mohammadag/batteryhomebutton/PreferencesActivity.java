package com.mohammadag.batteryhomebutton;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String INTENT_SETTINGS_CHANGED = "com.mohammadag.batteryhomebutton.SETTINGS_CHANGED";
	private static final String URL_MY_MODULES = "http://repo.xposed.info/users/mohammadag";
	private static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return super.getSharedPreferences(name, Context.MODE_WORLD_READABLE);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		initCopyright();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				sendBroadcast(new Intent(INTENT_SETTINGS_CHANGED));
			}
		}, 1000);
	}

	@SuppressWarnings("deprecation")
	private void initCopyright() {
		Preference copyrightPreference = findPreference("copyright_key");
		copyrightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
				builder.setTitle("")
				.setItems(R.array.my_apps, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = null;
						Intent intent = new Intent(Intent.ACTION_VIEW);
						switch (which) {
						case 0:
							uri = Uri.parse(URL_MY_APPS);
							intent.setPackage("com.android.vending");
							break;
						case 1:
							uri = Uri.parse(URL_MY_MODULES);
							break;
						}
						try {
							startActivity(intent.setData(uri));
						} catch (ActivityNotFoundException e) {
							Toast.makeText(PreferencesActivity.this, "Play Store not found", Toast.LENGTH_SHORT).show();
						}
					}
				});
				builder.create().show();
				return false;
			}
		});
	}
}
