package com.asyjaiz.A12blur;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
    public static final String[] scrimList = {"mScrimBehind", "mNotificationsScrim", "mScrimInFront"}; // , "mScrimForBubble"};
    public static final Float maxBlurDefault = 23f;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {prefs = getPreferences(MODE_WORLD_READABLE);}
        catch (SecurityException ignore) {
            prefs = getPreferences(MODE_PRIVATE);
            Snackbar snackbar = Snackbar.make(findViewById(R.id.MainActivity), R.string.activateWarning, 3000);
            final View snackBarView = snackbar != null ? snackbar.getView() : null;
            final TextView tv = snackbar != null? snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) : null;
            tv.setAutoLinkMask(Linkify.ALL);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            snackbar.show();

        }

        fadeIn.setDuration(250);

        Slider maxBlur = findViewById(R.id.maxBlur);
        EditText maxBlurValue = findViewById(R.id.maxBlurValue);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list, scrimList);
        TextInputLayout scrim = findViewById(R.id.selScrim);
        AutoCompleteTextView selector = ((AutoCompleteTextView) scrim.getEditText());
        assert selector != null;
        selector.setAdapter(adapter);

        Slider alphaValue = findViewById(R.id.alphaValue);

        maxBlur.addOnChangeListener((slider, value, fromUser) -> maxBlurValue.setText(String.valueOf(Math.round(value))));
        maxBlur.setValue(prefs.getFloat("max_window_blur_radius", maxBlurDefault));
        final boolean[] warn = {false};
        maxBlurValue.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @SuppressLint("ResourceType")
            @Override
            public void afterTextChanged(Editable s) {
                int value;

                if (s.toString().equals(""))
                    value = 0;
                else value = Integer.parseInt(s.toString());

                if (!(prefs.getFloat("max_window_blur_radius", 23f) == (float) value))
                {
                    if (value > 23) {
                        if (!warn[0]) {
                            maxBlur.setValueTo(150f);
                            Toast.makeText(MainActivity.this, R.string.maxBlurWarning, LENGTH_SHORT).show();
                            warn[0] = true;
                        }
                    }

                    if (value < 0) {
                        maxBlurValue.setText(String.valueOf(0));
                        maxBlur.setValue(0);
                    }
                    else if (value > 150) {
                        maxBlurValue.setText(String.valueOf(150));
                        maxBlur.setValue(150);
                    }
                    else maxBlur.setValue(value);

                    prefs.edit().putFloat("max_window_blur_radius", value).apply();
                }
            }
        });
        selector.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (alphaValue.getVisibility() == View.INVISIBLE) {
                    fadeIn.setFillAfter(true);
                    alphaValue.setVisibility(View.VISIBLE);
                    alphaValue.startAnimation(fadeIn);
                }

                alphaValue.setValue(prefs.getFloat(s.toString(), 1.0f));
            }
        });

        alphaValue.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser)
                prefs.edit().putFloat(selector.getText().toString(), value).apply();
        });

    }
}