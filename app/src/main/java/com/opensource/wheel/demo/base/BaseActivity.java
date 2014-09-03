/*
 * Copyright (C) 2014. The Android Open Source Project.
 *
 *         yinglovezhuzhu@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.opensource.wheel.demo.base;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.opensource.dialog.CustomProgressDialog;
import com.opensource.wheel.demo.R;
import com.opensource.wheel.demo.task.CustomAsyncTask;
import com.opensource.wheel.demo.utils.LogUtil;

import java.lang.reflect.Field;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-06-06.
 */
public class BaseActivity extends FragmentActivity {

    protected String TAG = this.getClass().getSimpleName();

    protected CustomProgressDialog mProgressDialog = null;

    protected KeyUpListener mKeyUpListener = null;
    


    /******************************** 【Activity LifeCycle For Debug】 *******************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onCreate() invoked!!");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onStart() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onStart() invoked!!");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onRestart() invoked!!");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onResume() invoked!!");
//		MobclickAgent.onResume(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onPause() invoked!!");
//		MobclickAgent.onPause(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onStop() invoked!!");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onDestroy() invoked!!");
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        LogUtil.d(TAG, this.getClass().getSimpleName() + " onCreateView() invoked!!");
        return super.onCreateView(name, context, attrs);
    }

    /******************************** 【Activity LifeCycle For Debug】 *******************************************/

    protected void showShortToast(int pResId) {
        showShortToast(getString(pResId));
    }

    protected void showLongToast(String pMsg) {
        Toast.makeText(this, pMsg, Toast.LENGTH_LONG).show();
    }

    protected void showShortToast(String pMsg) {
        Toast.makeText(this, pMsg, Toast.LENGTH_SHORT).show();
    }


    protected CharSequence getText(TextView tv) {
        return tv.getText();
    }

    /**
     * 通过反射来设置对话框是否要关闭，在表单校验时很管用， 因为在用户填写出错时点确定时默认Dialog会消失， 所以达不到校验的效果
     * 而mShowing字段就是用来控制是否要消失的，而它在Dialog中是私有变量， 所有只有通过反射去解决此问题
     *
     * @param pDialog
     * @param pIsClose
     */
    public void setAlertDialogIsClose(DialogInterface pDialog, Boolean pIsClose) {
        try {
            Field field = pDialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(pDialog, pIsClose);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示加载对话框
     * @param title
     * @param message
     * @param isCanceledOnTouchOutside
     * @param isCancelable
     * @param listener
     */
    protected void showProgressDialog(CharSequence title, CharSequence message, boolean isCanceledOnTouchOutside,
                                      boolean isCancelable, DialogInterface.OnCancelListener listener) {
//        mProgressDialog = CustomProgressDialog.show(this, title, message,  true, 
//        		getResources().getDrawable(R.drawable.progress_drawable_round_white));
    	mProgressDialog = CustomProgressDialog.show(this, title, message);
        mProgressDialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside);
        mProgressDialog.setCancelable(isCancelable);
        if(listener != null) {
            mProgressDialog.setOnCancelListener(listener);
        }
    }

    /**
     * 取消加载对话框
     */
    protected void cancelProgressDialog() {
        if(mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    /**
     * 终止异步任务的执行
     */
    protected void cancelTask(CustomAsyncTask<?, ?, ?> task) {
        if(task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    /**
     * 隐藏软件盘输入法
     * @param view
     */
    protected void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }


    /**
     * 带动画的跳转
     * @param intent
     */
    public void startActivityLeft(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    protected void startActivityRight(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    protected void startActivityForResultRight(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    protected void startActivityForResultLeft(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    protected void startActivityForResultTop(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.push_top_in, R.anim.not_change);
    }

    protected void startActivityTop(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.push_top_in, R.anim.push_top_out);
    }

    protected void finishRight() {
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    protected void finishLeft() {
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }

    protected void finishBottom() {
        finish();
        overridePendingTransition(R.anim.not_change, R.anim.push_bottom_out);
    }

    protected void defaultFinish() {
        super.finish();
    }

    public boolean hasExtra(String key) {
        return getIntent().hasExtra(key);
    }


    public void setKeyUpListener(KeyUpListener kl) {
        this.mKeyUpListener = kl;
    }

    /**
     * 功能：KeyUp事件，用于加入该FragmentActivity中的Fragment的KeyUp事件
     * @author xiaoying
     *
     */
    public static interface KeyUpListener {
        /**
         * keyUp事件处理
         * @param keyCode
         * @param ev
         * @return true Fragment中有KeyUp事件被处理，false则Fragment中没有KeyUp事件被处理
         */
        public boolean onKeyUp(int keyCode, KeyEvent ev);
    }

}
