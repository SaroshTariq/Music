package test.music;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import test.music.MusicService.LocalBinder;

public class MainActivity extends AppCompatActivity {

    MusicService musicService;
    boolean serviceBound = false;

    ImageButton PPImgBtn, prevImgBtn, nextImgBtn, settingsImgBtn;

    TextView nowPlayingNameTxtV, covtimeTxtV, tottimeTxtV;
    SongsListAdapter songListAdapter;
    ListView songsListV;

    SeekBar songSeekBar;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForPermissions();
    }

    private void askForPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 77);
        }else{
            initializeUi();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 77){
            if(grantResults.length>0){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initializeUi();
                }else{
                    System.exit(0);
                }
            }
        }
    }

    void initializeUi(){
        intent = new Intent(this, MusicService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        seekbarHandler = new Handler();

        songsListV = (ListView)findViewById(R.id.songs_listv);

        PPImgBtn = (ImageButton)findViewById(R.id.pp_imgbtn);
        prevImgBtn = (ImageButton)findViewById(R.id.prev_imgbtn);
        nextImgBtn = (ImageButton)findViewById(R.id.next_imgbtn);
        settingsImgBtn = (ImageButton)findViewById(R.id.settings_imgbtn);

        songSeekBar = (SeekBar)findViewById(R.id.song_seekbar);

        nowPlayingNameTxtV = (TextView)findViewById(R.id.nowplayingname_txtv);

        covtimeTxtV = (TextView)findViewById(R.id.covtimetxtv);
        tottimeTxtV = (TextView)findViewById(R.id.tottimetxtv);
    }

    //----------Connection with service and ui----------

    ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            initFunctionality();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }

    };

    Runnable seekbarRunnable;
    Handler seekbarHandler;

    void setUpUi(){

        if(musicService.isPlaying()){
            PPImgBtn.setImageResource(R.mipmap.pause);
        }else{
            PPImgBtn.setImageResource(R.mipmap.play);
        }

        songSeekBar.setMax(musicService.getDuration());
        songSeekBar.setProgress(musicService.getCurrentPosition());
        tottimeTxtV.setText(getFormatedTime(musicService.getDuration()));
        covtimeTxtV.setText(getFormatedTime(musicService.getCurrentPosition()));

        Song nowPlayingSong = musicService.getPlayingSong();

        nowPlayingNameTxtV.setText(nowPlayingSong.name);
        seekbarRunnable = new Runnable() {
            @Override
            public void run() {
                setUpUi();
            }
        };
        seekbarHandler.postDelayed(seekbarRunnable, 300);
    }


    //----------Functiona Feature of activity----------

    void initFunctionality(){
        if(serviceBound==false){
            Toast.makeText(getApplicationContext(), "No Service::"+serviceBound, Toast.LENGTH_LONG).show();
        }else{
            if(musicService.getNumSongs()==0){
                Toast.makeText(getApplicationContext(), "Cannot detect any music files on your device", Toast.LENGTH_LONG).show();
                return;
            }

            setUpUi();
            settingsImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu optionnMenu = new PopupMenu(getApplicationContext(), view);
                    optionnMenu.getMenuInflater().inflate(R.menu.settings_menu, optionnMenu.getMenu());

                    optionnMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if(menuItem.getItemId()==R.id.sq){
                                try{
                                    if(musicService.queueIsEmpty()){
                                        Toast.makeText(getApplicationContext(), "No Songs in Queue.", Toast.LENGTH_LONG).show();
                                    }else{
                                        showQueueListDiaog();
                                    }

                                }catch (Exception e){
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                }

                            }else if(menuItem.getItemId()==R.id.ts){
                                Toast.makeText(getApplicationContext(), "This feature is not available yet.", Toast.LENGTH_LONG).show();
                            }
                            return true;
                        }
                    });
                    optionnMenu.show();
                }
            });

            songListAdapter = new SongsListAdapter(getApplicationContext(), musicService.songs) {
                @Override
                void performAction(int action, int position) {
                    try{
                        if(action<2){
                            showSongInfo(action, position);
                        }else{
                            if(musicService.queueSong(position)){
                                Toast.makeText(getApplicationContext(), musicService.getSongByIndex(position).name+" is added to queue.", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(), "Queue Full. You can only 15 songs at once.", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }


                }
            };
            songsListV.setAdapter(songListAdapter);

            songsListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    try {
                        musicService.playSong(i);
                        PPImgBtn.setImageResource(R.mipmap.pause);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "playSong() error::"+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            PPImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(musicService.isPlaying()){
                        musicService.pauseSong();
                        PPImgBtn.setImageResource(R.mipmap.play);
                    }else{
                        try {
                            musicService.playSong();
                            PPImgBtn.setImageResource(R.mipmap.pause);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "ppSong() error::"+e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            prevImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        musicService.prevSong();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "prevSong() error::"+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            nextImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        musicService.nextSong();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "nextSong() error::"+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int input, boolean user) {
                    if(user){
                        musicService.seekTo(input);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
    }

    void showSongInfo(int type, int songPosition){
        if(type==0){
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.info_dialog);

            TextView  songNameTxtv = (TextView) dialog.findViewById(R.id.songname_txtv);
            TextView songArtistTxtv = (TextView) dialog.findViewById(R.id.songartist_txtv);
            TextView songDurationTxtv = (TextView) dialog.findViewById(R.id.songDuration_txtv);
            Song song = musicService.getSongByIndex(songPosition);
            songNameTxtv.setText(songNameTxtv.getText().toString()+song.name);
            songArtistTxtv.setText(songArtistTxtv.getText().toString()+song.artist);
            songDurationTxtv.setText(songDurationTxtv.getText().toString()+getFormatedTime(Integer.parseInt(song.duration)));


            Button dialogButton = (Button) dialog.findViewById(R.id.ok_btn);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }else if(type==1){
            Toast.makeText(getApplicationContext(), "This feature is not available yet", Toast.LENGTH_LONG).show();
        }
    }

    void showQueueListDiaog(){
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.queue_dialog);

        ListView  queuedSongsListv = (ListView) dialog.findViewById(R.id.queue_istv);

        ArrayAdapter<String> queueAdapt = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, musicService.getQueuedSongs());
        queuedSongsListv.setAdapter(queueAdapt);

        Button doneQueueButton = (Button) dialog.findViewById(R.id.donequeue_btn);
        doneQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button clearQueueButton = (Button) dialog.findViewById(R.id.clearqueue_btn);
        clearQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Queue Cleared", Toast.LENGTH_LONG).show();
                musicService.clearQueue();

                dialog.dismiss();
            }
        });

        dialog.show();
    }


    String getFormatedTime(int millis){
        String duration = "";

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        if(hour!=0){
            duration = duration+hour+":";
        }
        if(minute!=0){
            if(minute<10){
                duration = duration+"0";
            }
            duration = duration+minute;
        }else{
            duration = duration+"00";
        }
        if(second!=0){
            duration = duration+":";
            if(second<10){
                duration = duration+"0";
            }
            duration = duration+second;
        }else{
            duration = duration+":00";
        }
        return duration;

    }


}
