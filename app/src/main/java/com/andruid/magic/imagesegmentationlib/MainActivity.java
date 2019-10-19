package com.andruid.magic.imagesegmentationlib;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.IpSecManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.andruid.magic.imagesegmentation.BitmapUtils;
import com.andruid.magic.imagesegmentation.ModelAPI;
import com.andruid.magic.imagesegmentation.Segmentor;
import com.andruid.magic.imagesegmentationlib.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 0;
    private ActivityMainBinding binding;
    private Segmentor segmentor;
    private ImageView resultImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.fab.setOnClickListener(v -> pickImage());

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            segmentor = ModelAPI.create(getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        segmentor.close();
    }

    public void pickImageR(){
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.imgd);

        readValues(bmp);
    }

    ByteBuffer imByte;
    ByteBuffer outData = ByteBuffer.allocateDirect(512*512*2*4);


    public void addPixelValue(int pixelValue){
        //imByte.putFloat((((pixelValue >> 24) & 0xFF) - 128.f)/ 128.f);
        //imByte.putFloat((((pixelValue >> 16) & 0xFF)-128.f) / 128.f);
        //imByte.putFloat((((pixelValue >> 8) & 0xFF)-128.f) / 128.f);
        //imByte.putFloat(((pixelValue >> 0xff)-128.f) / 128.f);
        //Log.d("VALUE_ALPHA",""+((pixelValue >> 0xff)) / 255.f);
        //imByte.putInt(Color.blue(pixelValue));
        //imByte.putInt(Color.green(pixelValue));
        //imByte.putInt(Color.red(pixelValue));
        //imByte.putInt(Color.alpha(pixelValue));

        imByte.putFloat(((pixelValue >> 24) & 0xFF) / 255.f);
        imByte.putFloat(((pixelValue >> 16) & 0xFF) / 255.f);//red24
        imByte.putFloat(((pixelValue >> 8) & 0xFF) / 255.f);//g16
        imByte.putFloat((pixelValue & 0xFF) / 255.f);//b8



    }

    public void readValues(Bitmap bmp){

        imByte = ByteBuffer.allocateDirect(

                         512
                        * 512
                        *4
                        * 4);
        imByte.order(ByteOrder.nativeOrder());
       // Log.d("Buffer_SIZE_INPUT",""+imByte.)


      int [] pixels = new int[512 * 512];
      bmp = Bitmap.createScaledBitmap(bmp,512,512,true);
      bmp.getPixels(pixels,0,512,0,0,512,512);
      Bitmap out=Bitmap.createBitmap(pixels,512,512, Bitmap.Config.ARGB_8888);
      //binding.imageView.setImageBitmap(out);
    imByte.rewind();
    int p=0;
     for (int i=0;i<512;i++){
         for(int j=0;j<512;j++){

             if(p>pixels.length){
                 break;
             }
             final int val = bmp.getPixel(i,j);
             //Log.d("PIXEL_IS","Pixel is "+val+" with "+bmp.getPixel(i,j));
             addPixelValue(val);
             //byte r = ((val >> 16) & 0xFF) / 255.f;
             /*int pix = bmp.getPixel(i,j);
             int r = Color.red(pix);
             int g = Color.green(pix);
             int b = Color.blue(pix);
             int a = Color.alpha(pix);
             Log.d("VALUES_RED","int is "+r+" byte is "+(byte)r);
             Log.d("VALUES_ALPHA","int is "+a+" byte is "+(byte)a);*/


         }

     }



        /*binding.imageView.post(new Runnable() {
            @Override
            public void run() {
                binding.imageView.setImageBitmap(mp);
            }
        });*/

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);*/
                        outData.rewind();
                        Bitmap mp = segmentor.segment(imByte,outData, getApplicationContext());
                        binding.imageView.setImageBitmap(mp);
                        //segmentor.generateNoteOnSD(getApplicationContext(),"mFile");
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(),response.getPermissionName()+" denied",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public void pickImage(){
       /* Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(),response.getPermissionName()+" denied",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();*/

        pickImageR();

        /*Bitmap resource = BitmapFactory.decodeResource(getResources(),R.drawable.imgd);
        Bitmap resized = BitmapUtils.resize(resource);
        Bitmap outBitmap = segmentor.segment(resized);
        binding.imageView.setImageBitmap(outBitmap);*/


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==PICK_IMAGE && resultCode==RESULT_OK){
            if(data==null)
                return;
            Uri uri = data.getData();
            Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Bitmap resized = BitmapUtils.resize(resource);
                            Bitmap outBitmap = segmentor.segment(resized);
                            binding.imageView.setImageBitmap(outBitmap);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }
}