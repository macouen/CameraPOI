package com.oakzmm.camerapoi;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.oakzmm.camerapoi.model.CameraPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * TODO: 2015/12/9
 * 1. 0~360 的平滑过度
 * 2. 相机的自动聚焦
 * 3. poiItem相互的遮挡 get
 * 4. 展示添加POI距离 get
 * 5. 半透明背景 get
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final int width = 3600;
    private static final int height = 1000;
    private static final int radius = 1000;
    private final List<CameraPoint> pointList = new ArrayList<>();
    private final Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        public void onAutoFocus(boolean autoFocusSuccess, Camera arg1) {
            Log.i(TAG, "AutoFocus");
        }
    };
    @Bind(R.id.hsvLabels)
    HorizontalScrollView hsvLabels;
    @Bind(R.id.sensorData)
    TextView sensorData;
    @Bind(R.id.layout)
    RelativeLayout layout;
    @Bind(R.id.sv_camera)
    SurfaceView svCamera;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ArrayList<PoiItem> items;
    private LatLonPoint coo;
    //添加方向
    private SensorManager sensorManager;
    private MySensorEventListener mySensorEventListener;
    private int widthPixels;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        //搜索到的POI 点
        items = getIntent().getParcelableArrayListExtra("POI");
        //中心点
        coo = getIntent().getParcelableExtra("COO");
        initData();
//        intiLabelView();

    }

    @Override
    protected void onResume() {
        Sensor sensor_orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(mySensorEventListener, sensor_orientation, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                intiLabelView();
            }
        }, 500);
    }


    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(mySensorEventListener);
        releaseCamera();
    }

    /**
     * 初始化相关data
     */
    private void initData() {
        // 获得句柄
        mHolder = svCamera.getHolder();
        // 添加回调
        mHolder.addCallback(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mySensorEventListener = new MySensorEventListener();
        widthPixels = getResources().getDisplayMetrics().widthPixels;
        Log.d(TAG, "widthPixels" + widthPixels);
    }

    private void intiLabelView() {
        layout.setBackgroundColor(Color.TRANSPARENT);
        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
        int count = items.size();
        layoutParams.width = width + widthPixels;
        layoutParams.height = height + 100;
        layout.setLayoutParams(layoutParams);
        pointList.clear();
        for (int i = 0; i < count; i++) {
            final PoiItem poiItem = items.get(i);
            final CameraPoint cameraPoint = new CameraPoint();
            cameraPoint.setTitle(poiItem.getTitle());
            cameraPoint.setUrl(poiItem.getWebsite());
            int distance = poiItem.getDistance();
            final double angle = calAngle(coo, poiItem.getLatLonPoint());
            int left = (int) (angle * width / 360) + widthPixels / 2;
            int top = (radius - distance) * height / radius;
            cameraPoint.setAngle((int) angle);
            cameraPoint.setDistance(distance);
            cameraPoint.setPoint(new Point(left, top));
            pointList.add(cameraPoint);
//            layoutChildView(cameraPoint);
        }
        layout.removeAllViews();

        reviseViewLayout(pointList);

    }

    /**
     * 校正childView的位置 位置重叠 遮挡
     * fixme 并不能保证所有的点完全的不重叠 有待改进
     *
     * @param points
     */
    private void reviseViewLayout(List<CameraPoint> points) {
        Collections.sort(points, new Comparator<CameraPoint>() {
            @Override
            public int compare(CameraPoint lhs, CameraPoint rhs) {
                return lhs.getAngle() - rhs.getAngle();
            }
        });
        int count = points.size();
        int referLeft;
        int referRight;
        int referTop;
        int referBottom;
        for (int i = 0; i < count; i++) {
            CameraPoint cameraPoint = points.get(i);
            if (i > 0) {
                TextView referView = (TextView) layout.getChildAt(i - 1);
                CameraPoint tempPoint = points.get(i - 1);
                referView.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                referLeft = tempPoint.getPoint().x;
                referRight = referLeft + referView.getMeasuredWidth();
                referTop = tempPoint.getPoint().y;
                referBottom = referTop + referView.getMeasuredHeight();
                Log.i(TAG, "left:" + referLeft + " right:" + referRight + "  top:" + referTop + " bottom:" + referBottom);
                int left = cameraPoint.getPoint().x;
                int top = cameraPoint.getPoint().y;
                TextView textView = getTextView(cameraPoint);
                textView.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                int height = textView.getMeasuredHeight();
                Log.i(TAG, "height:" + height);
                int bottom = top + height;
                if (left >= referLeft && left <= referRight) {
                    if (top >= referTop && top <= referBottom) {
                        top = top + height;
                        cameraPoint.setPoint(new Point(left, top));
                    } else if (bottom >= referTop && bottom <= referBottom) {
                        top = top - height;
                        cameraPoint.setPoint(new Point(left, top));
                    }
                }
            }
            layoutChildView(cameraPoint);
        }
    }

    /**
     * add childView
     *
     * @param cameraPoint
     */
    private void layoutChildView(CameraPoint cameraPoint) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        final TextView textView = getTextView(cameraPoint);
        params.leftMargin = cameraPoint.getPoint().x;
        params.topMargin = cameraPoint.getPoint().y;
        layout.addView(textView, params);
    }

    /**
     * getTextView
     *
     * @param poiItem
     * @return
     */
    @NonNull
    private TextView getTextView(final CameraPoint poiItem) {
        final TextView textView = new TextView(this);
        textView.setBackgroundColor(Color.parseColor("#30FFFFFF"));
        textView.setTextColor(Color.WHITE);
        textView.setText(poiItem.getTitle() + "\n" + poiItem.getDistance() + "m");
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(15, 15, 15, 15);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 2015/12/14 跳转到详情页
                Log.i(TAG, poiItem.getTitle());
            }
        });
        return textView;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 当surfaceView关闭时，关闭预览并释放资源
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
        holder = null;
        svCamera = null;
    }

    /**
     * 设置camera显示取景画面,并预览
     *
     * @param camera
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.checkCameraHardware(this) && (mCamera == null)) {
            // 打开camera
            mCamera = getCamera();
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(ImageFormat.JPEG);
//            params.setPreviewSize(1920, 1080);
            params.setRotation(90);
            // 自动对焦
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(params);
            setCameraFocus(myAutoFocusCallback);
            if (mHolder != null) {
                setStartPreview(mCamera, mHolder);
            }
        }
    }

    public void setCameraFocus(Camera.AutoFocusCallback autoFocus) {
        if (mCamera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                mCamera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
            Log.i(TAG, "setAuto");
            mCamera.autoFocus(autoFocus);
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }

    /**
     * 获取camera
     *
     * @return
     */
    private Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            camera = null;
            Log.e(TAG, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 将短距离的经纬度视为平面坐标
     * 计算两点之间的角度 Y坐标为上(北)方向 X坐标向右
     *
     * @param coo      中心点坐标
     * @param position 要计算的点的坐标
     * @return
     */
    private float calAngle(LatLonPoint coo, LatLonPoint position) {
        final double y0 = coo.getLatitude();
        final double x0 = coo.getLongitude();
        final double evX = position.getLongitude();
        final double evY = position.getLatitude();
        final double L = Math.sqrt((evX - x0) * (evX - x0) + (evY - y0) * (evY - y0));
        double Lsin = Math.abs(evX - x0);
        float asin0 = radiansToDegrees((float) Math.asin(Lsin / L));
        asin0 = evY < y0 ? 180 - asin0 : asin0;
        asin0 = evX < x0 ? 360 - asin0 : asin0;
        return asin0;

    }

    /**
     * 弧度转换为角度
     */
    private float radiansToDegrees(float radians) {
        return (float) (radians / Math.PI * 180f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //添加方向监听
    private final class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                //x表示手机指向的方位，0表示北,90表示东，180表示南，270表示西
                final float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                String orientation = "方向" + "X:" + x;
                hsvLabels.post(new Runnable() {
                    @Override
                    public void run() {
                        //根据角度hsv滑动到相应的位置。
                        hsvLabels.smoothScrollTo((int) (width * x) / 360, 0);
                    }
                });
                sensorData.setText(orientation);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


}
