package it.unindubria.pdm.weekplanning;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class GoogleAPIHelper {

    // Return a new instance of a Google SignIn Client
    public static GoogleSignInClient getGoogleSignInClient(String defaultWebClientId, Context context) {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()
                .build();

        return GoogleSignIn.getClient(context, gso);
    }

    // Checks whether the device currently has a network connection.
    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if(!(networkInfo != null && networkInfo.isConnected())) {
            new Helper().displayWithDialog(
                context,
                context.getString(R.string.error_no_network_title),
                context.getString(R.string.error_no_network_message)
            );
            return false;
        } else {
            return true;
        }
    }

    // Check that Google Play services APK is installed and up to date
    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
        int resultCode = gApi.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (gApi.isUserResolvableError(resultCode)) {
                gApi.getErrorDialog(activity, resultCode, 1200).show();
            } else {
                Toast.makeText(activity, "toast_playservices_unrecoverable", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }
}
