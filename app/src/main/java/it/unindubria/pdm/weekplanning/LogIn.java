package it.unindubria.pdm.weekplanning;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogIn extends AppCompatActivity implements View.OnClickListener {

    private static final int SIGN_IN_ACTIVITY_CODE = 88;

    // Firebase & Google API for sign in
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // UI elements
    private Button loginButton;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mGoogleSignInClient = GoogleAPIHelper
                .getGoogleSignInClient(getString(R.string.default_web_client_id), LogIn.this);
        mAuth = FirebaseAuth.getInstance();

        // for debug and development only
        //getApplicationContext().deleteDatabase(DBContract.DB_NAME);

        loginButton = findViewById(R.id.login_button);

        if(GoogleAPIHelper.isGooglePlayServicesAvailable(LogIn.this)) {
            if(mAuth.getCurrentUser() != null) {
                startActivity(helper.changeActivity(this, MainActivity.class));
            }
            loginButton.setOnClickListener(this);
        } else {
            helper.displayWithDialog(
                LogIn.this,
                getString(R.string.error_google_play_services_unavailable_title),
                getString(R.string.error_google_play_services_unavailable_message)
            );
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 5000);
        }
    }

    @Override
    public void onClick(View view) {
        if(GoogleAPIHelper.isDeviceOnline(LogIn.this)) {
            if(view.getId() == R.id.login_button) {
                // login
                login();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == SIGN_IN_ACTIVITY_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("ON ACTIVITY RESULT", "firebaseAuthWithGoogle:" + account.getEmail());

                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.e("ON ACTIVITY RESULT", "Google sign in failed", e);
            }
        }
    }

    private void login() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGN_IN_ACTIVITY_CODE);
    }

    public void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth
            .signInWithCredential(credential)
            .addOnCompleteListener(LogIn.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("GOOGLE SIGNIN", "signInWithCredential:success");
                        startActivity(helper.changeActivity(LogIn.this, MainActivity.class));
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("GOOGLE SIGNIN", "signInWithCredential:failure", task.getException());
                        //Snackbar.make(mBinding.mainLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }
}
