package com.example.nostalgianest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private Context context;
    private List<String> memberList; // List of member UIDs
    private String galleryUid;
    private String ownerUid;
    private String currentUserUid;

    public MemberAdapter(Context context, List<String> memberList, String galleryUid, String ownerUid, String currentUserUid) {
        this.context = context;
        this.memberList = memberList;
        this.galleryUid = galleryUid;
        this.ownerUid = ownerUid;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for member
        View view = LayoutInflater.from(context).inflate(R.layout.member_item, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String memberUid = memberList.get(position);

        // Fetch the member's name from Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(memberUid); // Correct path
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Retrieve the name from the correct field under the member's UID
                String memberName = snapshot.child("name").getValue(String.class); // Correct field name
                if (memberName != null) {
                    holder.memberNameTextView.setText(memberName); // Set member name
                } else {
                    holder.memberNameTextView.setText("Unknown Member");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.memberNameTextView.setText("Failed to load name");
                // Optionally log error
            }
        });

        // Check if the current user is the owner and show/remove the remove button accordingly
        if (memberUid.equals(ownerUid)) {
            holder.removeButton.setVisibility(View.GONE); // Hide the remove button for the owner
        } else if(currentUserUid.equals(ownerUid)) {
            // Show remove button if the current user is the owner
            holder.removeButton.setVisibility(View.VISIBLE);

            holder.removeButton.setOnClickListener(v -> {
            // Remove member when button is clicked
            removeMember(galleryUid, memberUid, position);
        });

        }else {
            // Hide remove button for non-owners
            holder.removeButton.setVisibility(View.GONE);
        }
    }



    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberNameTextView;
        Button removeButton;

        public MemberViewHolder(View itemView) {
            super(itemView);
            memberNameTextView = itemView.findViewById(R.id.memberNameTextView);
            removeButton = itemView.findViewById(R.id.removeMemberButton);
        }
    }

    private void removeMember(String galleryUid, String memberUid, int position) {
        DatabaseReference galleryRef = FirebaseDatabase.getInstance().getReference("albums").child(galleryUid);
        galleryRef.child("members").child(memberUid).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Member removed successfully.", Toast.LENGTH_SHORT).show();
                        // Update the list by removing the member
                        memberList.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        Toast.makeText(context, "Failed to remove member.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
