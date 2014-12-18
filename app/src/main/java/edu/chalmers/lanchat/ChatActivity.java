package edu.chalmers.lanchat;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import edu.chalmers.lanchat.connect.MessageService;
import edu.chalmers.lanchat.db.ClientContentProvider;
import edu.chalmers.lanchat.db.MessageContentProvider;
import edu.chalmers.lanchat.db.MessageTable;


public class ChatActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_GROUP_OWNER = "EXTRA_GROUP_OWNER";

    private SimpleCursorAdapter adapter;
    private ListView chatList;
    private Button sendButton;
    private EditText inputText;
    private TextView groupOwnerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatList = (ListView) findViewById(R.id.chatList);

        String[] from = new String[] { MessageTable.COLUMN_MESSAGE };
        int[] to = new int[] { android.R.id.text1 };
        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, from, to, 0);

        chatList.setAdapter(adapter);

        groupOwnerText = (TextView) findViewById(R.id.groupOwnerText);

        if (getIntent().getBooleanExtra(EXTRA_GROUP_OWNER, false)) {
            groupOwnerText.setText("Owner");
        } else {
            groupOwnerText.setText("Client");
        }


        inputText = (EditText) findViewById(R.id.inputText);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = inputText.getText().toString().trim();
                if (message.length() > 0) {
                    Intent serviceIntent = new Intent(ChatActivity.this, MessageService.class);
                    serviceIntent.setAction(MessageService.ACTION_SEND);
                    serviceIntent.putExtra(MessageService.EXTRAS_MESSAGE, message);
                    startService(serviceIntent);
                }
                inputText.setText("");
            }
        });
    }

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { MessageTable.COLUMN_ID, MessageTable.COLUMN_MESSAGE };
        CursorLoader cursorLoader = new CursorLoader(this, MessageContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }
}
