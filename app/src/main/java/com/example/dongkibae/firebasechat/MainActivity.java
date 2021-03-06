package com.example.dongkibae.firebasechat;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int RC_SIGN_IN = 1001;

    // Firebase - Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    // Firebase - Authentication
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;

    // Views
    private ListView mListView;
    private EditText mEdtMessage;
    private SignInButton mBtnGoogleSignIn; // 로그인 버튼
    private Button mBtnGoogleSignOut; // 로그아웃 버튼
    private TextView mTxtProfileInfo; // 사용자 정보 표시
    private ImageView mImgProfile; // 사용자 프로필 이미지 표시

    // Values
    private ChatAdapter mAdapter;
    private String userName;

    public class ChatData {
        public String firebaseKey; // Firebase Realtime Database 에 등록된 Key 값
        public String userName; // 사용자 이름
        public String userPhotoUrl; // 사용자 사진 URL
        public String userEmail; // 사용자 이메일주소
        public String message; // 작성한 메시지
        public long time; // 작성한 시간
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initFirebaseDatabase();
        initFirebaseAuth();
        initValues();
    }

    private void initViews() {
        mListView = (ListView) findViewById(R.id.list_message);
        mAdapter = new ChatAdapter(this, 0);
        mListView.setAdapter(mAdapter);

        mEdtMessage = (EditText) findViewById(R.id.edit_message);
        findViewById(R.id.btn_send).setOnClickListener(this);

        mBtnGoogleSignIn = (SignInButton) findViewById(R.id.btn_google_signin);
        mBtnGoogleSignOut = (Button) findViewById(R.id.btn_google_signout);
        mBtnGoogleSignIn.setOnClickListener(this);
        mBtnGoogleSignOut.setOnClickListener(this);

        mTxtProfileInfo = (TextView) findViewById(R.id.txt_profile_info);
        mImgProfile = (ImageView) findViewById(R.id.img_profile);
    }

    private void initFirebaseDatabase() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference("message");
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                ChatData chatData = dataSnapshot.getValue(ChatData.class);
//                chatData.firebaseKey = dataSnapshot.getKey();
//                mAdapter.add(chatData);
                mListView.smoothScrollToPosition(mAdapter.getCount());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String firebaseKey = dataSnapshot.getKey();
                int count = mAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    if (mAdapter.getItem(i).firebaseKey.equals(firebaseKey)) {
                        mAdapter.remove(mAdapter.getItem(i));
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mDatabaseReference.addChildEventListener(mChildEventListener);
    }

    private void initFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateProfile();
            }
        };
    }

    private void initValues() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            userName = "Guest" + new Random().nextInt(5000);
        } else {
            userName = user.getDisplayName();
        }
    }

    private void updateProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // 비 로그인 상태 (메시지를 전송할 수 없다.)
            mBtnGoogleSignIn.setVisibility(View.VISIBLE);
            mBtnGoogleSignOut.setVisibility(View.GONE);
            mTxtProfileInfo.setVisibility(View.GONE);
            mImgProfile.setVisibility(View.GONE);
            findViewById(R.id.btn_send).setVisibility(View.GONE);
            mAdapter.setEmail(null);
            mAdapter.notifyDataSetChanged();
        } else {
            // 로그인 상태
            mBtnGoogleSignIn.setVisibility(View.GONE);
            mBtnGoogleSignOut.setVisibility(View.VISIBLE);
            mTxtProfileInfo.setVisibility(View.VISIBLE);
            mImgProfile.setVisibility(View.VISIBLE);
            findViewById(R.id.btn_send).setVisibility(View.VISIBLE);

            userName = user.getDisplayName(); // 채팅에 사용 될 닉네임 설정
            String email = user.getEmail();
            StringBuilder profile = new StringBuilder();
            profile.append(userName).append("\n").append(user.getEmail());
            mTxtProfileInfo.setText(profile);
            mAdapter.setEmail(email);
            mAdapter.notifyDataSetChanged();

            Picasso.with(this).load(user.getPhotoUrl()).into(mImgProfile);
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateProfile();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabaseReference.removeEventListener(mChildEventListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                updateProfile();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                String message = mEdtMessage.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    mEdtMessage.setText("");
                    ChatData chatData = new ChatData();
                    chatData.userName = userName;
                    chatData.message = message;
                    chatData.time = System.currentTimeMillis();
                    chatData.userEmail = mAuth.getCurrentUser().getEmail(); // 사용자 이메일 주소
                    chatData.userPhotoUrl = mAuth.getCurrentUser().getPhotoUrl().toString(); // 사용자 프로필 이미지 주소
                    mDatabaseReference.push().setValue(chatData.toString().trim());
                }
                break;
            case R.id.btn_google_signin:
                signIn();
                break;
            case R.id.btn_google_signout:
                signOut();
                break;
        }
    }


    public class ChatAdapter extends ArrayAdapter<ChatData> {
        private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("a h:mm", Locale.getDefault());
        private final static int TYPE_MY_SELF = 0;
        private final static int TYPE_ANOTHER = 1;
        private String mMyEmail;

        public ChatAdapter(Context context, int resource) {
            super(context, resource);
        }

        public void setEmail(String email) {
            mMyEmail = email;
        }

        private View setAnotherView(LayoutInflater inflater) {
            View convertView = inflater.inflate(R.layout.listitem_chat, null);
            ViewHolderAnother holder = new ViewHolderAnother();
            holder.bindView(convertView);
            convertView.setTag(holder);
            return convertView;
        }

        private View setMySelfView(LayoutInflater inflater) {
            View convertView = inflater.inflate(R.layout.listitem_chat_my, null);
            ViewHolderMySelf holder = new ViewHolderMySelf();
            holder.bindView(convertView);
            convertView.setTag(holder);
            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (convertView == null) {
                if (viewType == TYPE_ANOTHER) {
                    convertView = setAnotherView(inflater);
                } else {
                    convertView = setMySelfView(inflater);
                }
            }

            if (convertView.getTag() instanceof ViewHolderAnother) {
                if (viewType != TYPE_ANOTHER) {
                    convertView = setAnotherView(inflater);
                }
                ((ViewHolderAnother) convertView.getTag()).setData(position);
            } else {
                if (viewType != TYPE_MY_SELF) {
                    convertView = setMySelfView(inflater);
                }
                ((ViewHolderMySelf) convertView.getTag()).setData(position);
            }

            return convertView;
        }


        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            String email = getItem(position).userEmail;
            if (!TextUtils.isEmpty(mMyEmail) && mMyEmail.equals(email)) {
                return TYPE_MY_SELF; // 나의 채팅내용
            } else {
                return TYPE_ANOTHER; // 상대방의 채팅내용
            }
        }

        private class ViewHolderAnother {
            private ImageView mImgProfile;
            private TextView mTxtUserName;
            private TextView mTxtMessage;
            private TextView mTxtTime;

            private void bindView(View convertView) {
                mImgProfile = (ImageView) convertView.findViewById(R.id.img_profile);
                mTxtUserName = (TextView) convertView.findViewById(R.id.txt_userName);
                mTxtMessage = (TextView) convertView.findViewById(R.id.txt_message);
                mTxtTime = (TextView) convertView.findViewById(R.id.txt_time);
            }

            private void setData(int position) {
                ChatData chatData = getItem(position);
                Picasso.with(getContext()).load(chatData.userPhotoUrl).into(mImgProfile);
                mTxtUserName.setText(chatData.userName);
                mTxtMessage.setText(chatData.message);
                mTxtTime.setText(mSimpleDateFormat.format(chatData.time));
            }
        }

        private class ViewHolderMySelf {
            private TextView mTxtMessage;
            private TextView mTxtTime;

            private void bindView(View convertView) {
                mTxtMessage = (TextView) convertView.findViewById(R.id.txt_message);
                mTxtTime = (TextView) convertView.findViewById(R.id.txt_time);
            }

            private void setData(int position) {
                ChatData chatData = getItem(position);
                mTxtMessage.setText(chatData.message);
                mTxtTime.setText(mSimpleDateFormat.format(chatData.time));
            }
        }
    }
}
