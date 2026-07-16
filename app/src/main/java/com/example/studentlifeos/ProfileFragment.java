package com.example.studentlifeos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Dark mode toggle + logout only need to be wired up once.
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(StudentLifeOSApp.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(StudentLifeOSApp.KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(StudentLifeOSApp.KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class))
        );

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-fetch every time this tab becomes visible — including right
        // after returning from EditProfileActivity — so saved changes
        // actually show up instead of leaving stale/placeholder text.
        loadProfile();
    }

    private void loadProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rootView == null) return;

        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Couldn't load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void bindProfile(DocumentSnapshot doc) {
        if (!isAdded() || rootView == null) return;

        if (!doc.exists()) {
            Toast.makeText(getContext(), "No profile document found for this account", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> personal = (Map<String, Object>) doc.get("personal");
        Map<String, Object> academic = (Map<String, Object>) doc.get("academic");
        Map<String, Object> metrics = (Map<String, Object>) doc.get("metrics");

        String firstName = personal != null && personal.get("firstName") != null ? personal.get("firstName").toString() : "";
        String lastName = personal != null && personal.get("lastName") != null ? personal.get("lastName").toString() : "";
        String fullName = (firstName + " " + lastName).trim();

        String branch = academic != null && academic.get("branch") != null ? academic.get("branch").toString() : "—";
        Object semester = academic != null ? academic.get("semester") : null;
        String rollNumber = academic != null && academic.get("rollNumber") != null ? academic.get("rollNumber").toString() : "—";
        String university = academic != null && academic.get("university") != null ? academic.get("university").toString() : "—";
        String phone = personal != null && personal.get("phone") != null ? personal.get("phone").toString() : "Not added";
        Object cpi = metrics != null ? metrics.get("cpi") : null;

        ((TextView) rootView.findViewById(R.id.tvName)).setText(fullName.isEmpty() ? "Student" : fullName);
        ((TextView) rootView.findViewById(R.id.tvBranchSem)).setText(branch + " · Sem " + (semester != null ? semester : "—"));
        ((TextView) rootView.findViewById(R.id.tvRollNumber)).setText(rollNumber);
        ((TextView) rootView.findViewById(R.id.tvCgpa)).setText(cpi != null ? String.valueOf(cpi) : "—");
        ((TextView) rootView.findViewById(R.id.tvEmail)).setText(doc.getString("email"));
        ((TextView) rootView.findViewById(R.id.tvPhone)).setText(phone);
        ((TextView) rootView.findViewById(R.id.tvUniversity)).setText(university);

        TextView tvAvatarInitials = rootView.findViewById(R.id.tvAvatarInitials);
        if (!firstName.isEmpty()) {
            String initials = (firstName.charAt(0) + "") + (lastName.isEmpty() ? "" : lastName.charAt(0) + "");
            tvAvatarInitials.setText(initials.toUpperCase());
        } else {
            tvAvatarInitials.setText("?");
        }
    }
}