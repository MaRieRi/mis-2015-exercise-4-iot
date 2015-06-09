//Quellenangabe: android developer
//http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
//http://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278



package mmbuw.com.exercise4;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;


public class exercise4 extends Activity {
    private TextView textView;
    private TextView techView;
    private TextView tagIdView;
    private TextView mimeView;
    private TextView hexView;
    private TextView asciiView;

    private NfcAdapter myNfcAdapter;
    public static final String MIMI="text/plain";
    public static final String TAG = "NfcBlubb";
    public String asciiTag;
    public String hexTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise4);

        textView = (TextView) findViewById(R.id.textView);
        techView = (TextView) findViewById(R.id.techView);
        tagIdView = (TextView) findViewById(R.id.tagIDView);
        mimeView = (TextView) findViewById(R.id.mimeView);
        hexView = (TextView) findViewById(R.id.hexView);
        asciiView= (TextView)findViewById(R.id.asciiView);
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (myNfcAdapter == null) {
            Toast.makeText(this, "NFC kaputt oder nicht vorhanden:(.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!myNfcAdapter.isEnabled()) {
            textView.setText("NFC ist aus...");
        }
        handleIntent(getIntent());

    }
    @Override
    protected void onResume(){
        super.onResume();
        setupForegroundDispatch(this, myNfcAdapter);
    }
    @Override
    protected void onPause(){
        stopForegroundDispatch(this, myNfcAdapter);
        super.onPause();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            String type = intent.getType();
            mimeView.setText("MIME/Type: "+type);

            if (MIMI.equals(type)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NfcReader().execute(tag);
                tagIdView.setText("TagID: "+byteArrayToHexString(tag.getId()));
                String[] techList = tag.getTechList();
                String s =" Technology: ";
                for (String tech : techList) {
                    s+=tech+"  ";
                }
                techView.setText(s);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tagIdView.setText("TagID: "+byteArrayToHexString(tag.getId()));
            String[] techList = tag.getTechList();
            String s =" Technology: ";
            for (String tech : techList) {
                s+=tech+"  ";
            }
            techView.setText(s);

            String searchedTech = Ndef.class.getName();
            techView.setText(byteArrayToHexString(tag.getId()));
            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NfcReader().execute(tag);
                    break;
                }
            }
        }
    }

    public byte[] readTag(Tag tag){
        MifareUltralight ultra = MifareUltralight.get(tag);
        byte[] payload = new byte[1000];
        try{
            ultra.connect();
            payload = ultra.readPages(4);

        }catch (IOException e){
            Log.e(TAG,"Tag konnte nicht gelesen werde.",e);
        }finally{
            if(ultra!=null){
                try{
                    ultra.close();
                }
                catch(IOException e){
                    Log.e(TAG,"Fehler beim schließen des Tags.",e);
                }
            }
        }
        return payload;
    }

    private String byteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    public void textToHex(View view){
        if(asciiTag!=""){
            hexView.setText("RawData in ASCII: "+asciiTag);
            asciiView.setText("RawData in HeX:"+hexTag);
        }else
            hexView.setText("No Tag read.");
    }

    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIMI);
        } catch (IntentFilter.MalformedMimeTypeException e) {
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    public class NfcReader extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                return null;
            }
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                byte[] payload = ndefRecord.getPayload();

                hexTag=byteArrayToHexString(payload);

                try {
                    asciiTag= new String(payload, "US-ASCII");
                    return readText(ndefRecord);

                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Unsupported Encoding", e);
                }
            }
            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
            byte[] payload = record.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

            @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                textView.setText("Read content: " + result);
            }
        }
    }
}