package com.example.whatsapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
{
    private List<Messages> userMessagesList;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;

    public MessageAdapter ( List<Messages> userMessagesList){
        this.userMessagesList= userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{
        public TextView senderMessageText, ReceiverMessageText;
        public CircleImageView receiverProfileImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText=(TextView)itemView.findViewById(R.id.sender_message_text);
            ReceiverMessageText=(TextView)itemView.findViewById(R.id.receiever_message_text);
            receiverProfileImage=(CircleImageView) itemView.findViewById(R.id.message_profile_image);

        }
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout,parent,false);
        mAuth=FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {

        String messageSenderId = mAuth.getCurrentUser().getUid();
        Messages  messages =userMessagesList.get(position);

        String fromUserId =messages.getFrom();
        String fromMessageType =messages.getType();

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);

        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("image")){
                    String receiverImage = dataSnapshot.child("image").getValue().toString();

                    Picasso.get().load(receiverImage).placeholder(R.drawable.profilevicky).into(holder.receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (fromMessageType.equals("text")){
            holder.ReceiverMessageText.setVisibility(View.INVISIBLE);
            holder.receiverProfileImage.setVisibility(View.INVISIBLE);
            holder.senderMessageText.setVisibility(View.INVISIBLE);

            if(fromUserId.equals(messageSenderId)){

                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setTextColor(Color.BLACK);
                holder.senderMessageText.setText(messages.getMessage());
            }
            else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.ReceiverMessageText.setVisibility(View.VISIBLE);
                holder.ReceiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.ReceiverMessageText.setTextColor(Color.BLACK);
                holder.ReceiverMessageText.setText(messages.getMessage());
            }
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }


}
