package com.andruid.magic.imagesegmentation;

import android.content.Context;
import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public interface Segmentor {
    Bitmap segment(Bitmap bitmap);
    Bitmap segment(ByteBuffer inBuffer, ByteBuffer outuffer,Context context);
    public void generateNoteOnSD(Context context, String sFileName);

    void close();
}