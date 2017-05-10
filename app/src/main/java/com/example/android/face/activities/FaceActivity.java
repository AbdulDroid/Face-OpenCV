package com.example.android.face.activities;

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
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.face.NativeModel;
import com.example.android.face.R;

import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class FaceActivity extends AppCompatActivity {
        Button detect_face,add_name,take_picture;
        TextView tv ;
        TextView detectedName;
        private String File_Image_TAG = "image_db";
        private String File_Name_TAG = "name_db";
        public static Map<Integer, String> idToImage;
        public static Map<Integer,String> idToName;
        public static String current_name = "temp";
        ImageView captured_image ;
        public static boolean face_detected ;
        public static File working_Dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/opencv");
        public CascadeClassifier haar_cascade;
        public static int ID;
        private String Name_obt;
        BufferedWriter bW;
        static File fileC;
        static {
            working_Dir.mkdirs();
            fileC = new File(FaceActivity.working_Dir,"csv.txt");

        }
        public static boolean pictureTaken,recognized;
        //Preview preview;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Name_obt = getIntent().getStringExtra("Name");
            if(Name_obt != null){
                Log.i("Ajay","Detected Name"+Name_obt);
            }

            setContentView(R.layout.activity_face);
            ButtonListener listener = new ButtonListener();
            detect_face = ((Button)findViewById(R.id.detect_face));
            detect_face.setOnClickListener(listener);

            take_picture = (Button) findViewById(R.id.take_picture);
            take_picture.setOnClickListener(listener);

            //Load from file:
            File imagefile = new File(working_Dir,File_Image_TAG);
            File nameFile = new File(working_Dir,File_Name_TAG);
            if(imagefile.exists()){
                try{
                    FileInputStream f = new FileInputStream(imagefile);
                    ObjectInputStream s = new ObjectInputStream(f);
                    idToImage= (Map<Integer, String>) s.readObject();
                    FaceActivity.ID = idToImage.size();

                    s.close();
                    Log.i("Ajay","Database Exists");
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            if(nameFile.exists()){
                try{
                    FileInputStream f = new FileInputStream(nameFile);
                    ObjectInputStream s = new ObjectInputStream(f);
                    idToName= (Map<Integer, String>) s.readObject();

                    s.close();
                    Log.i("Ajay","Database Exists");
                }catch(Exception e){
                    e.printStackTrace();
                }
            }


            add_name = (Button) findViewById(R.id.Add_Name);
            add_name.setOnClickListener(listener);

            tv = (TextView) findViewById(R.id.name);
            tv.setVisibility(View.GONE);

            detectedName = (TextView) findViewById(R.id.Detected_name);
            detectedName.setVisibility(View.GONE);

            captured_image = (ImageView)findViewById(R.id.captured_image);
            if(!pictureTaken){
                captured_image.setVisibility(View.GONE);
                add_name.setVisibility(View.GONE);
                detect_face.setVisibility(View.GONE);
                take_picture.setVisibility(View.VISIBLE);
            }
            else{
                if(face_detected){
                    if(!recognized){
                        detect_face.setVisibility(View.GONE);
                        add_name.setVisibility(View.VISIBLE);
                        take_picture.setVisibility(View.GONE);
                    }
                    else{
//            		TODO: Print the hash value for ID obtained.
                        detectedName.setVisibility(View.VISIBLE);
                        detectedName.setText(Name_obt!=null?Name_obt:"");
                    }
                }
                else
                    detect_face.setVisibility(View.VISIBLE);
                captured_image.setVisibility(View.VISIBLE);


//        	add_name.setVisibility(View.GONE);
                take_picture.setVisibility(View.GONE);
                Bitmap bmp=null;
                File f = new File(working_Dir,current_name +"_det.jpg");
                if(f.exists()){
                    FaceActivity.face_detected = true;
                    bmp = decodeFile(new File(working_Dir.getAbsolutePath()+"/"
                            +current_name +"_det.jpg"), captured_image.getWidth(), captured_image.getHeight());
                }
                else{
                    FaceActivity.face_detected = false;
                    bmp = decodeFile(new File(working_Dir.getAbsolutePath()+"/"
                            +current_name +".jpg"), captured_image.getWidth(), captured_image.getHeight());
                }
                captured_image.setImageBitmap(bmp);
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.face, menu);
            return true;
        }
        private class ButtonListener implements View.OnClickListener{
            Intent i;
            public void onClick(View v) {

                if(v.equals(findViewById(R.id.detect_face))){
                    new ModelView(getApplicationContext()).FindFaces(working_Dir.getAbsolutePath()+"/"+current_name, ModelView.mCascadeFile.getAbsolutePath());
                    Log.d("Ajay","Image dir:"+working_Dir.getAbsolutePath()+"/"+current_name);
                    File f = new File(FaceActivity.working_Dir,"csv.txt");
                    int return_id=-1;
                    if(f.exists() && FaceActivity.ID>1)
                        return_id = new ModelView(getApplicationContext()).Find(working_Dir.getAbsolutePath()+"/"+current_name, ModelView.mCascadeFile.getAbsolutePath(),working_Dir.getAbsolutePath()+"/csv.txt");
                    FaceActivity.face_detected = true;
                    String Name=null;
                    if(return_id != -1 ){
                        Name = idToName.get(return_id);
                        FaceActivity.recognized=true;
                    }
                    startActivity(getIntent().putExtra("Name", Name));
                    //TODO : Add Face Detection
                }
                else if(v.equals(findViewById(R.id.Add_Name))){
                    tv.setVisibility(View.VISIBLE);
                    if(tv.getText().toString().equals(""))
                        return;
                    String new_name = tv.getText().toString();
//            	faces.put(current_name, new ArrayList<String>());
                    if(idToImage == null){
                        idToImage = new HashMap<Integer, String>();
                        FaceActivity.ID=0;
                    }
                    if(idToName == null)
                        idToName = new HashMap<Integer, String>();

                    idToImage.put(FaceActivity.ID, FaceActivity.working_Dir+"/"+new_name+".jpg");
                    idToImage.put(FaceActivity.ID, FaceActivity.working_Dir+"/"+new_name+"1.jpg");
                    idToName.put(FaceActivity.ID, new_name);


                    File f = new File(FaceActivity.working_Dir,FaceActivity.current_name+"_det.jpg");
                    if (!f.exists()) {
                        try {
                            f.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    File f_new = new File(FaceActivity.working_Dir,new_name+".jpg");
                    if (!f_new.exists()) {
                        try {
                            f_new.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i("Ajay",FaceActivity.working_Dir+"");
                    f.renameTo(f_new);
                    f = new File(FaceActivity.working_Dir,new_name+"1.jpg");

                    //copy the file content in bytes
                    try{
                        InputStream inStream = new FileInputStream(f_new);
                        OutputStream outStream = new FileOutputStream(f);

                        byte[] buffer = new byte[1024];

                        int length;

                        while ((length = inStream.read(buffer)) > 0){

                            outStream.write(buffer, 0, length);

                        }
                        inStream.close();
                        outStream.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }





                    try{
                        File file = new File(FaceActivity.working_Dir,File_Image_TAG);
                        FileOutputStream fo = new FileOutputStream(file);
                        ObjectOutputStream s = new ObjectOutputStream(fo);
                        s.writeObject(idToImage);
                        s.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    try{
                        File file = new File(FaceActivity.working_Dir,File_Name_TAG);
                        FileOutputStream fo = new FileOutputStream(file);
                        ObjectOutputStream s = new ObjectOutputStream(fo);
                        s.writeObject(idToName);
                        s.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    try{
                        try {
                            bW = new BufferedWriter(new FileWriter(fileC,true));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        bW.append(FaceActivity.working_Dir + "/").append(new_name)
                                .append(".jpg").append(";" + FaceActivity.ID);
                        bW.newLine();
                        bW.append(FaceActivity.working_Dir + "/" + new_name + "1.jpg" + ";" + FaceActivity.ID);
                        bW.newLine();
                        bW.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }


                    Toast.makeText(getApplicationContext(), "Name Added", Toast.LENGTH_SHORT).show();
                    FaceActivity.ID++;
                    startActivity(getIntent());
                    FaceActivity.face_detected=false;
                    FaceActivity.pictureTaken=false;
                    FaceActivity.recognized=false;
//            	Log.i("Ajay","String : " + current_name);
                }
                else if(v.equals(findViewById(R.id.take_picture))){
                    i= new Intent(FaceActivity.this,NativeModel.class);
                    startActivity(i);
                }
            }
        }

        public static Bitmap decodeFile(File f, int WIDTH, int HEIGHT){
            try{
                //Decode image size
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(new FileInputStream(f),null,o);

                //The new size we want to scale to
                final int REQUIRED_WIDTH = WIDTH;
                final int REQUIRED_HEIGHT = HEIGHT;

                //Find the correct scale value. It should be the power of 2.
                int scale = 1;
                while (o.outWidth/scale/2 > REQUIRED_WIDTH && o.outHeight/scale/2 > REQUIRED_HEIGHT)
                    scale *= 2;

                //Decode with inSampleSize
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = scale;
                return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
            }catch (FileNotFoundException e){}
            return null;
        }
    }
