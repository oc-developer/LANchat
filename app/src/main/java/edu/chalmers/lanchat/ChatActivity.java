package edu.chalmers.lanchat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Random;

import edu.chalmers.lanchat.connect.MessageService;
import edu.chalmers.lanchat.db.MessageContentProvider;
import edu.chalmers.lanchat.db.MessageTable;
import edu.chalmers.lanchat.util.Faker;


public class ChatActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_GROUP_OWNER = "EXTRA_GROUP_OWNER";
    public static final String EXTRA_DEBUG = "EXTRA_DEBUG";
    private static final String TAG = "ChatActivity";

    private ChatAdapter adapter;
    private ListView chatList;
    private ImageButton sendButton;
    private EditText inputText;
    private TextView groupOwnerText;
    private Gson gson;
    private boolean debug;
    private String user = "";
    private int textColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        debug = getIntent().getBooleanExtra(EXTRA_DEBUG, false);

        gson = new Gson();

        getUsername();

        textColor = randColor();

        chatList = (ListView) findViewById(R.id.chatList);

        chatList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage chatMessage = (ChatMessage) view.getTag();
                chatMessage.incPopularity(1);
                ContentValues values = new ContentValues();
                values.put(MessageTable.COLUMN_LIKES, chatMessage.getPopularity());
                getContentResolver().update(ContentUris.withAppendedId(MessageContentProvider.CONTENT_URI, id), values, null, null);
            }
        });

        chatList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateRowSize();
            }
        });

        // Subscribe to the message database table
        getLoaderManager().initLoader(0, null, this);

        // Make the list reflect the database
        adapter = new ChatAdapter(this);
        chatList.setAdapter(adapter);

        inputText = (EditText) findViewById(R.id.inputText);
        sendButton = (ImageButton) findViewById(R.id.sendButton);

        sendButton = (ImageButton) findViewById(R.id.sendButton);
        sendButton.setOnClickListener( (debug) ? new SendListenerDebug() : new SendListener() );
    }

    public void getUsername() {
        Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        c.moveToFirst();
        user = c.getString(c.getColumnIndex("display_name"));
        c.close();
    }

    private void updateRowSize() {

        int lastPos = chatList.getFirstVisiblePosition();
        int firstPos = chatList.getLastVisiblePosition();
        int diff = firstPos - lastPos;

        Log.d("FirstPos", firstPos + "");
        Log.d("ListLength", lastPos + "");
        Log.d("Diff", (diff) + "");

        for(int i = 0; i <= diff; i++) {
            View v = chatList.getChildAt(i);
            if (v != null) {
                Log.d("Hit", i + "");
                ChatMessage chat = (ChatMessage) v.getTag();
                Log.d("Message", chat.getMessage());

                float textSize = chat.getTextSize();


                TextView text = (TextView) v.findViewById(R.id.textViewNameAndMessage);

                //Get row and change size on that row
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) logUpdateList(i, textSize, diff));
                text.setPadding(20,(int) logUpdateList(i, 10, diff), 0, (int) logUpdateList(i, 10, diff));


            }
        }
    }

    private double logUpdateList(int pos, float textSize, int diff){
        /*
        Changes size on the message dependent on its popularity and position.
         */

        int x = pos-diff;
        double intensityOfCurve = 0.1;
        int minimumTextSize = 5;


        double eq = textSize*(2/(1+Math.exp((-x)*intensityOfCurve)));
        //double eq = textSize*(1/(1+Math.exp(-x)));

        Log.d("calc", eq + "");
        return eq;
        /*

        if(eq < minimumTextSize){

            return minimumTextSize;
        }

        else{

            return eq;
        }*/
    }

    /**
     * Override the back button pressed in order to send back an empty result to the activity
     * which started this one.
     */
    @Override
    public void onBackPressed() {
        // Make sure the activity gets notified when finishing
        setResult(RESULT_OK, new Intent());
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a loader which monitors the message table in the database.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
            MessageTable.COLUMN_ID,
            MessageTable.COLUMN_NAME,
            MessageTable.COLUMN_MESSAGE,
            MessageTable.COLUMN_COLOR,
            MessageTable.COLUMN_LIKES
        };
        CursorLoader cursorLoader = new CursorLoader(this, MessageContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data); // Update the list
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }

    private class SendListenerDebug implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String input = inputText.getText().toString().trim();


            if (input.length() == 0) {
                // Generate fake input.
                Faker faker = new Faker();
                input = faker.sentence(3, 8);
            }

            ChatMessage message = new ChatMessage(user, input, textColor);
            // Put the message in the database
            ContentValues values = new ContentValues();
            values.put(MessageTable.COLUMN_NAME, message.getName());
            values.put(MessageTable.COLUMN_MESSAGE, message.getMessage());
            values.put(MessageTable.COLUMN_COLOR, message.getColor());
            values.put(MessageTable.COLUMN_LIKES, message.getPopularity());
            getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);

            // Clear the input field
            inputText.setText("");
        }
    }

    private class SendListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String input = inputText.getText().toString().trim();
            if (input.length() > 0) {

                // For now, send the ip as username
                ChatMessage message = new ChatMessage(user, input, textColor);

                // Send the message to the server
                Intent serviceIntent = new Intent(ChatActivity.this, MessageService.class);
                serviceIntent.setAction(MessageService.ACTION_SEND);
                serviceIntent.putExtra(MessageService.EXTRAS_MESSAGE, message.toJson());
                startService(serviceIntent);
            }
            // Clear the input field
            inputText.setText("");
        }
    }

    private int randColor() {
        Random r = new Random();
        ArrayList list = new ArrayList();
        list.add(getResources().getColor(R.color.colorIndigo));
        list.add(getResources().getColor(R.color.colorPrimary));
        list.add(getResources().getColor(R.color.colorPurple));
        list.add(getResources().getColor(R.color.colorRed));
        list.add(getResources().getColor(R.color.colorTeal));
        int i = r.nextInt(list.size()-1);
        return (int) list.get(i);
    }
}
