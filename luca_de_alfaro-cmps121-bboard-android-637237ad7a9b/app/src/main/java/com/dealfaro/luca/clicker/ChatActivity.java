package com.dealfaro.luca.clicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;




public class ChatActivity extends ActionBarActivity {

    // Progress bar showing call to server in progress
    private ProgressBar spinner;

    AppInfo appInfo;

    Location lastLocation;
    private double lastAccuracy = (double) 1e10;
    private long lastAccuracyTime = 0;

    private double longitude;
    private double latitude;

    private static final String TAG = "MyActivity";

    private static final String LOG_TAG = "lclicker";

    private static final float GOOD_ACCURACY_METERS = 100;

    // This is an id for my app, to keep the key space separate from other apps.
    private static final String MY_APP_ID = "luca_bboard";

    private static final String SERVER_URL_PREFIX = "https://hw3n-dot-luca-teaching.appspot.com/store/default/";

    // To remember the favorite account.
    public static final String PREF_ACCOUNT = "pref_account";

    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;

    // Remember whether we have already successfully checked in.
    private boolean checkinSuccessful = false;

    private ArrayList<String> accountList;

    private class ListElement {
        ListElement() {};

        public String textLabel;
        public String buttonLabel;
        //public String dest;
    }

    private ArrayList<ListElement> aList;

    private class MyAdapter extends ArrayAdapter<ListElement> {



        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            final ListElement w = getItem(position);

            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource,  newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            TextView tv = (TextView) newView.findViewById(R.id.itemText);
            Button b = (Button) newView.findViewById(R.id.itemButton);
            tv.setText(w.textLabel);
            b.setText(w.buttonLabel);

            // Sets a listener for the button, and a tag for the button as well.
            b.setTag(new Integer(position));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Reacts to a button press.
                    // Gets the integer tag of the button.
//                    String s = v.getTag().toString();
//                    int duration = Toast.LENGTH_SHORT;
//                    Toast toast = Toast.makeText(context, s, duration);
//                    toast.show();
                }
            });

            // Set a listener for the whole list item.
            newView.setTag(w.textLabel);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent intent = new Intent(MainActivity.this,ChatActivity.class);
//                    intent.putExtra("dest", w.dest);
//                    startActivity(intent);
//                    context.startActivity(intent);

                }
            });

            return newView;
        }
    }

    private MyAdapter aa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String dest = getIntent().getStringExtra("dest");
        Log.v(TAG, "dest = " + dest);
        appInfo = AppInfo.getInstance(this);
        setContentView(R.layout.activity_main);
        aList = new ArrayList<ListElement>();
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
        aa.notifyDataSetChanged();

        // Progress bar is initially set to not visible
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);

        Log.v(TAG, "Initialized properly");

//        PostMessageSpec myCallSpec = new PostMessageSpec();
//
//
//        String lat= "" + latitude ;
//        String lng= "" + longitude ;
//        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
//        myCallSpec.context = ChatActivity.this;
//
//        // Let's add the parameters.
//        HashMap<String,String> m = new HashMap<String,String>();
//        // m.put("app_id", MY_APP_ID);
//        // m.put("msg", msg);
//        m.put("lat", lat);
//        m.put("lng", lng);
//        myCallSpec.setParams(m);
//        // Actual server call.
//        if (uploader != null) {
//            // There was already an upload in progress.
//            uploader.cancel(true);
//        }
//        uploader = new ServerCall();
//        uploader.execute(myCallSpec);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Then start to request location updates, directing them to locationListener.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Button button;

        button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(refresh);

        // First super, then do stuff.
        // Let us display the previous posts, if any.

    }

    @Override
    protected void onPause() {
        // Stops the location updates.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);

        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }
        super.onPause();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Do something with the location you receive.

            TextView labelView = (TextView) findViewById(R.id.currLocation);
            if (location == null) {
                String acc = String.format("Unable to connect");
                labelView.setText(acc);

                //Log.v(TAG, "Hitting null");
            } else {

                String acc = String.format("Latitude: %.6f\nLongitude: %.6f", +location.getLatitude(), +location.getLongitude());
                labelView.setText(acc);
                latitude=location.getLatitude();
                longitude=location.getLongitude();
                //Log.v(TAG, "Working " +location.getLatitude() +location.getLongitude());
            }


            //Log.v(TAG, "location is:" + location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };



    public void clickButton(View v) {

        // Make progressBar visible
        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.VISIBLE);


        // Get the text we want to send.
        EditText et = (EditText) findViewById(R.id.editText);
        String msg = et.getText().toString();

        // Reset the editText as null after posting
        et.setText(null);

        // Then, we start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();
        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        myCallSpec.context = ChatActivity.this;

        String dest = getIntent().getStringExtra("dest");

        String lat= "" + latitude ;
        String lng= "" + longitude ;
        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        myCallSpec.context = ChatActivity.this;


        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();
        //m.put("app_id", MY_APP_ID);
        m.put("msgid", reallyComputeHash(msg));
        m.put("msg", msg);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("userid", appInfo.PREF_USERID);
        //m.put("dest",getIntent().getStringExtra("dest"));
        m.put("dest", dest);

        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);


    }






    View.OnClickListener refresh = new View.OnClickListener() {
        public void onClick(View v) {

            // Make progressBar visible
            spinner = (ProgressBar)findViewById(R.id.progressBar);
            spinner.setVisibility(View.VISIBLE);

            Log.v(TAG, "Getting here after refresh button click");

            PostMessageSpec myCallSpec = new PostMessageSpec();


            String dest = getIntent().getStringExtra("dest");
            String lat= "" + latitude ;
            String lng= "" + longitude ;
            myCallSpec.url = SERVER_URL_PREFIX + "get_local";
            myCallSpec.context = ChatActivity.this;

            // Let's add the parameters.
            HashMap<String,String> m = new HashMap<String,String>();

            m.put("lat", lat);
            m.put("lng", lng);
            m.put("userid", appInfo.PREF_USERID);

            //m.put("lat", lat);
            //m.put("lng", lng);
            m.put("dest", dest);
            myCallSpec.setParams(m);
            // Actual server call.
            if (uploader != null) {
                // There was already an upload in progress.
                uploader.cancel(true);
            }
            uploader = new ServerCall();
            uploader.execute(myCallSpec);



        }
    };


    private String reallyComputeHash(String s) {
        // Computes the crypto hash of string s, in a web-safe format.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            digest.update("My secret key".getBytes());
            byte[] md = digest.digest();
            // Now we need to make it web safe.
            String safeDigest = Base64.encodeToString(md, Base64.URL_SAFE);
            return safeDigest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
        }
    }


    private void displayResult(String result) {
        Log.v(TAG, "Result is: " + result);
        spinner.setVisibility(View.GONE);
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);
        // Fills aList, so we can fill the listView.
        aList.clear();
        for (int i = 0; i < ml.messages.length; i++) {
            ListElement ael = new ListElement();
            ael.textLabel = ml.messages[i].msg+"\n"+ml.messages[i].ts;
            ael.buttonLabel = "Talk";
            //ael.dest =ml.messages[i].userid;
            aList.add(ael);
        }
        aa.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
