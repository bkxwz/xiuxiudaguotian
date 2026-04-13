package com.xiuxiudaguotian;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "leave_data";
    private static final String KEY_LEAVES = "leaves";
    private static final int TOTAL_SLOTS = 6;

    private GridLayout gridLayout;
    private TextView tvRemaining;
    private Button btnReset;
    private List<LeaveSlot> leaveSlots = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        gridLayout = findViewById(R.id.gridLayout);
        tvRemaining = findViewById(R.id.tvRemaining);
        btnReset = findViewById(R.id.btnReset);

        initSlots();
        loadData();
        updateUI();
        
        btnReset.setOnClickListener(v -> resetData());
    }

    private void initSlots() {
        leaveSlots.clear();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            leaveSlots.add(new LeaveSlot());
        }
        
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            View slotView = LayoutInflater.from(this).inflate(R.layout.item_slot, gridLayout, false);
            
            LinearLayout container = slotView.findViewById(R.id.slotContainer);
            TextView tvSlot = slotView.findViewById(R.id.tvSlot);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.rowSpec = GridLayout.spec(i / 3);
            params.setMargins(12, 12, 12, 12);
            container.setLayoutParams(params);
            
            final int index = i;
            container.setOnClickListener(v -> onSlotClick(index));
            
            container.setTag(tvSlot);
            slotView.setTag(container);
            
            gridLayout.addView(slotView);
        }
    }

    private void onSlotClick(int index) {
        LeaveSlot slot = leaveSlots.get(index);
        if (!slot.used) {
            showDateDialog(index);
        }
    }

    private void showDateDialog(int index) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_date, null);
        
        NumberPicker datePicker = dialogView.findViewById(R.id.datePicker);
        RadioGroup timeGroup = dialogView.findViewById(R.id.timeGroup);
        RadioButton rbMorning = dialogView.findViewById(R.id.rbMorning);
        RadioButton rbAfternoon = dialogView.findViewById(R.id.rbAfternoon);
        
        int currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH);
        datePicker.setMinValue(1);
        datePicker.setMaxValue(30);
        datePicker.setValue(currentDay);
        
        new AlertDialog.Builder(this)
            .setTitle("选择日期")
            .setView(dialogView)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                int day = datePicker.getValue();
                String time = rbMorning.isChecked() ? "上午" : "下午";
                
                LeaveSlot slot = leaveSlots.get(index);
                slot.used = true;
                slot.day = day;
                slot.timePeriod = time;
                
                saveData();
                updateUI();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void updateUI() {
        int usedCount = 0;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            View slotView = gridLayout.getChildAt(i);
            LinearLayout container = (LinearLayout) slotView.findViewById(R.id.slotContainer);
            TextView tvSlot = (TextView) slotView.findViewById(R.id.tvSlot);
            
            LeaveSlot slot = leaveSlots.get(i);
            if (slot.used) {
                container.setBackgroundResource(R.drawable.slot_used_background);
                tvSlot.setText(slot.day + "日 " + slot.timePeriod);
                tvSlot.setTextColor(getResources().getColor(R.color.text_black));
                usedCount++;
            } else {
                container.setBackgroundResource(R.drawable.slot_background);
                tvSlot.setText(R.string.click_to_book);
                tvSlot.setTextColor(getResources().getColor(R.color.text_white));
            }
        }
        
        int remaining = TOTAL_SLOTS - usedCount;
        tvRemaining.setText("剩余假期: " + (remaining / 2) + "天" + (remaining % 2 == 1 ? "半" : ""));
    }

    private void saveData() {
        JSONArray array = new JSONArray();
        for (LeaveSlot slot : leaveSlots) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("used", slot.used);
                obj.put("day", slot.day);
                obj.put("time", slot.timePeriod);
            } catch (Exception e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        prefs.edit().putString(KEY_LEAVES, array.toString()).apply();
    }

    private void loadData() {
        String json = prefs.getString(KEY_LEAVES, "");
        if (!json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length() && i < TOTAL_SLOTS; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    leaveSlots.get(i).used = obj.getBoolean("used");
                    leaveSlots.get(i).day = obj.optInt("day", 0);
                    leaveSlots.get(i).timePeriod = obj.optString("time", "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetData() {
        new AlertDialog.Builder(this)
            .setTitle("确认重置")
            .setMessage("确定要重置所有假期吗？")
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                for (LeaveSlot slot : leaveSlots) {
                    slot.used = false;
                    slot.day = 0;
                    slot.timePeriod = "";
                }
                saveData();
                updateUI();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    static class LeaveSlot {
        boolean used = false;
        int day = 0;
        String timePeriod = "";
    }
}