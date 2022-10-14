package com.asyjaiz.A12blur;

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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
    public static final String[] scrimList = {"mScrimBehind", "mNotificationsScrim", "mScrimInFront"}; // , "mScrimForBubble"};
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list, scrimList);
        TextInputLayout scrim = findViewById(R.id.selScrim);
        AutoCompleteTextView selector = ((AutoCompleteTextView) scrim.getEditText());
        assert selector != null;
        selector.setAdapter(adapter);

        Slider alphaValue = findViewById(R.id.alphaValue);

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