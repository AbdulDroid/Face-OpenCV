package com.example.android.face.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.face.Locker;
import com.example.android.face.MessageAlert;
import com.example.android.face.R;
import com.example.android.face.adapter.ListAdapter;

import java.io.File;

import static com.example.android.face.activities.FaceActivity.current_name;
import static com.example.android.face.activities.FaceActivity.working_Dir;

/**
 * Created by Isa Abuljalil with Matriculation number: 13/SCI01/010
 * on 04/19/2017.
 */

public class FileActivity extends AppCompatActivity implements View.OnClickListener {

    private String path = "";
    private String selectedFile = "";
    private Context context;
    private ListAdapter listAdapter;
    private Button lockButton, unlockButton;
    private static final int LOCK_REQUEST_CODE = 1, UNLOCK_REQUEST_CODE = 2;
    private File checker = new File(working_Dir, current_name + ".jpg");

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        lockButton = (Button) findViewById(R.id.bt_lock);
        lockButton.setOnClickListener(this);
        unlockButton = (Button) findViewById(R.id.bt_unlock);
        unlockButton.setOnClickListener(this);

        File new_Checker = new File(checker.getAbsolutePath());
        if (!(new_Checker.exists())) {
            Intent intent = new Intent(FileActivity.this, FaceActivity.class);
            startActivity(intent);
        }
    }

    protected void onStart() {
        super.onStart();
        ListView lv = (ListView) findViewById(R.id.files_list);
        if (lv != null) {
            lv.setSelector(R.drawable.selection_style);
            lv.setOnItemClickListener(new ClickListener());
        }
        path = "/mnt";
        listDirContents();
    }

    public void onBackPressed() {
        if (path.length() > 1) { //up one level of directory structure
            File f = new File(path);
            path = f.getParent();
            listDirContents();
        } else {
            refreshThumbnails();
            System.exit(0); //exit app
        }
    }

    private void refreshThumbnails() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(new File(path));
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_lock:
                if (selectedFile.length() > 0) {
                    Intent lockIntent = new Intent(FileActivity.this, FaceActivity.class);
                    startActivityForResult(lockIntent, LOCK_REQUEST_CODE);
                    break;
                } else {
                    MessageAlert.showAlert("Please a select a file to lock", context);
                    break;
                }
            case R.id.bt_unlock:
                if (selectedFile.length() > 0) {
                    Intent unlockIntent = new Intent(FileActivity.this, FaceActivity.class);
                    startActivityForResult(unlockIntent, UNLOCK_REQUEST_CODE);
                    break;
                } else {
                    MessageAlert.showAlert("Please select a file to unlock", context);
                    break;
                }
        }
    }

    private class ClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //selected item
            ViewGroup vg = (ViewGroup) view;
            String selectedItem = ((TextView) vg.findViewById(R.id.label)).getText().toString();
            path = path + "/" + selectedItem;
            //et.setText(path);
            listDirContents();
        }
    }

    private void listDirContents() {
        ListView l = (ListView) findViewById(R.id.files_list);
        if (path != null) {
            try {
                File f = new File(path);
                if (f != null) {
                    if (f.isDirectory()) {
                        String[] contents = f.list();
                        if (contents.length > 0) {
                            //create the data source for the list
                            listAdapter = new ListAdapter(this, R.layout.list_layout, R.id.label, contents, path);
                            //supply the data source to the list so that they are ready to display
                            l.setAdapter(listAdapter);
                        } else {
                            //keep track the parent directory of empty directory
                            path = f.getParent();
                        }
                    } else {
                        //capture the selected file path
                        selectedFile = path;
                        //keep track the parent directory of the selected file
                        path = f.getParent();
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void lockFile(View view) {
        if (selectedFile.length() > 0) {
            BackTaskLock btlock = new BackTaskLock();
            btlock.execute();
        } else {
            MessageAlert.showAlert("Please a select a file to lock", context);
        }
    }

    public void startLock() {
        Locker locker = new Locker(context, selectedFile);
        locker.lock();
//            MessageAlert.showAlert("Failed to lock file, Face match failed", context);
    }

    public void unlockFile(View view) {
        if (selectedFile.length() > 0) {
            BackTaskUnlock btunlock = new BackTaskUnlock();
            btunlock.execute();
        } else {
            MessageAlert.showAlert("Please select a file to unlock", context);
        }
    }

    public void startUnlock() {
        Locker locker = new Locker(context, selectedFile);
        locker.unlock();
//            MessageAlert.showAlert("Failed to unlock file, Face match failed", context);
    }

    private class BackTaskLock extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;

        protected void onPreExecute() {
            super.onPreExecute();
            //show process dialog
            pd = new ProgressDialog(context);
            pd.setTitle("Locking the file");
            pd.setMessage("Please wait.");
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();
        }

        protected Void doInBackground(Void... params) {
            try {
                startLock();

            } catch (Exception e) {
                pd.dismiss();   //close the dialog if error occurs
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            pd.dismiss();
            listDirContents();
        }
    }

    private class BackTaskUnlock extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd;

        protected void onPreExecute() {
            super.onPreExecute();
            //show process dialog
            pd = new ProgressDialog(context);
            pd.setTitle("UnLocking the file");
            pd.setMessage("Please wait.");
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                startUnlock();
            } catch (Exception e) {
                pd.dismiss();   //close the dialog if error occurs
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            pd.dismiss();
            listDirContents();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCK_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                boolean result = data.getBooleanExtra("result", false);
                if (result) {
                    BackTaskLock btlock = new BackTaskLock();
                    btlock.execute();
                } else
                    MessageAlert.showAlert("Failed to lock file, Face match failed", context);
            } else
                MessageAlert.showAlert("File Lock task failed, please retry", context);
        } else if (requestCode == UNLOCK_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                boolean result = data.getBooleanExtra("result", false);
                if (result) {
                    BackTaskUnlock btunlock = new BackTaskUnlock();
                    btunlock.execute();
                } else
                    MessageAlert.showAlert("Failed to unlock file, Face match failed", context);
            } else
                MessageAlert.showAlert("File Unlock task failed, please retry", context);
        }
    }
}
