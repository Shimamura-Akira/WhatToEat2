package com.example.whattoeat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.widget.EditText;
import android.widget.Toast;
import android.view.HapticFeedbackConstants;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import android.app.ProgressDialog;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.example.whattoeat.databinding.ActivityMainBinding;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.transition.TransitionManager;
import com.google.android.material.transition.MaterialSharedAxis;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private FoodAdapter adapter;
    private DataManager dataManager;
    private List<FoodItem> foodList;

    private Handler handler;
    private Random random;
    private boolean isRolling = false;
    private Runnable rollRunnable;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    private long lastShakeTime = 0;
    private boolean isNear = false;
    private static final float SHAKE_THRESHOLD = 15.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        handler = new Handler(Looper.getMainLooper());
        random = new Random();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        initData();
        initView();
    }

    private void updateRouletteData() {
        List<String> activeNames = new ArrayList<>();
        for (FoodItem item : foodList) {
            if (item.isEnabled()) {
                activeNames.add(item.getName());
            }
        }
        binding.rouletteView.setData(activeNames);
    }

    private void initData() {
        dataManager = new DataManager(this);
        foodList = new ArrayList<>(dataManager.getFoodList());
    }

    private void initView() {
        adapter = new FoodAdapter(
            item -> {
                foodList.remove(item);
                dataManager.saveFoodList(foodList);
                adapter.submitList(new ArrayList<>(foodList));
                updateRouletteData();
            },
            (item, isChecked) -> {
                item.setEnabled(isChecked);
                dataManager.saveFoodList(foodList);
                adapter.submitList(new ArrayList<>(foodList));
                updateRouletteData();
            },
            item -> showEditDialog(item),
            item -> openMap(item.getName())
        );
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(foodList));
        updateRouletteData();

        binding.fabAdd.setOnClickListener(v -> showAddDialog());
        binding.fabNearby.setOnClickListener(v -> fetchNearbyRestaurants());
        binding.btnStart.setOnClickListener(v -> startRolling());
        
        binding.rouletteView.setSpinListener((result, isRigged) -> {
             binding.btnStart.setEnabled(true);
             binding.tvResult.setText(result);
             isRolling = false;
             showResultDialog(result, isRigged);
        });

        // 设置底部导航栏点击事件
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int currentItemId = binding.bottomNavigation.getSelectedItemId();
            
            if (itemId == currentItemId) return true;

            // 增加平滑的左右滑动转场动画 (Material Shared Axis X)
            boolean forward = itemId == R.id.nav_list; // 从左(home)向右(list)滑动为正向
            MaterialSharedAxis sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.X, forward);
            sharedAxis.setDuration(300);
            TransitionManager.beginDelayedTransition(binding.main, sharedAxis);

            if (itemId == R.id.nav_home) {
                binding.groupHome.setVisibility(android.view.View.VISIBLE);
                binding.groupList.setVisibility(android.view.View.GONE);
                return true;
            } else if (itemId == R.id.nav_list) {
                binding.groupHome.setVisibility(android.view.View.GONE);
                binding.groupList.setVisibility(android.view.View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    private void openMap(String restaurantName) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(restaurantName));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有找到可以打开地图的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private void fetchNearbyRestaurants() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        try {
            Location lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastKnownGPS != null) location = lastKnownGPS;
            else if (lastKnownNetwork != null) location = lastKnownNetwork;
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (location == null) {
            Toast.makeText(this, "无法获取当前位置，请检查是否已开启定位开关", Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在搜索附近的美食...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // 使用 Overpass API 获取附近的餐厅信息 (不需要 API 密钥的大众版)
                String overpassQuery = "[out:json];node[\"amenity\"~\"restaurant|fast_food|cafe\"](around:2000," + lat + "," + lon + ");out 15;";
                URL url = new URL("https://overpass-api.de/api/interpreter");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(overpassQuery.getBytes("UTF-8"));
                }
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray elements = jsonResponse.getJSONArray("elements");

                    List<String> newRestaurants = new ArrayList<>();
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        if (element.has("tags")) {
                            JSONObject tags = element.getJSONObject("tags");
                            if (tags.has("name")) {
                                newRestaurants.add(tags.getString("name"));
                            }
                        }
                    }

                    handler.post(() -> {
                        progressDialog.dismiss();
                        if (newRestaurants.isEmpty()) {
                            Toast.makeText(MainActivity.this, "附近没有找到相关结果", Toast.LENGTH_SHORT).show();
                        } else {
                            // 过滤重复和空名字
                            Set<String> existingNames = new HashSet<>();
                            for (FoodItem item : foodList) existingNames.add(item.getName());
                            
                            int addedCount = 0;
                            for (String name : newRestaurants) {
                                if (!name.trim().isEmpty() && !existingNames.contains(name)) {
                                    foodList.add(new FoodItem(name));
                                    existingNames.add(name);
                                    addedCount++;
                                }
                            }
                            
                            if (addedCount > 0) {
                                dataManager.saveFoodList(foodList);
                                adapter.submitList(new ArrayList<>(foodList));
                                updateRouletteData();
                                Toast.makeText(MainActivity.this, "成功盲选获取了 " + addedCount + " 家附近美食！", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "获取的美食都已经在列表中了", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "网络请求失败，请稍后重试", Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "获取失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchNearbyRestaurants();
            } else {
                Toast.makeText(this, "需要定位权限才能获取附近的美食哦", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAddDialog() {
        EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("添加候选菜单")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = editText.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "菜单名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    for (FoodItem item : foodList) {
                        if (item.getName().equals(name)) {
                            Toast.makeText(this, "该菜单已存在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    FoodItem newItem = new FoodItem(name);
                    foodList.add(newItem);
                    dataManager.saveFoodList(foodList);
                    adapter.submitList(new ArrayList<>(foodList));
                    updateRouletteData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditDialog(FoodItem editItem) {
        EditText editText = new EditText(this);
        editText.setText(editItem.getName());
        editText.setSelection(editText.getText().length());
        
        new AlertDialog.Builder(this)
                .setTitle("编辑候选菜单")
                .setView(editText)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = editText.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "菜单名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (name.equals(editItem.getName())) {
                        return; // 没有修改
                    }

                    for (FoodItem item : foodList) {
                        if (item != editItem && item.getName().equals(name)) {
                            Toast.makeText(this, "该菜单已存在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // 寻找修改项在列表中的位置
                    int index = adapter.getCurrentList().indexOf(editItem);
                    if (index != -1) {
                        editItem.setName(name);
                        dataManager.saveFoodList(foodList);
                        adapter.submitList(new ArrayList<>(foodList));
                        adapter.notifyItemChanged(index);
                        updateRouletteData();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void startRolling() {
        if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            return;
        }

        if (isRolling) return;

        List<FoodItem> activeList = new ArrayList<>();
        for (FoodItem item : foodList) {
            if (item.isEnabled()) {
                activeList.add(item);
            }
        }
        
        if (activeList.isEmpty()) {
            Toast.makeText(this, "请先勾选至少一个候选菜单", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (activeList.size() == 1) {
            String result = activeList.get(0).getName();
            binding.tvResult.setText(result);
            binding.tvResult.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            showResultDialog(result, false);
            return;
        }

        isRolling = true;
        binding.btnStart.setEnabled(false);
        binding.tvResult.setText("等待抽取...");
        
        int winnerIndex = random.nextInt(activeList.size());
        boolean isRigged = activeList.size() > 1 && random.nextInt(100) < 20; // 20%的几率触发彩蛋
        binding.rouletteView.spin(winnerIndex, isRigged);
    }

    private void showResultDialog(String result, boolean isRigged) {
        String title = isRigged ? "哎呀！它滑过去了！" : "决定了！";
        String positiveBtn = isRigged ? "有黑幕！" : "太棒了";
        String message = isRigged ? "命中注定今天去吃：" + result + "！" : "我们今天去吃：" + result + "！";
        
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveBtn, null)
                .show();
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                // 计算去除了重力的加速度
                double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
                if (acceleration > SHAKE_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > 1000) { // 1秒防抖，避免重复触发
                        lastShakeTime = currentTime;
                        if (!isRolling) {
                            startRolling();
                        }
                    }
                }
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                // 距离小于特定阈值（通常很小，部分厂商直接返回最大或0代表远近）时，判定为靠近
                if (distance < proximitySensor.getMaximumRange() && distance < 5f) {
                    isNear = true;
                } else {
                    // 当从靠近变为远离时，触发抽选（也就是“挥手”的动作结束时）
                    if (isNear && !isRolling) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastShakeTime > 1000) { // 和摇一摇共用防抖
                            lastShakeTime = currentTime;
                            startRolling();
                        }
                    }
                    isNear = false;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (proximitySensor != null) {
                sensorManager.registerListener(sensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && rollRunnable != null) {
            handler.removeCallbacks(rollRunnable);
        }
    }
}