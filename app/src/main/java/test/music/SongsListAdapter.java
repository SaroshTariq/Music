package test.music;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;



public abstract class SongsListAdapter extends ArrayAdapter<Song> {


    public SongsListAdapter(Context context, ArrayList<Song> resource) {
        super(context, R.layout.songs_list, resource);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    //-----------Setting  up Ui----------

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        convertView = inflater.inflate(R.layout.songs_list, parent, false);


        final Song song = getItem(position);

        final ImageButton optionsImgBtn = convertView.findViewById(R.id.options_imgbbtn);
        optionsImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu optionnMenu = new PopupMenu(getContext(), view);
                optionnMenu.getMenuInflater().inflate(R.menu.list_option, optionnMenu.getMenu());

                optionnMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if(menuItem.getItemId()==R.id.vd){
                            performAction(0, position);
                        }else if(menuItem.getItemId()==R.id.vdi){
                            performAction(1, position);
                        }else if(menuItem.getItemId()==R.id.q){
                            performAction(2, position);
                        }
                        return true;
                    }
                });
                optionnMenu.show();
            }
        });

        TextView txtvDname = convertView.findViewById(R.id.dname_txtv);
        TextView txtvArtist = convertView.findViewById(R.id.artist_txtv);
       // TextView txtvDuration = convertView.findViewById(R.id.duration_txtv);



        txtvDname.setText(song.name);
        txtvArtist.setText(song.artist);
        //txtvDuration.setText(getFormatedTime(Integer.parseInt(song.duration)));

        return  convertView;
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

    //----------This method will be implemented in MainActivity----------
    abstract void performAction(int action, int position);
}
