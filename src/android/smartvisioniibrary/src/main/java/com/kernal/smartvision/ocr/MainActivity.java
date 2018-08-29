package vinscan.ocr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import vinscan.utils.CameraSetting;


/**
 * 项目名称：慧视OCR
 * 类名称：首界面
 * 类描述：
 * 创建人：黄震
 * 创建时间：2016/02/03
 * 修改人：${user}
 * 修改时间：${date} ${time}
 * 修改备注：
 */
public class MainActivity extends Activity {
  private int width, height;
  private DisplayMetrics dm = new DisplayMetrics();
  private int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
  private ImageButton btn_set, btn_app_logo, btn_scanRecog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    this.getWindowManager().getDefaultDisplay().getMetrics(dm);
    width = dm.widthPixels;
    height = dm.heightPixels;
    setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));
    ShowResultActivity.hiddenVirtualButtons(getWindow().getDecorView());
    findview();

  }


  @Override
  protected void onStart() {
    super.onStart();


  }

  static final String[] PERMISSION = new String[]{
    Manifest.permission.WRITE_EXTERNAL_STORAGE,// 写入权限
    Manifest.permission.READ_EXTERNAL_STORAGE, // 读取权限
    Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.VIBRATE, Manifest.permission.INTERNET};

  /**
   * @return ${return_type}    返回类型
   * @throws
   * @Title: 主界面UI布局
   * @Description: 主要动态布局界面上的控件
   */
  public void findview() {
    CameraSetting.getInstance(this).hiddenVirtualButtons(getWindow().getDecorView());

    btn_set = (ImageButton) this.findViewById(getResources().getIdentifier("btn_set", "id", getPackageName()));
    btn_app_logo = (ImageButton) this.findViewById(getResources().getIdentifier("btn_app_logo", "id", getPackageName()));
    btn_scanRecog = (ImageButton) this.findViewById(getResources().getIdentifier("btn_scanRecog", "id", getPackageName()));
    btn_scanRecog.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
//				Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        Intent cameraintent = new Intent(MainActivity.this, CameraActivity.class);
        if (Build.VERSION.SDK_INT >= 23) {
          CheckPermission checkPermission = new CheckPermission(MainActivity.this);
          if (checkPermission.permissionSet(PERMISSION)) {
            PermissionActivity.startActivityForResult(MainActivity.this, 0, "CameraActivity", PERMISSION);
            MainActivity.this.finish();
          } else {

            startActivity(cameraintent);
            overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
            MainActivity.this.finish();
          }
        } else {

          startActivity(cameraintent);
          overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
          MainActivity.this.finish();
        }
//                startActivity(intent);
//                overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
//                MainActivity.this.finish();
      }
    });
    btn_set.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
        if (Build.VERSION.SDK_INT >= 23) {

          CheckPermission checkPermission = new CheckPermission(MainActivity.this);
          if (checkPermission.permissionSet(PERMISSION)) {
            PermissionActivity.startActivityForResult(MainActivity.this, 0, "SettingActivity", PERMISSION);
          } else {
            startActivity(intent);
            overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
//			                MainActivity.this.finish();
          }
        } else {
          startActivity(intent);
          overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));

        }
//				Intent intent = new Intent(MainActivity.this, SettingActivity.class);
//                startActivity(intent);
//                overridePendingTransition(getResources().getIdentifier("zoom_enter", "anim", getApplication().getPackageName()), getResources().getIdentifier("push_down_out", "anim", getApplication().getPackageName()));
      }
    });
//        btn_about.setOnClickListener(this);
    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (width * 0.2), (int) (width * 0.2));
    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
    params.topMargin = (int) (0.7 * height);
    btn_scanRecog.setLayoutParams(params);
    params = new RelativeLayout.LayoutParams((int) (width * 0.4), (int) (height * 0.18));
    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
    params.topMargin = (int) (0.2 * height);
    btn_app_logo.setLayoutParams(params);
    params = new RelativeLayout.LayoutParams((int) (width * 0.1), (int) (width * 0.1));
    params.leftMargin = (int) (width * 0.75);
    params.topMargin = (int) (0.02 * height);
    btn_set.setLayoutParams(params);
  }
}
