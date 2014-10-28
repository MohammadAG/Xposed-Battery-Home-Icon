package com.mohammadag.batteryhomebutton;

import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
	private BatteryDrawable mBatteryDrawable;

	private boolean mBroadcastRegistered;

	private XSharedPreferences mPrefs;

	
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		mPrefs = new XSharedPreferences("com.mohammadag.batteryhomebutton");
		boolean isLGDevice = Build.MANUFACTURER.toLowerCase(Locale.getDefault()).equals("lge");
		String NavigationBarViewClassName="com.android.systemui.statusbar.phone.NavigationBarView";
		if (isLGDevice) {
			NavigationBarViewClassName = "com.android.systemui.statusbar.phone.LGNavigationBarView";
		}
		Class<?> NavigationBarView = XposedHelpers.findClass(NavigationBarViewClassName, lpparam.classLoader);

		XC_MethodHook hook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					ImageView imageView = (ImageView) XposedHelpers.callMethod(param.thisObject, "getHomeButton");
					createBatteryIfNeeded(imageView);
					imageView.setImageDrawable(mBatteryDrawable);
				} catch (NoSuchMethodError e) {
					// ROMs with editable nav bar
					try {
						Class<?> NavBarEditior = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavbarEditor",
								param.thisObject.getClass().getClassLoader());
						ImageView imageView = null;
						// CyanogenMod
						try {
							imageView = (ImageView) XposedHelpers.callMethod(param.thisObject, "findButton",
									XposedHelpers.getStaticObjectField(NavBarEditior, "NAVBAR_HOME"));
						} catch (NoSuchMethodError e2) {
							// OmniROM
							try {
								View mCurrentView = (View) XposedHelpers.getObjectField(param.thisObject, "mCurrentView");
								imageView = (ImageView) mCurrentView.findViewWithTag(
										XposedHelpers.getStaticObjectField(NavBarEditior, "NAVBAR_HOME"));
							} catch (Exception e3) {
								e3.printStackTrace();
							}
						}

						if (imageView != null) {
							createBatteryIfNeeded(imageView);
							imageView.setImageDrawable(mBatteryDrawable);
						}
					} catch (Exception e1) {

					}
				}
			}
		};

		XposedHelpers.findAndHookMethod(NavigationBarView, "onFinishInflate", hook);
		XposedHelpers.findAndHookMethod(NavigationBarView, "reorient", hook);
		if (!isLGDevice) {
			XposedHelpers.findAndHookMethod(NavigationBarView, "getIcons", Resources.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					try {
						XposedHelpers.setObjectField(param.thisObject, "mHomeIcon", mBatteryDrawable);
						XposedHelpers.setObjectField(param.thisObject, "mHomeLandIcon", mBatteryDrawable);
					} catch (NoSuchFieldError e) {
	
					}
				}
			});
		}
		XposedBridge.hookAllConstructors(NavigationBarView, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mBroadcastRegistered)
					return;

				View v = (View) param.thisObject;

				IntentFilter iF = new IntentFilter();
				iF.addAction(Intent.ACTION_BATTERY_CHANGED);
				iF.addAction(Intent.ACTION_POWER_CONNECTED);
				iF.addAction(Intent.ACTION_POWER_DISCONNECTED);
				iF.addAction(Intent.ACTION_SCREEN_ON);
				iF.addAction(Intent.ACTION_SCREEN_OFF);
				iF.addAction(PreferencesActivity.INTENT_SETTINGS_CHANGED);
				v.getContext().registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent intent) {
						if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
							// No idea what the f this is, but AOSP does it so it must be awesome
							int level = (int) (100f
									* intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
									/ intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
							if (mBatteryDrawable != null)
								mBatteryDrawable.setBatteryLevel(level);
						} else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
							if (mBatteryDrawable != null)
								mBatteryDrawable.setBatteryCharging(true);
						} else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
							if (mBatteryDrawable != null)
								mBatteryDrawable.setBatteryCharging(false);
						} else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
							if (mBatteryDrawable != null)
								mBatteryDrawable.setScreenOn(true);
						} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
							if (mBatteryDrawable != null)
								mBatteryDrawable.setScreenOn(false);
						} else if (PreferencesActivity.INTENT_SETTINGS_CHANGED.equals(intent.getAction())) {
							if (mBatteryDrawable != null) {
								reloadSettings();
							}
						}
					}
				}, iF);

				mBroadcastRegistered = true;
			}
		});

		if (isLGDevice) {
			try {
				Class<?> LGHomeButton = XposedHelpers.findClass("com.lge.navigationbar.HomeButton", lpparam.classLoader);
				XposedHelpers.findAndHookConstructor(LGHomeButton, Context.class, AttributeSet.class, int.class, boolean.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						ImageView view = (ImageView) param.thisObject;
						createBatteryIfNeeded(view);

						view.setImageDrawable(mBatteryDrawable);
					}
				});
			} catch (Throwable t) {
				XposedBridge.log("BatteryHomeIcon: Failed to apply LG hook: " + t.getMessage());
				t.printStackTrace();
			}
			
			try {
				Class<?> NavigationThemeResource = XposedHelpers.findClass("com.lge.navigationbar.NavigationThemeResource", lpparam.classLoader);
				XposedHelpers.findAndHookMethod(NavigationThemeResource, "getThemeResource", View.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						try {
							ImageView v = (ImageView)param.args[0];
							if (v.getClass().getSimpleName().equalsIgnoreCase("HomeButton")) {
								createBatteryIfNeeded(v);
								param.setResult(mBatteryDrawable);
							}							
						} catch (NoSuchFieldError e) {
						}
					}
				});
			} catch (Throwable t) {
				XposedBridge.log("BatteryHomeIcon: Failed to apply LG hook: " + t.getMessage());
				t.printStackTrace();
			}
		}
	}

	private void reloadSettings() {
		mPrefs.reload();
		int padding = (int) (mPrefs.getFloat("padding", 0.7F) * 50);
		int lPadding = (int) (mPrefs.getFloat("padding_landscape", 0.5F) * 50);
		int width = (int) (mPrefs.getFloat("stroke_width", 0.1F) * 50);
		mBatteryDrawable.setChargingAnimationEnabled(mPrefs.getBoolean("charging_animation", true));
		mBatteryDrawable.setPercentageEnabled(mPrefs.getBoolean("battery_percentage", true));
		mBatteryDrawable.setPadding(padding, lPadding);
		mBatteryDrawable.setStrokeWidth(width);
	}

	private void createBatteryIfNeeded(ImageView view) {
		if (mBatteryDrawable == null) {
			mBatteryDrawable = new BatteryDrawable(view);
			reloadSettings();
		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (!"com.android.systemui".equals(resparam.packageName))
			return;

		XSharedPreferences prefs = new XSharedPreferences("com.mohammadag.batteryhomebutton");
		if (!prefs.getBoolean("hide_battery", false))
			return;

		resparam.res.hookLayout(resparam.res.getIdentifier("super_status_bar", "layout",
				"com.android.systemui"), new XC_LayoutInflated() {
			@Override
			public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
				View view = liparam.view.findViewById(liparam.res.getIdentifier("battery",
						"id", "com.android.systemui"));

				view.setVisibility(View.GONE);
			}
		});
	}
}
