package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID, senderUserId, currentState;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button SendMessageRequestButton, DeclineMessageRequestButton;

    private DatabaseReference UsersRef, ChatRequestsRef, ContactsRef, NotificationsRef;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationsRef = FirebaseDatabase.getInstance().getReference().child("Notifications");


        receiverUserID = getIntent().getExtras().getString("visit_user_id").toString();
        senderUserId=mAuth.getCurrentUser().getUid();

        userProfileImage=(CircleImageView)findViewById(R.id.visit_profile_image);
        userProfileName=(TextView)findViewById(R.id.visit_user_name);
        userProfileStatus=(TextView)findViewById(R.id.visit_profile_status);
        SendMessageRequestButton=(Button)findViewById(R.id.send_message_request_button);
        DeclineMessageRequestButton =(Button)findViewById(R.id.decline_message_request_button);

        currentState ="new";

        RetrieveUserInfo();
    }

    private void RetrieveUserInfo() {

        UsersRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Picasso.get().load(R.drawable.profilevicky).into(userProfileImage);

                    if((dataSnapshot.exists()) && (dataSnapshot.hasChild("image"))){
                        String userName =dataSnapshot.child("name").getValue().toString();
                        String userStatus =dataSnapshot.child("status").getValue().toString();
                        //String userProfileImage =dataSnapshot.child("image").getValue().toString();

                        userProfileName.setText(userName);
                        userProfileStatus.setText(userStatus);

                        ManageChatRequests();
                    }
                    else {
                        String userName =dataSnapshot.child("name").getValue().toString();
                        String userStatus =dataSnapshot.child("status").getValue().toString();
                        //String userProfileImage =dataSnapshot.child("image").getValue().toString();

                        userProfileName.setText(userName);
                        userProfileStatus.setText(userStatus);

                        ManageChatRequests();


                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void ManageChatRequests() {

        ChatRequestsRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverUserID)){
                    String request_type = dataSnapshot.child(receiverUserID).child("request_type").getValue().toString();
                    if (request_type.equals("sent")){
                        currentState ="request_sent";
                        SendMessageRequestButton.setText("Cancel Request");
                    }
                    else if(request_type.equals("received")){
                        currentState = "request_received";
                        SendMessageRequestButton.setText("Accept Message Request");

                        DeclineMessageRequestButton.setVisibility(View.VISIBLE);
                        DeclineMessageRequestButton.setEnabled(true);

                        DeclineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CancelChatRequest();
                            }
                        });
                    }
                }

                else {
                    ContactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(receiverUserID)) {

                                currentState ="friends";
                                SendMessageRequestButton.setText("Remove this contact");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (!senderUserId.equals(receiverUserID)) {
            SendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SendMessageRequestButton.setEnabled(false);
                    if (currentState.equals("new")) {
                        SendChatRequest();
                    }
                    if (currentState.equals("request_sent")){
                        CancelChatRequest();
                    }
                    if(currentState.equals("request_received")){
                        AcceptChatRequest();
                    }
                    if (currentState.equals("friends")){
                        RemoveSpecificContact();
                    }

                }
            });

        }
        else {
            SendMessageRequestButton.setVisibility(View.INVISIBLE);
        }

    }

    private void RemoveSpecificContact() {
        ContactsRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    ContactsRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            SendMessageRequestButton.setEnabled(true);
                            currentState="new";
                            Toast.makeText(ProfileActivity.this, "You unfriended successfully", Toast.LENGTH_SHORT).show();
                            SendMessageRequestButton.setText("Send Message Request");

                            DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                            DeclineMessageRequestButton.setEnabled(false);
                        }
                    });
                }
            }
        });
    }

    private void AcceptChatRequest() {

        ContactsRef.child(senderUserId).child(receiverUserID).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    ContactsRef.child(receiverUserID).child(senderUserId).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                ChatRequestsRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            ChatRequestsRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        SendMessageRequestButton.setEnabled(true);
                                                        currentState ="friends";
                                                        Toast.makeText(ProfileActivity.this, "You are friends now", Toast.LENGTH_SHORT).show();
                                                        SendMessageRequestButton.setText("Remove this Contact");

                                                        DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                        DeclineMessageRequestButton.setEnabled(false);
;                                                    }

                                                }
                                            });
                                        }

                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }

    private void CancelChatRequest() {
        ChatRequestsRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        ChatRequestsRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                SendMessageRequestButton.setEnabled(true);
                                currentState="new";
                                SendMessageRequestButton.setText("Send Message Request");

                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                DeclineMessageRequestButton.setEnabled(false);
                            }
                        });
                    }
            }
        });

    }

    private void SendChatRequest() {
        ChatRequestsRef.child(senderUserId).child(receiverUserID).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    ChatRequestsRef.child(receiverUserID).child(senderUserId).child("request_type").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){

                                HashMap<String, String> chatNotificationMap = new HashMap<>();
                                chatNotificationMap.put("from",senderUserId);
                                chatNotificationMap.put("type","request");

                                NotificationsRef.child(receiverUserID).push()
                                        .setValue(chatNotificationMap)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    SendMessageRequestButton.setEnabled(true);
                                                    currentState ="request_sent";
                                                    SendMessageRequestButton.setText("Cancel Request");
                                                }
                                            }
                                        });


                            }
                        }
                    });
                }
            }
        });
    }


}