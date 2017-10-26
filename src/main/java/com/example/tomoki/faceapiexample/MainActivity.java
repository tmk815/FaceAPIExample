package com.example.tomoki.faceapiexample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    private FaceServiceClient faceServiceClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = (Button) findViewById(R.id.button1);
        final EditText api=(EditText) findViewById(R.id.api);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                faceServiceClient = new FaceServiceRestClient(api.getText().toString());
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                detectAndFrame(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            FaceServiceClient.FaceAttributeType[] faceAttributeTypes = {FaceServiceClient.FaceAttributeType.Age};
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    faceAttributeTypes           // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null) {
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {

                        detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(String... progress) {

                        detectionProgressDialog.setMessage(progress[0]);
                    }

                    @Override
                    protected void onPostExecute(Face[] result) {

                        detectionProgressDialog.dismiss();
                        if (result == null) return;
                        ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                        imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result));
                        imageBitmap.recycle();
                    }
                };
        detectTask.execute(inputStream);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        int stokeWidth = 2;
        paint.setStrokeWidth(stokeWidth);
        if (faces != null) {
            for (Face face : faces) {
                Random r = new Random();
                int red = r.nextInt(255) + 1;
                int green = r.nextInt(255) + 1;
                int blue = r.nextInt(255) + 1;
                paint.setColor(Color.rgb(red, green, blue));
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
                paint.setTextSize(faceRectangle.width/2);
                FaceAttribute attribute = face.faceAttributes;
                float size = paint.measureText(String.valueOf(attribute.age));
                canvas.drawText(String.valueOf(attribute.age) + "歳", faceRectangle.left + (faceRectangle.width / 2) - size, (float) (faceRectangle.top + faceRectangle.height*1.5), paint);
            }
        }
        return bitmap;
    }
}