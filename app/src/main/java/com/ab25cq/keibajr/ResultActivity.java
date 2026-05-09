package com.ab25cq.keibajr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ResultActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(Texts.t("レース結果", "Race Result"));

        int pad = dp(16);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(246, 242, 232));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad * 2);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText(Texts.t("レース結果", "Race Result"));
        title.setTextSize(24);
        title.setTextColor(Color.rgb(33, 45, 38));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView summaryView = new TextView(this);
        summaryView.setText(getIntent().getStringExtra("summary"));
        summaryView.setTextSize(16);
        summaryView.setTextColor(Color.rgb(35, 45, 40));
        summaryView.setGravity(Gravity.START);
        summaryView.setPadding(dp(14), dp(12), dp(14), dp(12));
        summaryView.setBackgroundColor(Color.rgb(255, 252, 244));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(12);
        root.addView(summaryView, summaryParams);

        Button backButton = new Button(this);
        backButton.setText(Texts.t("厩舎へ戻る", "Back to Stable"));
        backButton.setAllCaps(false);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        root.addView(backButton);

        setContentView(scrollView);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
