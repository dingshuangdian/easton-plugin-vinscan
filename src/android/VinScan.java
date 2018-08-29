package vinscan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import vinscan.ocr.CameraActivity;
import static android.app.Activity.RESULT_OK;
/**
 * This class echoes a string called from JavaScript.
 */
public class VinScan extends CordovaPlugin {
  private Activity cordovaActivity;
  private static final int requestCodeOcr = 1002;
  private CordovaInterface cordovaInterface;
  public CallbackContext callback;
  private Context mContext;
  private String company_name;
  private Map<String, String> list = new HashMap<>(2);

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    cordovaActivity = cordova.getActivity();
    cordovaInterface = cordova;
    mContext = cordova.getContext();
    getCode();
  }

  /**
   * 安卓6以上动态权限相关
   */
  private static final int REQUEST_CODE = 100001;

  private boolean needsToAlertForRuntimePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return !cordova.hasPermission(Manifest.permission.CAMERA) || !cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    } else {
      return false;
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    callback = callbackContext;
    if (action.equals("goScan")) {
      goScan();
      return true;
    }
    return false;
  }

  private void goScan() {
    if (!needsToAlertForRuntimePermission()) {
      Intent cameraintent = new Intent(cordovaActivity, CameraActivity.class);
      cordovaInterface.startActivityForResult(this, cameraintent, requestCodeOcr);
    } else {
      requestPermission();
    }
  }

  private void requestPermission() {
    ArrayList<String> permissionsToRequire = new ArrayList<String>();
    if (!cordova.hasPermission(Manifest.permission.CAMERA))
      permissionsToRequire.add(Manifest.permission.CAMERA);
    if (!cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
      permissionsToRequire.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    String[] _permissionsToRequire = new String[permissionsToRequire.size()];
    _permissionsToRequire = permissionsToRequire.toArray(_permissionsToRequire);
    cordova.requestPermissions(this, REQUEST_CODE, _permissionsToRequire);
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    if (requestCode != REQUEST_CODE)
      return;
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        callback.error("权限被拒绝,请手动打开权限");
        return;
      }
    }
    Intent cameraintent = new Intent(cordovaActivity, CameraActivity.class);
    cordovaInterface.startActivityForResult(this, cameraintent, requestCodeOcr);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == requestCodeOcr && resultCode == RESULT_OK) {
      String carNo = data.getStringExtra("carNo");
      callback.success(carNo);
    } else {
      callback.error(data.getAction());
    }
    super.onActivityResult(requestCode, resultCode, data);

  }

  private void getCode() {
    try {
      ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
      company_name = appInfo.metaData.getString("COMPANY_NAME");
      String devcodeKey = appInfo.metaData.getString("DEVCODE_KEY");
      list.put("company_name", company_name);
      list.put("devcodeKey", devcodeKey);
      SaveDevcode.getInstance().setData(list);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }


}
