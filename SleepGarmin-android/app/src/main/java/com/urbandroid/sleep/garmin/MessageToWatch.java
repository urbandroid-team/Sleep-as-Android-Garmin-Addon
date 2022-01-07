package com.urbandroid.sleep.garmin;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Objects;

public class MessageToWatch {
    public String command;
    public Long param;

    MessageToWatch(String command) {
        this.command = command;
    }

    MessageToWatch(String command, Long param) {
        this.command = command;
        this.param = param;
    }

    @NonNull
    @Override
    public String toString() {
        if (param == null) return command;
        return command + ";" + param;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return this.toString().equals(obj.toString());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(command, param);
    }
}
