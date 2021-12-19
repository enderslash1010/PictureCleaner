package com.example.picturecleaner_enderk2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ImageView picture;
    TextView text;
    TextView fileNameText;

    final int MENU_EMPTY = 0;
    final int MENU_RESET = 1;

    float x = 0, y = 0;
    boolean isPicSelected = false;
    boolean isPicValid = false;

    SpringAnimation springX;
    SpringAnimation springY;
    Point screen = new Point();

    String[] filePathColumn = {MediaStore.MediaColumns.DATA};
    ArrayList<File> galleryArray = new ArrayList<File>();
    int currIndex = 0;
    ArrayList<File> deleteArray = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.textView);
        picture = (ImageView) findViewById(R.id.imageView);
        fileNameText = (TextView) findViewById(R.id.photoNameText);

        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(screen);

        allowStoragePermissions();
        initGalleryArray();

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.add(Menu.NONE, MENU_EMPTY, Menu.NONE, "Empty Trash");
        menu.add(Menu.NONE, MENU_RESET, Menu.NONE, "Reset Trash");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EMPTY:
                emptyTrash();
                break;
            case MENU_RESET:
                resetTrash();
                break;
        }
        return true;
    }

    public void undoButtonClicked(View view) {
        if (currIndex != 0) { // make sure its not the first picture
            // if it was the last picture, make pic valid
            if (currIndex == galleryArray.size()) {
                isPicValid = true;
                text.setVisibility(TextView.INVISIBLE);
                fileNameText.setVisibility(TextView.VISIBLE);
            }

            currIndex--;
            if (deleteArray.contains(galleryArray.get(currIndex))) { // if the previous picture was put in trash can
                deleteArray.remove(galleryArray.get(currIndex));
            }

            // reload previous photo
            Bitmap bitmap = BitmapFactory.decodeFile(galleryArray.get(currIndex).getPath());
            picture.setImageBitmap(bitmap);
            fileNameText.setText(galleryArray.get(currIndex).getName());
        }
        else {
            Toast.makeText(this, "No previous photos to undo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: { // click down
                x = event.getRawX();
                y = event.getRawY() - 200;

                if (x > picture.getX() && x < picture.getX() + picture.getWidth() && y > picture.getY() && y < picture.getY() + picture.getHeight()) {
                    isPicSelected = true;
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: { // move cursor
                if (isPicSelected) {
                    x = event.getRawX() - (picture.getWidth()/2);
                    y = event.getRawY() - 200 - (picture.getHeight()/2);

                    picture.setX(x);
                    picture.setY(y);
                }
            }
            break;
            case MotionEvent.ACTION_UP: { // release click
                if (isPicSelected && isPicValid) {
                    x = event.getRawX();
                    y = event.getRawY() - 200;

                    if (x - (picture.getWidth() / 4) < 0) { // if picture is on left side of screen
                        // add to delete array
                        String fileName = galleryArray.get(currIndex).getName();
                        deleteArray.add(galleryArray.get(currIndex));
                        Toast.makeText(this, "Moved to trash can: " + fileName, Toast.LENGTH_SHORT).show();
                        nextPicture();
                        snapToCenter();
                    } else if (x + (picture.getWidth() / 4) > screen.x) { // if picture is on right side of screen
                        // keep
                        nextPicture();
                        snapToCenter();
                    }
                    returnToCenter();
                }
                isPicSelected = false;
            }

        }

        return true;
    }

    public void returnToCenter() {
        if (springX == null) {
            springX = new SpringAnimation(picture, DynamicAnimation.X, (screen.x/2) - (picture.getWidth()/2));
            springY = new SpringAnimation(picture, DynamicAnimation.Y, (screen.y - 200)/2 - (picture.getHeight()/2));
        }
        springX.start();
        springY.start();
    }

    public void snapToCenter() {
        picture.setX((screen.x/2) - (picture.getWidth()/2));
        picture.setY((screen.y/2) - (picture.getHeight()/2) - 200);
    }

    public void initGalleryArray() {
        galleryArray.clear();
        text.setVisibility(TextView.INVISIBLE);

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePathColumn, null, null, null);
        while (cursor.moveToNext()) {
            galleryArray.add(new File(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))));
        }
        cursor.close();

        // load first image if there is one
        if (galleryArray.size() != 0) {
            Bitmap bitmap = BitmapFactory.decodeFile(galleryArray.get(0).getPath());
            picture.setImageBitmap(bitmap);
            fileNameText.setText(galleryArray.get(0).getName());
            isPicValid = true;
        }
        else {
            fileNameText.setVisibility(TextView.INVISIBLE);
            endOfPictures("Could Not Find Photos on the Device");
        }
    }

    public void nextPicture() {
        currIndex++; // increment to next picture
        if (currIndex >= galleryArray.size()) { // reached end of pictures
            fileNameText.setVisibility(TextView.INVISIBLE);
            endOfPictures("All Photos have been Reviewed")
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Do you want to empty the trash?");
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // do nothing
                }
            });
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // empty trash
                    emptyTrash();
                }
            });
            alert.show();
        }
        else {
            Bitmap bitmap = BitmapFactory.decodeFile(galleryArray.get(currIndex).getPath());
            picture.setImageBitmap(bitmap);
            fileNameText.setText(galleryArray.get(currIndex).getName());
        }
    }

    public void endOfPictures(String msg) {
        isPicValid = false;
        picture.setImageBitmap(null);
        text.setText(msg);
        text.setVisibility(TextView.VISIBLE);
    }

    public void emptyTrash() {
        // popup to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Are you sure you want to delete everything in the trash?");
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // do nothing
            }
        });
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int x) {
                // empty trash
                for (int i = 0; i < deleteArray.size(); i++) {
                    int index = galleryArray.indexOf(deleteArray.get(i));
                    if (index < currIndex) { // if the removed item is before the currIndex, decrement currIndex
                        currIndex--;
                    }
                    galleryArray.remove(deleteArray.get(i)); // delete from galleryArray
                    deleteArray.get(i).delete(); // delete from storage
                }
                deleteArray.clear();
                Toast.makeText(getApplicationContext(), "Emptied Trash Can", Toast.LENGTH_SHORT).show();
            }
        });
        alert.show();

    }

    public void resetTrash() {
        deleteArray.clear();
        Toast.makeText(this, "Reset Trash Can", Toast.LENGTH_SHORT).show();
    }

    public void allowStoragePermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION}, 1);
    }

}