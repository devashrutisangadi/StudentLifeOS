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

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvBranchSem = view.findViewById(R.id.tvBranchSem);
        TextView tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        TextView tvRollNumber = view.findViewById(R.id.tvRollNumber);
        TextView tvCgpa = view.findViewById(R.id.tvCgpa);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        TextView tvUniversity = view.findViewById(R.id.tvUniversity);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);

        // --- Load profile data from Firestore ---
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(this::bindProfile)
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Couldn't load profile", Toast.LENGTH_SHORT).show());
        }

        // --- Dark mode toggle ---
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(StudentLifeOSApp.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(StudentLifeOSApp.KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(StudentLifeOSApp.KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            // Recreating the Activity applies the theme change immediately
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        // --- Buttons ---
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                        Toast.makeText(getContext(), "Edit profile screen coming soon", Toast.LENGTH_SHORT).show()
                // TODO: replace with startActivity(new Intent(getContext(), EditProfileActivity.class))
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

    private void bindProfile(DocumentSnapshot doc) {
        if (!isAdded() || getView() == null) return;

        String name = doc.getString("displayName");
        String branch = doc.getString("branch");
        Long semester = doc.getLong("semester");
        String rollNumber = doc.getString("rollNumber");
        Double cgpa = doc.getDouble("cgpa");
        String email = doc.getString("email");
        String contact = doc.getString("contact");
        String university = doc.getString("university");

        ((TextView) getView().findViewById(R.id.tvName))
                .setText(name != null ? name : "Student");
        ((TextView) getView().findViewById(R.id.tvBranchSem))
                .setText((branch != null ? branch : "—") + " · Sem " + (semester != null ? semester : "—"));
        ((TextView) getView().findViewById(R.id.tvRollNumber))
                .setText(rollNumber != null ? rollNumber : "—");
        ((TextView) getView().findViewById(R.id.tvCgpa))
                .setText(cgpa != null ? String.valueOf(cgpa) : "—");
        ((TextView) getView().findViewById(R.id.tvEmail))
                .setText(email != null ? email : "—");
        ((TextView) getView().findViewById(R.id.tvPhone))
                .setText(contact != null ? contact : "Not added");
        ((TextView) getView().findViewById(R.id.tvUniversity))
                .setText(university != null ? university : "—");

        // Avatar initials from name
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length > 1
                    ? ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
                    : parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            ((TextView) getView().findViewById(R.id.tvAvatarInitials)).setText(initials);
        }
    }
}