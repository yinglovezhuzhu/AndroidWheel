package com.opensource.wheel.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.opensource.dialog.SlidingDialog;
import com.opensource.wheel.OnWheelChangedListener;
import com.opensource.wheel.OnWheelScrollListener;
import com.opensource.wheel.WheelView;
import com.opensource.wheel.adapter.BaseWheelAdapter;
import com.opensource.wheel.demo.base.BaseActivity;
import com.opensource.wheel.demo.bean.CityResp;
import com.opensource.wheel.demo.utils.FileUtil;
import com.opensource.wheel.demo.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private TextView mTvLocation;
    private WheelView mProviceView;
    private WheelView mCityView;
    private SlidingDialog mSlidingDialog;
    private CityAdapter mProvinceAdapter;
    private CityAdapter mCityAdatper;
    private boolean mProviceScrolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        new LoadCityThread().start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_location:
                mSlidingDialog.show();
                break;
            default:
                break;
        }
    }

    private void initView() {
        mTvLocation = (TextView) findViewById(R.id.tv_location);
        mTvLocation.setOnClickListener(this);

        mSlidingDialog = new SlidingDialog(this);

        View view = View.inflate(this, R.layout.layout_cities_chooser, null);
        mProviceView = (WheelView) view.findViewById(R.id.wv_cities_chooser_provices);
        mCityView = (WheelView) view.findViewById(R.id.wv_cities_chooser_cities);
        mProviceView.setVisibleItems(5);
        mCityView.setVisibleItems(5);
        mProviceView.addScrollingListener(new OnWheelScrollListener() {

            @Override
            public void onScrollingStarted(WheelView wheel) {
                mProviceScrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                mProviceScrolling = false;
                CityResp.City province = mProvinceAdapter.getItem(mProviceView.getCurrentItem());
                if(null != province) {
                    List<CityResp.City> citieDatas = province.getChildCity();
                    LogUtil.w(TAG, "Cities-->>" + citieDatas);
                    //这里每次的重新new一个CityAdapter并重新set，否则数据不出现
                    mCityAdatper = new CityAdapter(MainActivity.this, citieDatas);
                    mCityView.setViewAdapter(mCityAdatper);
                    mCityView.setCurrentItem(2);
                }
            }
        });
        mProviceView.addChangingListener(new OnWheelChangedListener() {

            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                LogUtil.w(TAG, "oldValue-->>> " + oldValue + "<> newValue--->>> " + newValue);
                if(!mProviceScrolling) {
                    CityResp.City province = mProvinceAdapter.getItem(newValue);
                    if(null != province) {
                        List<CityResp.City> citieDatas = province.getChildCity();
                        LogUtil.w(TAG, "Cities-->>" + citieDatas);
                        //这里每次的重新new一个CityAdapter并重新set，否则数据不出现
                        mCityAdatper = new CityAdapter(MainActivity.this, citieDatas);
                        mCityView.setViewAdapter(mCityAdatper);
                        mCityView.setCurrentItem(2);
                    }
                }
            }
        });

        mSlidingDialog.setContentView(view);
        mSlidingDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLocation();
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });
    }

    /**
     * 设置地址数据
     */
    private void setLocation() {
        StringBuilder sb = new StringBuilder();
        if(null != mProvinceAdapter && null != mProviceView) {
            CityResp.City provice = mProvinceAdapter.getItem(mProviceView.getCurrentItem());
            if(null != provice) {
                sb.append(provice.getName());
            }
        }
        if(null != mCityAdatper && null != mCityView) {
            CityResp.City city = mCityAdatper.getItem(mCityView.getCurrentItem());
            if(null != city) {
                sb.append(" ").append(city.getName());
            }
        }
        mTvLocation.setText(sb);
    }

    /**
     * 退出
     */
    private void exit() {
        defaultFinish();
    }

    private static final int MSG_LOAD_CITY_START = 0x0;
    private static final int MSG_LOAD_CITY_FINISHED = 0x1;
    private static final int MSG_LOAD_CITY_FAILED = 0x2;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_CITY_START:
                    showProgressDialog(null, getString(R.string.loading), false, false, null);
                    break;
                case MSG_LOAD_CITY_FINISHED:
                    CityResp resp = (CityResp) msg.obj;
                    if(null == resp) {
                        cancelProgressDialog();
                        exit();
                        return;
                    }
                    LogUtil.i(TAG, resp);
                    mProvinceAdapter = new CityAdapter(MainActivity.this, resp.getPageList());
                    mProviceView.setViewAdapter(mProvinceAdapter);
                    mProviceView.setCurrentItem(2);
                    CityResp.City provice = mProvinceAdapter.getItem(2);
                    if(null != provice) {
                        mCityAdatper = new CityAdapter(MainActivity.this, provice.getChildCity());
                        mCityView.setViewAdapter(mCityAdatper);
                        mCityView.setCurrentItem(2);
                    }
                    cancelProgressDialog();
                    break;
                case MSG_LOAD_CITY_FAILED:
                    cancelProgressDialog();
                    exit();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 加载城市数据的线程
     *
     * @author yinglovezhuzhu@gmail.com
     *
     */
    private class LoadCityThread extends Thread {

        @Override
        public synchronized void start() {
            mHandler.sendEmptyMessage(MSG_LOAD_CITY_START);
            super.start();
        }

        @Override
        public void run() {
            try {
                String jsonStr = FileUtil.readStringFromAssetFile(MainActivity.this, "cities.json");
                if(isInterrupted()) {
                    mHandler.sendEmptyMessage(MSG_LOAD_CITY_FAILED);
                    return;
                }
                Gson gson = new Gson();
                CityResp resp = gson.fromJson(jsonStr, CityResp.class);
                if(null == resp || isInterrupted()) {
                    mHandler.sendEmptyMessage(MSG_LOAD_CITY_FAILED);
                    return;
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_LOAD_CITY_FINISHED, resp));
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(MSG_LOAD_CITY_FAILED);
            }
            super.run();
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }
    }

    /**
     * 城市选择适配器
     *
     * @author yinglovezhuzhu@gmail.com
     *
     */
    private class CityAdapter extends BaseWheelAdapter {

        private Context mmContext;
        private List<CityResp.City> mmCityDatas = new ArrayList<CityResp.City>();

        public CityAdapter(Context context, Collection<CityResp.City> cityDatas) {
            this.mmContext = context;
            if(null != cityDatas && !cityDatas.isEmpty()) {
                mmCityDatas.addAll(cityDatas);
            }
        }

        public CityResp.City getItem(int positon) {
            if(positon >= 0 && positon < getItemsCount()) {
                return mmCityDatas.isEmpty() ? null : mmCityDatas.get(positon);
            }
            return null;
        }

        @Override
        public int getItemsCount() {
            return mmCityDatas.isEmpty() ? 1 : mmCityDatas.size();
        }

        @Override
        public View getItem(int index, View convertView, ViewGroup parent) {
            if(index >= 0 && index < getItemsCount() && !mmCityDatas.isEmpty()) {
                TextView tv = null;
                if(null == convertView) {
                    tv = new TextView(mmContext);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    tv.setGravity(Gravity.CENTER);
                    tv.setPadding(8, 15, 8, 15);
                    convertView = tv;
                } else {
                    tv = (TextView) convertView;
                }
                CityResp.City city = mmCityDatas.get(index);
                if(null != city) {
                    tv.setText(city.getName());
                }
                return tv;
            }
            return null;
        }
    }
}
