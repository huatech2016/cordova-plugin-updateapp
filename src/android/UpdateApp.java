package com.phonegap.plugins.updateapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
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
import java.util.List;


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
	private Boolean forceUpdate;
	/* 下载中 */
	private static final int DOWNLOAD = 1;
	/* 下载结束 */
	private static final int DOWNLOAD_FINISH = 2;
	/* 下载保存路径 */
	private String mSavePath;
	/* 记录进度条数量 */
	private int progress;
	private int oldProgress;
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
		mSavePath = Environment.getExternalStorageDirectory() + File.separator + "download";

		this.mContext = cordova.getActivity();


		if ("hasNewVersion".equals(action)) {
			this.checkPath = args.getString(0);

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (getServerVerInfo()) {
						int currentVerCode = getCurrentVerCode();
						try {
							JSONObject jsonObject = new JSONObject();
							if (newVerCode > currentVerCode) {
								jsonObject.put("needUpdate", true);
							} else {
								jsonObject.put("needUpdate", false);
							}
							jsonObject.put("currentVersion", currentVerCode);
							jsonObject.put("newVersion", newVerCode);
							jsonObject.put("updateContent", updateContent);
							jsonObject.put("forceUpdate", forceUpdate);
							jsonObject.put("downloadPath", downloadPath);
							callbackContext.success(jsonObject);

						} catch (JSONException e) {
							callbackContext.success(UPDATE_EXCEPTION_OCCURED);
						}
					} else {
						callbackContext.error(UPDATE_EXCEPTION_OCCURED);
					}
				}
			};
			cordova.getThreadPool().execute(runnable);
			return true;
		} else if ("downloadApp".equals(action)) {
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
		} else if (("cancelUpdate").equals(action)) {
			cancelUpdate();
			callbackContext.success("取消息下载成功");
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
			currentVer = this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
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
				newVerName = obj.getString("verName") + ".apk";
				downloadPath = obj.getString("downloadPath");
				updateContent = obj.getString("updateContent");
//				forceUpdate = obj.getBoolean("forceUpdate");
				forceUpdate = false;
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
		cancelUpdate = false;
		progress = 0;
		oldProgress = 0;
		new downloadApkThread().start();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				// 正在下载
				case DOWNLOAD:


					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, msg.arg1);
					pluginResult.setKeepCallback(true);
					downloadCallbackContext.sendPluginResult(pluginResult);
					break;
				case DOWNLOAD_FINISH:
					// 安装文件
					//进度 大于 100 不再显示 下载窗口

					PluginResult pluginResult1 = new PluginResult(PluginResult.Status.OK, 101);
					pluginResult1.setKeepCallback(false);
					downloadCallbackContext.sendPluginResult(pluginResult1);
					installApk(cordova.getActivity().getApplicationContext());


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
//					String sdpath = cordova.getActivity().getExternalFilesDir("") + File.separator;
//					mSavePath = sdpath;
					URL url = new URL(downloadPath);
					// 创建连接
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
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
					byte buf[] = new byte[4096];
					// 写入到文件中
					do {
						int numread = is.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) (((float) count / length) * 100);
						if (progress - oldProgress > 0) {
							oldProgress = progress;
							Message msg = new Message();
							msg.arg1 = progress;
							msg.what = DOWNLOAD;
							mHandler.sendMessage(msg);
						}
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


	private void installApk(Context context) {
		Intent intent = new Intent(Intent.ACTION_VIEW);

		File file = new File(mSavePath, newVerName);
		if (!file.exists()) {
			return;
		}
		Uri apkUri;
		if (Build.VERSION.SDK_INT >= 24) {

			apkUri = FileProvider.getUriForFile(context, "PACKAGE-NAME-TO-REPLACE" + ".fileProvider", file);

			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} else {
			apkUri = Uri.fromFile(file);
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
		// 查询所有符合 intent 跳转目标应用类型的应用，注意此方法必须放置在 setDataAndType 方法之后 解决android10 无法解析后无法安装问题
		List<ResolveInfo> resolveLists = mContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		// 然后全部授权
		for (ResolveInfo resolveInfo : resolveLists) {
			String packageName = resolveInfo.activityInfo.packageName;
			mContext.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
		context.startActivity(intent);
	}


	private void cancelUpdate() {

		cancelUpdate = true;
	}
}
 
