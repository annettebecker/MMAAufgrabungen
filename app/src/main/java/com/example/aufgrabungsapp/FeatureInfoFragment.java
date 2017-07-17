package com.example.aufgrabungsapp;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.esri.core.map.Feature;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Fragement to display FeatureInfo
 */
public class FeatureInfoFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Read Data
        Bundle arguments = getArguments();
        HashSet attr = null;

        if(arguments != null) {
            attr = (HashSet) arguments.getSerializable("features");
        }

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_feature_info, container, false);

        // put FeatureInfos
        putElement(v, attr);

        // Show Fragment
        return v;
    }

    /**
     * Define Fragmentlayout
     * @param v
     * @param features
     */
    public void putElement(View v, HashSet features){
        Iterator i = features.iterator();
        //find tableLayout
        TableLayout tl = (TableLayout) v.findViewById(R.id.tableLayout);

        // Put Title
        TextView title = new TextView(getActivity());
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, Typeface.BOLD);
        title.setText(R.string.sachdaten);
        tl.addView(title);

        int counter = 0;
        while(i.hasNext()) {
            Feature feature = (Feature)i.next();
            HashMap<String, Object> attribute = (HashMap) feature.getAttributes();
            Set<String> keys = attribute.keySet();
            //Write TableHead
            if(counter == 0){
                createTableHead(tl, keys);
                counter = 1;
            }
            //Add Feature Info
            addFeatureInfo(attribute, tl, keys);
        }

    }

    /**
     * Add Table head to FeatureInfoTable
     * according https://stackoverflow.com/questions/7279501/programmatically-adding-tablerow-to-tablelayout-not-working
     * @param tl
     * @param keys
     */
    private void createTableHead(TableLayout tl, Set<String> keys) {
        //Create Row
        TableRow tr = new TableRow(getActivity());
        tr.setBackgroundResource(R.drawable.table_row_bg);
        tr.setPadding(2,2,2,2);
        //Define Head
        for (String key : keys) {
            TextView td = new TextView(getActivity());
            td.setTextSize(18);
            td.setTypeface(null, Typeface.BOLD);
            td.setText(" " +key+ " ");
            // Set Border
            // according: https://android-examples.blogspot.de/2015/05/how-to-use-tablelayout-in-android.html
            td.setBackgroundResource(R.drawable.table_cell_bg);
            td.setPadding(2,2,2,2);
            tr.addView(td);
        }
        //add element to fragment
        tl.addView(tr);
    }


    /**
     * Add new Row to Table
     * @param attribute
     * @param tl
     * @param keys
     */
    private void addFeatureInfo(HashMap attribute, TableLayout tl, Set<String> keys) {
        TableRow tr = new TableRow(getActivity());
        tr.setBackgroundResource(R.drawable.table_row_bg);
        tr.setPadding(2,2,2,2);
        for(String key:keys) {
            TextView td = new TextView(getActivity());

            String value = attribute.get(key).toString();
            //Change Dateformat
            if(key.equals("beginn")){

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long)attribute.get(key));
                value = simpleDateFormat.format(calendar.getTime());
            }

            //Important, Split long valueStrings > otherwise Error with FrameLayout> cant show FeatureInfo
            if(value.length() > 80){
                String[] array = splitStringEvery(value, 80);
                // /add new line
                value = "";
                for (int i = 0; i< array.length; i++){
                    value += array[i];
                    if(i <  array.length -1)
                        value += "-\n ";
                }
            }
            td.setText(" " + value + " ");
            td.setBackgroundResource(R.drawable.table_cell_bg);
            td.setTextSize(16);
            td.setPadding(2,2,2,2);
            tr.addView(td);
        }
        tl.addView(tr);
    }

    /**
    * Split long strings
    * according: https://stackoverflow.com/questions/12295711/split-a-string-at-every-nth-position
    */
    public String[] splitStringEvery(String s, int interval) {
        int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
        String[] result = new String[arrayLength];

        int j = 0;
        int lastIndex = result.length - 1;
        for (int i = 0; i < lastIndex; i++) {
            result[i] = s.substring(j, j + interval);
            j += interval;
        } //Add the last bit
        result[lastIndex] = s.substring(j);

        return result;
    }
}
