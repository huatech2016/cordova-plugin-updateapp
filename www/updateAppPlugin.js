
    /**
     * 检查并更新APP
     * @author robin
     */
    function UpdateApp() {
    }


    UpdateApp.prototype.hasNewVersion = function (successCallback, failureCallback, checkPath) {
        cordova.exec(successCallback, failureCallback, "UpdateApp", "hasNewVersion", [checkPath]);
    }
    UpdateApp.prototype.downloadApp = function (successCallback, failureCallback, checkPath) {
        cordova.exec(successCallback, failureCallback, "UpdateApp", "downloadApp", [checkPath]);
    }
    UpdateApp.prototype.cancelUpdate = function (successCallback, failureCallback) {
        cordova.exec(successCallback, failureCallback, "UpdateApp", "cancelUpdate");
    }
    cordova.addConstructor(function () {
        if (!window.plugins) {
            window.plugins = {};
        }
        window.plugins.updateApp = new UpdateApp();
    });

