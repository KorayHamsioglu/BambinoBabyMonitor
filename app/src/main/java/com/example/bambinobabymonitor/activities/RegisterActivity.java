package com.example.bambinobabymonitor.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.bambinobabymonitor.R;
import com.example.bambinobabymonitor.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding activityRegisterBinding;
    Dialog dialog;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String userID;
    private static final Pattern PASSWORD_PATTERN=Pattern.compile("^"+"(?=.*[0-9])"+"(?=.*[a-z])"+"(?=.*[A-Z])"+"(?=.*[@^#+%$&=])"+"(?=\\S+$)"+".{8,16}"+"$");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityRegisterBinding=ActivityRegisterBinding.inflate(getLayoutInflater());
        View view=activityRegisterBinding.getRoot();
        setContentView(view);

        final View popupView=getLayoutInflater().inflate(R.layout.popup,null);
        Button popupButton=popupView.findViewById(R.id.popupButton);


        firebaseAuth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();

        activityRegisterBinding.buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=activityRegisterBinding.editTextRegisterEmail.getText().toString();
                String password=activityRegisterBinding.editTextRegisterPassword.getText().toString();
                String rePassword=activityRegisterBinding.editTextRegisterRePassword.getText().toString();
                activityRegisterBinding.textViewError1.setVisibility(View.INVISIBLE);
                activityRegisterBinding.textViewError2.setVisibility(View.INVISIBLE);
                activityRegisterBinding.textViewError3.setVisibility(View.INVISIBLE);

   if(isValidEmail(email)&& isValidPassword(password) && password.equals(rePassword)) {
       if(activityRegisterBinding.registerCheckBox.isChecked()){
           firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
               @Override
               public void onComplete(@NonNull Task<AuthResult> task) {
                   if (task.isSuccessful()) {
                       view.setClickable(false);
                       FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                       firebaseUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                           @Override
                           public void onSuccess(Void unused) {
                               userID = firebaseAuth.getCurrentUser().getUid();
                               DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
                               Map<String, Object> user = new HashMap<>();
                               user.put("email", email);
                               user.put("password", password);
                               documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                   @Override
                                   public void onSuccess(Void unused) {
                                       startPopup(popupView);
                                   }
                               });
                           }
                       }).addOnFailureListener(new OnFailureListener() {
                           @Override
                           public void onFailure(@NonNull Exception e) {
                               activityRegisterBinding.textViewError1.setVisibility(View.VISIBLE);
                               activityRegisterBinding.textViewError1.setText(R.string.mail_does_not_exist);
                               firebaseAuth.getCurrentUser().delete();
                           }
                       });

                   }else{
                       activityRegisterBinding.textViewError1.setVisibility(View.VISIBLE);
                       activityRegisterBinding.textViewError1.setText(R.string.the_mail_is_already_registered);
                   }

               }
           });
       }else{

       }

   }
   else if(isValidEmail(email)&& isValidPassword(password) && !password.equals(rePassword)){
        activityRegisterBinding.textViewError3.setVisibility(View.VISIBLE);
        activityRegisterBinding.textViewError3.setText(R.string.password_not_match);
   }
   else if(!isValidEmail(email)&& isValidPassword(password) && password.equals(rePassword)){
       activityRegisterBinding.textViewError1.setVisibility(View.VISIBLE);
       activityRegisterBinding.textViewError1.setText(R.string.valid_email);
   }
   else if(isValidEmail(email)&& !isValidPassword(password) && password.equals(rePassword)){
       activityRegisterBinding.textViewError2.setVisibility(View.VISIBLE);
       activityRegisterBinding.textViewError2.setText(R.string.valid_password);
   }
   else if(!isValidEmail(email)&& !isValidPassword(password) && password.equals(rePassword)){
       activityRegisterBinding.textViewError1.setVisibility(View.VISIBLE);
       activityRegisterBinding.textViewError1.setText(R.string.valid_email);
       activityRegisterBinding.textViewError2.setVisibility(View.VISIBLE);
       activityRegisterBinding.textViewError2.setText(R.string.valid_password);
   }
            }
        });

        popupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RegisterActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        activityRegisterBinding.buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void startPopup(View popup){
        dialog=new Dialog(RegisterActivity.this);
        dialog.setContentView(popup);
        dialog.create();
        dialog.setCancelable(false);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    public boolean isValidEmail(CharSequence text){
        if(TextUtils.isEmpty(text)){
          return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(text).matches()){
          return false;
        }
        else{
            return true;
        }
    }

    public boolean isValidPassword(CharSequence text){
        if (TextUtils.isEmpty(text)){
            return false;
        }else if(!PASSWORD_PATTERN.matcher(text).matches()){
            return false;
        }else{
            return true;
        }

    }
}