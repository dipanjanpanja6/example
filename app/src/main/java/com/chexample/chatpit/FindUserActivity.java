package com.chexample.chatpit;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.chexample.chatpit.User.UserListAdapter;
import com.chexample.chatpit.User.UserObject;
import com.chexample.chatpit.Utils.CountryToPhonePrefix;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FindUserActivity extends AppCompatActivity {

    private RecyclerView mUserList;
    private RecyclerView.Adapter mUserListAdapter;
    private RecyclerView.LayoutManager mUserListLayoutManager;

    private String m_Text = "";
    ArrayList<UserObject> userList, contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_find_user);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        contactList= new ArrayList<>();
        userList= new ArrayList<>();
        Button backja =findViewById(R.id.backop);
        backja.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backout();
            }


        });
        Button mCreate = findViewById(R.id.createy);
        mCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogy();
            }
        });

        initializeRecyclerView();
        getContactList();
    }

    private void backout() {
        //startActivity(new Intent(getApplicationContext(), MainPageActivity.class));
        finish();
    }

    private void showDialogy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Group Name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                createChat(m_Text);
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

    private void createChat(String textin){
        String key = FirebaseDatabase.getInstance().getReference().child("chat").push().getKey();

        DatabaseReference chatInfoDb = FirebaseDatabase.getInstance().getReference().child("chat").child(key).child("info");
        DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("user");

        HashMap newChatMap = new HashMap();
        newChatMap.put("id", textin);
        newChatMap.put("users/" + FirebaseAuth.getInstance().getUid(), true);

        Boolean validChat = false;
        for(UserObject mUser : userList){
            if(mUser.getSelected()){
                validChat = true;
                newChatMap.put("users/" + mUser.getUid(), true);
                userDb.child(mUser.getUid()).child("chat").child(key).setValue(true);
            }
        }

        if(validChat){
            chatInfoDb.updateChildren(newChatMap);
            userDb.child(FirebaseAuth.getInstance().getUid()).child("chat").child(key).setValue(true);
        }
        backout();

    }

    private void getContactList(){

        String ISOPrefix = getCountryISO();

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while(phones.moveToNext()){
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            phone = phone.replace(" ", "");
            phone = phone.replace("-", "");
            phone = phone.replace("(", "");
            phone = phone.replace(")", "");

            if(!String.valueOf(phone.charAt(0)).equals("+"))
                phone = ISOPrefix + phone;

            UserObject mContact = new UserObject("", name, phone);
            contactList.add(mContact);
            getUserDetails(mContact);
        }
        phones.close();
    }

    private void getUserDetails(UserObject mContact) {
        DatabaseReference mUserDB = FirebaseDatabase.getInstance().getReference().child("user");
        Query query = mUserDB.orderByChild("phone").equalTo(mContact.getPhone());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String  phone = "",
                            name = "";
                    for(DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                        if(childSnapshot.child("phone").getValue()!=null)
                            phone = childSnapshot.child("phone").getValue().toString();
                        if(childSnapshot.child("name").getValue()!=null)
                            name = childSnapshot.child("name").getValue().toString();


                        UserObject mUser = new UserObject(childSnapshot.getKey(), name, phone);
                        if (name.equals(phone))
                            for(UserObject mContactIterator : contactList){
                                if(mContactIterator.getPhone().equals(mUser.getPhone())){
                                    mUser.setName(mContactIterator.getName());
                                }
                            }

                        userList.add(mUser);
                        mUserListAdapter.notifyDataSetChanged();
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private String getCountryISO(){
        String iso = null;

        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        if(telephonyManager.getNetworkCountryIso()!=null)
            if (!telephonyManager.getNetworkCountryIso().toString().equals(""))
                iso = telephonyManager.getNetworkCountryIso().toString();

        return CountryToPhonePrefix.getPhone(iso);
    }

    private void initializeRecyclerView() {
        mUserList= findViewById(R.id.userList);
        mUserList.setNestedScrollingEnabled(false);
        mUserList.setHasFixedSize(false);
        mUserListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.VERTICAL, false);
        mUserList.setLayoutManager(mUserListLayoutManager);
        mUserListAdapter = new UserListAdapter(userList);
        mUserList.setAdapter(mUserListAdapter);
    }
}
