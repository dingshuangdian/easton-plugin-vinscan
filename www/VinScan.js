var exec = require('cordova/exec');

var vinsacn = {
    goScan: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'VinScan', 'goScan', []);
    }
};

module.exports = vinsacn