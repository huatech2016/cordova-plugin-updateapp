cordova-plugin-updateapp
=========

基于cordova插件形式开发的版本升级插件，支持android和iOS。

说明：
========
+ 该版本插件android和iOS均是基于服务器上的版本信息文件
+ iOS的更新是基于企业证书签名，内网分发的应用编写的
+ iOS下需要基于appstore的lookup接口做检查更新的话，这个插件不适用，需要稍微做些修改
+ 2015年3月更新，提交到appstore的应用如果有检查更新功能，会被拒绝

安装：
========
1. `cordova plugin add https://github.com/huatech2016/cordova-plugin-updateapp.git`
2. `cordova build android` 或者 `cordova build ios`
3. 在服务器上放置版本信息文件`androidVersion.json` 或者 `iosVersion.json`

使用：
========
~~```javascript

// 检查是否有更新，checkPath为存放更新信息的json文件路径
window.plugins.updateApp.hasNewVersion(function (updateContent) {
    console.log(updateContent);
}, function (error) {
    console.log("出现异常"+error);
}, checkPath);


// 后台下载并安装，checkPath为存放更新信息的json文件路径
window.plugins.updateApp.downloadApk(function () {
    console.log(arguments);
}, function () {
    console.log(arguments);
}, checkPath);


```

androidVersion.json:
=========
`[{"verCode":"最新版apk的versionCode","verName":"最新版apk的versionName","apkPath":"apk的地址","updateContent":"更新内容"}]`

iosVersion.json:
=========
`{"verName":"最新版ipa的版本号","ipaPath":"plist的地址或者app的itunes地址"}`
