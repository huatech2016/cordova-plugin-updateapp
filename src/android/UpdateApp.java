package com.phonegap.plugins.updateapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class UpdateApp extends CordovaPlugin {

    /* 版本号检查路径 */
    private String checkPath;
    /* 新版本号 */
    private int newVerCode;
    /* 新版本名称 */
    private String newVerName;
    /* APK 下载路径 */
    private String downloadPath;
    /* APK 更新内容 */
    private String updateContent;
    /* 下载中 */
    private static final int DOWNLOAD = 1;
    /* 下载结束 */
    private static final int DOWNLOAD_FINISH = 2;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;
    /* 上下文 */
    private Context mContext;
    /* 更新进度条 */
    //private ProgressBar mProgress;
    //private Dialog mDownloadDialog;
    private final int UPDATE_NOT_FOUND = 0;
    private final int UPDATE_EXCEPTION_OCCURED = 1;

    protected static final String LOG_TAG = "UpdateApp";

    private CallbackContext downloadCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        this.mContext = cordova.getActivity();
//        if ("checkAndUpdate".equals(action)) {
//            this.checkPath = args.getString(0);
//            checkAndUpdate();
//            return true;
//        } else if ("getCurrentVersion".equals(action)) {
//            callbackContext.success(this.getCurrentVerCode() + "");
//            return true;
//        } else if ("getServerVersion".equals(action)) {
//            this.checkPath = args.getString(0);
//            cordova.getThreadPool().execute(new Runnable() {
//                public void run() {
//                    if (getServerVerInfo()) {
//                        callbackContext.success(newVerCode + "");
//                    } else {
//                        callbackContext
//                                .error("can't connect to the server!please check [checkpath]");
//                    }
//                }
//            });
//            return true;
//        } else if ("getVersionName".equals(action)) {
//            callbackContext.success(this.getCurrentVerName());
//            return true;
//        } else


        if ("hasNewVersion".equals(action)) {
            this.checkPath = args.getString(0);
            int currentVersionCode = this.getCurrentVerCode();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (getServerVerInfo()) {
                        int currentVerCode = getCurrentVerCode();
                        if (newVerCode > currentVerCode) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("currentVerCode", currentVerCode);
                                jsonObject.put("newVerCode", newVerCode);
                                jsonObject.put("updateContent", updateContent);
                                jsonObject.put("downloadPath", downloadPath);
                            } catch (JSONException e) {
                                callbackContext.error(UPDATE_EXCEPTION_OCCURED);
                            }
                            callbackContext.success(jsonObject);
                        } else {
                            callbackContext.error(UPDATE_NOT_FOUND);
                        }
                    } else {
                        callbackContext.error(UPDATE_EXCEPTION_OCCURED);
                    }
                }
            };
            cordova.getThreadPool().execute(runnable);
            return true;
        } else if ("downloadApk".equals(action)) {
            downloadCallbackContext = callbackContext;
            checkPath = args.getString(0);
            Runnable runnable = new Runnable() {
                public void run() {
                    if (getServerVerInfo()) {
                        downloadApk();
                    } else {
                        callbackContext.error("服务器获取版本信息出错");
                    }
                }
            };
            cordova.getThreadPool().execute(runnable);
            return true;
        }
        return false;
    }


    /**
     * 获取应用当前版本代码versionCode
     *
     * @param
     * @return
     */
    private int getCurrentVerCode() {
        String packageName = this.mContext.getPackageName();
        int currentVer = -1;
        try {
            currentVer = this.mContext.getPackageManager().getPackageInfo(
                    packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "获取应用当前版本代码versionCode异常：" + e.toString());
        }
        return currentVer;
    }

    /**
     * 获取应用当前版本代码versionName
     *
     * @param
     * @return
     */
    private String getCurrentVerName() {
        String packageName = this.mContext.getPackageName();
        String currentVer = "";
        try {
            currentVer = this.mContext.getPackageManager().getPackageInfo(
                    packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "获取应用当前版本代码versionName异常：" + e.toString());
        }
        return currentVer;
    }

    /**
     * 获取服务器上的版本信息
     *
     * @param
     * @return
     * @throws Exception
     */
    private boolean getServerVerInfo() {
        try {
            StringBuilder verInfoStr = new StringBuilder();
            URL url = new URL(checkPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), "UTF-8"), 8192);
            String line = null;
            while ((line = reader.readLine()) != null) {
                verInfoStr.append(line + "\n");
            }
            reader.close();

            JSONArray array = new JSONArray(verInfoStr.toString());
            if (array.length() > 0) {
                JSONObject obj = array.getJSONObject(0);
                newVerCode = obj.getInt("verCode");
                newVerName = obj.getString("verName");
                downloadPath = obj.getString("downloadPath");
                updateContent = obj.getString("updateContent");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "获取服务器上的版本信息异常：" + e.toString());
            return false;
        }
        return true;
    }


    /**
     * 下载apk文件
     */
    private void downloadApk() {
        // 启动新线程下载软件
        new downloadApkThread().start();
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 正在下载
                case DOWNLOAD:
                    // 设置进度条位置
                    //mProgress.setProgress(progress);
                    break;
                case DOWNLOAD_FINISH:
                    // 安装文件
                    downloadCallbackContext.success("download finished");
                    installApk();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 下载文件线程
     */
    private class downloadApkThread extends Thread {
        @Override
        public void run() {
            try {
                // 判断SD卡是否存在，并且是否具有读写权限
                if (Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED)) {
                    // 获得存储卡的路径
                    String sdpath = Environment.getExternalStorageDirectory()
                            + "/";
                    mSavePath = sdpath + "download";
                    URL url = new URL(downloadPath);
                    // 创建连接
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.connect();
                    // 获取文件大小
                    int length = conn.getContentLength();
                    // 创建输入流
                    InputStream is = conn.getInputStream();

                    File file = new File(mSavePath);
                    // 判断文件目录是否存在
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    File apkFile = new File(mSavePath, newVerName);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 缓存
                    byte buf[] = new byte[1024];
                    // 写入到文件中
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        // 计算进度条位置
                        progress = (int) (((float) count / length) * 100);
                        // 更新进度
                        // TODO: 2016/9/5 通知栏
                        //mHandler.sendEmptyMessage(DOWNLOAD);
                        if (numread <= 0) {
                            // 下载完成
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        // 写入文件
                        fos.write(buf, 0, numread);
                    } while (!cancelUpdate);// 点击取消就停止下载.
                    fos.close();
                    is.close();
                } else {
                    Log.d(LOG_TAG, "手机没有SD卡");
                }
            } catch (MalformedURLException e) {
                downloadCallbackContext.error("下载文件线程异常MalformedURLException：" + e.toString());
                Log.d(LOG_TAG, "下载文件线程异常MalformedURLException：" + e.toString());
            } catch (IOException e) {
                downloadCallbackContext.error("下载文件线程异常IOException：" + e.toString());
                Log.d(LOG_TAG, "下载文件线程异常IOException：" + e.toString());
            }
            // 取消下载对话框显示
            //mDownloadDialog.dismiss();
        }
    }

    /**
     * 安装APK文件
     */
    private void installApk() {
        File apkfile = new File(mSavePath, newVerName);
        if (!apkfile.exists()) {
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
                "application/vnd.android.package-archive");
        mContext.startActivity(i);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
 
