package com.sovoro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.sovoro.LoginFlag;
import com.sovoro.R;
import com.sovoro.SoVoRoMain;
import com.sovoro.TimeLeft;
import com.sovoro.databinding.ActivitySovoroSignupBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class SoVoRoSignup extends AppCompatActivity {
    private ActivitySovoroSignupBinding activitySovoroSignupBinding;
    private RequestQueue queue;
    private JSONObject signupInfo;
    private JSONObject mailAuthInfo=new JSONObject();
    private final String SIGNUP_PATH="/signup";
    private String URL;
    private LoginFlag loginFlag=new LoginFlag();
    private Socket socket;
    {
        try {
            socket = IO.socket("http://13.58.48.132:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    private boolean mailSendFlag=false;
    private TimeLeft timeLeft;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sovoro_signup);

        socket.on("nickname check", nicknameCheckListener);
        socket.on("id check", idCheckListener);
        socket.on("mail check", mailCheckListener);
        socket.on("timer start", timerStartListener);
        socket.on("timer stop", timerStopListener);
        socket.connect();

        /**????????? ?????? ??????**/
        /**onclick ???????????? ???????????? ????????? ?????? ??????????????? ????????? ??? ?????? ??????**/
        activitySovoroSignupBinding=ActivitySovoroSignupBinding.inflate(getLayoutInflater());
        setContentView(activitySovoroSignupBinding.getRoot());

//        String URL = AppHelper.getURL(SIGNUP_PATH);
        signupInfo=new JSONObject();

        activitySovoroSignupBinding.sovoroSignupIdConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socket.emit("id check", activitySovoroSignupBinding.sovoroSignupId.getText().toString());
            }
        });
        activitySovoroSignupBinding.sovoroSignupNicknameDuplicationCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                socket.emit("nickname check", activitySovoroSignupBinding.sovoroSignupId.getText().toString());
            }
        });
        activitySovoroSignupBinding.sovoroSignupMailSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mailSendFlag) {
                    socket.emit("mail check", activitySovoroSignupBinding.sovoroSignupMailSendInput.getText().toString());
                    socket.emit("timer start");
                    activitySovoroSignupBinding.sovoroSignupMailSendButton.setText("???????????? ?????? ??????");
                    mailSendFlag=true;
                    timeLeft=new TimeLeft();
                }
            }
        });
        activitySovoroSignupBinding.sovoroSignupMailConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mailAuthInfo.getString("mailAuth").equals(activitySovoroSignupBinding.sovoroMailConfirmInput.getText().toString())) {
                        activitySovoroSignupBinding.sovoroSignupMailCheck.setTextColor(Color.parseColor("#FF000000"));
                        activitySovoroSignupBinding.sovoroSignupMailCheck.setText("?????? ?????????????????????");
                    } else {
                        activitySovoroSignupBinding.sovoroSignupMailCheck.setTextColor(Color.parseColor("#E57373"));
                        activitySovoroSignupBinding.sovoroSignupMailCheck.setText("?????? ????????? ????????????");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        activitySovoroSignupBinding.sovoroSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!loginFlag.isIdCheck()) {
                    activitySovoroSignupBinding.signupCheck.setText("????????? ??????????????? ???????????????");
                    return;
                }
                else if(!loginFlag.isPasswordCheck()) {
                    activitySovoroSignupBinding.signupCheck.setText("????????? ??????????????? ????????? ?????????");
                }
                else if(!loginFlag.isNicknameCheck())
                    activitySovoroSignupBinding.signupCheck.setText("????????? ??????????????? ???????????????");
                else if(!loginFlag.isSmsCheck())
                    activitySovoroSignupBinding.signupCheck.setText("SMS ????????? ???????????????");
                else {
                    try {
                        signupInfo.put("userId", activitySovoroSignupBinding.sovoroSignupId.getText().toString());
                        signupInfo.put("password", activitySovoroSignupBinding.sovorosignuppassword.getText().toString());
                        signupInfo.put("userNickname", activitySovoroSignupBinding.sovorosignupnickname.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getBoolean("success")) {
                                    Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(getApplicationContext(), SoVoRoMain.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("SignUperr", error.toString());
                            Toast.makeText(getApplicationContext(), "???????????? ?????? ??????", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    };
                    RegisterRequest registerRequest = new RegisterRequest(URL, signupInfo, responseListener, errorListener);
                    queue = Volley.newRequestQueue(SoVoRoSignup.this);
                    queue.add(registerRequest);
                }
            }
        });
    }

    public class RegisterRequest extends JsonObjectRequest {

        public RegisterRequest(String URL, JSONObject jsonObject, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(
                    Method.POST,
                    URL,
                    jsonObject,
                    listener,
                    errorListener);
        }
    }
    private Emitter.Listener idCheckListener=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView=findViewById(R.id.idCheckResult);
                    JSONObject jsonObject=(JSONObject)args[0];
                    try {
                        if(jsonObject.getBoolean("returnValue")) {
                            textView.setTextColor(Color.parseColor("#FF000000"));
                            textView.setText("????????? ?????? ??????");
                            loginFlag.setIdCheck(true);
                        }
                        else {
                            textView.setTextColor(Color.parseColor("#E57373"));
                            textView.setText("????????? ??????");
                            loginFlag.setIdCheck(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };
    private Emitter.Listener nicknameCheckListener=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView=findViewById(R.id.nicknameCheckResult);
                    JSONObject jsonObject=(JSONObject)args[0];
                    try {
                        if(jsonObject.getBoolean("returnValue")) {
                            textView.setTextColor(Color.parseColor("#FF000000"));
                            textView.setText("????????? ?????? ??????");
                            loginFlag.setIdCheck(true);
                        }
                        else {
                            textView.setTextColor(Color.parseColor("#E57373"));
                            textView.setText("????????? ??????");
                            loginFlag.setIdCheck(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };
    private Emitter.Listener mailCheckListener=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mailAuthInfo=(JSONObject) args[0];
                }
            });
        }
    };
    private Emitter.Listener timerStartListener=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            AppCompatTextView timeLeftTextView=findViewById(R.id.sovoroSignupTimeLeft);
            timeLeftTextView.setTextColor(Color.parseColor("#FF000000"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeLeft.setTimeFlow((Integer)args[0]);
                    timeLeft.calcCurrentTimeLeft();
                    timeLeft.calcMinute();
                    timeLeft.calcSecond();
                    timeLeftTextView.setText(timeLeft.getTimeLeft());
                }
            });
        }
    };
    private Emitter.Listener timerStopListener=new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            AppCompatTextView timeLeft=findViewById(R.id.sovoroSignupTimeLeft);
            AppCompatButton mailSendButton=findViewById(R.id.sovoroSignupMailSendButton);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeLeft.setTextColor(Color.parseColor("#E57373"));
                    timeLeft.setText("?????? ????????? ?????????????????????");
                    mailSendButton.setText("???????????? ??????");
                    mailSendFlag=false;
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }
}