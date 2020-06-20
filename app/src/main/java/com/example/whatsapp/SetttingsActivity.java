package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class SetttingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, UserStatus;
    private TextView CurrentUserName;
    private CircleImageView userProfileImage;
    private Toolbar SettingsToolbar;

    private String currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private ProgressDialog loadingBar;

    private static final int Gallerypick = 1;
    public StorageReference UserProfileImagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setttings);

        mAuth =FirebaseAuth.getInstance();
        currentUserId=mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");


        initializefields();

        userName.setVisibility(View.INVISIBLE);
        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSettings();
            }
        });

        RetriveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent GalleryIntent = new Intent();
                GalleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                GalleryIntent.setType("image/*");
                startActivityForResult(GalleryIntent, Gallerypick);
            }
        });
    }




    private void initializefields() {
        updateAccountSettings=(Button)findViewById(R.id.update_settings_button);
        userName=(EditText)findViewById(R.id.set_username);
        UserStatus=(EditText)findViewById(R.id.set_user_ststus);
        CurrentUserName=(TextView)findViewById(R.id.current_user);
        userProfileImage=(CircleImageView)findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);

        SettingsToolbar=(Toolbar)findViewById(R.id.settings_toolbar);
        setSupportActionBar(SettingsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==Gallerypick && resultCode ==RESULT_OK && data!=null){
            Uri ImageUri = data.getData();

            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){

                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("please wait while vicky updates your profile image");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();

                final StorageReference filePath = UserProfileImagesRef.child(currentUserId + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SetttingsActivity.this, "Profile image updated successfully...", Toast.LENGTH_SHORT).show();
                            final String downLoadUrl = task.getResult().getStorage().getDownloadUrl().toString();

                            RootRef.child("Users").child(currentUserId).child("image").setValue(downLoadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(SetttingsActivity.this, "image location successfully saved in database", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                    else {
                                        String message=task.getException().toString();
                                        Toast.makeText(SetttingsActivity.this, "Error: "+ message, Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                }
                            });

                        }
                        else {
                            String message= task.getException().toString();
                            Toast.makeText(SetttingsActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });

            }

        }
    }

    private void updateSettings() {
        String setUserName= userName.getText().toString();
        String setStatus = UserStatus.getText().toString();

        if (TextUtils.isEmpty(setUserName)) {
            Toast.makeText(this, "Please set Username..", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setStatus)) {
            Toast.makeText(this, "Please update your status..", Toast.LENGTH_SHORT).show();
        }
        else {
            HashMap <String, Object> profileMap = new HashMap<>();
            profileMap.put("uid",currentUserId);
            profileMap.put("name",setUserName);
            profileMap.put("status",setStatus);
            RootRef.child("Users").child(currentUserId).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(SetttingsActivity.this, "Profile Update Succesfully", Toast.LENGTH_SHORT).show();
                        SendUsertoMainActivty();
                    }
                    else {
                        String message= task.getException().toString();
                        Toast.makeText(SetttingsActivity.this, "Error: "+ message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    private void RetriveUserInfo() {

        Picasso.get().load(R.drawable.profilevicky).into(userProfileImage);

        RootRef.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.exists()) && dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image"))){
                    String retriveUsername =dataSnapshot.child("name").getValue().toString();
                    String retriveStatus =dataSnapshot.child("status").getValue().toString();
                    String retriveProfileImage =dataSnapshot.child("image").getValue().toString();

                    CurrentUserName.setText(retriveUsername);

                    userName.setText(retriveUsername);
                    UserStatus.setText(retriveStatus);
                }
                else if((dataSnapshot.exists()) && dataSnapshot.hasChild("name")){
                    String retriveUsername =dataSnapshot.child("name").getValue().toString();
                    String retriveStatus =dataSnapshot.child("status").getValue().toString();

                    CurrentUserName.setText(retriveUsername);

                    userName.setText(retriveUsername);
                    UserStatus.setText(retriveStatus);


                }
                else {
                    userName.setVisibility(View.VISIBLE);
                    Toast.makeText(SetttingsActivity.this, "Please set and Update your Profile first..", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUsertoMainActivty() {
        Intent mainIntent = new Intent(SetttingsActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}