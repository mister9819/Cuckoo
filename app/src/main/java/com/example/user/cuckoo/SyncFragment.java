package com.example.user.cuckoo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.content.ContentValues.TAG;


public class SyncFragment extends Fragment {

    View view;
    RecyclerView rview;
    SwipeRefreshLayout slayout;
    MyDBHandler dbHandler;
    List<LocObject> data;
    SyncAdapter syncAdapter;

    public SyncFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sync, container, false);

        rview = view.findViewById(R.id.rview);
        slayout = view.findViewById(R.id.slayout);
        dbHandler = new MyDBHandler(getContext(), null);

        data = new ArrayList<>();
        syncAdapter = new SyncAdapter(data);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        rview.setLayoutManager(layoutManager);
        rview.setAdapter(syncAdapter);

        slayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(hasInternetAccess()) {
                    int count = 0;
                    for(LocObject loc : data)
                        if(!loc.isSync()) {
                            dbHandler.syncLocation(loc.getId());
                            loc.setSync(true);
                        } else {
                            count++;
                            if(count >= 10) break;
                        }
                    syncAdapter.notifyDataSetChanged();
                }
                slayout.setRefreshing(false);
            }
        });

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            data.clear();
            data.addAll(dbHandler.getLocationData());
            syncAdapter.notifyDataSetChanged();
        }
    }

    public class SyncAdapter extends RecyclerView.Adapter<SyncAdapter.SyncHolder> {

        List<LocObject> data;

        public SyncAdapter(List<LocObject> loc) {
            data = loc;
        }

        @NonNull
        @Override
        public SyncHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout, parent, false);
            return new SyncHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull SyncHolder holder, int position) {
            String title = data.get(position).getTitle();
            double lat = data.get(position).getLat();
            double lon = data.get(position).getLon();
            long ntimestamp = data.get(position).getNtimestamp();
            boolean sync = data.get(position).isSync();
            String comment = data.get(position).getComment();
            boolean arrive = data.get(position).isArrive();
            long time = data.get(position).getTimestamp();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            SimpleDateFormat myDateFormat = new SimpleDateFormat("EEE MMM dd yyyy hh:mm a"); //or ("dd.MM.yyyy"), If you want days before months.
            String formattedDate = myDateFormat.format(calendar.getTime());

            holder.tv_title.setText(title);
            holder.tv_lat.setText("LAT: " + String.valueOf(lat));
            holder.tv_lon.setText("LON: " + String.valueOf(lon));
            holder.tv_time.setText(formattedDate);
            if(sync) holder.iv_sync.setImageResource(R.drawable.ic_sync_black_24dp);
            else holder.iv_sync.setImageResource(R.drawable.ic_sync_problem_black_24dp);
            if(!comment.equals(" ")) holder.iv_comment.setImageResource(R.drawable.ic_comment_black_24dp);
            else holder.iv_comment.setImageResource(R.drawable.ic_comment_gray_24dp);
            if(ntimestamp == 0) holder.iv_alarm.setImageResource(R.drawable.ic_alarm_off_black_24dp);
            else holder.iv_alarm.setImageResource(R.drawable.ic_alarm_black_24dp);
            if(arrive) holder.iv_arrive.setImageResource(R.drawable.ic_arrow_downward_24dp);
            else holder.iv_arrive.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class SyncHolder extends RecyclerView.ViewHolder {

            TextView tv_lat, tv_lon, tv_title, tv_time;
            ImageView iv_sync, iv_comment, iv_alarm, iv_arrive;

            SyncHolder(View itemView) {
                super(itemView);
                tv_title = itemView.findViewById(R.id.tv_title);
                tv_lat = itemView.findViewById(R.id.tv_lat);
                tv_lon = itemView.findViewById(R.id.tv_lon);
                tv_time = itemView.findViewById(R.id.tv_time);
                iv_sync = itemView.findViewById(R.id.iv_sync);
                iv_comment = itemView.findViewById(R.id.iv_comment);
                iv_alarm = itemView.findViewById(R.id.iv_alarm);
                iv_arrive = itemView.findViewById(R.id.iv_arrive);

                tv_lat.setOnClickListener(mapListener);
                tv_lon.setOnClickListener(mapListener);

                iv_sync.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(hasInternetAccess()) {
                            int id = data.get(getAdapterPosition()).getId();
                            dbHandler.syncLocation(id);
                            data.clear();
                            data.addAll(dbHandler.getLocationData());
                            syncAdapter.notifyDataSetChanged();
                        }
                    }
                });
                iv_comment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String comment;
                        comment = data.get(getAdapterPosition()).getComment();
                        if(comment.equals(" ")) comment = "No comment so far...";
                        AlertDialog.Builder builder;
                        builder = new AlertDialog.Builder(getContext());
                        final String finalComment = comment;
                        builder.setTitle("Comment")
                                .setMessage(comment)
                                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("Edit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        editBox(finalComment, "Edit");
                                    }
                                })
                                .show();
                    }
                });

                iv_alarm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long alarm = data.get(getAdapterPosition()).getNtimestamp();
                        if(alarm == 0) showDateTimePicker(alarm);
                        else {
                            Calendar date1 = Calendar.getInstance();
                            date1.setTimeInMillis(alarm);
                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Reminder")
                                    .setMessage("The reminder time is " + date1.getTime())
                                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Remove", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            cancelAlarm(data.get(getAdapterPosition()).getId(), data.get(getAdapterPosition()).getTitle(),
                                                    data.get(getAdapterPosition()).getComment());
                                            dbHandler.updateAlarm(data.get(getAdapterPosition()).getId(), 0);
                                            data.get(getAdapterPosition()).setNtimestamp(0);
                                            syncAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .show();
                        }
                    }
                });
            }

            public View.OnClickListener mapListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMaps(data.get(getAdapterPosition()).getLat(), data.get(getAdapterPosition()).getLon());
                }
            };


            public void editBox(final String comment, String title) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(title);

                // Set up the input
                final EditText input = new EditText(getContext());

                input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSingleLine(false);  //add this
                input.setMaxLines(5);
                input.setGravity(Gravity.LEFT | Gravity.TOP);
                input.setHorizontalScrollBarEnabled(false);
                if(!comment.equals("No comment so far..."))
                    input.setText(comment);
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
                            editBox("No comment so far...", "Edit - cannot use invalid symbols");
                            dialog.dismiss();
                            return;
                        }
                        int id = data.get(getAdapterPosition()).getId();
                        String ntitle = data.get(getAdapterPosition()).getTitle();
                        long alarm = data.get(getAdapterPosition()).getNtimestamp();
                        data.get(getAdapterPosition()).setComment(ncomment);
                        if(alarm != 0) {
                            cancelAlarm(id, ntitle, comment);
                            setAlarm(alarm, id, ntitle, ncomment);
                        }
                        dbHandler.updateComment(id, ncomment);
                        syncAdapter.notifyDataSetChanged();
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
            public void showDateTimePicker(long alarm) {
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
                                int id = data.get(getAdapterPosition()).getId();
                                long alarm = date.getTimeInMillis();
                                String title = data.get(getAdapterPosition()).getTitle();
                                String comment = data.get(getAdapterPosition()).getComment();
                                Log.e(TAG, String.valueOf(alarm));
                                dbHandler.updateAlarm(id, alarm);
                                data.get(getAdapterPosition()).setNtimestamp(alarm);
                                syncAdapter.notifyDataSetChanged();
                                setAlarm(alarm, id, title, comment);
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

            public void cancelAlarm(int id, String title, String comment) {
                AlarmManager alarmManager;
                PendingIntent pendingIntent;
                alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

                Intent intent = new Intent(getContext(), AlarmReceiver.class);
                intent.putExtra("id", id);
                intent.putExtra("title", title);
                intent.putExtra("comment", comment);
                pendingIntent = PendingIntent.getBroadcast(getContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }

            public void openMaps(double lat, double lon) {
                String query = "geo:" + String.valueOf(lat) + "," + String.valueOf(lon) + "?z=17";
                Uri gmmIntentUri = Uri.parse(query);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(getContext(), "Install maps to view location", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
