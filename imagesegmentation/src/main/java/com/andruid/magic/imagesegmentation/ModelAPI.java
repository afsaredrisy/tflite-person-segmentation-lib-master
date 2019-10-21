package com.andruid.magic.imagesegmentation;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;
import  org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ModelAPI implements Segmentor {
    private final static String MODEL_PATH = "hair_segmentation.tflite";
    //private final static String MODEL_PATH = "softmax_cropped_4channel_512.tflite";
    private final static boolean USE_GPU = true;

    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    final static int INPUT_SIZE = 257;
    private final static int NUM_CLASSES = 21;
    private final static int COLOR_CHANNELS = 3;
    private final static int BYTES_PER_POINT = 4;
    private static final int PIXEL_PERSON = 15;

    private Interpreter tflite;
    private List<PointF> personPixels = new ArrayList<>();

    private ByteBuffer mImageData;
    private ByteBuffer mOutputs;
    private int[][] mSegmentBits;
    private int[] mSegmentColors;

    private final static Random RANDOM = new Random(System.currentTimeMillis());

    public static Segmentor create(final AssetManager assetManager) throws IOException {
        final ModelAPI d = new ModelAPI();
        Interpreter.Options options = new Interpreter.Options();
        if (USE_GPU) {
            options.addDelegate(new GpuDelegate());
        }
        d.tflite = new Interpreter(loadModelFile(assetManager), options);
        d.mImageData = ByteBuffer.allocateDirect(
                INPUT_SIZE * INPUT_SIZE * COLOR_CHANNELS * BYTES_PER_POINT);
        d.mImageData.order(ByteOrder.nativeOrder());

        d.mOutputs = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * NUM_CLASSES * BYTES_PER_POINT);
        d.mOutputs.order(ByteOrder.nativeOrder());

        d.mSegmentBits = new int[INPUT_SIZE][INPUT_SIZE];
        d.mSegmentColors = new int[NUM_CLASSES];
        for (int i = 0; i < NUM_CLASSES; i++) {
            if (i == 0) {
                d.mSegmentColors[i] = Color.TRANSPARENT;
            } else {
                d.mSegmentColors[i] = Color.rgb(
                        (int) (255 * RANDOM.nextFloat()),
                        (int) (255 * RANDOM.nextFloat()),
                        (int) (255 * RANDOM.nextFloat()));
            }
        }
        return d;
    }



    @Override
    public Bitmap segment(Bitmap bitmap) {
        if (bitmap == null)
            return null;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        mImageData.rewind();
        mOutputs.rewind();

        int[] mIntValues = new int[w * h];
        bitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                if (pixel >= mIntValues.length)
                    break;
                final int val = mIntValues[pixel++];
                Log.d("PIXEL_VALUES",""+val);
                mImageData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                mImageData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                mImageData.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }

        tflite.run(mImageData, mOutputs);

        Bitmap maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        fillZeroes(mSegmentBits);
        float maxVal = 0;
        personPixels.clear();
        Log.d("mylog", "segment: "+w+":"+h);
        int index = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Log.d("VALUES",""+mOutputs.getFloat(index++));
                mSegmentBits[x][y] = 0;
                for (int c = 0; c < NUM_CLASSES; c++) {
                    float val = mOutputs.getFloat((y * w * NUM_CLASSES + x * NUM_CLASSES + c) * BYTES_PER_POINT);
                    if (c == 0 || val > maxVal) {
                        maxVal = val;
                        mSegmentBits[x][y] = c;
                        if(c==PIXEL_PERSON)
                            personPixels.add(new PointF(x,y));
                        //  Log.d("mylog", "("+x+","+y+")"+":"+c);
                    }
                }
                maskBitmap.setPixel(x, y, mSegmentColors[mSegmentBits[x][y]]);
            }
        }
        return maskBitmap;
    }

    /**
     *
     * float c = outuffer.getFloat(index)/outuffer.getFloat(index + 512*512);
     *                 index =index+1;
     *                 if(c<1.0){
     *                    pixelsx[j*512+i] = Color.WHITE;
     *
     *                 }
     *                 else{
     *                     //Log.d("CHANNEL_!","2");
     *                     pixelsx[j*512+i] = Color.BLACK;
     *                 }
     * @param inBuffer
     * @param outuffer
     * @param context
     * @return
     */

    //String body = "";
    @Override
    public Bitmap segment(ByteBuffer inBuffer, ByteBuffer outuffer,Context context) {
        Log.d("VALUES","Inside segment Starting");
        Bitmap bmp = Bitmap.createBitmap(512,512, Bitmap.Config.ARGB_8888);
       // byte[] segmap = new byte[512*512*2];
        tflite.run(inBuffer,outuffer);
        Log.d("VALUES","Starting");
        Log.d("RUN_FINISH","true");
//pixels[j * 257 + i] =o_pixels[j*257+i]

        int[] pixelsx = new int[512*512];
        int index=0;
        for(int i=0;i<512;i++){
            for (int j=0;j<512;j++){





            }
        }
        Bitmap mp = Bitmap.createBitmap(pixelsx,512,512, Bitmap.Config.ARGB_8888);
        Log.d("RUN_FINISH","mask generated");
        return  mp;

        //generate file
      /*  try {
            File file = new File(Environment.getExternalStorageDirectory(),"/nex2me/media");
            String root1 = Environment.getExternalStorageDirectory().toString()+"/nex2me/media";
            File root = new File(root1);
            if(!root.exists())
                root.mkdirs();
            String destinationFilename =root.getAbsolutePath()+""+File.separatorChar+"myfile.txt";
            File mfile=new File(root.getAbsolutePath(),"myg.txt");

            Log.d("ERROR_FILE","File creation");
            //File gpxfile = new File(root, "nfins");
            FileWriter writer = new FileWriter(mfile);
            for (index =0;index<512*512*2;index++){
                String body = ", "+outuffer.getFloat(index);
                writer.append(body);
                //Log.d("ERROR_FILE","appending");

            }
            writer.flush();
            writer.close();
            Log.d("ERROR_FILE","Done");
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
            Log.d("ERROR_FILE",e.toString());
        }

        */

        //return bmp;




    }


    @Override
    public void generateNoteOnSD(Context context, String sFileName) {
        String sBody = "";//this.body;

        Log.d("ERROR_FILE","Starting "+sBody.length());



        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Log.d("ERROR_FILE","Done");
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ERROR_FILE",e.toString());
        }
    }

    @Override
    public void close() {
        tflite.close();
    }

    private void fillZeroes(int[][] array) {
        if (array == null)
            return;
        for (int[] anArray : array) {
            Arrays.fill(anArray, 0);
        }
    }

    private static MappedByteBuffer loadModelFile(final AssetManager assetManager) throws IOException {
        AssetFileDescriptor df = assetManager.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(df.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = df.getStartOffset();
        long declaredLength = df.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}