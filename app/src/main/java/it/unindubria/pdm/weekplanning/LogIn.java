package it.unindubria.pdm.weekplanning;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LogIn extends AppCompatActivity implements View.OnClickListener {

    // Firebase
    private FirebaseAuth mAuth;

    // UI elements
    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private TextView linkSignUpPage;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    public void onStart() {
        super.onStart();

        mAuth = FirebaseAuth.getInstance();

        //getApplicationContext().deleteDatabase(DBContract.DB_NAME);

        if(mAuth.getCurrentUser() != null) {
            startActivity(helper.changeActivity(this, MainActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        emailField = findViewById(R.id.email_field);
        passwordField = findViewById(R.id.password_field);
        loginButton = findViewById(R.id.login_button);
        linkSignUpPage = findViewById(R.id.signup_link);

        loginButton.setOnClickListener(this);
        linkSignUpPage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.login_button) {
            // login
            login();
        } else if(view.getId() == R.id.signup_link) {
            // go to create a new account page
            startActivity(helper.changeActivity(this, SignUp.class));
        }
    }

    private void login() {
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        if(email.isEmpty() || password.isEmpty()) {
            helper.displayWithDialog(
                this,
                R.string.error_title,
                R.string.login_error_fields
            );
        } else {
            mAuth
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        helper.displayWithToast(
                            getApplicationContext(),
                            R.string.login_success
                        );
                        startActivity(helper.changeActivity(LogIn.this, MainActivity.class));
                    } else {
                        // If sign in fails, display a message to the user.
                        helper.displayWithDialog(
                            LogIn.this,
                            R.string.error_title,
                            R.string.login_error_auth
                        );
                    }
                }
            });
        }
    }
}
