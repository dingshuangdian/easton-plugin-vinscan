package vinscan.ocr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;


import com.kernal.smartvisionocr.utils.Utils;
import com.kernal.smartvisionocr.utils.WriteToPCTask;
import com.kernal.vinparseengine.VinParseInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vinscan.adapter.ShowResultListAdapter;
import vinscan.adapter.VinParseResultAdapter;
import vinscan.utils.EditTextWatcher;
import vinscan.utils.Utills;

/**
 *
 * 项目名称：慧视OCR 类名称：ShowResultActivity 类描述：显示界面 创建人：黄震 创建时间：2016-04-15 修改人：${user}
 * 修改时间：${date} ${time} 修改备注：
 *
 * @version
 *
 */
public class ShowResultActivity extends Activity {
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private int srcWidth, srcHeight;
	private RelativeLayout FLayout_recogResult;
	private TextView tv_recogResult, vin_et_FieldName;
	private ListView list_recogRecog, list_vinParse;
	private ShowResultListAdapter adapter;
	private VinParseResultAdapter VPRadapter;
	private ArrayList<String> recogResult;
	private ImageButton imbtn_submit, imbtn_back,btn_feed;
	private Intent intent;
	private String templateName = "";
	public static int selectedTemplateTypePosition = 0;
	private ArrayList<String> picturePath;// 图片路径集合
	private double screenInches;
	private VinParseInfo vpi;
	private List<HashMap<String, String>> vinInfo;
	private EditTextWatcher myNum1_show;
	private EditText editTextTemp;
	private ImageView image;

	private int[] regionPos;
	private Bitmap bitmap;
	private String SavePicPath;
	private View Dialogview;
	private String httpPath;
	private int rotation;
	// public static boolean isFirst=true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		// activity_show_result
		setContentView(getResources().getIdentifier("activity_show_result",
				"layout", getPackageName()));
		srcWidth = displayMetrics.widthPixels;
		srcHeight = displayMetrics.heightPixels;
		// 启动activity时不自动弹出软键盘
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		// hiddenVirtualButtons(getWindow().getDecorView());
		// android设备的物理尺寸
		double x = Math
				.pow(displayMetrics.widthPixels / displayMetrics.xdpi, 2);
		double y = Math.pow(displayMetrics.heightPixels / displayMetrics.ydpi,
				2);
		screenInches = Math.sqrt(x + y);

		intent = getIntent();
		templateName = intent.getStringExtra("templateName");
		regionPos  = intent.getIntArrayExtra("regionPos");
		httpPath = intent.getStringExtra("httpPath");
		rotation = intent.getIntExtra("rotation",-1);
		findView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		recogResult = new ArrayList<String>();
		picturePath = new ArrayList<String>();
		recogResult.clear();
		picturePath.clear();
		tv_recogResult.setText(templateName + "识别结果");
		recogResult = intent.getStringArrayListExtra("recogResult");
		picturePath = intent.getStringArrayListExtra("savePath");

		if (templateName.equals("VIN码")) {
			vpi = new VinParseInfo();
			vinInfo = vpi.getVinParseInfo(recogResult.get(0).split(":")[1]);
			VPRadapter = new VinParseResultAdapter(ShowResultActivity.this,
					vinInfo, srcWidth, srcHeight);
			list_vinParse.setAdapter(VPRadapter);
		}
		adapter = new ShowResultListAdapter(ShowResultActivity.this, srcWidth,
				srcHeight, recogResult, picturePath, screenInches, VPRadapter);
		list_recogRecog.setAdapter(adapter);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

	}

	/**
	 * @Title: findView
	 * @Description: 显示界面UI布局
	 * @return void 返回类型
	 * @throws
	 */
	private void findView() {
		// TODO Auto-generated method stub

		FLayout_recogResult = (RelativeLayout) this.findViewById(getResources()
				.getIdentifier("FLayout_recogResult", "id", getPackageName()));
		tv_recogResult = (TextView) this.findViewById(getResources()
				.getIdentifier("tv_recogResult", "id", getPackageName()));
		list_recogRecog = (ListView) this.findViewById(getResources()
				.getIdentifier("list_recogRecog", "id", getPackageName()));
		list_vinParse = (ListView) this.findViewById(getResources()
				.getIdentifier("list_vinParse", "id", getPackageName()));
		imbtn_submit = (ImageButton) this.findViewById(getResources()
				.getIdentifier("imbtn_submit", "id", getPackageName()));
		btn_feed = (ImageButton) this.findViewById(getResources()
				.getIdentifier("imbtn_feedback", "id", getPackageName()));
		imbtn_back = (ImageButton) this.findViewById(getResources()
				.getIdentifier("imbtn_back", "id", getPackageName()));
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				srcWidth, (int) (srcHeight * 0.06));
		layoutParams.topMargin = 0;
		FLayout_recogResult.setLayoutParams(layoutParams);
		LayoutParams layoutParams01 = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParams01.addRule(RelativeLayout.CENTER_IN_PARENT);
		tv_recogResult.setLayoutParams(layoutParams01);
		layoutParams01 = new LayoutParams(
				(int) (srcWidth * 0.9), (int) (srcHeight * 0.65));
		layoutParams01.topMargin = (int) (srcHeight * 0.05);
		layoutParams01.leftMargin = (int) (srcWidth * 0.05);
		list_recogRecog.setLayoutParams(layoutParams01);
		if (templateName.equals("VIN码")) {
			list_vinParse.setVisibility(View.VISIBLE);
			layoutParams01 = new LayoutParams(
					(int) (srcWidth * 0.9), (int) (srcHeight * 0.45));
			layoutParams01.topMargin = (int) (srcHeight * 0.34);
			layoutParams01.leftMargin = (int) (srcWidth * 0.05);
			list_vinParse.setLayoutParams(layoutParams01);
		}
		layoutParams01 = new LayoutParams(
				(int) (srcWidth * 0.7), (int) (srcHeight * 0.055));
		if (templateName.equals("VIN码")) {

			layoutParams01.topMargin = (int) (srcHeight * 0.8);
		} else {
			layoutParams01.topMargin = (int) (srcHeight * 0.72);
		}
		layoutParams01.leftMargin = (int) (srcWidth * 0.15);
		imbtn_submit.setLayoutParams(layoutParams01);
		imbtn_submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(SavePicPath!=null&&!SavePicPath.equals("")){
					DeletePic(SavePicPath);
				}
				Intent intent = new Intent(ShowResultActivity.this,
						MainActivity.class);
				ShowResultActivity.this.finish();
				startActivity(intent);
			}
		});
		//报错 将错误信息及样本图发送至我们服务器  没有需求的客户可以不写下边东西
		layoutParams01 = new LayoutParams(
				(int) (srcWidth * 0.7), (int) (srcHeight * 0.055));
		if (templateName.equals("VIN码")) {
			layoutParams01.topMargin = (int) (srcHeight * 0.86);
		} else {
			layoutParams01.topMargin = (int) (srcHeight * 0.78);
		}
		layoutParams01.leftMargin = (int) (srcWidth * 0.15);
		btn_feed.setLayoutParams(layoutParams01);
		saveHttpPic(httpPath);
		btn_feed.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//上传步骤
				LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				Dialogview = inflater.inflate(getResources().getIdentifier("dialog_layout","layout",getPackageName()), null);
				editTextTemp = (EditText) Dialogview .findViewById(getResources().getIdentifier("dialog_editText_result","id",getPackageName()));
				image = (ImageView) Dialogview .findViewById(getResources().getIdentifier("dialog_imageView_result","id",getPackageName()));
				image.setImageBitmap(bitmap);
				new  AlertDialog.Builder(ShowResultActivity.this)
						.setTitle("VIN报错提示")
						.setIcon(android.R.drawable.ic_dialog_info)
						.setView( Dialogview )
						.setPositiveButton("确定", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if(Utills.CheckInternet(ShowResultActivity.this)){

									String[] httpContent =new String[]{SavePicPath, editTextTemp.getText().toString()};
									new WriteToPCTask(ShowResultActivity.this)
											.execute(httpContent);
								}else{
									Toast.makeText(ShowResultActivity.this, "请检查网络！", Toast.LENGTH_SHORT).show();
								}
							}

						})
						.setNegativeButton("取消", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();

			}
		});
		//报错 end
		layoutParams01 = new LayoutParams(
				(int) (srcWidth * 0.18), (int) (srcHeight * 0.03));
		layoutParams01.topMargin = (int) (srcHeight * 0.0155);
		layoutParams01.leftMargin = (int) (srcWidth * 0.02);
		imbtn_back.setLayoutParams(layoutParams01);
		imbtn_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				intent = new Intent(ShowResultActivity.this,
						CameraActivity.class);
				// intent.putExtra("selectedTemplateTypePosition",selectedTemplateTypePosition);
				if(SavePicPath!=null&&!SavePicPath.equals("")){
					DeletePic(SavePicPath);
				}
				ShowResultActivity.this.finish();
				startActivity(intent);
			}
		});
		// View =list_recogRecog.getLayoutParams();

	}

	/**
	 * @param mDecorView
	 *            {tags} 设定文件
	 * @return ${return_type} 返回类型
	 * @throws
	 * @Title: 沉寂模式
	 * @Description: 隐藏虚拟按键
	 */
	@TargetApi(19)
	public static void hiddenVirtualButtons(View mDecorView) {
		if (Build.VERSION.SDK_INT >= 19) {
			mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE);
		}
	}

	// @Override
	// public void onClick(View view) {
	// switch (view.getId())
	// {
	// case R.id.imbtn_submit:
	// Intent intent =new Intent(ShowResultActivity.this,MainActivity.class);
	// ShowResultActivity.this.finish();
	// startActivity(intent);
	// break;
	// case R.id.imbtn_back:
	// intent =new Intent(ShowResultActivity.this,CameraActivity.class);
	// //
	// intent.putExtra("selectedTemplateTypePosition",selectedTemplateTypePosition);
	// ShowResultActivity.this.finish();
	// startActivity(intent);
	// break;
	// }
	// }
	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			intent = new Intent(ShowResultActivity.this, CameraActivity.class);
			// intent.putExtra("selectedTemplateTypePosition",selectedTemplateTypePosition);
			ShowResultActivity.this.finish();
			startActivity(intent);
		}
		return true;
	}
//	public void saveHttpPic(int preW,int preH,int rotation){
	public void saveHttpPic(String path){

//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//		options.inPurgeable = true;
//		options.inInputShareable = true;
//		YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, preW,
//				preH, null);
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		yuvimage.compressToJpeg(new Rect(0,0, preW,preH), 100,
//				baos);
//		 bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0,
//				baos.size(), options);
//		Matrix matrix = new Matrix();
//		matrix.reset();
//		if (rotation == 90) {
//			matrix.setRotate(90);
//		} else if (rotation == 180) {
//			matrix.setRotate(180);
//		} else if (rotation == 270) {
//			matrix.setRotate(270);
//		}
		bitmap = BitmapFactory.decodeFile(path);
		Matrix matrix = new Matrix();
		matrix.reset();
		if (rotation == 90) {
			matrix.setRotate(90);
		} else if (rotation == 180) {
			matrix.setRotate(180);
		} else if (rotation == 270) {
			matrix.setRotate(270);
		}
		bitmap = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(),
				bitmap.getHeight(),matrix,true);
		bitmap = Bitmap.createBitmap(bitmap, regionPos[0], regionPos[1], regionPos[2]-regionPos[0],
				regionPos[3]-regionPos[1]);
		SavePicPath="";
		String PATH = Environment.getExternalStorageDirectory().toString()
				+ "/DCIM/Camera/";

		File dirs = new File(PATH);
		if (!dirs.exists()) {
			dirs.mkdirs();
		}
		String name = Utils.pictureName();
		if(templateName.equals("VIN码")){
			SavePicPath = PATH + "Android_VIN_" + name + ".jpg";
		}else {
			SavePicPath = PATH + "Android_PhoneNumber_" + name + ".jpg";
		}
		File file = new File(SavePicPath);
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
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.parse("file://" +SavePicPath )));
		DeletePic(path);


//		return bitmap;
	};
	private void DeletePic(String path){
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.parse("file://" +path )));
	}
}
