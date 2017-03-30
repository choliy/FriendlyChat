package com.choliy.igor.friendlychat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.udacity.friendlychat.R;

import java.util.List;

class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder> {

    private Context mContext;
    private List<MessageModel> mMessages;

    MessageAdapter(Context context, List<MessageModel> messages) {
        mContext = context;
        mMessages = messages;
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        return new MessageHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageHolder holder, int position) {
        holder.bindView(position);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    void addMessage(MessageModel message) {
        mMessages.add(message);
        notifyDataSetChanged();
    }

    void clearAdapter() {
        mMessages.clear();
        notifyDataSetChanged();
    }

    class MessageHolder extends RecyclerView.ViewHolder {

        private ImageView mMessageImage;
        private TextView mMessageText;
        private TextView mUserText;

        MessageHolder(View itemView) {
            super(itemView);
            mMessageImage = (ImageView) itemView.findViewById(R.id.messageImageView);
            mMessageText = (TextView) itemView.findViewById(R.id.messageTextView);
            mUserText = (TextView) itemView.findViewById(R.id.userTextView);
        }

        private void bindView(int position) {
            boolean isPhotoPresent = mMessages.get(position).getPhotoUrl() != null;
            if (isPhotoPresent) {
                mMessageImage.setVisibility(View.VISIBLE);
                mMessageText.setVisibility(View.GONE);
                Glide.with(mContext)
                        .load(mMessages.get(position).getPhotoUrl())
                        .into(mMessageImage);
            } else {
                mMessageImage.setVisibility(View.GONE);
                mMessageText.setVisibility(View.VISIBLE);
                mMessageText.setText(mMessages.get(position).getMessage());
            }
            mUserText.setText(mMessages.get(position).getUser());
        }
    }
}