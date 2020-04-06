package com.urbandroid.sleep.garmin;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.urbandroid.common.error.ErrorReporter;
import com.urbandroid.common.logging.Logger;

public class ErrorReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_report);
        GlobalInitializer.initializeIfRequired(this);

        Logger.logDebug("ErrorReportActivity onCreate");

        sendReport(this.getIntent());
    }

    private void sendReport(Intent intent) {
        Logger.logInfo("Generating on demand report");
        Logger.logInfo(this.getPackageName());

        String comment = "No comment";
        if (intent.hasExtra("USER_COMMENT")) {
            comment = intent.getStringExtra("USER_COMMENT");
        }
        ErrorReporter.getInstance().generateOnDemandReport(null, "Manual report", comment);

        finish();
    }

}