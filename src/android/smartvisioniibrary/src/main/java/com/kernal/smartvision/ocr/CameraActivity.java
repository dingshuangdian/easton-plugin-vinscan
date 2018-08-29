package vinscan.ocr;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.kernal.smartvisionocr.RecogService;
import com.kernal.smartvisionocr.model.RecogResultModel;
import com.kernal.smartvisionocr.utils.KernalLSCXMLInformation;
import com.kernal.smartvisionocr.utils.SharedPreferencesHelper;
import com.kernal.smartvisionocr.utils.Utils;
import com.kernal.smartvisionocr.utils.WriteToPCTask;
import com.kernal.vinparseengine.VinParseInfo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.ionic.starter.R;
import vinscan.SaveDevcode;
import vinscan.adapter.CameraDocTypeAdapter;
import vinscan.adapter.RecogResultAdapter;
import vinscan.utils.CameraParametersUtils;
import vinscan.utils.CameraSetting;
import vinscan.view.HorizontalListView;
import vinscan.view.RotateLayout;
import vinscan.view.ViewfinderView;

/**
 * 项目名称：慧视OCR 类名称：相机界面 类描述： 创建人：黄震 创建时间：2016/02/03 修改人：${user} 修改时间：${date}
 * ${time} 修改备注：
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback,
  Camera.PreviewCallback, ActivityCompat.OnRequestPermissionsResultCallback {
  private int srcWidth, srcHeight, screenWidth, screenHeight;
  private VinParseInfo vpi;
  private String deviceCode = SaveDevcode.getInstance().getData().get("devcodeKey");
  ;
  private SurfaceView surfaceView;
  private SurfaceHolder surfaceHolder;
  private static final int requestCodeVin = 1010;
  private Camera camera;
  private List<Integer> srcList = new ArrayList<Integer>();// 拍照分辨率集合
  private DisplayMetrics dm = new DisplayMetrics();
  // private FocusManager mFocusManager;
  private Camera.AutoFocusCallback mAutoFocusCallback;
  private RotateLayout mFocusIndicator;
  private Timer timeAuto;
  private TimerTask timer;
  private int selectedTemplateTypePosition = 0;
  private boolean isTouch = false;
  private Vibrator mVibrator;
  private boolean isToastShow = true;
  private int tempUiRot = 0;
  private boolean isFirstHandCheckField = false;// 是否跳转到点击左侧识别结果字段前 选中的字段位置
  public Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      getPhoneSizeAndRotation();
      if (recogResultAdapter.isRecogSuccess) {
        // 识别完成旋转时，先将当前识别字段的选中状态置为初始状态再消除布局View
        myViewfinderView.fieldsPosition = 0;
      }
      RemoveView();
      isFirstProgram = true;
      if (msg.what == 5) {
        if (islandscape) {
          LandscapeView();
        } else {
          PortraitView();
        }

        if (!recogResultAdapter.isRecogSuccess) {
          AddView();
        } else {
          DestiroyAnimation();
          btn_summit.setVisibility(View.VISIBLE);
        }
      } else {
        SetScreenOritation();
        changeData();
        if (recogResultAdapter.isRecogSuccess) {
          // 识别完成旋转时，保留确定按钮显示状态，扫描框不显示，二维码扫描线不显示 二维码扫描动画清除
          btn_summit.setVisibility(View.VISIBLE);
          DestiroyAnimation();
        } else {
          AddView();
          btn_summit.setVisibility(View.GONE);
          if (wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId
            .equals("SV_ID_BARCODE_QR")) {
            if (islandscape) {
              ScanlineLandscapeView();
            } else {
              ScanlinePortraitView();
            }
          }
        }

        if (camera != null) {
          rotation = CameraSetting.getInstance(CameraActivity.this)
            .setCameraDisplayOrientation(uiRot);
          camera.setDisplayOrientation(rotation);
        }

      }

    }
  };
  private int rotation = 0; // 屏幕取景方向
  private Message msg;
  private List<HashMap<String, String>> vinInfo;
  private CameraParametersUtils cameraParametersUtils;
  private RelativeLayout.LayoutParams layoutParams;
  private ListView type_template_listView;
  private HorizontalListView type_template_HorizontallistView;
  private ListView recogResult_listView;
  private Button btn_summit;
  public ViewfinderView myViewfinderView;
  private ImageView iv_camera_back, iv_camera_flash;
  private KernalLSCXMLInformation wlci_Landscape, wlci_Portrait, wlci;
  public static RelativeLayout re, bg_template_listView;
  public static int nMainIDX;
  private CameraDocTypeAdapter adapter;
  public static RecogResultAdapter recogResultAdapter;
  private int[] regionPos = new int[4];// 敏感区域
  private boolean isFirstProgram = true;
  private Camera.Size size;
  private int[] nCharCount = new int[2];// 返回结果中的字符数
  Handler handler2 = new Handler();
  Runnable touchTimeOut = new Runnable() {
    @Override
    public void run() {

      isTouch = false;
    }
  };
  private byte[] data;
  private boolean isTakePic = false;
  private ImageButton imbtn_takepic;
  public static List<RecogResultModel> recogResultModelList = new ArrayList<RecogResultModel>();
  private RecogThread recogThread;
  private boolean isOnClickRecogFields = false;// 点击识别结果分项，
  private int tempPosition = -1;// -1是初始化的值；-2是识别完成后再点击识别不对的项，隐藏确定按钮，识别被选择的项
  private ArrayList<String> list_recogSult;
  private int returnResult = -1;// 测试返回值
  private boolean isChangeType = false;// 是否切换过模板类型
  public RecogService.MyBinder recogBinder;
  private int iTH_InitSmartVisionSDK = -1;
  private TranslateAnimation animation;
  private ImageView iv_camera_scanline;
  private boolean isFirst = true;// 第一次在识别线程中给二维码布局
  // private boolean isTakebuttonShow = true; // imbtn_takepic 拍照按钮是否显示
  private String SavePicPath;// 图片路径
  private ArrayList<String> savePath;// 图片路径的集合
  private String Imagepath;// 点击拍照按钮 保存的完整图像的路径
  private int[] LeftUpPoint = new int[2];
  private int[] RightDownPoint = new int[2];
  private int position;// 记录当前识别字段的下标
  private boolean isClick = false;// 是否点击了识别结果按钮
  private boolean isFirstIn = true;
  private boolean islandscape = true;// 是否为横向
  private RecogResultModel recogResultModel;
  private Bitmap bitmap;
  private String[] httpContent;//上传服务的内容  包括图片路径  以及 识别结果
  public static byte[] Data;
  public ServiceConnection recogConn = new ServiceConnection() {
    @Override
    public void onServiceDisconnected(ComponentName name) {
      recogConn = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      recogBinder = (RecogService.MyBinder) service;
      iTH_InitSmartVisionSDK = recogBinder.getInitSmartVisionOcrSDK();

      if (iTH_InitSmartVisionSDK == 0) {
        recogBinder.AddTemplateFile();// 添加识别模板
        recogBinder
          .SetCurrentTemplate(wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId);// 设置当前识别模板ID

      } else {
        Toast.makeText(CameraActivity.this,
          "核心初始化失败，错误码：" + iTH_InitSmartVisionSDK,
          Toast.LENGTH_LONG).show();
      }

    }
  };
  final int requestCodeOcr = 1001;

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == requestCodeOcr && resultCode == RESULT_OK) {
      String carNo = data.getStringExtra("carNo");
      Intent old = getIntent();
      old.putExtra("carNo", carNo);

      setResult(RESULT_OK, old);
      finish();
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void sendCarNo(String carNo) {
    Intent old = getIntent();
    old.putExtra("carNo", carNo);
    setResult(RESULT_OK, old);
    finish();
  }

  public Runnable updateUI = new Runnable() {
    public void run() {
      CloseCameraAndStopTimer(1);
      list_recogSult.clear();
      list_recogSult
        .add(recogResultModelList.get(0).resultValue);


          /*  String httpPath = savePicture(data, 0);

            Intent intent = new Intent(CameraActivity.this,
                    ShowResultActivity.class);
            intent.putStringArrayListExtra("recogResult",
                    list_recogSult);
            intent.putStringArrayListExtra("savePath", savePath);
            intent.putExtra(
                    "templateName",
                    wlci.template.get(selectedTemplateTypePosition).templateName);
            intent.putExtra("httpPath", httpPath);
            intent.putExtra("rotation", rotation);
            intent.putExtra("regionPos", regionPos);*/
      sendCarNo(list_recogSult.get(0).split(":")[1]);
      overridePendingTransition(
        getResources().getIdentifier("zoom_enter",
          "anim",
          getApplication().getPackageName()),
        getResources().getIdentifier("push_down_out",
          "anim",
          getApplication().getPackageName()));
      CameraActivity.this.finish();
    }
  };

  /*
   * (non-Javadoc)
   *
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    this.getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		this.getWindow().setFlags(
//				WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
//				WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    setContentView(getResources().getIdentifier("activity_camera",
      "layout", getApplication().getPackageName()));
    //ininCar();
    Utils.copyFile(this);// 写入本地文件
    // 已写入的情况下，根据version.txt中版本号判断是否需要更新，如不需要不执行写入操作
    this.getWindowManager().getDefaultDisplay().getMetrics(dm);
    cameraParametersUtils = new CameraParametersUtils(this);
    uiRot = getWindowManager().getDefaultDisplay().getRotation();// 获取屏幕旋转的角度
    getPhoneSizeAndRotation();
    ParseXml();
    tempPosition = -1;

    findView();
    ClickEvent();
    initData();

    SetScreenOritation();
    AddView();

    if (recogBinder == null) {
      Intent authIntent = new Intent(CameraActivity.this,
        RecogService.class);
      bindService(authIntent, recogConn, Service.BIND_AUTO_CREATE);
    }
    surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(CameraActivity.this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//		mAutoFocusCallback = new Camera.AutoFocusCallback() {
//			@Override
//			public void onAutoFocus(boolean success, Camera camera) {
////				 CameraSetting.getInstance(CameraActivity.this)
////				 .setCameraParameters(CameraActivity.this,
////				 surfaceHolder, CameraActivity.this, camera,
////				 (float) srcWidth / srcHeight, srcList, true);
////				CameraSetting.getInstance(CameraActivity.this)
////						.setCameraParameters(CameraActivity.this,
////								surfaceHolder, CameraActivity.this, camera,
////								(float) srcHeight / srcWidth, srcList, true, 0);
//
//				if (timer == null) {
//					timer = new TimerTask() {
//						public void run() {
//							msg = new Message();
//							handler.sendMessage(msg);
//						}
//					};
//				}
//
//				if (Utils.isCPUInfo64()) {
//					timeAuto.schedule(timer, 1000);
//				} else {
//					timeAuto.schedule(timer, 2000);
//				}
//				//
//				// }
//				// };
//
//			}
//		};
  }


  public static final int WRITE_EXTERNAL_OK = 2;

  private void ininCar() {
    if (Build.VERSION.SDK_INT >= 23) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_PHONE_STATE_OK);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_OK);
        }
      }
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case CAMERA_OK: {
        //如果数组大于0 用户确定给予向相机权限
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

          if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_OK);
          } else {
            Toast.makeText(this, "请重新操,作初始化相机", Toast.LENGTH_SHORT).show();
          }

        } else {
          finish();
          Toast.makeText(this, "请手动打开相机和电话权限", Toast.LENGTH_SHORT).show();
        }
        break;
      }
      case READ_PHONE_STATE_OK:
        //如果数组大于0 用户确定给予向相机权限
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_OK);
          } else {
            Toast.makeText(this, "请重新操作,初始化相机", Toast.LENGTH_SHORT).show();
          }
        } else {
          finish();
          Toast.makeText(this, "请手动打开相机和电话权限", Toast.LENGTH_SHORT).show();
        }
        break;
    }
  }

  public static final int CAMERA_OK = 0;
  public static final int READ_PHONE_STATE_OK = 1;

  // 获取设备分辨率 不受虚拟按键影响
  public void getPhoneSizeAndRotation() {

    cameraParametersUtils.setScreenSize(CameraActivity.this);
    srcWidth = cameraParametersUtils.srcWidth;
    srcHeight = cameraParametersUtils.srcHeight;
    getScreenSize();
  }

  // 解析横竖屏下 两个XML文件内容，赋予不同的对象
  private void ParseXml() {
    // 横屏模板解析
    wlci_Landscape = Utils
      .parseXmlFile(this, "appTemplateConfig.xml", true);
    if (wlci_Landscape.template == null
      || wlci_Landscape.template.size() == 0) {
      // 如果用户不选择任何模板类型，我们会强制选择第一个模板类型
      wlci_Landscape = Utils.parseXmlFile(this, "appTemplateConfig.xml",
        false);
      wlci_Landscape.template.get(0).isSelected = true;
      Utils.updateXml(this, wlci_Landscape, "appTemplateConfig.xml");
      wlci_Landscape = Utils.parseXmlFile(this, "appTemplateConfig.xml",
        true);
    }
    // 竖屏模板解析
    wlci_Portrait = Utils.parseXmlFile(this,
      "appTemplatePortraitConfig.xml", true);
    if (wlci_Portrait.template == null
      || wlci_Portrait.template.size() == 0) {
      // 如果用户不选择任何模板类型，我们会强制选择第一个模板类型
      wlci_Portrait = Utils.parseXmlFile(this,
        "appTemplatePortraitConfig.xml", false);
      wlci_Portrait.template.get(0).isSelected = true;
      Utils.updateXml(this, wlci_Portrait,
        "appTemplatePortraitConfig.xml");
      wlci_Portrait = Utils.parseXmlFile(this,
        "appTemplatePortraitConfig.xml", true);
    }
    if (uiRot == 0 || uiRot == 2) // 竖屏状态下
    {

      islandscape = false;
      wlci = wlci_Portrait;
    } else { // 横屏状态下

      islandscape = true;
      wlci = wlci_Landscape;
    }
  }

  // 设置横竖屏状态和变换数据对象
  private void SetScreenOritation() {

    if (uiRot == 0 || uiRot == 2) // 竖屏状态下
    {
      islandscape = false;
      wlci = wlci_Portrait;
      ;
      PortraitView();
    } else { // 横屏状态下
      islandscape = true;
      wlci = wlci_Landscape;
      LandscapeView();
    }

  }

  /**
   * @param
   * @return void 返回类型
   * @throws
   * @Title: initData
   * @Description: TODO(这里用一句话描述这个方法的作用)
   */
  private void initData() {
    // 初始化数据
    if (islandscape) {
      adapter = new CameraDocTypeAdapter(this, wlci.template,
        screenWidth, screenWidth);
    } else {
      adapter = new CameraDocTypeAdapter(this, wlci.template,
        screenWidth, screenHeight);
    }
    selectedTemplateTypePosition = ShowResultActivity.selectedTemplateTypePosition;
    adapter.selectedPosition = selectedTemplateTypePosition;
    recogResultModelList.clear();

    for (int i = 0; i < wlci.fieldType.get(
      wlci.template.get(selectedTemplateTypePosition).templateType)
      .size(); i++) {
      recogResultModel = new RecogResultModel();
      recogResultModel.resultValue = wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(i).name
        + ":";
      recogResultModel.type = wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(i).type;

      recogResultModelList.add(recogResultModel);
    }

    // TODO Auto-generated method stub
    if (islandscape) {
      type_template_listView.setAdapter(adapter);
      recogResultAdapter = new RecogResultAdapter(this,
        recogResultModelList, screenWidth, screenWidth);
    } else {
      type_template_HorizontallistView.setAdapter(adapter);
      recogResultAdapter = new RecogResultAdapter(this,
        recogResultModelList, screenWidth, screenHeight);
    }

    // 由于记录功能的存在 这里在初始化化数据时 给isfirst赋值
    if ("营业执照"
      .equals(wlci.template.get(selectedTemplateTypePosition).templateName)) {
      isFirst = true;
    } else {
      isFirst = false;
    }
    recogResult_listView.setAdapter(recogResultAdapter);
    savePath = new ArrayList<String>();
  }

  public void changeData() {
    if (islandscape) {
      adapter = new CameraDocTypeAdapter(this, wlci.template,
        screenWidth, screenWidth);
      type_template_listView.setAdapter(adapter);
    } else {
      adapter = new CameraDocTypeAdapter(this, wlci.template,
        screenWidth, screenHeight);
      type_template_HorizontallistView.setAdapter(adapter);
    }
    adapter.selectedPosition = selectedTemplateTypePosition;
  }

  @Override
  protected void onStart() {
    super.onStart();
    isTouch = false;
    list_recogSult = new ArrayList<String>();

    isTakePic = false;
    isFirstProgram = true;

  }

  /**
   * @return ${return_type} 返回类型
   * @throws
   * @Title: 相机界面
   * @Description: 动态布局相机界面的控件
   */

  private void findView() {
    surfaceView = (SurfaceView) this.findViewById(getResources()
      .getIdentifier("surfaceview_camera", "id", getPackageName()));
    mFocusIndicator = (RotateLayout) findViewById(getResources()
      .getIdentifier("focus_indicator_rotate_layout", "id",
        getPackageName()));
    bg_template_listView = (RelativeLayout) findViewById(getResources()
      .getIdentifier("bg_template_listView", "id", getPackageName()));
    type_template_listView = (ListView) this
      .findViewById(getResources().getIdentifier(
        "type_template_listView", "id", getPackageName()));

    // 横向的Listview
    type_template_HorizontallistView = (HorizontalListView) findViewById(getResources()
      .getIdentifier("type_template_HorizontallistView", "id",
        getPackageName()));

    recogResult_listView = (ListView) this.findViewById(getResources()
      .getIdentifier("recogResult_listView", "id", getPackageName()));
    btn_summit = (Button) this.findViewById(getResources().getIdentifier(
      "btn_summit", "id", getPackageName()));
    re = (RelativeLayout) this.findViewById(getResources().getIdentifier(
      "re", "id", getPackageName()));
    imbtn_takepic = (ImageButton) this.findViewById(getResources()
      .getIdentifier("imbtn_takepic", "id", getPackageName()));
    iv_camera_back = (ImageView) this.findViewById(getResources()
      .getIdentifier("iv_camera_back", "id", getPackageName()));
    iv_camera_flash = (ImageView) this.findViewById(getResources()
      .getIdentifier("iv_camera_flash", "id", getPackageName()));
    iv_camera_scanline = (ImageView) this.findViewById(getResources()
      .getIdentifier("iv_scaner_line", "id", getPackageName()));
    re.addOnLayoutChangeListener(new OnLayoutChangeListener() {

      @Override
      public void onLayoutChange(View v, int left, int top, int right,
                                 int bottom, int oldLeft, int oldTop, int oldRight,
                                 int oldBottom) {
        if ((bottom != oldBottom && right == oldRight)
          || (bottom == oldBottom && right != oldRight)) {
          Message mesg = new Message();
          mesg.what = 5;
          handler.sendMessage(mesg);
        }

      }
    });

  }

  //
  // 获取屏幕尺寸 会受到虚拟按键影响而改变值
  public void getScreenSize() {
    DisplayMetrics dm = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(dm);
    screenWidth = dm.widthPixels;
    screenHeight = dm.heightPixels;
//		screenWidth = srcWidth;
//		screenHeight =srcHeight;
//		System.out.println("屏幕高：" + screenHeight + " 宽： " + screenWidth);
  }

  public void LandscapeView() {
    // 重新编辑扫描框
    if (srcWidth == screenWidth) {

      layoutParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.FILL_PARENT, srcHeight);
      surfaceView.setLayoutParams(layoutParams);

      // 右侧菜单栏的背景布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), srcHeight);
      layoutParams.leftMargin = (int) (0.85 * srcWidth);
      layoutParams.topMargin = 0;
      bg_template_listView.setLayoutParams(layoutParams);

      // 隐藏竖屏布局下的菜单栏
      type_template_HorizontallistView.setVisibility(View.GONE);
      // 右侧菜单栏的布局
      type_template_listView.setVisibility(View.VISIBLE);
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), (int) (srcHeight * 0.95));
      layoutParams.leftMargin = (int) (0.85 * srcWidth);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      type_template_listView.setLayoutParams(layoutParams);

      // 识别结果UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.85 * srcWidth), (int) (srcHeight * 0.3));
      layoutParams.leftMargin = 0;
      layoutParams.topMargin = (int) (srcHeight * 0.7);
      recogResult_listView.setLayoutParams(layoutParams);

      // 确定按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), (int) (srcHeight * 0.1));
      layoutParams.leftMargin = (int) (0.7 * srcWidth);
      layoutParams.topMargin = (int) (srcHeight * 0.8);
      btn_summit.setLayoutParams(layoutParams);

      // 闪光灯按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.75);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_flash.setLayoutParams(layoutParams);

      // 返回按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.05);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_back.setLayoutParams(layoutParams);

    } else if (srcWidth > screenWidth) {
      // 如果将虚拟硬件弹出则执行如下布局代码，相机预览分辨率不变压缩屏幕的高度
      int surfaceViewHeight = (screenWidth * srcHeight) / srcWidth;
      layoutParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.FILL_PARENT, surfaceViewHeight);
      layoutParams.topMargin = (srcHeight - surfaceViewHeight) / 2;
      surfaceView.setLayoutParams(layoutParams);
      // 右侧菜单栏的背景布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), srcHeight);
      layoutParams.leftMargin = (int) (0.85 * srcWidth)
        - (srcWidth - screenWidth);
      ;
      layoutParams.topMargin = 0;
      bg_template_listView.setLayoutParams(layoutParams);

      // 隐藏竖屏布局下的菜单栏
      type_template_HorizontallistView.setVisibility(View.GONE);
      // 右侧菜单栏的布局
      type_template_listView.setVisibility(View.VISIBLE);
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), srcHeight);
      layoutParams.leftMargin = (int) (0.85 * srcWidth)
        - (srcWidth - screenWidth);
      layoutParams.topMargin = 0;
      type_template_listView.setLayoutParams(layoutParams);

      // 识别结果UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.85 * srcWidth), (int) (srcHeight * 0.3));
      layoutParams.leftMargin = 0;
      layoutParams.topMargin = (int) (srcHeight * 0.7)
        - (srcHeight - surfaceViewHeight) / 2;
      recogResult_listView.setLayoutParams(layoutParams);
      // 确定按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), (int) (srcHeight * 0.1));
      layoutParams.leftMargin = (int) (0.7 * srcWidth)
        - (srcWidth - screenWidth);
      layoutParams.topMargin = (int) (srcHeight * 0.8);
      btn_summit.setLayoutParams(layoutParams);

      // 闪光灯按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.75)
        - (srcWidth - screenWidth);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_flash.setLayoutParams(layoutParams);

      // 返回按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.05);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_back.setLayoutParams(layoutParams);

    }

    if (wlci.fieldType.get(
      wlci.template.get(selectedTemplateTypePosition).templateType)
      .get(myViewfinderView.fieldsPosition).ocrId
      .equals("SV_ID_BARCODE_QR")) {
      ScanlineLandscapeView();
    }
    takepicButtonLandscapeView();

    btn_summit.setVisibility(View.GONE);

    // if (!isTakebuttonShow) {
    // imbtn_takepic.setVisibility(View.GONE);
    // }
    // 当识别条目只有一个时 不显示右下角的识别结果ListView
    if (recogResultModelList.size() == 1) {
      recogResult_listView.setVisibility(View.GONE);
    } else {
      recogResult_listView.setVisibility(View.VISIBLE);
    }

    // 扫描框的UI布局
    // layoutParams = new RelativeLayout.LayoutParams(srcWidth, srcHeight);
    // layoutParams.leftMargin = 0;
    // layoutParams.topMargin = 0;
    // myViewfinderView.setLayoutParams(layoutParams);

  }

  public void PortraitView() {

    if (srcHeight == screenHeight) {
      layoutParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.FILL_PARENT, srcHeight);
      surfaceView.setLayoutParams(layoutParams);

      // 底侧菜单栏背景的竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (0.085 * srcHeight));
      layoutParams.topMargin = (int) (0.93 * srcHeight);
      bg_template_listView.setLayoutParams(layoutParams);

      // 底侧侧菜单栏的竖屏布局
      type_template_listView.setVisibility(View.GONE);
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (0.085 * srcHeight));
      layoutParams.topMargin = (int) (srcHeight * 0.95);
      type_template_HorizontallistView.setLayoutParams(layoutParams);
      type_template_HorizontallistView.setVisibility(View.VISIBLE);

      // 识别结果UI竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (srcHeight * 0.13));
      layoutParams.topMargin = (int) (srcHeight * 0.8);
      recogResult_listView.setLayoutParams(layoutParams);

      // 确定按钮UI竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (0.85 * srcWidth);
      layoutParams.topMargin = (int) (srcHeight * 0.88);
      btn_summit.setLayoutParams(layoutParams);

      // 闪光灯按钮UI竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.8);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_flash.setLayoutParams(layoutParams);

      // 扫描框的UI布局
      // layoutParams = new RelativeLayout.LayoutParams(srcWidth,
      // srcHeight);
      // layoutParams.leftMargin = 0;
      // layoutParams.topMargin = 0;
      // myViewfinderView.setLayoutParams(layoutParams);

      // 返回按钮UI竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.1);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_back.setLayoutParams(layoutParams);

    } else if (srcHeight > screenHeight) {
      // 如果将虚拟硬件弹出则执行如下布局代码，相机预览分辨率不变压缩屏幕的宽度
      int surfaceViewWidth = (screenHeight * srcWidth) / srcHeight;
      layoutParams = new RelativeLayout.LayoutParams(surfaceViewWidth,
        RelativeLayout.LayoutParams.FILL_PARENT);
      layoutParams.leftMargin = (srcWidth - surfaceViewWidth) / 2;
      surfaceView.setLayoutParams(layoutParams);

      // 底侧菜单栏的背景布局
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (srcHeight * 0.085));
      layoutParams.topMargin = (int) (0.93 * srcHeight)
        - (srcHeight - screenHeight);
      ;
      layoutParams.leftMargin = (srcWidth - surfaceViewWidth) / 2;
      bg_template_listView.setLayoutParams(layoutParams);

      // 底侧菜单栏的布局
      type_template_listView.setVisibility(View.GONE);
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (0.085 * srcHeight));
      layoutParams.topMargin = (int) (0.95 * srcHeight)
        - (srcHeight - screenHeight);

      layoutParams.leftMargin = (srcWidth - surfaceViewWidth) / 2;
      type_template_HorizontallistView.setLayoutParams(layoutParams);
      type_template_HorizontallistView.setVisibility(View.VISIBLE);

      // 识别结果UI布局
      layoutParams = new RelativeLayout.LayoutParams(srcWidth,
        (int) (srcHeight * 0.13));
      layoutParams.leftMargin = (srcWidth - surfaceViewWidth) / 2;
      layoutParams.topMargin = (int) (srcHeight * 0.8)
        - (srcHeight - screenHeight);
      recogResult_listView.setLayoutParams(layoutParams);

      // 确定按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (0.15 * srcWidth), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (0.85 * srcWidth)
        - (srcWidth - surfaceViewWidth) / 2;
      layoutParams.topMargin = (int) (srcHeight * 0.88)
        - (srcHeight - screenHeight);
      btn_summit.setLayoutParams(layoutParams);

      // 闪光灯按钮UI布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.8);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_flash.setLayoutParams(layoutParams);

      // 返回按钮UI竖屏布局
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      layoutParams.leftMargin = (int) (srcWidth * 0.1);
      layoutParams.topMargin = (int) (srcHeight * 0.05);
      iv_camera_back.setLayoutParams(layoutParams);
    }
    // // 闪光灯按钮UI布局
    // layoutParams = new RelativeLayout.LayoutParams(
    // (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
    // layoutParams.leftMargin = (int) (srcWidth * 0.8);
    // layoutParams.topMargin = (int) (srcHeight * 0.05);
    // iv_camera_flash.setLayoutParams(layoutParams);

    if (wlci.fieldType.get(
      wlci.template.get(selectedTemplateTypePosition).templateType)
      .get(myViewfinderView.fieldsPosition).ocrId
      .equals("SV_ID_BARCODE_QR")) {
      ScanlinePortraitView();
    }
    takepicButtonPortraitView();
    btn_summit.setVisibility(View.GONE);
    // 当识别条目只有一个时 不显示识别结果ListView

    if (recogResultModelList.size() == 1) {
      recogResult_listView.setVisibility(View.GONE);
    } else {
      recogResult_listView.setVisibility(View.VISIBLE);
    }

  }

  public void ClickEvent() {

    iv_camera_back.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
        CloseCameraAndStopTimer(0);
//
//				Intent intent = new Intent(CameraActivity.this,
        //MainActivity.class);
//				startActivity(intent);
//				overridePendingTransition(
//						getResources().getIdentifier("zoom_enter", "anim",
//								getApplication().getPackageName()),
//						getResources().getIdentifier("push_down_out", "anim",
//								getApplication().getPackageName()));
        CameraActivity.this.finish();
      }
    });
    iv_camera_flash.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (SharedPreferencesHelper.getBoolean(CameraActivity.this,
          "isOpenFlash", false)) {
          iv_camera_flash.setImageResource(getResources()
            .getIdentifier("flash_on", "drawable",
              getPackageName()));
          SharedPreferencesHelper.putBoolean(CameraActivity.this,
            "isOpenFlash", false);
          CameraSetting.getInstance(CameraActivity.this)
            .closedCameraFlash(camera);
        } else {
          SharedPreferencesHelper.putBoolean(CameraActivity.this,
            "isOpenFlash", true);
          iv_camera_flash.setImageResource(getResources()
            .getIdentifier("flash_off", "drawable",
              getPackageName()));
          CameraSetting.getInstance(CameraActivity.this)
            .openCameraFlash(camera);
        }
      }
    });
    imbtn_takepic.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
        isTakePic = true;
        isFirstProgram = true;

      }
    });
    btn_summit.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        // TODO Auto-generated method stub
        CloseCameraAndStopTimer(0);
        list_recogSult.clear();

        for (int i = 0; i < recogResultModelList.size(); i++) {
          list_recogSult.add(recogResultModelList.get(i).resultValue);

        }
        ;

        Intent intent = new Intent(CameraActivity.this,
          ShowResultActivity.class);
        intent.putStringArrayListExtra("recogResult", list_recogSult);
        intent.putStringArrayListExtra("savePath", savePath);
        intent.putExtra(
          "templateName",
          wlci.template.get(selectedTemplateTypePosition).templateName);
        startActivity(intent);
        overridePendingTransition(
          getResources().getIdentifier("zoom_enter", "anim",
            getApplication().getPackageName()),
          getResources().getIdentifier("push_down_out", "anim",
            getApplication().getPackageName()));
        CameraActivity.this.finish();
      }
    });
    type_template_listView.setDividerHeight(0);

    recogResult_listView.setDividerHeight(0);
    //
    type_template_listView.setOnTouchListener(new MyOnTouchListener());
    type_template_HorizontallistView
      .setOnTouchListener(new MyOnTouchListener());
    recogResult_listView.setOnTouchListener(new MyOnTouchListener());
    // 识别结果点击触发事件
    recogResult_listView
      .setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView,
                                View view, int i, long position) {

          if (savePath.size() > position) {
            isClick = true;
            isFirstHandCheckField = true;
          }
          isTakePic = false;
          // isFirstHandCheckField = true;
          // if (isTakebuttonShow) {
          imbtn_takepic.setVisibility(view.VISIBLE);
          // } else {
          // imbtn_takepic.setVisibility(view.GONE);
          // }
          ;
          if (CameraActivity.recogResultModelList
            .get((int) position).resultValue.split(":").length <= 1) {

            return;
          }
          isFirstProgram = true;
          isOnClickRecogFields = true;
          recogResultAdapter.isRecogSuccess = false;
          // 屏幕旋转后 重新添加布局
          if (myViewfinderView == null) {
            AddView();
          }
          // 正在使用的布局间的切换
          if (myViewfinderView != null) {
            for (int y = 0; y < CameraActivity.recogResultModelList
              .size(); y++) {
              if (CameraActivity.recogResultModelList.get(y).resultValue
                .split(":").length <= 1) {
                tempPosition = y;

                break;
              }
            }

            if (tempPosition == -1) {
              myViewfinderView.fieldsPosition = Integer
                .valueOf(recogResultModelList
                  .get((int) position).type);

              btn_summit.setVisibility(View.VISIBLE);
              myViewfinderView.setVisibility(View.GONE);
            } else if (tempPosition == -2) {
              myViewfinderView.fieldsPosition = Integer
                .valueOf(recogResultModelList
                  .get((int) position).type);

              btn_summit.setVisibility(View.GONE);
              myViewfinderView.setVisibility(View.VISIBLE);
            } else {
              myViewfinderView.fieldsPosition = Integer
                .valueOf(recogResultModelList
                  .get((int) position).type);

              btn_summit.setVisibility(View.GONE);
              myViewfinderView.setVisibility(View.VISIBLE);
            }
          }

          if (wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId
            .equals("SV_ID_BARCODE_QR")) {
            // imbtn_takepic.setVisibility(View.GONE);
            iv_camera_scanline.setVisibility(View.VISIBLE);
            iv_camera_scanline.startAnimation(animation);

          } else {
            iv_camera_scanline.clearAnimation();
            iv_camera_scanline.destroyDrawingCache();
            // imbtn_takepic.setVisibility(View.GONE);
            iv_camera_scanline.setVisibility(View.GONE);
          }

          recogResultAdapter.selectedRecogResultPosition = Integer
            .valueOf(recogResultModelList
              .get((int) position).type);
          recogResultAdapter.notifyDataSetChanged();
          recogResult_listView.requestFocusFromTouch();
          if (recogResultAdapter.selectedRecogResultPosition == 0) {
            recogResult_listView.setSelection(0);
          } else {
            recogResult_listView
              .setSelection(recogResultAdapter.selectedRecogResultPosition);
          }

          if (islandscape) {
            takepicButtonLandscapeView();
          } else {
            takepicButtonPortraitView();
          }
        }
      });
    // 右侧菜单栏点击触发事件
    type_template_listView
      .setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView,
                                View view, int i, long position) {

          recogResultModelList.clear();
          savePath.clear();
          isClick = false;
          selectedTemplateTypePosition = (int) position;
          ShowResultActivity.selectedTemplateTypePosition = selectedTemplateTypePosition;
          // 向识别结果字段数组中 添加识别结果字段条目
          for (int x = 0; x < wlci.fieldType.get(
            wlci.template.get((int) position).templateType)
            .size(); x++) {
            RecogResultModel recogResultModel = new RecogResultModel();
            recogResultModel.resultValue = wlci.fieldType
              .get(wlci.template.get((int) position).templateType)
              .get(x).name
              + ":";
            recogResultModel.type = wlci.fieldType
              .get(wlci.template.get((int) position).templateType)
              .get(x).type;

            recogResultModelList.add(recogResultModel);

          }
          // 屏幕旋转后 重新添加布局
          if (myViewfinderView == null) {
            AddView();
          }
          // 正在使用的布局间的切换
          if (myViewfinderView != null) {
            re.removeView(myViewfinderView);
            myViewfinderView = null;
            myViewfinderView = new ViewfinderView(
              CameraActivity.this,
              wlci,
              wlci.template
                .get(selectedTemplateTypePosition).templateType,
              true);
            myViewfinderView.fieldsPosition = 0;
            re.addView(myViewfinderView);
            isFirstProgram = true;
            btn_summit.setVisibility(View.GONE);
            isFirstHandCheckField = false;
            isChangeType = true;
            adapter.selectedPosition = (int) position;
            adapter.notifyDataSetChanged();
            recogResultAdapter = new RecogResultAdapter(
              CameraActivity.this, recogResultModelList,
              screenWidth, screenWidth);
            recogResultAdapter.isRecogSuccess = false;
            recogResult_listView.setAdapter(recogResultAdapter);

          }
          if (islandscape) {
            takepicButtonLandscapeView();
          } else {
            takepicButtonPortraitView();
          }
          imbtn_takepic.setVisibility(View.VISIBLE);

          if (recogResultModelList.size() == 1) {
            recogResult_listView.setVisibility(View.GONE);

          } else {
            recogResult_listView.setVisibility(View.VISIBLE);
          }

          if (wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId
            .equals("SV_ID_BARCODE_QR")) {
            iv_camera_scanline.setVisibility(View.VISIBLE);
            iv_camera_scanline.startAnimation(animation);

          } else {
            iv_camera_scanline.clearAnimation();
            iv_camera_scanline.destroyDrawingCache();
            iv_camera_scanline.setVisibility(View.GONE);
          }

        }
      });

    // 竖屏下 底侧菜单栏点击事件
    type_template_HorizontallistView
      .setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView,
                                View view, int i, long position) {
          recogResultModelList.clear();
          savePath.clear();
          isClick = false;
          selectedTemplateTypePosition = (int) position;
          ShowResultActivity.selectedTemplateTypePosition = selectedTemplateTypePosition;
          for (int x = 0; x < wlci.fieldType.get(
            wlci.template.get((int) position).templateType)
            .size(); x++) {
            RecogResultModel recogResultModel = new RecogResultModel();
            recogResultModel.resultValue = wlci.fieldType
              .get(wlci.template.get((int) position).templateType)
              .get(x).name
              + ":";
            recogResultModel.type = wlci.fieldType
              .get(wlci.template.get((int) position).templateType)
              .get(x).type;

            recogResultModelList.add(recogResultModel);

          }
          // 屏幕旋转后 重新添加布局
          if (myViewfinderView == null) {
            AddView();
          }
          // 正在使用的布局间的切换
          if (myViewfinderView != null) {
            re.removeView(myViewfinderView);
            myViewfinderView = null;
            myViewfinderView = new ViewfinderView(
              CameraActivity.this,
              wlci,
              wlci.template
                .get(selectedTemplateTypePosition).templateType,
              false);
            myViewfinderView.fieldsPosition = 0;
            re.addView(myViewfinderView);
            isFirstProgram = true;
            btn_summit.setVisibility(View.GONE);
            isFirstHandCheckField = false;
            isChangeType = true;

            adapter.selectedPosition = (int) position;
            adapter.notifyDataSetChanged();
            recogResultAdapter = new RecogResultAdapter(
              CameraActivity.this, recogResultModelList,
              srcWidth, screenHeight);
            recogResultAdapter.isRecogSuccess = false;
            recogResult_listView.setAdapter(recogResultAdapter);
          }

          if (islandscape) {
            takepicButtonLandscapeView();
          } else {
            takepicButtonPortraitView();
          }
          imbtn_takepic.setVisibility(View.VISIBLE);

          if (recogResultModelList.size() == 1) {
            recogResult_listView.setVisibility(View.GONE);

          } else {
            recogResult_listView.setVisibility(View.VISIBLE);
          }

          if (wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId
            .equals("SV_ID_BARCODE_QR")) {
            iv_camera_scanline.setVisibility(View.VISIBLE);
            iv_camera_scanline.startAnimation(animation);

          } else {
            iv_camera_scanline.clearAnimation();
            iv_camera_scanline.destroyDrawingCache();
            iv_camera_scanline.setVisibility(View.GONE);
          }

        }
      });
  }

  public void RemoveView() {
    if (myViewfinderView != null) {
      myViewfinderView.destroyDrawingCache();
      re.removeView(myViewfinderView);
      myViewfinderView = null;
    }
  }

  public void AddView() {
    if (islandscape) {
      myViewfinderView = new ViewfinderView(
        CameraActivity.this,
        wlci,
        wlci.template.get(selectedTemplateTypePosition).templateType,
        true);

    } else {
      myViewfinderView = new ViewfinderView(
        CameraActivity.this,
        wlci,
        wlci.template.get(selectedTemplateTypePosition).templateType,
        false);
    }
    re.addView(myViewfinderView);
  }

  // 横屏下拍照按钮 在调出虚拟导航栏和收起导航栏时的布局
  public void takepicButtonLandscapeView() {

    if (srcWidth == screenWidth) {
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      if (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).ocrId
        .equals("SV_ID_BARCODE_QR")) {
        layoutParams.leftMargin = (int) (srcWidth * 0.62);
        layoutParams.topMargin = (int) (srcHeight * 0.34);
      } else {
        layoutParams.leftMargin = (int) (srcWidth * 0.7);
        ;
        layoutParams.topMargin = (int) (srcHeight * 0.43);
      }
      imbtn_takepic.setLayoutParams(layoutParams);

    } else if (srcWidth > screenWidth) {
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcWidth * 0.05), (int) (srcWidth * 0.05));
      if (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).ocrId
        .equals("SV_ID_BARCODE_QR")) {
        layoutParams.leftMargin = (int) (srcWidth * 0.6);
        layoutParams.topMargin = (int) (srcHeight * 0.34);
      } else {
        layoutParams.leftMargin = (int) (srcWidth * 0.66);
        layoutParams.topMargin = (int) (srcHeight * 0.43);
      }
      imbtn_takepic.setLayoutParams(layoutParams);

    }

  }

  // 竖屏下拍照按钮 在调出虚拟导航栏和收起导航栏时的布局
  public void takepicButtonPortraitView() {

    if (srcHeight == screenHeight) {
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      if (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).ocrId
        .equals("SV_ID_BARCODE_QR")) {
        layoutParams.leftMargin = (int) (srcWidth * 0.8);
        layoutParams.topMargin = (int) (srcHeight * 0.45);
      } else {
        layoutParams.leftMargin = (int) (srcWidth * 0.86);
        ;
        layoutParams.topMargin = (int) (srcHeight * 0.415);
      }

      imbtn_takepic.setLayoutParams(layoutParams);

    } else if (srcHeight > screenHeight) {
      layoutParams = new RelativeLayout.LayoutParams(
        (int) (srcHeight * 0.05), (int) (srcHeight * 0.05));
      if (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).ocrId
        .equals("SV_ID_BARCODE_QR")) {
        layoutParams.leftMargin = (int) (srcWidth * 0.8);
        layoutParams.topMargin = (int) (srcHeight * 0.42);
      } else {
        layoutParams.leftMargin = (int) (srcWidth * 0.86);
        ;
        layoutParams.topMargin = (int) (srcHeight * 0.39);

      }
      imbtn_takepic.setLayoutParams(layoutParams);
    }

  }

  // 竖屏，二维码扫描时，扫描线的布局
  public void ScanlinePortraitView() {

    layoutParams = new RelativeLayout.LayoutParams(
      (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).width * screenWidth),
      (int) (screenHeight * 0.01));
    iv_camera_scanline.setLayoutParams(layoutParams);
    animation = new TranslateAnimation(
      (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX * screenWidth),
      (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX * screenWidth),
      (screenHeight * wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY),

      (screenHeight
        * wlci.fieldType
        .get(wlci.template
          .get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY + screenHeight
        * wlci.fieldType
        .get(wlci.template
          .get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).height));

    // }
    animation.setRepeatMode(Animation.REVERSE);
    animation.setRepeatCount(Integer.MAX_VALUE);
    animation.setDuration(500);
    iv_camera_scanline.setVisibility(View.VISIBLE);
    iv_camera_scanline.startAnimation(animation);
  }

  // 横屏，二维码扫描时，扫描线的布局
  public void ScanlineLandscapeView() {
    layoutParams = new RelativeLayout.LayoutParams(
      (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).width * screenWidth),
      (int) (screenHeight * 0.02));
    iv_camera_scanline.setLayoutParams(layoutParams);
    animation = new TranslateAnimation(
      wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX
        * screenWidth,
      wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX
        * screenWidth,
      srcHeight
        * wlci.fieldType
        .get(wlci.template
          .get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY,
      screenHeight
        * wlci.fieldType
        .get(wlci.template
          .get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY
        + screenHeight
        * wlci.fieldType
        .get(wlci.template
          .get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).height);

    animation.setRepeatMode(Animation.REVERSE);
    animation.setRepeatCount(Integer.MAX_VALUE);
    animation.setDuration(500);
    iv_camera_scanline.setVisibility(View.VISIBLE);
    iv_camera_scanline.startAnimation(animation);

  }

  public void DestiroyAnimation() {
    iv_camera_scanline.clearAnimation();
    iv_camera_scanline.destroyDrawingCache();
    iv_camera_scanline.setVisibility(View.GONE);
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {

    if (camera != null) {
      if (islandscape) {
        CameraSetting.getInstance(CameraActivity.this)
          .setCameraParameters(CameraActivity.this,
            surfaceHolder, CameraActivity.this, camera,
            (float) srcWidth / srcHeight, srcList, false,
            rotation);

      } else {
        CameraSetting.getInstance(CameraActivity.this)
          .setCameraParameters(CameraActivity.this,
            surfaceHolder, CameraActivity.this, camera,
            (float) srcHeight / srcWidth, srcList, false,
            rotation);

      }

    }
    if (SharedPreferencesHelper.getBoolean(CameraActivity.this,
      "isOpenFlash", false)) {
      iv_camera_flash.setImageResource(getResources().getIdentifier(
        "flash_off", "drawable", getPackageName()));
      CameraSetting.getInstance(this).openCameraFlash(camera);
    } else {
      iv_camera_flash.setImageResource(getResources().getIdentifier(
        "flash_on", "drawable", getPackageName()));
      CameraSetting.getInstance(this).closedCameraFlash(camera);
    }
    recogThread = new RecogThread();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
                             int height) {

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

  }

  // 小米PAD 解锁屏时执行surfaceChanged surfaceCreated，容易出现超时卡死现象，故在此处打开相机和设置参数
  @Override
  protected void onResume() {
    // TODO Auto-generated method stub
    super.onResume();
    OpenCameraAndSetParameters();

  }

  int uiRot;

  @Override
  public void onPreviewFrame(byte[] bytes, Camera camera) {
    // 实时监听屏幕旋转角度

    uiRot = getWindowManager().getDefaultDisplay().getRotation();// 获取屏幕旋转的角度

    if (uiRot != tempUiRot) {
      Message mesg = new Message();
      mesg.what = uiRot;
      handler.sendMessage(mesg);
      tempUiRot = uiRot;

    }
    if (isTouch) {
      return;

    }
    data = bytes;
    if (iTH_InitSmartVisionSDK == 0) {
      synchronized (recogThread) {
        recogThread.run();

      }
    }
  }

  @Override
  protected void onDestroy() {
    if (myViewfinderView != null) {
      re.removeView(myViewfinderView);
      myViewfinderView.destroyDrawingCache();
      myViewfinderView.fieldsPosition = 0;

      myViewfinderView = null;
    }
    if (recogBinder != null) {
      unbindService(recogConn);
      recogBinder = null;
    }
    if (bitmap != null) {
      bitmap.recycle();
      bitmap = null;
    }
    if (handler2 != null)
      handler2.removeCallbacks(touchTimeOut);
    super.onDestroy();

  }

  // 监听返回键事件
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
      CloseCameraAndStopTimer(0);
//			Intent intent = new Intent(CameraActivity.this, MainActivity.class);
//			startActivity(intent);
//			overridePendingTransition(
//					getResources().getIdentifier("zoom_enter", "anim",
//							getApplication().getPackageName()),
//					getResources().getIdentifier("push_down_out", "anim",
//							getApplication().getPackageName()));
      CameraActivity.this.finish();
      return true;
    }
    return true;
  }

  @Override
  public void onPause() {
    super.onPause();
    CloseCameraAndStopTimer(0);
  }


  private class MyOnTouchListener implements View.OnTouchListener {

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
          isTouch = true;
          break;
        case MotionEvent.ACTION_DOWN:
          handler2.removeCallbacks(touchTimeOut);
          isTouch = true;
          break;
        case MotionEvent.ACTION_OUTSIDE:
          isTouch = false;
          break;
        case MotionEvent.ACTION_UP:
          synchronized (event) {
            handler2.postDelayed(touchTimeOut, 1500);

          }
          break;
      }
      return false;
    }
  }

  /**
   * @param
   * @return void 返回类型 leftPointX 扫描框左坐标系数 leftPointY 纵坐标系数
   * @Title: setRegion 计算识别区域
   * @Description: TODO(这里用一句话描述这个方法的作用)
   */
  public void setRegion() {
    if (islandscape) {
      regionPos[0] = (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX * size.width);
      regionPos[1] = (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY * size.height);
      regionPos[2] = (int) ((wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX + wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).width) * size.width);
      regionPos[3] = (int) ((wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY + wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).height) * size.height);
    } else {
      regionPos[0] = (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX * size.height);
      regionPos[1] = (int) (wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY * size.width);
      regionPos[2] = (int) ((wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointX + wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).width) * size.height);
      regionPos[3] = (int) ((wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).leftPointY + wlci.fieldType
        .get(wlci.template.get(selectedTemplateTypePosition).templateType)
        .get(myViewfinderView.fieldsPosition).height) * size.width);
    }

  }

  class RecogThread extends Thread {

    public long time;

    public RecogThread() {

      time = 0;

    }

    @Override
    public void run() {

      if (recogResultAdapter.isRecogSuccess) {
        return;
      }
      time = System.currentTimeMillis();
      // 第一次加载布局 计算识别区域 也用于识别模板切换时调用

      if (isFirstProgram) {
        size = camera.getParameters().getPreviewSize();
        setRegion();
        recogBinder
          .SetCurrentTemplate(wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId);
        position = myViewfinderView.fieldsPosition;
        recogBinder.SetROI(regionPos, size.width, size.height);

        isFirstProgram = false;

      }
      // 点击拍照按钮 保存图片，强制跳过未自动识别条目

      if (isTakePic) {
        Imagepath = savePicture(data, 1);
        if (Imagepath != null && !"".equals(Imagepath)) {
          // 根据图片路径 加载图片
          recogBinder.LoadImageFile(Imagepath, 0);
          Imagepath = null;
        }
      } else {
        // 加载视频流数据源
//
        if (rotation == 90) {
          recogBinder
            .LoadStreamNV21(data, size.width, size.height, 1);
        } else if (rotation == 0) {
          recogBinder
            .LoadStreamNV21(data, size.width, size.height, 0);
        } else if (rotation == 180) {
          recogBinder
            .LoadStreamNV21(data, size.width, size.height, 2);
        } else {
          recogBinder
            .LoadStreamNV21(data, size.width, size.height, 3);
        }
      }

      returnResult = recogBinder
        .Recognize(
          deviceCode,
          wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .get(myViewfinderView.fieldsPosition).ocrId);
      // 黄震 测试 start
      // String jstrSaveImageFileName="";
      // jstrSaveImageFileName
      // =Environment.getExternalStorageDirectory().toString()
      // + "/DCIM/Camera/"+"测试_"+ Utils.pictureName()+"_nv21.jpg";
      // recogBinder.svSaveImage(jstrSaveImageFileName);
      // 黄震 测试 end

      if (returnResult == 0) {
        // 获取识别结果
        String recogResultString = recogBinder.GetResults(nCharCount);

        time = System.currentTimeMillis() - time;
        if ((recogResultString != null && !recogResultString.equals("") && nCharCount[0] > 0 && RecogService.response == 0)
          || isTakePic) {
          if (myViewfinderView.fieldsPosition < wlci.fieldType
            .get(wlci.template
              .get(selectedTemplateTypePosition).templateType)
            .size()
            && (CameraActivity.recogResultModelList
            .get(myViewfinderView.fieldsPosition).resultValue
            .split(":").length == 1 || myViewfinderView.fieldsPosition == 0)
            || isOnClickRecogFields) {
            if ((recogResultString != null && !recogResultString
              .equals(""))) {
              // 有识别结果时，保存识别核心裁切到的图像

              //SavePicPath = saveROIPicture(data, false);


            } else {
              // 未识别到结果，此时保存识别区域ROI内的图像
              recogResultString = " ";

              SavePicPath = saveROIPicture(data, true);
            }
            if (SavePicPath != null && !"".equals(SavePicPath)) {
              if (SharedPreferencesHelper.getBoolean(
                getApplicationContext(), "upload", false)) {
                httpContent = new String[]{SavePicPath, ""};
                new WriteToPCTask(CameraActivity.this)
                  .execute(httpContent);
              }
            }

            // 将识别结果 赋值到识别结果集合中 以便完成识别后跳转传输
            CameraActivity.recogResultModelList
              .get(myViewfinderView.fieldsPosition).resultValue = wlci.fieldType
              .get(wlci.template
                .get(selectedTemplateTypePosition).templateType)
              .get(myViewfinderView.fieldsPosition).name
              + ":" + recogResultString;
            // 识别完某字段后，跳转到下一字段识别
            if (myViewfinderView.configParamsModel.size() > (myViewfinderView.fieldsPosition + 1)) {
              if (isFirstHandCheckField) {
                if (tempPosition != -2 && tempPosition != -1) {
                  myViewfinderView.fieldsPosition = tempPosition;
                  isFirstHandCheckField = false;
                }

              } else {
                myViewfinderView.fieldsPosition = myViewfinderView.fieldsPosition + 1;
              }

              if (isChangeType) { // 选择营业执照后，当将公司名称识别出来后，点击公司名称重新识别，再次识别成功后，选择其他模板类型，然后再点击营业执照重新识别会走这些代码

                myViewfinderView.fieldsPosition = recogResultAdapter.selectedRecogResultPosition;
                myViewfinderView.fieldsPosition = myViewfinderView.fieldsPosition + 1;
                // 当第一次全部识别完成，然后点击其中一项进行重新识别，然后再次点击其他模板类型，然后再次回到上一次点击的模板类型，重新识别完成所有的项，然后再次重新点击其中一项进行识别，识别完成后，显示确定按钮并隐藏扫描框
                if (tempPosition == -2
                  && CameraActivity.recogResultModelList
                  .get(myViewfinderView.fieldsPosition).resultValue
                  .split(":").length == 1) {
                  tempPosition = myViewfinderView.fieldsPosition;
                }
                isChangeType = false;
              }
              isFirstProgram = true;
              if (tempPosition == -1 && isOnClickRecogFields) {
                // 当识别条目只有一条时 不显示确定按钮
                if (recogResultModelList.size() >= 2) {
                  btn_summit.setVisibility(View.VISIBLE);
                }
                recogResultAdapter.isRecogSuccess = true;
                myViewfinderView.setVisibility(View.GONE);
                imbtn_takepic.setVisibility(View.GONE);

              } else if (tempPosition == -2
                && isOnClickRecogFields) {
                if (recogResultModelList.size() >= 2) {
                  btn_summit.setVisibility(View.VISIBLE);
                }
                recogResultAdapter.isRecogSuccess = true;
                myViewfinderView.setVisibility(View.GONE);
                imbtn_takepic.setVisibility(View.GONE);
              }
            } // 所有识别字段识别完成
            else if (myViewfinderView.configParamsModel.size() <= (myViewfinderView.fieldsPosition + 1)) {
              if (recogResultModelList.size() >= 2) {
                btn_summit.setVisibility(View.VISIBLE);
              }
              recogResultAdapter.isRecogSuccess = true;
              myViewfinderView.setVisibility(View.GONE);
              imbtn_takepic.setVisibility(View.GONE);
              tempPosition = -2;
            }

            if (wlci.fieldType
              .get(wlci.template
                .get(selectedTemplateTypePosition).templateType)
              .get(myViewfinderView.fieldsPosition).ocrId
              .equals("SV_ID_BARCODE_QR")) {

              if (!recogResultAdapter.isRecogSuccess) {
                if (islandscape) {
                  ScanlineLandscapeView();
                } else {
                  ScanlinePortraitView();
                }
              } else {
                DestiroyAnimation();
              }
              imbtn_takepic.setVisibility(View.GONE);
            } else if ((tempPosition == -2 || tempPosition == -1)
              && isOnClickRecogFields) {
              DestiroyAnimation();
            } else {
              DestiroyAnimation();
            }
            CameraActivity.recogResultAdapter.selectedRecogResultPosition = myViewfinderView.fieldsPosition;
            recogResultAdapter.notifyDataSetChanged();
            recogResult_listView.requestFocusFromTouch();
            if (CameraActivity.recogResultAdapter.selectedRecogResultPosition == 0) {
              recogResult_listView.setSelection(0);
            } else {
              recogResult_listView
                .setSelection(CameraActivity.recogResultAdapter.selectedRecogResultPosition - 1);
            }
            isTakePic = false;
          }
          if (isClick) {
            savePath.remove(position);
            isClick = false;
          }
          savePath.add(position, SavePicPath);
          mVibrator = (Vibrator) getApplication().getSystemService(
            Service.VIBRATOR_SERVICE);
          mVibrator.vibrate(200);
          // 如果识别条目只有一个 则直接跳转到结果显示界面 否则显示确定按钮 点击之后才进行跳转

          if (recogResultModelList.size() == 1) {
            // 可以避免有时进入相机界面布局混乱的问题
            CameraActivity.this.runOnUiThread(updateUI);


          }
        }


        // 测试 存储每一帧图像
        // savePicture(data, recogResultString);

      } else {

        if (isToastShow) {
          Toast.makeText(getApplicationContext(),
            "识别错误，错误码：" + returnResult, Toast.LENGTH_LONG)
            .show();
          isToastShow = false;

        }

      }

    }

  }

  /*  final int requestCodeVin = 1010;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == requestCodeVin && resultCode == RESULT_OK) {
            String carNo = data.getStringExtra("car");
            Intent car = getIntent();
            car.putExtra("car", carNo);
            setResult(RESULT_OK, car);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }*/


  public String saveROIPicture(byte[] data1, boolean bol) {
    // huangzhen测试
    String picPathString1 = "";
    if (!wlci.fieldType.get(
      wlci.template.get(selectedTemplateTypePosition).templateType)
      .get(myViewfinderView.fieldsPosition).ocrId
      .equals("SV_ID_BARCODE_QR")) {
      String PATH = Environment.getExternalStorageDirectory().toString()
        + "/DCIM/Camera/";
      File file = new File(PATH);
      if (!file.exists()) {
        file.mkdirs();
      }
      String name = Utils.pictureName();
      picPathString1 = PATH + "smartVisition" + name + ".jpg";

      if (bol) {
        // 识别区域内图片
        recogBinder.svSaveImage(picPathString1);
      } else {
        // 识别成功剪切的图片

        recogBinder.svSaveImageResLine(picPathString1);
      }
    }
    return picPathString1;
  }


  /**
   * @param data1
   * @param type  0为保存全图 目的为了报错显示图片，其他为了点击拍照按钮，传入正图给核心
   * @return
   */
  public String savePicture(byte[] data1, int type) {
    if (type == 0) {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = Config.ARGB_8888;
      options.inPurgeable = true;
      options.inInputShareable = true;
      YuvImage yuvimage = new YuvImage(data1, ImageFormat.NV21, size.width,
        size.height, null);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      yuvimage.compressToJpeg(new Rect(0, 0, size.width, size.height), 80,
        baos);
      String picPathString2 = "";
      String PATH = Environment.getExternalStorageDirectory().toString()
        + "/DCIM/Camera/";
      String name = Utils.pictureName();
      picPathString2 = PATH + "smartVisitionComplete" + name + ".jpg";
      File dirs = new File(PATH);
      if (!dirs.exists()) {
        dirs.mkdirs();
      }
      File file = new File(picPathString2);
      if (file.exists()) {
        file.delete();
      }
      FileOutputStream outStream = null;
      try {
        outStream = new FileOutputStream(picPathString2);
        outStream.write(baos.toByteArray());
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        try {
          outStream.close();
          baos.close();
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
      return picPathString2;

    } else {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = Config.ARGB_8888;
      options.inPurgeable = true;
      options.inInputShareable = true;
      YuvImage yuvimage = new YuvImage(data1, ImageFormat.NV21,
        size.width, size.height, null);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      yuvimage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100,
        baos);
      bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(),
        0, baos.size(), options);
      Matrix matrix = new Matrix();
      matrix.reset();
      if (rotation == 90) {
        matrix.setRotate(90);
      } else if (rotation == 180) {
        matrix.setRotate(180);
      } else if (rotation == 270) {
        matrix.setRotate(270);
      }
      bitmap = Bitmap
        .createBitmap(bitmap, 0, 0, bitmap.getWidth(),
          bitmap.getHeight(), matrix, true);
      String picPathString2 = "";
      String PATH = Environment.getExternalStorageDirectory().toString()
        + "/DCIM/Camera/";
      String name = Utils.pictureName();
      picPathString2 = PATH + "smartVisitionComplete" + name + ".jpg";
      File file = new File(picPathString2);
      if (!file.exists()) {
        file.mkdirs();
      }
      if (file.exists()) {
        file.delete();
      }
      try {
        file.createNewFile();
        BufferedOutputStream bos = new BufferedOutputStream(
          new FileOutputStream(file));

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();

      } catch (Exception e) {
        e.printStackTrace();
      }
      return picPathString2;


    }


  }

  public void autoFocus() {

    if (camera != null) {
      try {
        if (camera.getParameters().getSupportedFocusModes() != null
          && camera.getParameters().getSupportedFocusModes()
          .contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
          camera.autoFocus(null);
        } else {
          Toast.makeText(
            getBaseContext(),
            getString(getResources().getIdentifier(
              "unsupport_auto_focus", "string",
              getPackageName())), Toast.LENGTH_LONG)
            .show();
        }

      } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(
          this,
          getResources().getIdentifier("toast_autofocus_failure",
            "string", getPackageName()), Toast.LENGTH_SHORT)
          .show();

      }
    }
  }

  public void OpenCameraAndSetParameters() {
    try {
      if (null == camera) {
        camera = CameraSetting.getInstance(CameraActivity.this).open(0,
          camera);
        rotation = CameraSetting.getInstance(CameraActivity.this)
          .setCameraDisplayOrientation(uiRot);
        if (!isFirstIn) {
          if (islandscape) {
            CameraSetting.getInstance(CameraActivity.this)
              .setCameraParameters(CameraActivity.this,
                surfaceHolder, CameraActivity.this,
                camera, (float) srcWidth / srcHeight,
                srcList, false, rotation);
          } else {
            CameraSetting.getInstance(CameraActivity.this)
              .setCameraParameters(CameraActivity.this,
                surfaceHolder, CameraActivity.this,
                camera, (float) srcHeight / srcWidth,
                srcList, false, rotation);
          }
        }
        isFirstIn = false;
        if (timer == null) {
          timer = new TimerTask() {
            public void run() {

              if (camera != null) {

                try {
                  autoFocus();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }

            ;
          };
        }
        if (timeAuto == null) {
          timeAuto = new Timer();
        }
        if (Utils.isCPUInfo64()) {
          timeAuto.schedule(timer, 2000, 2000);
        } else {
          timeAuto.schedule(timer, 3000, 3000);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void CloseCameraAndStopTimer(int type) {

    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (timeAuto != null) {
      timeAuto.cancel();
      timeAuto = null;
    }
    if (camera != null) {
      if (type == 1) {
        if (camera != null) {
          camera.setPreviewCallback(null);
          camera.stopPreview();
        }
      } else {
        camera = CameraSetting.getInstance(CameraActivity.this)
          .closeCamera(camera);
      }

    }
  }
}
