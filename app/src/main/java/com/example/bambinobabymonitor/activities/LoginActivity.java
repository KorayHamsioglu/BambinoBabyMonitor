package com.example.bambinobabymonitor.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.bambinobabymonitor.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding activityLoginBinding;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityLoginBinding=ActivityLoginBinding.inflate(getLayoutInflater());
        View view=activityLoginBinding.getRoot();
        setContentView(view);
        firebaseAuth=FirebaseAuth.getInstance();
        firebaseUser=firebaseAuth.getCurrentUser();

        activityLoginBinding.editTextPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b){
                    hideKeyboard(view);
                }
            }
        });

        activityLoginBinding.editTextEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b){
                    hideKeyboard(view);
                }
            }
        });
        if(firebaseUser!=null){
            Intent intent=new Intent(LoginActivity.this,OnlineActivity.class);
            startActivity(intent);
            finish();
        }
        activityLoginBinding.buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
        activityLoginBinding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=activityLoginBinding.editTextEmail.getText().toString();
                String password=activityLoginBinding.editTextPassword.getText().toString();
                activityLoginBinding.textViewErrorLogin.setVisibility(View.INVISIBLE);
                firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser firebaseUser= firebaseAuth.getCurrentUser();
                            if(firebaseUser.isEmailVerified()){
                                startActivity(new Intent(LoginActivity.this,OnlineActivity.class));
                                finish();
                            }
                            else{
                                activityLoginBinding.textViewErrorLogin.setVisibility(View.VISIBLE);
                                activityLoginBinding.textViewErrorLogin.setText("Lütfen hesabınızı onayladıktan sonra giriş yapınız.");
                            }

                        }else{
                            activityLoginBinding.textViewErrorLogin.setVisibility(View.VISIBLE);
                            activityLoginBinding.textViewErrorLogin.setText("E-mail veya şifre hatalı.");
                        }
                    }
                });
            }
        });
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}