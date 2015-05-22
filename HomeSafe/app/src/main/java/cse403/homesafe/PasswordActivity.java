package cse403.homesafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import cse403.homesafe.Data.Contacts;
import cse403.homesafe.Messaging.Messenger;

/**
 * Represents a timed password field where a user can type their password to gain extended
 * authority in the HomeSafe App.
 */
public class PasswordActivity extends ActionBarActivity {

    private static final String TAG = "PasswordActivity";
    // under-the-hood components
    private CountDownTimer timer;   // how much time the user has left to put in correct password
    private int numchances;         // how many chances the user has to put in correct password

    // on screen components
    private Button cancel;
    private Button confirm;
    private EditText passwordField;         // where user inputs their password
    private TextView timerView;             // how much time is left to correctly input password
    private TextView titleMsg;              // displays the purpose of entering the password

    /**
     * Represents the 3 possible user-interactions with the Password screen;
     * a user can either successfully enter their password, fail to put in their password, or
     * cancel the password screen.
     */
    public enum RetCode {
        SUCCESS,
        FAILURE,
        SPECIAL,
        CANCEL
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        // get the parameters from the previous activity
        ArrayList<String> params = getIntent().getStringArrayListExtra("passwordParams");
        int time = Integer.parseInt(params.get(0));
        String message = params.get(1);
        numchances = Integer.parseInt(params.get(2));
        String confirmButtonMessage = params.get(3);

        // Make on-screen components visible.
        cancel = (Button) findViewById(R.id.btnCancel);
        confirm = (Button) findViewById(R.id.btnConfirm);
        passwordField = (EditText) findViewById(R.id.passwordField);
        timerView = (TextView) findViewById(R.id.timerView);
        titleMsg = (TextView) findViewById(R.id.titleMsg);

        // set the button message and the test message
        confirm.setText(confirmButtonMessage);
        titleMsg.setText(message);

        timer = new CountDownTimer(time * 1000, 1) {
            @Override
            public void onTick(long l) {
                // re-calculate the text display for the timer
                long seconds = l / 1000;
                String hrs = String.format("%02d", seconds / 3600);
                seconds %= 3600;       // strip off seconds that got converted to hours
                String mins = String.format("%02d", seconds / 60);
                seconds %= 60;         // strip off seconds that got converted to minutes
                String secs = String.format("%02d", seconds % 60);
                timerView.setText(hrs + ":" + mins + ":" + secs);
            }

            @Override
            public void onFinish() {
                timer.cancel();
                Intent i = new Intent();
                i.putExtra("retval", RetCode.FAILURE);
                setResult(RESULT_OK, i);
                finish();
            }
        }.start();

        // this will happen...eventually... (make the password field focus when clicked on.
        passwordField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordField.requestFocus();
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {

            // Can only confirm if entered something in the password field
            // check that there is something in the password field, otherwise make a
            // toast saying you haven't entered a password yet.
            @Override
            public void onClick(View view) {
                String password = passwordField.getText().toString();   // the entered password
                Intent i = new Intent();
                if (password.length() == 0) {
                    Toast.makeText(PasswordActivity.this, "You have not entered a password", Toast.LENGTH_SHORT).show();
                } else {
                    if (comparePassword("pin", password)) {
                        Log.d(TAG, "Return code Success");
                        Toast.makeText(PasswordActivity.this, "You entered correct password", Toast.LENGTH_SHORT).show();
                        i.putExtra("retval", RetCode.SUCCESS);
                        setResult(RESULT_OK, i);
                        finish();
                    } else if (comparePassword("pin_mock", password)) {
                        Log.d(TAG, "Return code Special");
                        Toast.makeText(PasswordActivity.this, "You entered your emergency password!", Toast.LENGTH_SHORT).show();
                        i.putExtra("retval", RetCode.SPECIAL);
                        setResult(RESULT_OK, i);
                        finish();
                    } else {
                        Toast.makeText(PasswordActivity.this, "You entered an incorrect password", Toast.LENGTH_SHORT).show();
                        numchances--;
                        if (numchances <= 0) {
                            Toast.makeText(PasswordActivity.this, "No more chances!", Toast.LENGTH_SHORT).show();
                            confirm.setEnabled(false);
                            Log.d(TAG, "Return code Special");
                            i.putExtra("retval", RetCode.FAILURE);
                            setResult(RESULT_OK, i);
                            finish();
                        }
                    }
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.putExtra("retval", RetCode.CANCEL);
                setResult(RESULT_OK, i);
                finish();
            }
        });

    }

    /**
     * Private helper function to compare a password to the user's stored one
     * @param enteredPassword the password to compare against the stored one; cannot be null.
     * @return true if the two passwords match, false otherwise.
     */
    private boolean comparePassword(String key, String enteredPassword) {
        // get the user's stored data
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String pin = prefs.getString(key, null);
        return enteredPassword.equals(pin);
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent();
        i.putExtra("retval", RetCode.CANCEL);
        setResult(RESULT_OK, i);
        finish();
    }
}