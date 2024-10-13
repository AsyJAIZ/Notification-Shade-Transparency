package com.asyjaiz.A12blur;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("set", MODE_WORLD_READABLE);

        ImageView shade = findViewById(R.id.shade);
        LayerDrawable drawable = (LayerDrawable) shade.getDrawable();
        GradientDrawable behindScr = (GradientDrawable) drawable.findDrawableByLayerId(R.id.behindScrim);
        GradientDrawable notifScr = (GradientDrawable) drawable.findDrawableByLayerId(R.id.notificationScrim);

        Slider behindAlpha = findViewById(R.id.behind);
        Slider notifAlpha = findViewById(R.id.notif);
        MaterialSwitch auto = findViewById(R.id.auto);
        behindAlpha.setValue(prefs.getFloat(behindAlpha.getTag().toString(), 1f));
        notifAlpha.setValue(prefs.getFloat(notifAlpha.getTag().toString(), 1f));
        auto.setChecked(prefs.getBoolean("auto", true));

        SharedPreferences.Editor editor = prefs.edit();
        Slider.OnChangeListener listener = (slider, value, fromUser) -> {
            String key = slider.getTag().toString();

            int alpha = (int) (value * 255);
            if (key.contains("behind"))
                behindScr.setAlpha(alpha);
            else {
                notifScr.setAlpha(alpha);
                if (prefs.getBoolean("auto", true)) {
                    auto.setChecked(false);
                }
            }
            editor.putFloat(key, value).apply();
        };
        behindAlpha.addOnChangeListener(listener);
        notifAlpha.addOnChangeListener(listener);
        auto.setOnCheckedChangeListener((buttonView, isChecked) -> editor.putBoolean("auto", isChecked));
    }
}