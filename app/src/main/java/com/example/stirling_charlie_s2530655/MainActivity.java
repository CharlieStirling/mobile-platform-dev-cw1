/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Charlie Stirling
// Student ID           S2530655
// Programme of Study   BSc/BSc (Hons) Software Development
//

// UPDATE THE PACKAGE NAME to include your Student Identifier
package com.example.stirling_charlie_s2530655;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

public class MainActivity extends AppCompatActivity {
    private LinearLayout eventContainer;
    private ArrayList<TrafficEvent> allSituations; // only touched on the UI thread
    private GestureDetector swipeDetector; // for swiping to map activity

    //DOWNLOADING FROM TRAFFIC SCOTLAND
    final String clientId = "64950cd5-abed-4126-b8de-15ef1049a084";
    private String clientKey = ""; // gets changed in onCreate
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    final String currentRoadworksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Set up the links to the graphical components
        eventContainer = findViewById(R.id.eventContainer);

        allSituations = new ArrayList<>(); // used for saving situations

        // get client key from local.properties
        Bundle metaData = null;
        try {
            metaData = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            // set default value so that it can't be null
            clientKey = metaData.getString("stirling_charlie_s2530655.MY_KEY", "");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        Log.d("My key: ", clientKey);

        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                // a clearly horizontal, fast swipe to the left
                if (dx < -150 && Math.abs(dx) > Math.abs(dy) * 2 && Math.abs(velocityX) > 800) {
                    startActivity(new Intent(MainActivity.this, MapsActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    return true;
                }
                return false;
            }
        });

        startProgress();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        swipeDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    public void startProgress() {
        // swap commented lines below to just load unplanned events
        new Thread(new Task(unplannedEventsURL, currentRoadworksURL)).start();
        //new Thread(new Task(unplannedEventsURL)).start();
    }

    // displays parsed data
    private void showSituations() {
        eventContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (TrafficEvent event : allSituations) {
            View row = inflater.inflate(R.layout.item_event, eventContainer, false);
            ((TextView) row.findViewById(R.id.eventType)).setText(event.getImpactType());
            ((TextView) row.findViewById(R.id.eventComment)).setText(event.getGeneralPublicComment());
            if (event.getEnd() != null) {
                ((TextView) row.findViewById(R.id.eventStart)).setText(event.getLocalisedDateTime(event.getStart()) + "\n" + event.getLocalisedDateTime(event.getEnd()));
            } else {
                ((TextView) row.findViewById(R.id.eventStart)).setText(event.getLocalisedDateTime(event.getStart()));
            }


            // hidden until a situation is tapped
            TextView more = row.findViewById(R.id.eventMore);
            more.setText("Direction: " + event.getDirection() + "\n"
                    + "From: " + event.getFromDescriptor() + "\n"
                    + "To: " + event.getToDescriptor() + "\n"
                    + event.getLanes());

            ((ImageView) row.findViewById(R.id.eventIcon)) // background image
                    .setImageResource(watermarkImageFor(event));

            ((TextView) row.findViewById(R.id.eventWatermark)) // background text
                    .setText(event.getBgText().toUpperCase());

            row.setOnClickListener(v ->
                    more.setVisibility(more.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

            eventContainer.addView(row);

            EventStore.events = new ArrayList<>(allSituations); // add situations to arraylist so that maps can access it
        }
    }

    // icons created by me
    private int watermarkImageFor(TrafficEvent event) { // icon picker for background
        switch (event.getImpactType()) {
            case "Road Closure":
                return R.drawable.red_triangle;
            case "Road Works":
                return R.drawable.yellow_triangle;
            case "Queuing Traffic":
                return R.drawable.blue_triangle;
            default:
                return R.drawable.pink_triangle;
        }
    }

    //
    // thread for downloading & parsing
    //
    private class Task implements Runnable {
        private final String[] urls;

        public Task(String... urls) {
            this.urls = urls;
        }

        @Override
        public void run() {
            ArrayList<TrafficEvent> parsed = new ArrayList<>();
            for (String u : urls) {
                fetchAndParse(u, parsed);
            }
            runOnUiThread(() -> {
                if (parsed.isEmpty()) return; // keep old data if everything failed
                allSituations.clear();
                allSituations.addAll(parsed);
                showSituations();
                Toast.makeText(MainActivity.this, "Downloaded " + parsed.size() + " situations", Toast.LENGTH_LONG).show();
            });
        }

        //
        // download & parse
        //
        private void fetchAndParse(String myurl, ArrayList<TrafficEvent> out) {
            Log.d("MyTask", "in fetchAndParse: " + myurl);
            String result = "";
            BufferedReader in = null;
            String inputLine = "";

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Downloading data...", Toast.LENGTH_LONG).show()); // toast needs to run on UI thread

            try {
                URL url = new URL(myurl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //ADD AUTHORISATION (string with clientIID:clientKey)
                urlConnection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                try {
                    in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    result = sb.toString();
                    in.close();
                } catch (IOException e) {
                    Log.e("urlConnection", "Download failed: " + e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to access site", Toast.LENGTH_LONG).show()); // toast needs to run on UI thread
                    try {
                        Log.e("urlConnection", "HTTP code: " + urlConnection.getResponseCode());
                    } catch (IOException ignored) {
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("URL", "ioexception");
            }

            //Clean up any leading garbage characters
            int i = result.indexOf("<?");
            if (i < 0) {
                Log.e("MyTask", "No XML received from " + myurl);
                return;
            }
            result = result.substring(i);

            // Now that you have the xml data into result, you can parse it
            TrafficEvent aSituation = null; // declare the object
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(result));

                boolean insideSituation = false;
                boolean insideGeneralPublicComment = false;
                boolean insideFrom = false;
                boolean insideTo = false;

                int eventType = xpp.getEventType(); // gets the type of event

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) { // start tag
                        if (xpp.getName().equalsIgnoreCase("situation")) { // brand new situation
                            insideSituation = true;
                            aSituation = new TrafficEvent(); // create a new object for the parsed data
                            Log.d("PullParser", "New Situation found");

                            // get the ID from the situation start tag
                            String s = xpp.getAttributeValue(0);
                            Log.d("PullParser", "Situation ID: " + s);
                            aSituation.setId(s);
                        }

                        // get the type from the situationRecord start tag
                        else if (xpp.getName().equalsIgnoreCase("situationrecord")) {
                            String s = xpp.getAttributeValue(0);
                            if (insideSituation) {
                                aSituation.setSituationType(s);
                                Log.d("PullParser", "Situation Type: " + s);
                            }
                        }

                        // get source of situation
                        else if (xpp.getName().equalsIgnoreCase("sourceidentification")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setSource(temp);
                                Log.d("PullParser", "Source: " + temp);
                            }
                        }

                        // get type of impact (closures, roadworks and queuing traffic)
                        else if (xpp.getName().equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                                || xpp.getName().equalsIgnoreCase("roadMaintenanceType")
                                || xpp.getName().equalsIgnoreCase("abnormalTrafficType")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setImpactType(temp);
                                Log.d("PullParser", "Impact type: " + temp);
                            }
                        }

                        // get number of lanes operational
                        else if (xpp.getName().equalsIgnoreCase("numberofoperationallanes")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setLanesOperational(temp);
                                aSituation.setLanesRestricted(null);
                                Log.d("PullParser", "Operational lanes: " + temp);
                            }
                        }

                        // get number of lanes restricted
                        else if (xpp.getName().equalsIgnoreCase("numberoflanesrestricted")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setLanesRestricted(temp);
                                aSituation.setLanesOperational(null);
                                Log.d("PullParser", "Restricted lanes: " + temp);
                            }
                        }

                        // get direction
                        else if (xpp.getName().equalsIgnoreCase("tpegdirection")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setDirection(temp);
                                Log.d("PullParser", "Direction: " + temp);
                            }
                        }

                        // set bool for being inside generalpubliccomment
                        else if (xpp.getName().equalsIgnoreCase("generalpubliccomment")) {
                            if (insideSituation) {
                                insideGeneralPublicComment = true;
                            }
                        }

                        // set bool for being inside to
                        else if (xpp.getName().equalsIgnoreCase("to")) {
                            if (insideSituation) {
                                insideTo = true;
                            }
                        }

                        // set bool for being inside from
                        else if (xpp.getName().equalsIgnoreCase("from")) {
                            if (insideSituation) {
                                insideFrom = true;
                            }
                        }

                        // check the 'public comment', 'from', and 'to' bools when 'value' is encountered
                        else if (xpp.getName().equalsIgnoreCase("value")) {
                            String temp = xpp.nextText();
                            if (insideGeneralPublicComment) { // get text from value while inside generalpubliccomment
                                aSituation.setGeneralPublicComment(temp);
                                Log.d("PullParser", "General public comment: " + temp);
                            } else if (insideFrom) { // get all the descriptors for from
                                aSituation.setFromDescriptor(temp);
                                Log.d("PullParser", "From descriptor added: " + temp);
                            } else if (insideTo) { // get all the descriptors for to
                                aSituation.setToDescriptor(temp);
                                Log.d("PullParser", "To descriptor added: " + temp);
                            }
                        }

                        // check 'to' and 'from' bools for latitude
                        else if (xpp.getName().equalsIgnoreCase("latitude")) {
                            String temp = xpp.nextText();
                            if (insideFrom) { // get lat while inside from
                                aSituation.setFromLat(temp);
                                Log.d("PullParser", "From latitude: " + temp);
                            } else if (insideTo) { // get lat while inside to
                                aSituation.setToLat(temp);
                                Log.d("PullParser", "To latitude: " + temp);
                            }
                        }

                        // check 'to' and 'from' bools for longitude
                        else if (xpp.getName().equalsIgnoreCase("longitude")) {
                            String temp = xpp.nextText();
                            if (insideFrom) {
                                aSituation.setFromLon(temp); // get long while inside from
                                Log.d("PullParser", "From longitude: " + temp);
                            } else if (insideTo) {
                                aSituation.setToLon(temp); // get long while inside to
                                Log.d("PullParser", "To longitude: " + temp);
                            }
                        }

                        // get compliance option
                        else if (xpp.getName().equalsIgnoreCase("complianceoption")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setCompliance(temp);
                                Log.d("PullParser", "Compliance option: " + temp);
                            }
                        }

                        // get start date & time
                        else if (xpp.getName().equalsIgnoreCase("overallstarttime")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setStart(temp);
                                Log.d("PullParser", "Start date and time: " + temp);
                            }
                        }

                        //
                        // roadworks specific entries
                        //

                        // get end date & time
                        else if (xpp.getName().equalsIgnoreCase("overallendtime")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setEnd(temp);
                                Log.d("PullParser", "End date and time: " + temp);
                            }
                        }

                        // get delays type
                        else if (xpp.getName().equalsIgnoreCase("delaysType")) {
                            String temp = xpp.nextText();
                            if (insideSituation) {
                                aSituation.setDelaysType(temp);
                                Log.d("PullParser", "Delay type: " + temp);
                            }
                        }

                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (xpp.getName().equalsIgnoreCase("situation")) {
                            insideSituation = false;
                            if (aSituation != null) {
                                out.add(aSituation);
                                Log.d("PullParser", "Saved situation, total: " + out.size());
                                aSituation = null;
                            }
                        } else if (xpp.getName().equalsIgnoreCase("generalpubliccomment")) {
                            insideGeneralPublicComment = false;
                        } else if (xpp.getName().equalsIgnoreCase("from")) {
                            insideFrom = false;
                        } else if (xpp.getName().equalsIgnoreCase("to")) {
                            insideTo = false;
                        }
                    }
                    eventType = xpp.next();
                } // end of while loop

                Log.d("PullParser", "Finished " + myurl + ", running total " + out.size() + " situations");

            } catch (IOException | XmlPullParserException e) {
                Log.e("PullParser", "Parsing error: " + e);
            }
        }
    }
}