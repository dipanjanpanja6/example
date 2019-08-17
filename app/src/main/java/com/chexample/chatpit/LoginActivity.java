package com.chexample.chatpit;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText mPhoneNumber, mCode;
    private Button mSend; 
    
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    String mVerificationId;
    CountryCodePicker ccp;

    //private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        //MobileAds.initialize(this,"ca-app-pub-5565078232945269~8884904375");

       // FirebaseApp.initializeApp(this);

        userIsLoggedIn();
       // mAdView = findViewById(R.id.adView);


        //AdRequest adRequest = new AdRequest.Builder().build();
       // AdRequest.Builder builder = new AdRequest.Builder();
       // if (BuildConfig.DEBUG) {
       //     builder.addTestDevice("C93E96336E19BC9A577F07D78C449232");
       // }
       // builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
       // AdRequest adRequest = builder.build();
       // mAdView.loadAd(adRequest);
        mPhoneNumber = findViewById(R.id.phoneNumber);
        mCode = findViewById(R.id.code);
        ccp = findViewById(R.id.ccp);
        mSend = findViewById(R.id.send);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mVerificationId != null)
                    verifyPhoneNumberWithCode();
                else
                    startPhoneNumberVerification();
            }
        });


        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
            }


            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);

                mVerificationId = verificationId;
                mSend.setText(R.string.verifyi);
            }
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Locate MenuItem with ShareActionProvider
        //MenuItem item = menu.findItem(R.id.share_button);


        // Return true to display menu
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){

            case R.id.share_button:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody ="https://subratasarkar32.github.io/chatpit/";
                sharingIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
                startActivity(Intent.createChooser(sharingIntent,"Share using:"));
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void verifyPhoneNumberWithCode(){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mCode.getText().toString());
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){

                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if(user != null){
                        final DatabaseReference mUserDB = FirebaseDatabase.getInstance().getReference().child("user").child(user.getUid());
                        mUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()){
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("phone", user.getPhoneNumber());
                                    userMap.put("name", user.getPhoneNumber());
                                    mUserDB.updateChildren(userMap);
                                }
                                userIsLoggedIn();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                }

            }
        });
    }

    private void userIsLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            startActivity(new Intent(getApplicationContext(), MainPageActivity.class));
            finish();
            return;
        }
    }

    private void startPhoneNumberVerification() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mPhoneNumber.getText().toString(),
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks);
    }
}
