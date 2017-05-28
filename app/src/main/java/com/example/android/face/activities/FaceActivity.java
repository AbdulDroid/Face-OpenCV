package com.example.android.face.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.android.face.ModelView;
import com.example.android.face.NativeModel;
import com.example.android.face.R;

import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.util.Map;

/**
 * Created by Isa Abuljalil with Matriculation number: 13/SCI01/010
 * on 04/19/2017.
 */

public class FaceActivity extends AppCompatActivity {
    Button detect_face, take_picture;
    private String File_Image_TAG = "image_db";
    public static Map<Integer, String> idToImage;
    public static Map<Integer, String> idToName;
    public static String default_name = "temp";
    public static String current_name = "test";
    ImageView captured_image;
    public static boolean face_detected;
    public static File working_Dir = new File(Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/File Locker");
    public CascadeClassifier haar_cascade;
    public static int ID;
    private String Name_obt;
    BufferedWriter bW;
    static File fileC;
    Intent i;

    static {
        working_Dir.mkdirs();
        fileC = new File(FaceActivity.working_Dir, "csv.txt");
    }

    public static boolean pictureTaken = false, recognized = false;

    //Preview preview;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Name_obt = getIntent().getStringExtra("Name");
        if (Name_obt != null) {
            Log.i("LOG_TAG", "Detected Name" + Name_obt);
        }

        setContentView(R.layout.activity_face);
        ButtonListener listener = new ButtonListener();
        detect_face = (Button) findViewById(R.id.detect_face);
        detect_face.setOnClickListener(listener);
        take_picture = (Button) findViewById(R.id.take_picture);
        take_picture.setOnClickListener(listener);

        //Load from file:
        File imagefile = new File(working_Dir, File_Image_TAG);
        if (imagefile.exists()) {
            try {
                FileInputStream f = new FileInputStream(imagefile);
                ObjectInputStream s = new ObjectInputStream(f);
                idToImage = (Map<Integer, String>) s.readObject();
                FaceActivity.ID = idToImage.size();

                s.close();
                Log.i("LOG_TAG", "Database Exists");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        captured_image = (ImageView) findViewById(R.id.captured_image);
        if (!pictureTaken) {
            captured_image.setVisibility(View.GONE);
            detect_face.setVisibility(View.GONE);
            take_picture.setVisibility(View.VISIBLE);
            i = new Intent(FaceActivity.this, NativeModel.class);
            startActivity(i);
        } else {
            if (face_detected) {
                if (!recognized) {
                    detect_face.setVisibility(View.GONE);
                    take_picture.setVisibility(View.VISIBLE);
                } else {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", recognized);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            } else
                detect_face.setVisibility(View.VISIBLE);
            captured_image.setVisibility(View.VISIBLE);
            take_picture.setVisibility(View.GONE);

            Bitmap bmp = null;
            File f = new File(working_Dir, current_name + "_det.jpg");
            if (f.exists()) {
                FaceActivity.face_detected = true;
                bmp = decodeFile(new File(working_Dir.getAbsolutePath() + "/"
                        + current_name + "_det.jpg"), captured_image.getWidth(), captured_image
                        .getHeight());
            } else {
                FaceActivity.face_detected = false;
                bmp = decodeFile(new File(working_Dir.getAbsolutePath() + "/"
                        + current_name + ".jpg"), captured_image.getWidth(), captured_image
                        .getHeight());
            }
            captured_image.setImageBitmap(bmp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.face, menu);
        return true;
    }

    private class ButtonListener implements View.OnClickListener {


        public void onClick(View v) {

            if (v.equals(findViewById(R.id.detect_face))) {
                new ModelView(getApplicationContext()).FindFaces(working_Dir
                        .getAbsolutePath() + "/" + current_name, ModelView.mCascadeFile
                        .getAbsolutePath());
                Log.d("LOG_TAG", "Image dir:" + working_Dir.getAbsolutePath() + "/" + current_name);
                File f = new File(FaceActivity.working_Dir, "csv.txt");
                int return_id = -1;
                if (f.exists() && FaceActivity.ID > 1)
                    return_id = new ModelView(getApplicationContext()).Find(working_Dir
                            .getAbsolutePath() + "/" + current_name, ModelView.mCascadeFile
                            .getAbsolutePath(), working_Dir.getAbsolutePath() + "/csv.txt");
                FaceActivity.face_detected = true;
                if (return_id != -1) {
                    FaceActivity.recognized = true;
                }
                if (recognized) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", recognized);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                } else {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", recognized);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
                finish();
            }
        }
    }

    public static Bitmap decodeFile(File f, int WIDTH, int HEIGHT) {
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);



            //Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 > WIDTH && o.outHeight / scale / 2 > HEIGHT)
                scale *= 2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
