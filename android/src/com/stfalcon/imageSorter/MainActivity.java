package com.stfalcon.imageSorter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.haarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.haarman.listviewanimations.itemmanipulation.SwipeDismissAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity implements OnDismissCallback {

    private GoogleCardsAdapter mGoogleCardsAdapter;
    ArrayList<String> folderUris = new ArrayList<String>();
    ArrayList<String> folders = new ArrayList<String>();
    Spinner spinnerFrom, spinnerTo;
    ArrayList<Integer> items = new ArrayList<Integer>();
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ListView listView = (ListView) findViewById(R.id.activity_googlecards_listview);
        mGoogleCardsAdapter = new GoogleCardsAdapter(this);
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mGoogleCardsAdapter, this));
        swingBottomInAnimationAdapter.setInitialDelayMillis(300);
        swingBottomInAnimationAdapter.setAbsListView(listView);
        listView.setAdapter(swingBottomInAnimationAdapter);

        spinnerFrom = (Spinner) findViewById(R.id.from);
        spinnerTo = (Spinner) findViewById(R.id.to);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getAllDirectoriesFromSDCard());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerFrom.setPrompt("Title");
        spinnerTo.setPrompt("Title");
        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Loger","POS " + adapterView.getSelectedItemPosition());
                mGoogleCardsAdapter.clear();
                mGoogleCardsAdapter.addAll(getItems(folderUris.get(adapterView.getSelectedItemPosition())));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mGoogleCardsAdapter.addAll(getItems(null));
            }
        });
    }


    private ArrayList<Integer> getItems(String dir) {

        items.clear();
        String folder = dir;
        folder = folder + "/%";
        Log.i("Loger", "FOLDER " + folder);
        String where = MediaStore.Images.Media.DATA + " LIKE ?";
        String[] whereArgs = new String[]{folder};

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };

        Cursor cursor = getContentResolver().query(uri, projection, where, whereArgs, null);
        int count = cursor.getCount();
        int image_column_index = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            items.add(cursor.getInt(image_column_index));
        }

        return items;
    }

    @Override
    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
        for (int position : reverseSortedPositions) {

            String[] column = { MediaStore.Images.Media.DATA };

            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = getContentResolver().
                    query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            column, sel, new String[]{ String.valueOf(items.get(position)) }, null);

            String filePath = "";

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            cursor.close();

            Uri uri = Uri.parse(filePath);

            Log.i("Loger", "FOLDER_FROM " + filePath);
            Log.i("Loger", "FOLDER_TO " + folderUris.get(spinnerTo.getSelectedItemPosition()) + "/" + uri.getLastPathSegment());

            File from = new File(filePath);
            File to = new File(folderUris.get(spinnerTo.getSelectedItemPosition()) + "/" + uri.getLastPathSegment());
            from.renameTo(to);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(from)));
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(to)));
            //copyFile(filePath, folderUris.get(spinnerTo.getSelectedItemPosition()) + "/" + uri.getLastPathSegment());
            removeItem(items, position);
            mGoogleCardsAdapter.remove(position);
        }
    }

    private void removeItem(ArrayList<Integer> list, int i){
        list.remove(i);
        for (int j = i; j + 1 < list.size(); j++){
            list.set(j, list.get(j + 1));
        }
    }


    public ArrayList<String> getAllDirectoriesFromSDCard() {
        folders.clear();
        folderUris.clear();

        File file[] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).listFiles();
        for (File f : file)
        {
            if (f.isDirectory()) {
                folderUris.add(f.getPath());
                Uri uri = Uri.parse(f.getPath());
                folders.add(uri.getLastPathSegment());
            }
        }

        File file1[] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).listFiles();
        for (File f : file1)
        {
            if (f.isDirectory()) {
                folderUris.add(f.getPath());
                Uri uri = Uri.parse(f.getPath());
                folders.add(uri.getLastPathSegment());
            }
        }

        Log.i("Loger","........Detected images for Grid....."
         + folderUris);
        return folders;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem addFolder =  menu.findItem(R.id.add_folder);
        addFolder.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showDialog();
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Create folder-album");
        builder.setMessage("Enter folder name");

        // Set an EditText view to get user input
        final EditText input = new EditText(MainActivity.this);
        builder.setView(input);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();

                //This is where you would put your make directory code
                File photos = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),value);
                photos.mkdir();
                getAllDirectoriesFromSDCard();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
