package com.choliy.igor.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.udacity.friendlychat.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String mUserName = ChatConstants.USER_ANONYMOUS;
    private MessageAdapter mMessageAdapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private ImageView mImageEmptyChat;
    private ImageButton mImagePickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private FirebaseAuth mFireBaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mMessageDatabaseReference;
    private ChildEventListener mChildEventListener;
    private StorageReference mPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFireBaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null)
            mFireBaseAuth.removeAuthStateListener(mAuthStateListener);
        removeDatabaseReadListener();
        mMessageAdapter.clearAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ChatConstants.RC_SIGN_IN) {
            if (resultCode == RESULT_CANCELED) finish();
        } else if (requestCode == ChatConstants.RC_PHOTO_PICKER && resultCode == RESULT_OK) {

            // Get a reference to store file at chat_photos/<FILENAME>
            Uri selectedImageUri = data.getData();
            StorageReference photoPreference =
                    mPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to FireBase Storage
            UploadTask uploadTask = photoPreference.putFile(selectedImageUri);
            uploadTask.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    if (downloadUri != null) {
                        MessageModel message = new MessageModel(null, mUserName, downloadUri.toString());
                        mMessageDatabaseReference.push().setValue(message);
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOutMenu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupUi() {

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        mFireBaseAuth = FirebaseAuth.getInstance();
        mMessageDatabaseReference = firebaseDatabase.getReference().child(ChatConstants.MESSAGES);
        mPhotosStorageReference = firebaseStorage.getReference().child(ChatConstants.PHOTOS);

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mImageEmptyChat = (ImageView) findViewById(R.id.imageEmptyChat);
        mImagePickerButton = (ImageButton) findViewById(R.id.imagePickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message RecyclerView and adapter
        LinearLayoutManager reverseLayout = new LinearLayoutManager(this);
        reverseLayout.setStackFromEnd(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(reverseLayout);
        mRecyclerView.setHasFixedSize(true);
        mMessageAdapter = new MessageAdapter(this, new ArrayList<MessageModel>());
        mRecyclerView.setAdapter(mMessageAdapter);

        // Set message length limit filter to EditText
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter
                .LengthFilter(ChatConstants.DEFAULT_MSG_LENGTH_LIMIT)});

        // Listener for controlling user signIn & signOut
        addAuthStateListener();

        // ImagePickerButton shows an image picker to upload a image for a message
        addImagePickerListener();

        // Enable Send button when there's no text to send
        addTextChangedListener();

        // mSendButton sends a message and clears the EditText
        addSendButtonListener();
    }

    private void addAuthStateListener() {
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedIn(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOut();
                    startActivityForResult(getSignInIntent(), ChatConstants.RC_SIGN_IN);
                }
            }
        };
    }

    private void addImagePickerListener() {
        mImagePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                startActivityForResult(
                        Intent.createChooser(intent, "Complete action using"),
                        ChatConstants.RC_PHOTO_PICKER);
            }
        });
    }

    private void addTextChangedListener() {
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) setEnableButton(true);
                else setEnableButton(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void addSendButtonListener() {
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageString = mMessageEditText.getText().toString();
                MessageModel message = new MessageModel(messageString, mUserName, null);

                // Add value to FireBase DataBase
                mMessageDatabaseReference.push().setValue(message);

                // Clear input box
                mMessageEditText.setText(ChatConstants.TEXT_NULL);
            }
        });
    }

    private void addDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    mMessageAdapter.addMessage(message);
                    mRecyclerView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mImageEmptyChat.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mMessageDatabaseReference.addChildEventListener(mChildEventListener);
            addSingleValueEventListener();
        }
    }

    private void addSingleValueEventListener() {
        mMessageDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Handle the case where the data already exists
                } else {
                    // Handle the case where the data does not yet exist
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mImageEmptyChat.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessageDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private Intent getSignInIntent() {

        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build());

        return AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setProviders(providers)
                .setTheme(R.style.FullscreenTheme)
                .build();
    }

    private void onSignedIn(String userName) {
        mUserName = userName;
        addDatabaseReadListener();
    }

    private void onSignedOut() {
        mUserName = ChatConstants.USER_ANONYMOUS;
        mMessageAdapter.clearAdapter();
        removeDatabaseReadListener();
    }

    private void setEnableButton(boolean isButtonEnable) {
        mSendButton.setEnabled(isButtonEnable);
        if (isButtonEnable) mSendButton.setBackground(
                ContextCompat.getDrawable(getApplicationContext(), R.drawable.selector_send));
        else mSendButton.setBackgroundColor(
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryNormal));
    }
}