package com.example.user.cuckoo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Instrumentation;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    View view;
    TextView textView2, alarm_empty;
    Button button;
    AutoCompleteTextView editText;
    SharedPreferences sp;
    boolean isArrive, update = false;
    MyDBHandler dbHandler;
    List<LocObject> locs;
    List<String> listFeatured, listAlarmed, titles;
    private static final int CAMERA_REQUEST = 1888;
    Bitmap photo;
    double lat, lon;
    String title, data;
    Map<String, Integer> locc;
    ListView featured, alarmed;
    ArrayAdapter<String> aadapter;

    public HomeFragment() {}

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_home, container, false);

        textView2 = view.findViewById(R.id.textView2);
        alarm_empty= view.findViewById(R.id.alarm_empty);
        button = view.findViewById(R.id.button);
        editText = view.findViewById(R.id.editText);
        featured = view.findViewById(R.id.featured);
        alarmed = view.findViewById(R.id.alarmed);
        sp = getActivity().getSharedPreferences("settings", MODE_PRIVATE);
        isArrive = sp.getBoolean("arrive", true);
        dbHandler = new MyDBHandler(getContext(), null);

        textView2.getPaint().setUnderlineText(true);
        photo = null;

        locs = new ArrayList<>();
        listFeatured = new ArrayList<>();
        listAlarmed = new ArrayList<>();
        locs.addAll(dbHandler.getLocationData());
        titles = new ArrayList<>();
        locc = new HashMap<>();
        for(LocObject loc : locs) {
            String title = loc.getTitle();
            Integer count = locc.get(title);
            locc.put(title, (count==null) ? 1 : count+1);
            if(loc.getNtimestamp() > 0) listAlarmed.add(title);
        }

        processFeatured();
        featured.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isArrive) {
                    editText.setText((String)featured.getItemAtPosition(position));
                    editText.setSelection(editText.getText().length());
                    editText.setThreshold(1000);
                }
            }
        });
        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                editText.setThreshold(1);
                return false;
            }
        });
        //Alarmed list
        if(listAlarmed.size() == 0) alarm_empty.setVisibility(View.VISIBLE);
        aadapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, listAlarmed);
        alarmed.setAdapter(aadapter);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText.getText().toString().equals("")) {
                    editText.setError("Required field!");
                    return;
                }
                GPStracker g = new GPStracker(getContext());
                Location l = g.getLocation();
                if(l != null) {
                    lat = l.getLatitude();
                    lon = l.getLongitude();
                    title = editText.getText().toString();
                    data = title + "\nLAT: " + lat + "\nLON: " + lon;
                    getImage();
                    if(photo != null) {

                    } else Log.e("Hmmm", "Photo not ready yet.");
                } else {
                    Log.e("HMM", "Object not added!");
                }
            }
        });
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isArrive = true;
                sp.edit().putBoolean("arrive", isArrive).apply();
                updateUI();
            }
        });

        updateUI();
        //update = true;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(photo != null) {
            Log.e("Hmm", String.valueOf(lat) + String.valueOf(lon));
            LocObject locObject = new LocObject(title, lat, lon, photo, hasInternetAccess(), isArrive);
            dbHandler.addLocation(locObject);
            if (hasInternetAccess())
                for (LocObject loc : locs)
                    if (!loc.isSync()) {
                        Log.e("Hmm", "Syncing...");
                        dbHandler.syncLocation(loc.getId());
                    } else break;
            Log.e("HMM", "Object added!");
            locs.clear();
            locs.addAll(dbHandler.getLocationData());
            int id = locs.get(0).getId();
            if (isArrive) sp.edit().putString("title", title).apply();
            else editBox(title, id);
            isArrive = !isArrive;
            sp.edit().putBoolean("arrive", isArrive).apply();
            textView2.setVisibility(View.VISIBLE);
            Integer count = locc.get(title);
            locc.put(title, (count==null) ? 1 : count+1);
            photo = null;
            processFeatured();
            updateUI();
        }
    }

    public void processFeatured() {
        titles.clear();
        listFeatured.clear();
        miniProcess(6);
        //Autocomplete list
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, titles);
        editText.setAdapter(adapter);
        //Featured list
        if(listFeatured.size() <= 2) {
            listFeatured.clear();
            miniProcess(4);
            if(listFeatured.size() <= 2) {
                listFeatured.clear();
                miniProcess(2);
                if(listFeatured.size() <= 2) listFeatured.add("Office");
            }
        }
        ArrayAdapter<String> fadapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, listFeatured);
        featured.setAdapter(fadapter);
    }

    public void miniProcess(int value) {
        Set<Map.Entry<String, Integer>> set = locc.entrySet();
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(set);
        Collections.sort( list, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );
        for(Map.Entry<String, Integer> entry:list){
            if(value >= 6)
                titles.add(entry.getKey());
            Log.e("Hmmm" ,entry.getKey()+" ==== "+entry.getValue());
            if(entry.getValue() >= value) listFeatured.add(entry.getKey());
        }
    }

    public void getImage() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    123);
        } else {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(getContext(), "camera permission denied", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data != null)
                if(data.getExtras() != null) {
                    Log.e("Hmmm", "pic taken");
                    photo = (Bitmap) data.getExtras().get("data");
                    return;
                }
        }
        Toast.makeText(getContext(), "Some error occured while taking the picture", Toast.LENGTH_LONG).show();
    }
    public void updateUI() {
        if(isArrive) {
            button.setText("arrive");
            textView2.setVisibility(View.GONE);
            editText.setEnabled(true);
            editText.setText("");
            editText.requestFocus();
            //InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        } else {
            String title = sp.getString("title", "error");
            button.setText("leave");
            textView2.setVisibility(View.VISIBLE);
            editText.setEnabled(false);
            editText.setText(title);
            editText.clearFocus();
        }
    }

    /*@Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser && update) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }*/

    public void editBox(final String title, final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Write a comment?");

        // Set up the input
        final EditText input = new EditText(getContext());

        input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);  //add this
        input.setMaxLines(5);
        input.setGravity(Gravity.LEFT | Gravity.TOP);
        input.setHorizontalScrollBarEnabled(false);
        input.setSelection(input.getText().length());
        FrameLayout container = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60;
        params.rightMargin = 60;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ncomment = input.getText().toString();
                if(ncomment.equals("")) ncomment = " ";
                if(ncomment.contains("\'")) {
                    editBox("Edit - cannot use invalid symbol - \'", id);
                    dialog.dismiss();
                    return;
                }
                dbHandler.updateComment(id, ncomment);
                showDateTimePicker(0, id, title, ncomment);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    Calendar date;
    public void showDateTimePicker(long alarm, final int id, final String title, final String comment) {
        final Calendar currentDate = Calendar.getInstance();
        if(alarm != 0) currentDate.setTimeInMillis(alarm);
        date = Calendar.getInstance();
        new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                date.set(year, monthOfYear, dayOfMonth);
                new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);
                        Log.e(TAG, "The choosen one " + date.getTime());
                        long alarm = date.getTimeInMillis();
                        Log.e(TAG, String.valueOf(alarm));
                        dbHandler.updateAlarm(id, alarm);
                        setAlarm(alarm, id, title, comment);
                        listAlarmed.add(title);
                        aadapter.notifyDataSetChanged();
                        alarm_empty.setVisibility(View.GONE);
                    }
                }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    public void setAlarm(long alarm, int id, String title, String comment) {
        AlarmManager alarmManager;
        PendingIntent pendingIntent;
        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("comment", comment);
        pendingIntent = PendingIntent.getBroadcast(getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
    }

    public boolean isInternetAvailable(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info == null) {
            Log.d("Internet","no internet connection");
            return false;
        } else {
            if(info.isConnected()) {
                Log.d("Internet"," internet connection available...");
                return true;
            } else {
                Log.d("Internet"," internet connection");
                return true;
            }
        }
    }

    public boolean hasInternetAccess() {
        if (isInternetAvailable(getContext())) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("http://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 204 &&
                        urlc.getContentLength() == 0);
            } catch (IOException e) {
                Log.e(TAG, "Error checking internet connection", e);
            }
        } else {
            Log.d(TAG, "No network available!");
        }
        return false;
    }
}
