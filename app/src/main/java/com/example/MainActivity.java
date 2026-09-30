package com.example;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.compose.ComponentActivityKt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.data.repository.OutpassRepository;
import com.example.util.OutpassNotificationHelper;
import java.lang.reflect.Method;
import kotlin.jvm.functions.Function2;

public final class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context applicationContext = getApplicationContext();
        OutpassRepository.Companion.getInstance(applicationContext).initialize(applicationContext);
        OutpassNotificationHelper.INSTANCE.initializeChannels(applicationContext);

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        EdgeToEdge.enable(this);

        try {
            Method m = ComposableSingletons$MainActivityKt.class.getMethod("getLambda$-601144069$app");
            Function2 content = (Function2) m.invoke(ComposableSingletons$MainActivityKt.INSTANCE);
            ComponentActivityKt.setContent(this, null, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
