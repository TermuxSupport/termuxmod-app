package com.termux.app.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.termux.R;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.profile_title));
        }

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        bindUserInfo(user);
        bindDeviceInfo();

        findViewById(R.id.btn_sign_out).setOnClickListener(v -> showSignOutDialog());
    }

    private void bindUserInfo(FirebaseUser user) {
        ImageView avatar = findViewById(R.id.profile_avatar);
        TextView name = findViewById(R.id.profile_name);
        TextView email = findViewById(R.id.profile_email);
        TextView uid = findViewById(R.id.profile_uid);

        name.setText(user.getDisplayName() != null ? user.getDisplayName() : "-");
        email.setText(user.getEmail() != null ? user.getEmail() : "-");
        uid.setText(user.getUid());

        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void bindDeviceInfo() {
        TextView deviceModel = findViewById(R.id.device_model);
        TextView deviceBrand = findViewById(R.id.device_brand);
        TextView androidVersion = findViewById(R.id.device_android_version);
        TextView deviceArch = findViewById(R.id.device_arch);
        TextView deviceId = findViewById(R.id.device_id);
        TextView deviceLanguage = findViewById(R.id.device_language);

        deviceModel.setText(Build.MODEL);
        deviceBrand.setText(Build.BRAND + " / " + Build.MANUFACTURER);
        androidVersion.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        deviceArch.setText(Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : Build.CPU_ABI);
        deviceId.setText(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        deviceLanguage.setText(Locale.getDefault().getDisplayLanguage() + " (" + Locale.getDefault().toLanguageTag() + ")");
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_sign_out)
                .setMessage(R.string.profile_sign_out_confirm)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> signOut())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(this, R.string.profile_signed_out, Toast.LENGTH_SHORT).show();
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
