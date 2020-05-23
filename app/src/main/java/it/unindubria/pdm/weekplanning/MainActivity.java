package it.unindubria.pdm.weekplanning;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Helper helper = new Helper();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(helper.changeActivity(this, LogIn.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.logout:
                logout();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void logout() {
        helper.displayWithToast(this, R.string.logout);
        FirebaseAuth.getInstance().signOut();
        startActivity(helper.changeActivity(this, LogIn.class));
    }
}
