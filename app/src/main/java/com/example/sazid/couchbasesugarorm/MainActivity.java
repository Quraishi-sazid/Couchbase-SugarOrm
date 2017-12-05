package com.example.sazid.couchbasesugarorm;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.orm.SugarContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String DATABASE_NAME = "todo";
    private static final String TAG = "tag";
    public static final String SYNC_URL = "http://193.34.145.251:4984/todo";
    protected static Manager manager;
    private Database database;
    LiveQuery liveQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SugarContext.init(this);
        try {
            startCBLite();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button button = (Button) findViewById(R.id.submit_button);
        button.setOnClickListener(this);
        Button checkButton = (Button) findViewById(R.id.check_button);
        checkButton.setOnClickListener(this);

    }
    void countAll(){
        List<AClass> aClassList = AClass.listAll(AClass.class);
        List<BClass> bClassList = BClass.listAll(BClass.class);
        List<CClass> cClassList = CClass.listAll(CClass.class);
        System.out.println(aClassList.size());
        Toast.makeText(this,"a= "+Integer.toString(aClassList.size())+" b= "+Integer.toString(bClassList.size())+" c= "+Integer.toString(cClassList.size()),Toast.LENGTH_LONG).show();
    }

    protected void startCBLite() throws Exception {

        manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);

        DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        database = manager.openDatabase(DATABASE_NAME, options);
        databaseChangedNotification();
        com.couchbase.lite.View nameView = getNameView();
       // startLiveQuery(nameView);

        startSync();
        runQuery();
        //runQuery();


    }

    private void runQuery() {
        Query query = database.getView("nameView").createQuery();
        QueryEnumerator result = null;
        try {
            result = query.run();

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {

            QueryRow row = it.next();
            Document document=row.getDocument();
            Object o=row.getValue();
            Object o2=document.getProperty("aClass");
            System.out.println(document.toString());
        }

    }

    private void startSync() {

        URL syncUrl;
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        final Replication pullReplication = database.createPullReplication(syncUrl);
        pullReplication.setContinuous(true);

        final Replication pushReplication = database.createPushReplication(syncUrl);
        pushReplication.setContinuous(true);

        pushReplication.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(final Replication.ChangeEvent event) {
            }
        });
        pullReplication.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
            }
        });
        pullReplication.start();
        pushReplication.start();


    }


    void databaseChangedNotification() {
        try {
            database = manager.getExistingDatabase(DATABASE_NAME);

            if (database != null) {
                database.addChangeListener(new Database.ChangeListener() {
                    public void changed(Database.ChangeEvent event) {

                        final List<DocumentChange> documentChanges = event.getChanges();
                        final List<String> aClassidList = new ArrayList<>();
                        final List<String> bClassidList = new ArrayList<>();
                        final List<String> cClassidList = new ArrayList<>();
                        for (DocumentChange documentChange : documentChanges) {
                            if (database.getDocument(documentChange.getDocumentId()).getProperty("aClass")!=null) {
                                aClassidList.add(documentChange.getDocumentId());
                            }else if (database.getDocument(documentChange.getDocumentId()).getProperty("bClass")!=null) {
                                bClassidList.add(documentChange.getDocumentId());
                            }else if (database.getDocument(documentChange.getDocumentId()).getProperty("cClass")!=null) {
                                cClassidList.add(documentChange.getDocumentId());
                            }

                        }
                        try {
                            System.out.println("size a"+aClassidList.size());
                            System.out.println("size b"+bClassidList.size());
                            System.out.println("size c"+cClassidList.size());
                            findAClassDocumentById(aClassidList);
                            findBClassDocumentById(bClassidList);
                            findCClassDocumentById(cClassidList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }


        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId()==R.id.submit_button){
            EditText aEditText= (EditText) findViewById(R.id.a_value);
            EditText bEditText= (EditText) findViewById(R.id.b_value);
            EditText cEditText= (EditText) findViewById(R.id.b_value);
            try {
                setValue(aEditText,bEditText,cEditText);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }else if (v.getId()==R.id.check_button){
            countAll();
        }

    }
    @NonNull
    private com.couchbase.lite.View getNameView() {
        com.couchbase.lite.View nameView = database.getView("nameView");
        nameView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object object = document.get("aClass");

                if (object != null) {
                    emitter.emit(object.toString(), document);
                }
            }
        }, "1.0");
        return nameView;
    }

    private void setValue(EditText aEditText, EditText bEditText, EditText cEditText) throws JsonProcessingException, CouchbaseLiteException {
        if (!aEditText.getText().toString().equals("") && !bEditText.getText().toString().equals("") && !cEditText.getText().toString().equals("")){
            List<AClass> aClassList = AClass.find(AClass.class, "a= ?", aEditText.getText().toString());
            List<BClass> bClassList = BClass.find(BClass.class, "b= ?", bEditText.getText().toString());
            List<CClass> cClassList = CClass.find(CClass.class, "c= ?", cEditText.getText().toString());
            if (aClassList.size()==0){
                AClass aClass = getaClass(aEditText);
                pushAclassDocumentIntoCouchbase(aClass);
            }
            if (bClassList.size()==0){
                BClass bClass = getbClass(bEditText);
                pushBclassDocumentIntoCouchbase(bClass);
            }
            if (cClassList.size()==0){
                CClass cClass = getcClass(cEditText);
                pushCclassDocumentIntoCouchbase(cClass);
            }

        }
    }

    private void pushAclassDocumentIntoCouchbase(AClass aClass) throws JsonProcessingException, CouchbaseLiteException {
        Document document=database.createDocument();
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(aClass);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("aClass",jsonInString);
        document.putProperties(properties);
    }
    private void pushBclassDocumentIntoCouchbase(BClass bClass) throws JsonProcessingException, CouchbaseLiteException {
        Document document=database.createDocument();
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(bClass);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("bClass",jsonInString);
        document.putProperties(properties);
    }
    private void pushCclassDocumentIntoCouchbase(CClass cClass) throws JsonProcessingException, CouchbaseLiteException {
        Document document=database.createDocument();
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(cClass);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("cClass",jsonInString);
        document.putProperties(properties);
    }

    @NonNull
    private AClass getaClass(EditText aEditText) {
        AClass aClass=new AClass();
        aClass.setA(Integer.parseInt(aEditText.getText().toString()));
        aClass.save();
        return aClass;
    }
    private BClass getbClass(EditText bEditText) {
        BClass bClass=new BClass();
        bClass.setB(Integer.parseInt(bEditText.getText().toString()));
        bClass.save();
        return bClass;
    }
    private CClass getcClass(EditText cEditText) {
        CClass cClass=new CClass();
        cClass.setC(Integer.parseInt(cEditText.getText().toString()));
        cClass.save();
        return cClass;
    }

    private void findAClassDocumentById(List<String> idList) throws JSONException {
        for (String id : idList) {
            Document document = database.getDocument(id);
            Object object= document.getProperty("aClass");
                JSONObject jsonObject=new JSONObject(object.toString());
                Gson gson = new Gson();
                AClass aClass = gson.fromJson(jsonObject.toString(), AClass.class);
                aClass.save();
        }
    }
    private void findBClassDocumentById(List<String> idList) throws JSONException {
        for (String id : idList) {
            Document document = database.getDocument(id);
            Object object= document.getProperty("bClass");
            JSONObject jsonObject=new JSONObject(object.toString());
            Gson gson = new Gson();
            BClass bClass = gson.fromJson(jsonObject.toString(), BClass.class);
            bClass.save();
        }
    }
    private void findCClassDocumentById(List<String> idList) throws JSONException {
        for (String id : idList) {
            Document document = database.getDocument(id);
            Object object= document.getProperty("cClass");
            JSONObject jsonObject=new JSONObject(object.toString());
            Gson gson = new Gson();
            CClass cClass = gson.fromJson(jsonObject.toString(), CClass.class);
            cClass.save();
        }
    }

}

