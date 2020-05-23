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

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    // Firebase
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // UI elements
    private EditText usernameField;
    private EditText emailField;
    private EditText passwordField;
    private Button signupButton;
    private TextView linkLoginPage;

    // Helpers & Others
    private Helper helper = new Helper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        usernameField = findViewById(R.id.signup_username_field);
        emailField = findViewById(R.id.signup_email_field);
        passwordField = findViewById(R.id.signup_password_field);
        signupButton = findViewById(R.id.signup_button);
        linkLoginPage = findViewById(R.id.login_link);

        signupButton.setOnClickListener(this);
        linkLoginPage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.signup_button) {
            createNewAccount();
        } else if(view.getId() == R.id.login_link) {
            // go to login into your account page
            startActivity(helper.changeActivity(this, LogIn.class));
        }
    }

    private void createNewAccount() {
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        if(email.isEmpty() || password.isEmpty()) {
            helper.displayWithDialog(
                getApplicationContext(),
                R.string.error_title,
                R.string.login_error_fields
            );
        } else {
            mAuth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        startActivity(helper.changeActivity(SignUp.this, MainActivity.class));
                    } else {
                        // If sign in fails, display a message to the user.
                        helper.displayWithDialog(
                            SignUp.this,
                            R.string.error_title,
                            R.string.signup_error_auth
                        );
                    }
                }
            });
        }
    }
}
