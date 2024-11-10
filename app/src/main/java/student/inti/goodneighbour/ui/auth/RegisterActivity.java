package student.inti.goodneighbour.ui.auth;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import student.inti.goodneighbour.R;
import student.inti.goodneighbour.utils.ValidationUtils;
import student.inti.goodneighbour.utils.UIUtils;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private MaterialButton registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());
        findViewById(R.id.loginPrompt).setOnClickListener(v -> finish());

        // Clear errors on text change
        nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nameLayout.setError(null);
        });
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) emailLayout.setError(null);
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) passwordLayout.setError(null);
        });
        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) confirmPasswordLayout.setError(null);
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        ValidationUtils.ValidationResult nameValidation = ValidationUtils.validateName(name);
        if (!nameValidation.isValid) {
            nameLayout.setError(nameValidation.errorMessage);
            isValid = false;
        }

        ValidationUtils.ValidationResult emailValidation = ValidationUtils.validateEmail(email);
        if (!emailValidation.isValid) {
            emailLayout.setError(emailValidation.errorMessage);
            isValid = false;
        }

        ValidationUtils.ValidationResult passwordValidation = ValidationUtils.validatePassword(password);
        if (!passwordValidation.isValid) {
            passwordLayout.setError(passwordValidation.errorMessage);
            isValid = false;
        }

        ValidationUtils.ValidationResult passwordMatchValidation =
                ValidationUtils.validatePasswordMatch(password, confirmPassword);
        if (!passwordMatchValidation.isValid) {
            confirmPasswordLayout.setError(passwordMatchValidation.errorMessage);
            isValid = false;
        }

        return isValid;
    }

    private void attemptRegistration() {
        if (!validateInputs()) {
            return;
        }

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        registerButton.setEnabled(false);
        UIUtils.showLoading(this, "Creating account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    UIUtils.hideLoading();
                                    Toast.makeText(RegisterActivity.this,
                                            "Registration successful!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    UIUtils.hideLoading();
                                    registerButton.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this,
                                            "Failed to save user data: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        UIUtils.hideLoading();
                        registerButton.setEnabled(true);
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}