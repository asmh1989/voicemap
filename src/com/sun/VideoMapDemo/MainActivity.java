package com.sun.VideoMapDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.support.v4.widget.DrawerLayout;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.search.*;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import java.io.File;
import java.lang.reflect.Field;


public class MainActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks{

	private static final String TAG = "SUN";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */

	private Context mContext;
	private CharSequence mTitle;
	private boolean mLocationInit;
	private Button mBtnSend;
	private TextView mBtnRcd;
	private RelativeLayout mBottom;
	private ImageView chatting_mode_btn;
	private EditText mEditTextContent;
	public boolean btn_vocie;
	private ImageView volume;
	private View rcChat_popup;
	private ImageView img1;
	private ImageView sc_img1;
	private LinearLayout del_re;
	private LinearLayout voice_rcd_hint_rcding;
	private LinearLayout voice_rcd_hint_loading;
	private LinearLayout voice_rcd_hint_tooshort;
	private SoundMeter mRecordManager;

	private enum E_BUTTON_TYPE {
		LOC,
		COMPASS,
		FOLLOW
	}

	private E_BUTTON_TYPE mCurBtnType;

	// 定位相关
	LocationClient mLocClient;
	LocationData locData = null;
	public MyLocationListenner myListener = new MyLocationListenner();

	//定位图层
	locationOverlay myLocationOverlay = null;
	//弹出泡泡图层
	private PopupOverlay pop  = null;//弹出泡泡图层，浏览节点时使用
	private TextView  popupText = null;//泡泡view
	private View viewCache = null;

	//地图相关，使用继承MapView的MyLocationMapView目的是重写touch事件实现泡泡处理
	//如果不处理touch事件，则无需继承，直接使用MapView即可
	private MyLocationMapView mMapView = null;	// 地图View
	private MapController mMapController = null;

	MKSearch mSearch = null;	// 搜索模块，也可去掉地图模块独立使用


	//UI相关
	RadioGroup.OnCheckedChangeListener radioButtonListener = null;
	Button requestLocButton = null;
	boolean isRequest = false;//是否手动触发请求定位
	boolean isFirstLoc = true;//是否首次定位

	private String curAddr = "正在获取....";
	private boolean traffic = false;
	private File mVoiceDir ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * 使用地图sdk前需先初始化BMapManager.
		 * BMapManager是全局的，可为多个MapView共用，它需要地图模块创建前创建，
		 * 并在地图地图模块销毁后销毁，只要还有地图模块在使用，BMapManager就不应该销毁
		 */
		SmApplication app = (SmApplication)this.getApplication();
		if (app.mBMapManager == null) {
			app.mBMapManager = new BMapManager(getApplicationContext());
			/**
			 * 如果BMapManager没有初始化则初始化BMapManager
			 */
			app.mBMapManager.init(SmApplication.strKey,new SmApplication.MyGeneralListener());
		}

		setContentView(R.layout.activity_main);

		mContext = this;

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				                            getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				                               (DrawerLayout) findViewById(R.id.drawer_layout));

		requestLocButton = (Button)findViewById(R.id.button1);
		mCurBtnType = E_BUTTON_TYPE.LOC;
		View.OnClickListener btnClickListener = new View.OnClickListener() {
			public void onClick(View v) {
				switch (mCurBtnType) {
					case LOC:
						//手动定位请求
						requestLocClick();
						break;
					case COMPASS:
						myLocationOverlay.setLocationMode(MyLocationOverlay.LocationMode.NORMAL);
						requestLocButton.setText("定位");
						mCurBtnType = E_BUTTON_TYPE.LOC;
						break;
					case FOLLOW:
						myLocationOverlay.setLocationMode(MyLocationOverlay.LocationMode.COMPASS);
						requestLocButton.setText("罗盘");
						mCurBtnType = E_BUTTON_TYPE.COMPASS;
						break;
				}
			}
		};

		requestLocButton.setOnClickListener(btnClickListener);

		//地图初始化
		mMapView = (MyLocationMapView)findViewById(R.id.bmapView);
		mMapController = mMapView.getController();
		mMapView.getController().setZoom(15);
		mMapView.getController().enableClick(true);
		mMapView.setBuiltInZoomControls(false);
		//创建 弹出泡泡图层
		createPaopao();

		//定位初始化
		mLocClient = new LocationClient(getApplicationContext());
		locData = new LocationData();

		try {
			LocationClientOption option = new LocationClientOption();
			option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
			option.setCoorType("bd09ll");//返回的定位结果是百度经纬度，默认值gcj02
			option.setScanSpan(5000);//设置发起定位请求的间隔时间为5000ms
			option.setIsNeedAddress(true);//返回的定位结果包含地址信息
			option.setNeedDeviceDirect(true);//返回的定位结果包含手机机头的方向
			mLocClient.setLocOption(option);
			mLocationInit = true;
		} catch (Exception e){
			e.printStackTrace();
			mLocationInit = false;
			Log.e(TAG, "ERR:"+e.getMessage());
		}
		if (mLocationInit) {
			mLocClient.start();
		} else {
			Toast.makeText(this, "定位参数设置失败", Toast.LENGTH_SHORT).show();
			return;
		}



		mLocClient.registerLocationListener(myListener);

		if (mLocClient != null && mLocClient.isStarted()){
			mLocClient.requestLocation();
		}
		else
			Log.d(TAG, "locClient is null or not started");

		//定位图层初始化
		myLocationOverlay = new locationOverlay(mMapView);
		//设置定位数据
		myLocationOverlay.setData(locData);
		//添加定位图层
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		//修改定位数据后刷新图层生效
		mMapView.refresh();


//		// 设置action bar 的 navigation mode
//		ActionBar actionBar  = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//		// 添加 action bar 的 tabs
//		ActionBar.Tab normalTab = actionBar.newTab().setText("平面图");
//		ActionBar.Tab statelliteTab = actionBar.newTab().setText("卫星图");
//
//
//		// 对 tabs 设置监听事件
//		normalTab.setTabListener(new MyTabListener(1));
//		statelliteTab.setTabListener(new MyTabListener(2));
//
//		// 最后把 tabs 加入监听事件
//		actionBar.addTab(normalTab);
//		actionBar.addTab(statelliteTab);

		initViews();

	}


	private void initViews() {
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mBtnRcd = (TextView) findViewById(R.id.btn_rcd);
		mBottom = (RelativeLayout) findViewById(R.id.btn_bottom);
		chatting_mode_btn = (ImageView) this.findViewById(R.id.ivPopUp);
		volume = (ImageView) this.findViewById(R.id.volume);
		rcChat_popup = this.findViewById(R.id.rcChat_popup);
		img1 = (ImageView) this.findViewById(R.id.img1);
		sc_img1 = (ImageView) this.findViewById(R.id.sc_img1);
		del_re = (LinearLayout) this.findViewById(R.id.del_re);
		voice_rcd_hint_rcding = (LinearLayout) findViewById(R.id.voice_rcd_hint_rcding);
		voice_rcd_hint_loading = (LinearLayout) findViewById(R.id.voice_rcd_hint_loading);
		voice_rcd_hint_tooshort = (LinearLayout) findViewById(R.id.voice_rcd_hint_tooshort);
		mRecordManager = new SoundMeter();
		mEditTextContent = (EditText) findViewById(R.id.et_sendmessage);

		//语音文字切换按钮
		chatting_mode_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setToSpeaks();
			}
		});

		mBtnRcd.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				//按下语音录制按钮时返回false执行父类OnTouch
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					flag = 1;
				}
				if(flag < 3) {
					onTouchEvent(event);
					return true;
				}else {
					return false;
				}
			}
		});

		setToSpeaks();

	}

	private void setToSpeaks(){
		if (btn_vocie) {
			mBtnRcd.setVisibility(View.GONE);
			mBottom.setVisibility(View.VISIBLE);
			btn_vocie = false;
			chatting_mode_btn.setImageResource(R.drawable.chatting_setmode_msg_btn);
		} else {
			mBtnRcd.setVisibility(View.VISIBLE);
			mBottom.setVisibility(View.GONE);
			chatting_mode_btn.setImageResource(R.drawable.chatting_setmode_voice_btn);
			btn_vocie = true;
		}
	}

	private int flag = 1;
	private Handler mHandler = new Handler();
	private boolean isShosrt = false;
	private long startVoiceT, endVoiceT;
	private String voiceName;


	//按下语音录制按钮时
	public boolean onTouchEvent(MotionEvent event) {

		if (!Environment.getExternalStorageDirectory().exists()) {
			showToast("No SDCard");
			return false;
		} else if(mVoiceDir == null){
			mVoiceDir = new File(android.os.Environment.getExternalStorageDirectory()+"/voidemapDemo");
			if(!mVoiceDir.exists()){
				mVoiceDir.mkdir();
			}
		}

		if (btn_vocie) {
			int[] location = new int[2];
			mBtnRcd.getLocationInWindow(location); // 获取在当前窗口内的绝对坐标
			int btn_rc_Y = location[1];
			int btn_rc_X = location[0];
			int[] del_location = new int[2];
			del_re.getLocationInWindow(del_location);
			int del_Y = del_location[1];
			int del_x = del_location[0];
			int cur_X = (int)event.getX();
			int cur_Y = (int)event.getY()+btn_rc_Y;
//			Log.d(TAG, "cur_X="+cur_X+" cur_Y="+cur_Y+" btn_X="+btn_rc_X+" btn_Y="+btn_rc_Y+" del_X="+del_x+" del_Y="+del_Y);
			if (event.getAction() == MotionEvent.ACTION_DOWN && flag == 1) {
				if (cur_Y > btn_rc_Y && cur_X > btn_rc_X) {//判断手势按下的位置是否是语音录制按钮的范围内
					mBtnRcd.setBackgroundResource(R.drawable.voice_rcd_btn_pressed);
					rcChat_popup.setVisibility(View.VISIBLE);
					voice_rcd_hint_loading.setVisibility(View.VISIBLE);
					voice_rcd_hint_rcding.setVisibility(View.GONE);
					voice_rcd_hint_tooshort.setVisibility(View.GONE);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (!isShosrt) {
								voice_rcd_hint_loading.setVisibility(View.GONE);
								voice_rcd_hint_rcding
										.setVisibility(View.VISIBLE);
							}
						}
					}, 300);
					img1.setVisibility(View.VISIBLE);
					del_re.setVisibility(View.GONE);
					startVoiceT = SystemClock.uptimeMillis();
					voiceName =mVoiceDir.getAbsolutePath()+'/'+startVoiceT + ".amr";
					start(voiceName);
					flag = 2;
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP && flag == 2) {//松开手势时执行录制完成
				mHandler.removeCallbacks(mLimitRecordTimeTask);
				mBtnRcd.setBackgroundResource(R.drawable.voice_rcd_btn_nor);
				if (cur_Y >= del_Y
						    && cur_Y <= del_Y + del_re.getHeight()
						    && cur_X >= del_x
						    && cur_X <= del_x + del_re.getWidth()) {
					rcChat_popup.setVisibility(View.GONE);
					img1.setVisibility(View.VISIBLE);
					del_re.setVisibility(View.GONE);
					stop();
					flag = 1;
					deleteCacheVideo();
					showToast("录音已取消");
				} else {
					voice_rcd_hint_rcding.setVisibility(View.GONE);
					stop();
					endVoiceT = SystemClock.uptimeMillis();
					flag = 1;
					int time = (int) ((endVoiceT - startVoiceT) / 1000);
					if (time < 1) {
						isShosrt = true;
						voice_rcd_hint_loading.setVisibility(View.GONE);
						voice_rcd_hint_rcding.setVisibility(View.GONE);
						voice_rcd_hint_tooshort.setVisibility(View.VISIBLE);
						mHandler.postDelayed(new Runnable() {
							public void run() {
								voice_rcd_hint_tooshort
										.setVisibility(View.GONE);
								rcChat_popup.setVisibility(View.GONE);
								isShosrt = false;
							}
						}, 500);
						return false;
					}
					rcChat_popup.setVisibility(View.GONE);
					if (cur_Y > btn_rc_Y && cur_X > btn_rc_X) {
						finishRecord();
					} else {
						deleteCacheVideo();
						showToast("录音已取消");
					}
				}
			}
			if (cur_Y < btn_rc_Y) {//手势按下的位置不在语音录制按钮的范围内
				Animation mLitteAnimation = AnimationUtils.loadAnimation(this, R.anim.cancel_rc);
				Animation mBigAnimation = AnimationUtils.loadAnimation(this,R.anim.cancel_rc2);
				img1.setVisibility(View.GONE);
				del_re.setVisibility(View.VISIBLE);
				del_re.setBackgroundResource(R.drawable.voice_rcd_cancel_bg);
				if (cur_Y >= del_Y
						    && cur_Y <= del_Y + del_re.getHeight()
						    && cur_X >= del_x
						    && cur_X <= del_x + del_re.getWidth()) {
					del_re.setBackgroundResource(R.drawable.voice_rcd_cancel_bg_focused);
					sc_img1.startAnimation(mLitteAnimation);
					sc_img1.startAnimation(mBigAnimation);
				}
			} else {
				img1.setVisibility(View.VISIBLE);
				del_re.setVisibility(View.GONE);
				del_re.setBackgroundResource(0);
			}
		}
		return super.onTouchEvent(event);
	}

	private void finishRecord() {
		AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage("录音已完成,保存在:\n" + voiceName)
				                             .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					                             @Override
					                             public void onClick(DialogInterface dialog, int which) {
						                             deleteCacheVideo();
						                             showToast("录音已删除");
						                             setDialogShow(dialog, true);
						                             mRecordManager.stopPlayback();
					                             }
				                             })
				                             .setPositiveButton("预览", new DialogInterface.OnClickListener() {
					                             @Override
					                             public void onClick(DialogInterface dialog, int which) {
						                             mRecordManager.startPlayback();
						                             setDialogShow(dialog, false);
					                             }
				                             })
				                             .setNeutralButton("发送", new DialogInterface.OnClickListener() {
					                             @Override
					                             public void onClick(DialogInterface dialog, int which) {
						                             showToast("语音已发送....");
						                             setDialogShow(dialog, true);
						                             mRecordManager.stopPlayback();
					                             }
				                             }).create();
		dialog.setCanceledOnTouchOutside(false);

		dialog.show();
	}

	private void setDialogShow(DialogInterface dialog, Boolean isShow){
		try {
			Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, isShow);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void deleteCacheVideo() {
		File file = new File(voiceName);
		if (file.exists()) {
			file.delete();
		}
	}

	private static final int POLL_INTERVAL = 300;
	private static  final int RECORD_VOICE_TIME = 10*1000;

	private Runnable mSleepTask = new Runnable() {
		public void run() {
			stop();
		}
	};
	private Runnable mPollTask = new Runnable() {
		public void run() {
			double amp = mRecordManager.getAmplitude();
			updateDisplay(amp);
			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}
	};

	private Runnable mLimitRecordTimeTask = new Runnable() {
		@Override
		public void run() {
			flag = 3;
			mRecordManager.stop();
			showToast("录音时间已超过最长限制...");
			mBtnRcd.setBackgroundResource(R.drawable.voice_rcd_btn_nor);
			rcChat_popup.setVisibility(View.GONE);
			finishRecord();
		}
	};

	private void start(String name) {
		mRecordManager.start(name);
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		mHandler.postDelayed(mLimitRecordTimeTask, RECORD_VOICE_TIME);
	}

	private void stop() {
		mHandler.removeCallbacks(mSleepTask);
		mHandler.removeCallbacks(mPollTask);
		mRecordManager.stop();
		volume.setImageResource(R.drawable.amp1);
	}

	private void updateDisplay(double signalEMA) {

		switch ((int) signalEMA) {
			case 0:
			case 1:
				volume.setImageResource(R.drawable.amp1);
				break;
			case 2:
			case 3:
				volume.setImageResource(R.drawable.amp2);

				break;
			case 4:
			case 5:
				volume.setImageResource(R.drawable.amp3);
				break;
			case 6:
			case 7:
				volume.setImageResource(R.drawable.amp4);
				break;
			case 8:
			case 9:
				volume.setImageResource(R.drawable.amp5);
				break;
			case 10:
			case 11:
				volume.setImageResource(R.drawable.amp6);
				break;
			default:
				volume.setImageResource(R.drawable.amp7);
				break;
		}
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		//退出时销毁定位
		if (mLocClient != null)
			mLocClient.stop();
		mMapView.destroy();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mMapView.onSaveInstanceState(outState);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mMapView.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * 手动触发一次定位请求
	 */
	public void requestLocClick(){
		isRequest = true;
		mLocClient.requestLocation();
		Toast.makeText(MainActivity.this, "正在定位……", Toast.LENGTH_SHORT).show();
	}
	/**
	 * 修改位置图标
	 * @param marker
	 */
	public void modifyLocationOverlayIcon(Drawable marker){
		//当传入marker为null时，使用默认图标绘制
		myLocationOverlay.setMarker(marker);
		//修改图层，需要刷新MapView生效
		mMapView.refresh();
	}
	/**
	 * 创建弹出泡泡图层
	 */
	public void createPaopao(){
		viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
		popupText =(TextView) viewCache.findViewById(R.id.textcache);
		//泡泡点击响应回调
		PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				Log.v("click", "clickapoapo");
			}
		};
		pop = new PopupOverlay(mMapView,popListener);
		MyLocationMapView.pop = pop;
	}
	/**
	 * 定位SDK监听函数
	 */
	public class MyLocationListenner implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return ;

			StringBuffer sb = new StringBuffer(256);
			sb.append("time : ");
			sb.append(location.getTime());
			sb.append("\nerror code : ");
			sb.append(location.getLocType());
			sb.append("\nlatitude : ");
			sb.append(location.getLatitude());
			sb.append("\nlontitude : ");
			sb.append(location.getLongitude());
			sb.append("\nradius : ");
			sb.append(location.getRadius());
			if (location.getLocType() == BDLocation.TypeGpsLocation){
				sb.append("\nspeed : ");
				sb.append(location.getSpeed());
				sb.append("\nsatellite : ");
				sb.append(location.getSatelliteNumber());
			} else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				curAddr = location.getAddrStr();
			}

//			Log.d(TAG,sb.toString());

			locData.latitude = location.getLatitude();
			locData.longitude = location.getLongitude();
			//如果不显示定位精度圈，将accuracy赋值为0即可
			locData.accuracy = location.getRadius();
			// 此处可以设置 locData的方向信息, 如果定位 SDK 未返回方向信息，用户可以自己实现罗盘功能添加方向信息。
			locData.direction = location.getDerect();
			//更新定位数据
			myLocationOverlay.setData(locData);
			//更新图层数据执行刷新后生效
			mMapView.refresh();
			//是手动触发请求或首次定位时，移动到定位点

			if (isRequest || isFirstLoc){
				//移动地图到定位点
				Log.d("LocationOverlay", "receive location, animate to it");
				mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
				isRequest = false;
				myLocationOverlay.setLocationMode(MyLocationOverlay.LocationMode.FOLLOWING);
				requestLocButton.setText("跟随");
				mCurBtnType = E_BUTTON_TYPE.FOLLOW;
			}
			//首次定位完成
			isFirstLoc = false;
		}

		public void onReceivePoi(BDLocation poiLocation) {
			if (poiLocation == null){
				return ;
			}
		}
	}

	//继承MyLocationOverlay重写dispatchTap实现点击处理
	public class locationOverlay extends MyLocationOverlay{

		public locationOverlay(MapView mapView) {
			super(mapView);
			// TODO Auto-generated constructor stub
		}
		@Override
		protected boolean dispatchTap() {
			// TODO Auto-generated method stub
			//处理点击事件,弹出泡泡
			popupText.setBackgroundResource(R.drawable.popup);
			popupText.setText(curAddr);
			pop.showPopup(BMapUtil.getBitmapFromView(popupText),
					             new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)),
					             8);
			return true;
		}

	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments

		if(mMapView != null) {
			switch (position) {
				case 0:
					mMapView.setSatellite(false);
					break;
				case 1:
					mMapView.setSatellite(true);
					break;
				case 2:
					traffic = !traffic;
					mMapView.setTraffic(traffic);
					mMapView.refresh();
			}
		}

		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
				.commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
			case 1:
				mTitle = getString(R.string.title_section1);
				break;
			case 2:
				mTitle = getString(R.string.title_section2);
				break;
			case 3:
				mTitle = getString(R.string.title_section3);
				break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();

		actionBar.setDisplayShowTitleEnabled(true);

		actionBar.setTitle(mTitle);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		} else if(id == R.id.action_example){
			mMapView.setTraffic(!item.isChecked());
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section
		 * number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		                         Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//			textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
		}
	}

	class MyTabListener implements ActionBar.TabListener{
		private int type = 0;
		public MyTabListener(int i ){
			type = i;
		}
		@Override
		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
			if(type == 1){
				mMapView.setSatellite(false);
			} else if(type == 2){
				mMapView.setSatellite(true);
			}
		}

		@Override
		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

		}

		@Override
		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

		}
	}

	private void showToast(String str){
		Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
	}
}

/**
 * 继承MapView重写onTouchEvent实现泡泡处理操作
 * @author hejin
 *
 */
class MyLocationMapView extends MapView{
	static PopupOverlay   pop  = null;//弹出泡泡图层，点击图标使用
	public MyLocationMapView(Context context) {
		super(context);
	}
	public MyLocationMapView(Context context, AttributeSet attrs){
		super(context,attrs);
	}
	public MyLocationMapView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event){
		if (!super.onTouchEvent(event)){
			//消隐泡泡
			if (pop != null && event.getAction() == MotionEvent.ACTION_UP)
				pop.hidePop();
		}
		return true;
	}
}


