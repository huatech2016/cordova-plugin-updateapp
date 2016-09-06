cordova.define("com.qinsilk.updateapp.updateAppPlugin", function (require, exports, module) {
    /**
     * 检查并更新APP
     * @author robin
     */
    function UpdateApp() {
    }

// UpdateApp.prototype.checkAndUpdate = function(checkPath) {
// 	cordova.exec(null, null, "UpdateApp", "checkAndUpdate", [checkPath]);
// }
// UpdateApp.prototype.getCurrentVerInfo = function(successCallback) {
// 	cordova.exec(successCallback, null, "UpdateApp", "getCurrentVersion", []);
// }
// UpdateApp.prototype.getServerVerInfo = function(successCallback, failureCallback, checkPath) {
// 	cordova.exec(successCallback, failureCallback, "UpdateApp", "getServerVersion", [checkPath]);
// }
    
// UpdateApp.prototype.getAppVersion = function(successCallback, failureCallback) {
// 	cordova.exec(successCallback, failureCallback, "UpdateApp", "getVersionName", []);
// }
    UpdateApp.prototype.hasNewVersion = function (successCallback, failureCallback, checkPath) {
        cordova.exec(successCallback, failureCallback, "UpdateApp", "hasNewVersion", [checkPath]);
    }
    UpdateApp.prototype.downloadApk = function (successCallback, failureCallback, checkPath) {
        cordova.exec(successCallback, failureCallback, "UpdateApp", "downloadApk", [checkPath]);
    }
    cordova.addConstructor(function () {
        if (!window.plugins) {
            window.plugins = {};
        }
        window.plugins.updateApp = new UpdateApp();
    });
});
