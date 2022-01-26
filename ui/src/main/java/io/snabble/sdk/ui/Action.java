package io.snabble.sdk.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Action {
    void execute(@NonNull Context context, @Nullable Bundle args);
}
