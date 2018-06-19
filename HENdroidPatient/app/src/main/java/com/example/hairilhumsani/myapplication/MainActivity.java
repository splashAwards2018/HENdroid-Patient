package com.example.hairilhumsani.myapplication;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.kitt.snowboy.SnowboyDetect;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.Manifest;

import static java.text.DateFormat.getDateTimeInstance;


public class MainActivity extends AppCompatActivity
{

    static
    {
        System.loadLibrary("snowboy-detect-android");
    }

    private EditText editText;
    private TextView textView;
    private Button btn;
    private ListView historyList;
    private ArrayList<String> listItems = new ArrayList<>();

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private AIDataService aiDataService;
    private Boolean shouldDetect;
    private SnowboyDetect snowboyDetect;


    private DatabaseReference mDataBase;
    private DatabaseReference mDataBase2;
    private DatabaseReference mDataBase3;
    private DatabaseReference mDataBase4;


    private StorageReference storageRef;
    private DatabaseReference databaseRef;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 111;


    Button btnSet;
    Button btnChoose;
    EditText fileName;
    Button btnUpload;
    ImageView imageView;
    private Uri imageUri;


    final private int REQUEST_STATIC_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STATIC_PERMISSION);

        editText = (EditText) findViewById(R.id.typeText);
        textView = (TextView) findViewById(R.id.textView);
        btn = (Button) findViewById(R.id.button);
        historyList = (ListView) findViewById(R.id.historyList);

        fileName = (EditText)findViewById(R.id.fileName);

        Button btnCamera = (Button)findViewById(R.id.btnCamera);
        imageView = (ImageView)findViewById(R.id.imageView);

        final Button btnChoose = (Button)findViewById(R.id.btnChoose);
        Button btnUpload = (Button)findViewById(R.id.btnUpload);


        //~Camera features~

        btnCamera.setOnClickListener((new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }));


        //Data for Patient
        mDataBase = FirebaseDatabase.getInstance().getReference().child("Patient").child("Jane Doe").child(getDateNo());

        mDataBase2 = FirebaseDatabase.getInstance().getReference().child("Patient").child("Jane Doe").child("Appointment Date");


        //Data for Booking Slot/Doctor to retrieve
        mDataBase3 = FirebaseDatabase.getInstance().getReference().child("Doctor");

        mDataBase4 = FirebaseDatabase.getInstance().getReference().child("Doctor").child("Appointment Date");

        mDataBase3.child("9 AM").child("Name").setValue("Georgiee");
        mDataBase3.child("10 AM").child("Name").setValue("Hairil");
        mDataBase3.child("11 AM").child("Name").setValue("Nicole");
        mDataBase3.child("1 PM").child("Name").setValue("Isaac");
        mDataBase3.child("2 PM").child("Name").setValue("Eddie");
        mDataBase3.child("3 PM").child("Name").setValue("Jeremy");
        mDataBase3.child("4 PM").child("Name").setValue("Malcolm");


        storageRef = FirebaseStorage.getInstance().getReference();
        databaseRef = FirebaseDatabase.getInstance().getReference();


        setupNlu();
        setupTts();
        setupAsr();
        setupHotword();
        startHotword();

        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startAsr();
                btn.setText("Listening");
            }
        });


        btnCamera.setOnClickListener((new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }));



        //CHOOSE IMAGE
        btnChoose.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileChooser();
            }
        });


        //UPLOAD IMAGE
        btnUpload.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                uploadFile(fileName.getText().toString());
            }
        });
    }



    private String getFileExtension(Uri uri)
    {

        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));

    }


    private void openFileChooser()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    private void uploadFile(String text)
    {
        if (imageUri != null)
        {

           // StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            StorageReference fileReference = storageRef.child(text + "." + getFileExtension(imageUri));
            fileReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {
//                    Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//
//                        }
//                    }, 5000);

//                    responseText = "Upload successful!";
                    Upload upload = new Upload(fileName.getText().toString().trim(), taskSnapshot.getDownloadUrl().toString());

                    String uploadId = databaseRef.push().getKey();
                    databaseRef.child(uploadId).setValue(upload);

                    mDataBase.child("Image").setValue(fileName.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {

                        }
                    });




                }
            })
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {

                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                        {

                        }
                    });
        }
        else
        {
            Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }


//    public Uri getImageUri(Context inContext, Bitmap inImage)
//    {
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
//        return Uri.parse(path);
//    }

    public void encodeBitmapAndSaveToFirebase(final Bitmap bitmap)
    {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        byte[] data = baos.toByteArray();

        storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://splashawards2018-b98bd.appspot.com");
        final StorageReference imageUrl = storageRef.child(imageEncoded);

        UploadTask uploadTask = imageUrl.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception exception)
            {
                // Handle unsuccessful uploads
                Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
            }
        })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                    {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        // Do what you want
                        String uploadId = databaseRef.push().getKey();



                        //            Log.e("URI", imageUri.toString());
                        //            //Picasso.with(this).load(imageUri).into(imageView);

                    }
                });

//        databaseRef = FirebaseDatabase.getInstance()
//                .getReference("Patient")
//                .child("Jane Doe")
//                .child("imageUrl");
//        databaseRef.setValue(imageEncoded);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {

//            Bundle extras = data.getExtras();
//            Bitmap bitmap = (Bitmap) extras.get("data");
//            imageView.setImageBitmap(bitmap);

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            encodeBitmapAndSaveToFirebase(bitmap);
            Log.e("USE","Passed");

        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            imageUri = data.getData();

            Log.e("URI", imageUri.toString());
            imageView.setImageURI(imageUri);

        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STATIC_PERMISSION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED)
            {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }
        }
    }



    public String getDateNo()
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM YYYY");
        String strDate = sdf.format(c.getTime());
        return strDate;
    }



    private void setupAsr()
    {
        speechRecognizer = speechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener()
        {
            @Override
            public void onReadyForSpeech(Bundle params)
            {

            }

            @Override
            public void onBeginningOfSpeech()
            {

            }

            @Override
            public void onRmsChanged(float rmsdB)
            {

            }

            @Override
            public void onBufferReceived(byte[] buffer)
            {

            }

            @Override
            public void onEndOfSpeech()
            {

            }

            @Override
            public void onError(int error)
            {
                Log.e("asr", "ERROR:" + Integer.toString(error));
                startHotword();
            }

            @Override
            public void onResults(Bundle results)
            {
                List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts == null || texts.isEmpty())
                {
                    editText.setText("Please Try Again");
                }
                else
                {
                    String text = texts.get(0);
                    editText.setText(text);
                    writeDatabase(text);
                    writeDatabase2(text);
                    startNlu(text);

                    btn.setText("Listen");
                }

                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (textToSpeech.isSpeaking() == false)
                        {
                            startAsr();
                        }
                    }
                };
            }

            @Override
            public void onPartialResults(Bundle partialResults)
            {

            }
            @Override
            public void onEvent(int eventType, Bundle params)
            {

            }
        });
    }


    public void startAsr()
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                // TODO: Set Language
                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "CHANGE_THIS_TO_LANGUAGE");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                // Stop hotword detection in case it is still running
                shouldDetect = false;

                // TODO: Start ASR
                speechRecognizer.startListening(recognizerIntent);
            }
        };
        Threadings.runInMainThread(this, runnable);
    }


    private void setupTts()
    {
        textToSpeech = new TextToSpeech(this, null);
    }


    private void startTts(String text)
    {

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH,null);

        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                while (textToSpeech.isSpeaking())
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        Log.e("tts", e.getMessage());
                    }
                }
                startHotword();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }


    private void setupHotword()
    {
        SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        File common = new File(snowboyDirectory, "common.res");

        File voice1 = new File(snowboyDirectory, "Also.pmdl");
        File voice2 = new File(snowboyDirectory, "Another_question.pmdl");
        File voice3 = new File(snowboyDirectory, "Another_thing.pmdl");
        File voice4 = new File(snowboyDirectory, "Aye.pmdl");
        File voice5 = new File(snowboyDirectory, "Er.pmdl");
        File voice6 = new File(snowboyDirectory, "Erm.pmdl");
        File voice7 = new File(snowboyDirectory, "Hey.pmdl");
        File voice8 = new File(snowboyDirectory, "Next.pmdl");
        File voice9 = new File(snowboyDirectory, "Nurse.pmdl");
        File voice10 = new File(snowboyDirectory, "Oi.pmdl");
        File voice13 = new File(snowboyDirectory, "Umm.pmdl");
        File voice11 = new File(snowboyDirectory, "so.pmdl");
        File voice12 = new File(snowboyDirectory, "uh.pmdl");

        String models = new String(voice1.getAbsolutePath() + "," + voice2.getAbsolutePath() + "," + voice3.getAbsolutePath() + "," + voice4.getAbsolutePath() + "," + voice5.getAbsolutePath() + "," + voice6.getAbsolutePath() + "," + voice7.getAbsolutePath() + "," + voice8.getAbsolutePath() + "," + voice9.getAbsolutePath() + "," + voice10.getAbsolutePath() + "," + voice11.getAbsolutePath() + "," + voice12.getAbsolutePath() + "," + voice13.getAbsolutePath());

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), models);
        //snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), small.getAbsolutePath());
        snowboyDetect.setSensitivity("0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40, 0.40");
        snowboyDetect.applyFrontend(true);
    }


    public void startHotword()
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {

                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                while (shouldDetect)
                {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0)
                    {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                startAsr();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }


    public void setupNlu()
    {
        // TODO: Change Client Access Token
        String clientAccessToken = "9405120034414136af3d856c34366f9d";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken, AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }


    public void startNlu(final String text)
    {

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems);
        // TODO: Start NLU
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);

                try
                {
                    AIResponse aiResponse = aiDataService.request(aiRequest);

                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String responseText = fulfillment.getSpeech();

                    if (responseText.equalsIgnoreCase("weather_function"))
                    {
                        responseText = getWeather();
                    }

                    if (responseText.equalsIgnoreCase("date_function"))
                    {
                        responseText = getDate();
                    }

                    if (responseText.equalsIgnoreCase("time_function"))
                    {
                        responseText = getCurrentTime();
                    }

                    if (responseText.equalsIgnoreCase("your temperature is normal. To ease your cough, remember to stay hydrated. You can try taking lozenges and hot drinks too. Hope you get well soon!"))

                    {
                        responseText = getCoughNormalTemp();
                    }

                    if (responseText.equalsIgnoreCase("you have a fever. Since you have a cough as well, I suggest you schedule an appointment with your doctor."))
                    {
                        responseText = getCoughHighTemp();
                    }

                    if (responseText.equalsIgnoreCase("alright, your temperature is in the normal range. That's the end of your checkup. You're doing it alright, keep it up!"))
                    {
                        responseText = getCheckupNormalTemp();
                    }

                    if (responseText.equalsIgnoreCase("your temperature is fine. That's the end of today's checkup. You're doing it fine, keep it up!"))
                    {
                        responseText = getCheckupNormalTemp();
                    }

                    if (responseText.equalsIgnoreCase("that's good, your temperature is normal. That's the end of today's checkup. You're doing well, keep up the good work!"))
                    {
                        responseText = getCheckupNormalTemp();
                    }

                    if (responseText.equalsIgnoreCase("okay, your temperature is completely normal. That's the end of your checkup. You're doing well, live long and prosper!"))
                    {
                        responseText = getCheckupNormalTemp();
                    }

                    if (responseText.equalsIgnoreCase("you seem to be running a fever. I'll let your doctor know, please get sufficient rest."))
                    {
                        responseText = getCheckupHighTemp();
                    }

                    if (responseText.equalsIgnoreCase("you've got a fever. Drink plenty of water and make sure you get enough rest."))
                    {
                        responseText = getCheckupHighTemp();
                    }

                    if (responseText.equalsIgnoreCase("you've got a fever, I'll let your doctor know. In the meantime, do get some rest and stay hydrated."))
                    {
                        responseText = getCheckupHighTemp();
                    }

                    if (responseText.equalsIgnoreCase("you've got a fever, I'll let your doctor know but in the meantime, do get sufficent rest."))
                    {
                        responseText = getCheckupHighTemp();
                    }


                    //Appointment 4pm

                    /*if (responseText.contains("post_schedule_function"))
                    {
                        int appointment = text.indexOf("4 p.m." + "," + "5 p.m.");

                        if (appointment == -1)
                        {
                            responseText = "Sorry Jane";
                        }
                        else
                        {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

                            responseText = "Alright, let me take a photo of you so that your doctor can verify your identity." + " " + getSlot7();

                        }

                    }

                    if (responseText.equalsIgnoreCase("getslot7"))
                    {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

                        responseText ="Alright, let me take a photo of you so that your doctor can verify your identity." + " " + getSlot7();
                    }*/

                    //Appointment 5pm

                    if (responseText.equalsIgnoreCase("post_schedule_function"))
                    {
                        int appointment = text.indexOf("5 p.m.");

                        if (appointment == -1)
                        {
                            String appointment2 = editText.getText().toString();

                            mDataBase2.setValue(appointment2);
                            mDataBase4.setValue(appointment2);

                            responseText = "Sorry Jane. We are only left with 5pm slot on this date. What slot would you like?";
                        }
                        else
                        {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                            responseText ="Alright, let me take a photo of you so that your doctor can verify your identity." + " " + getSlot8();
                        }
                    }

                    if (responseText.equalsIgnoreCase("getslot8"))
                    {

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

                        responseText ="Alright, let me take a photo of you so that your doctor can verify your identity." + " " + getSlot8();
                    }
                    

                    if (responseText.equalsIgnoreCase("last_checkup_function"))
                    {
                        responseText = "Your last checkup was " + getDateNo();
                    }


                    //cough high temperature
                    if (responseText.contains("You have a fever. Since you have a cough as well, I suggest you schedule an appointment with your doctor."))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });

                    }

                    //cough normal temperature
                    if (responseText.contains("Your temperature is normal. To ease your cough, remember to stay hydrated. You can try taking lozenges and hot drinks too. Hope you get well soon!"))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }

                    //Checkup normal temperature
                    if (responseText.contains("Alright, your temperature is in the normal range. That's the end of your checkup. You're doing alright, keep it up!"))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("Your temperature is fine. That's the end of today's checkup. You're doing fine, keep it up!"))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("That's good, your temperature is normal. That's the end of today's checkup. You're doing well, keep up the good work!"))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("Okay, your temperature is completely normal. That's the end of your checkup. You're doing well, live long and prosper!"))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }

                    //Checkup High temperature
                    if (responseText.contains("You seem to be running a fever. I'll let your doctor know, please get sufficient rest."))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("You've got a fever. Drink plenty of water and make sure you get enough rest."))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("You've got a fever, I'll let your doctor know. In the meantime, do get some rest and stay hydrated."))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }
                    if (responseText.contains("You've got a fever, I'll let your doctor know but in the meantime, do get sufficient rest."))
                    {
                        mDataBase.child("Temperature").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>()

                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {

                            }
                        });
                    }

                    final String newRespondText = responseText;

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            textView.setText(newRespondText);
                            String[] values = new String[]{newRespondText};

                            for (int i = 0; i < values.length; i++)
                            {
                                listItems.add(values[i]);
                            }
                            historyList.setAdapter(adapter);
                            startTts(newRespondText);
                        }
                    });
                }
                catch (AIServiceException e)
                {
                    Log.e("nlu", e.getMessage(), e);
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);

    }

    private String getWeather()
    {
        // TODO: (Optional) Get Weather Data via REST API
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https;//api.data.gov.sg/v1/environment/2-hour-weather-forecast")
                .addHeader("get", "application/json")
                .build();

        try
        {
            Response response = okHttpClient.newCall(request).execute();
            String responseString = response.body().string();

            JSONObject jsonObject = new JSONObject(responseString);
            JSONArray forecasts = jsonObject.getJSONArray("items")
                    .getJSONObject(0)
                    .getJSONArray("forecasts");

            for (int i = 0; i < forecasts.length(); i++)
            {
                JSONObject forecastObject = forecasts.getJSONObject(i);
                String area = forecastObject.getString("area");

                if (area.equalsIgnoreCase("bedok"))
                {
                    String forecast = forecastObject.getString("forecast");
                    return "The weather right now is " + forecast;
                }
            }

        }
        catch (IOException e)
        {
            Log.e("weather", e.getMessage(), e);
        }
        catch (JSONException e)
        {
            Log.e("weather", e.getMessage(), e);
        }
        return "No weather info";
    }



    public String getCurrentTime()
    {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss");
        String strTime = mdformat.format(calendar.getTime());

        return "The time is " + strTime;
    }

    public String getDate()
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM YYYY");
        String strDate = sdf.format(c.getTime());
        return "The date is " + strDate;
    }

    public String getCoughNormalTemp()
    {
        String strNormalTemp = editText.getText().toString().replace("[^0-9]", "");
        char degreesymbol = '\u2103';
        return strNormalTemp + degreesymbol + "," + " " + "Jane, Your temperature is normal. To ease your cough, remember to stay hydrated. You can try taking lozenges and hot drinks too. Hope you get well soon!";
    }

    public String getCoughHighTemp()
    {
        String strHighTemp2 = editText.getText().toString().replace("[^0-9]", "");
        char degreesymbol2 = '\u2103';
        return strHighTemp2 + degreesymbol2 + "," + " " + "Jane, You have a fever. Since you have a cough as well, I suggest you schedule an appointment with your doctor.";
    }

    public String getCheckupNormalTemp()
    {
        String chkupNormalTemp = editText.getText().toString().replace("[^0-9]", "");
        char degreesymbol = '\u2103';
        return chkupNormalTemp + degreesymbol + "," + " " + "Jane, Your temperature is normal, live long and prosper!";
    }

    public String getCheckupHighTemp()
    {
        String chkupNormalTemp2 = editText.getText().toString().replace("[^0-9]", "");
        char degreesymbol2 = '\u2103';
        return chkupNormalTemp2 + degreesymbol2 + "," + " " + "Jane " + "," + "It seems like you are having a fever. Please get some rest while I contact the doctor.";
    }

   /* public String getSlot7()
    {
        String appointment1 = editText.getText().toString();

        if (appointment1 != "4 p.m.")
        {
            return "You have successfully booked 4pm slot.";
        }
        else
        {
            return "Sorry Jane, the slots are taken. What slot will you like to book?";
        }

    }*/

    public String getSlot8()
    {
        String appointment3 = editText.getText().toString().replace("5 p.m.","July 1st");

        if (appointment3 != "5 p.m.")
        {
            mDataBase2.setValue(appointment3);
            mDataBase4.setValue(appointment3);
            return "You have successfully booked 5pm slot.";

        }
        else
        {
            return "Sorry Jane, the slots is taken. What slot will you like to book?";
        }

    }


    public void writeDatabase(final String string)
    {
        //Record Data
        String Symptom = string;



        final HashMap<String, String> dataMap = new HashMap<String, String>();

        //filter

        int symptom1 = string.indexOf("cough");


        if (symptom1 == -1)
        {

        }
        else
        {
            mDataBase.child("Symptom").push().setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });

        }

        int symptom2 = string.indexOf("anxiety");

        if (symptom2 == -1)
        {

        }
        else
        {
            mDataBase.child("Symptom").push().setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

        int symptom3 = string.indexOf("panic attack");

        if (symptom3 == -1)
        {

        }
        else
        {
            mDataBase.child("Symptom").push().setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

        int symptom4 = string.indexOf("back pain");

        if (symptom4 == -1)
        {

        }
        else
        {
            mDataBase.child("Symptom").push().setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

        int symptom5 = string.indexOf("dizziness");

        if (symptom5 == -1)
        {

        }
        else
        {
            mDataBase.child("Symptom").push().setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

        int checkup1 = string.indexOf("checkup");

        if (checkup1 == -1)
        {

        }
        else
        {
            mDataBase.child("Checkup").setValue(getDateNo()).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

        int checkup2 = string.indexOf("check up");

        if (checkup2 == -1)
        {

        }
        else
        {

            mDataBase.child("Checkup").setValue(getDateNo()).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }


        int temperature2 = -1;

        StringBuilder sb = new StringBuilder();

        boolean found = false;

        for (char c : string.toCharArray())
        {
            if (Character.isDigit(c))
            {
                sb.append(c);
                found = true;
            }
            else if (found == true)
            {
                break;
            }
        }
        if (!(sb.toString().equals("")))
        {
            temperature2 = Integer.parseInt(sb.toString());
        }

       /* if (temperature2 == -1)
        {

        }
        else
        {
            mDataBase.child("Temperature").setValue(Symptom).addOnCompleteListener(new OnCompleteListener<Void>()

            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }*/





    }


    public void writeDatabase2(final String string1)
    {

        //Record Slot
        //String slot = "Malcolm";
        String slot2 = "Jane";



        final HashMap<String, String> dataMap = new HashMap<String, String>();


        /*int slot7 = string1.indexOf("4 p.m.");

        if (slot7 == -1)
        {

        }
        else
        {
            mDataBase3.child("4 PM").child("Name").setValue(slot).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }*/

        int slot8 = string1.indexOf("5 p.m.");

        if (slot8 == -1)
        {

        }
        else
        {
            mDataBase3.child("5 PM").child("Name").setValue(slot2).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                }
            });
        }

    }

}






