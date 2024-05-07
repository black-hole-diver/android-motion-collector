var exec = require('cordova/exec');

exports.startRecording = function (success, error) {
    exec(success, error, 'SensorWrapper', 'startRecording', []);
};

exports.stopRecording = function (success, error) {
    exec(success, error, 'SensorWrapper', 'stopRecording', []);
};
