package com.homesoft.photo.libraw;

import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
public class LibRaw26 extends LibRaw {
    static {
        System.loadLibrary("androidraw26");
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private Bitmap getHardwareBitmap(int format) {
        final HardwareBuffer hardwareBuffer = HardwareBuffer.create(getWidth(), getHeight(), format,
                1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE| HardwareBuffer.USAGE_CPU_WRITE_RARELY);
        if (drawHardwareBuffer(hardwareBuffer)) {
            final Bitmap bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null);
            hardwareBuffer.close();
            return bitmap;
        } else {
            return null;
        }
    }
    public Bitmap getBitmap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getHardwareBitmap(HardwareBuffer.RGB_888);
        } else {
            return super.getBitmap();
        }
    }

    public Bitmap getBitmap16() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getHardwareBitmap(HardwareBuffer.RGBA_FP16);
        } else {
            return super.getBitmap16();
        }
    }

    private native boolean drawHardwareBuffer(HardwareBuffer hardwareBuffer);
}
