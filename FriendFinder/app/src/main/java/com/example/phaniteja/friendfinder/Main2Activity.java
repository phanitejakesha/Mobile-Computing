package com.example.phaniteja.friendfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class Main2Activity extends AppCompatActivity implements View.OnClickListener{

    private EditText editEmail = null;
    private EditText editPassword = null;
    private EditText editName = null;
    private Button registerButton = null;
    private Context classContext = null;
    private String[] urlJSON = new String[4];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        classContext = this;
        //Initilizations
        editEmail = (EditText)findViewById(R.id.editTextRegEmail);
        editPassword = (EditText)findViewById(R.id.editTextRegPassword);
        editName = (EditText)findViewById(R.id.editTextRegName);
        registerButton = (Button)findViewById(R.id.btnRegister);
        registerButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        String enteredEmail = editEmail.getText().toString().trim();
        String enteredPass = editPassword.getText().toString().trim();
        String enteredName = editName.getText().toString().trim();

        switch(v.getId()){
            case R.id.btnRegister:
                    urlJSON[0] = enteredEmail;
                    urlJSON[1] = enteredPass;
                    urlJSON[2] = enteredName;
                    urlJSON[3] = "1";
                    registerButton.setEnabled(false);
                    new registrationService().execute(urlJSON);
                    break;
                }

        }


    private class registrationService extends AsyncTask<String,Integer,String> {

        String[] urlContents = null;
        @Override
        protected String doInBackground(String... params) {
            urlContents = params;
            URL url;
            String res = "";
            String requestURL = "http://ec2-52-14-81-211.us-east-2.compute.amazonaws.com/usersAuthentication.php?";
            try{
                StringBuilder sb = new StringBuilder();

                sb.append("emailID="+params[0]+"&"+"pass="+params[1]+"&"+"name="+params[2]+"&"+"firstTime="+params[3]);
                Log.d("url",sb.toString());
                requestURL= requestURL+sb.toString();
                url = new URL(requestURL);
                Log.d("url",requestURL.toString());
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
                    Toast.makeText(classContext,"Connection Lost",Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(classContext,"Registered Successfully!",Toast.LENGTH_SHORT).show();
                                prevValueToPreferences();
                                Intent intent1 = new Intent(getApplicationContext(),friendSearch.class);
                                startActivity(intent1);
                                Main2Activity.this.finish();
                                break;
                            case "1":
                                Toast.makeText(classContext,"Email Not registered",Toast.LENGTH_SHORT).show();
                                break;
                            case "2":
                                Toast.makeText(classContext,"Try to signin,than registeration",Toast.LENGTH_SHORT).show();
                                break;
                            case "3":
                                Toast.makeText(classContext,"Login Successfull",Toast.LENGTH_SHORT).show();
                                Intent intent2 = new Intent(getApplicationContext(),friendSearch.class);
                                startActivity(intent2);
                                Main2Activity.this.finish();
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
            registerButton.setEnabled(true);
        }
    }

    private void prevValueToPreferences() {
        SharedPreferences sessionPref = PreferenceManager.getDefaultSharedPreferences(classContext);
        sessionPref.edit().putBoolean("isSignedIN",true).apply();
        sessionPref.edit().putString("emailID", urlJSON[0]).apply();
        sessionPref.edit().putString("name", urlJSON[2]).apply();

    }
}
