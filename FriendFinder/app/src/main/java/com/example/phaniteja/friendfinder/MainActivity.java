package com.example.phaniteja.friendfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText editEmail = null;
    private EditText editPassword = null;
    private Button loginButton = null;
    private Button registerButton = null;
    private Context classContext = null;
    private String[] urlJSON = new String[4];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initilizations of edit texts and buttons in onCreate
        classContext = this;
        editEmail = (EditText)findViewById(R.id.editTextEmail);
        editPassword = (EditText)findViewById(R.id.editTextPassword);
        loginButton = (Button)findViewById(R.id.buttonLogin);
        registerButton = (Button)findViewById(R.id.buttonRegister);
        registerButton.setOnClickListener(this);
        loginButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sessionPref = PreferenceManager.getDefaultSharedPreferences(classContext);
        if(sessionPref.getBoolean("isSignedIN",false)){
            Intent intent = new Intent(classContext,friendSearch.class);
            //Starting a new activty and finishing the older activity
            startActivity(intent);
            MainActivity.this.finish();
        }
    }


    @Override
    public void onClick(View view) {
        String enteredEmail = editEmail.getText().toString().trim();
        String enteredPass = editPassword.getText().toString().trim();
        //Switch to activities depending on the button clicks
        switch(view.getId()){
            case R.id.buttonRegister:
                Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
                startActivity(intent);
                break;
            case R.id.buttonLogin:
                    urlJSON[0] = enteredEmail;
                    urlJSON[1] = enteredPass;
                    urlJSON[2] = "";
                    urlJSON[3] = "0";
                    registerButton.setEnabled(false);
                    loginButton.setEnabled(false);
                    new loginService().execute(urlJSON);
                    break;
                }

        }


    private class loginService extends AsyncTask<String,Integer,String> {

        String[] urlContents = null;
        @Override
        protected String doInBackground(String... args) {
            urlContents = args;
            URL url;
            String res = "";
            String requestURL = "http://ec2-52-14-81-211.us-east-2.compute.amazonaws.com/usersAuthentication.php?";

            try{
                StringBuilder urlString = new StringBuilder();

                urlString.append("emailID="+args[0]+"&"+"pass="+args[1]+"&"+"name="+args[2]+"&"+"firstTime="+args[3]);

                requestURL= requestURL+urlString.toString();
                url = new URL(requestURL);
                HttpURLConnection myconnection = (HttpURLConnection) url.openConnection();
                myconnection.setRequestMethod("GET");

                if(200 == HttpURLConnection.HTTP_OK){
                    InputStream in = url.openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {

                        if(line.contains("Error")){
                            Toast.makeText(classContext,line,Toast.LENGTH_SHORT).show();
                            break;
                        }else {
                            result.append(line);
                        }
                    }
                    res = result.toString();

                }else{
                    Toast.makeText(classContext,"Connection lost!",Toast.LENGTH_SHORT).show();
                }
            }catch (Exception ex){
            }finally {
            }
            return res;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if(response!= null && !response.isEmpty()){
                try {
                    JSONObject jsonresponse = new JSONObject(response);
                    if(jsonresponse.has("status")){
                        switch(jsonresponse.get("status").toString()){
                            case "0":
                                Toast.makeText(classContext,"Registration Success!",Toast.LENGTH_SHORT).show();
                                Intent intent1 = new Intent(getApplicationContext(),friendSearch.class);
                                startActivity(intent1);
                                MainActivity.this.finish();
                                break;
                            case "1":
                                Toast.makeText(classContext,"Email ID is not Registered. Register yourself before SignIn.",Toast.LENGTH_SHORT).show();
                                break;
                            case "2":
                                Toast.makeText(classContext,"Email ID is already registered. Try SignIn instead of Register.",Toast.LENGTH_SHORT).show();
                                break;
                            case "3":
                                Toast.makeText(classContext,"Login Successful",Toast.LENGTH_SHORT).show();
                                String savedName = "";
                                if(jsonresponse.has("name")){
                                    savedName = jsonresponse.get("name").toString();
                                }
                                prevValueToPreferences(savedName);
                                Intent intent2 = new Intent(getApplicationContext(),friendSearch.class);
                                startActivity(intent2);
                                MainActivity.this.finish();
                                break;
                            case "4":
                                Toast.makeText(classContext,"Password Incorrect",Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(classContext,"Server not responding",Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(classContext,"Server not responding",Toast.LENGTH_SHORT).show();
            }
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);
        }
    }

    private void prevValueToPreferences(String userName) {
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(classContext);
        spref.edit().putBoolean("isSignedIN",true).apply();
        spref.edit().putString("emailID", urlJSON[0]).apply();
        spref.edit().putString("name",userName).apply();
    }

}
