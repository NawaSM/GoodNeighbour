package student.inti.goodneighbour.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.utils.UIUtils;
import student.inti.goodneighbour.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private MaterialButton resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailLayout = findViewById(R.id.emailLayout);
        emailEditText = findViewById(R.id.emailEditText);
        resetButton = findViewById(R.id.resetButton);

        // Setup listeners
        resetButton.setOnClickListener(v -> attemptPasswordReset());
        findViewById(R.id.backToLoginPrompt).setOnClickListener(v -> finish());

        // Clear error on text change
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) emailLayout.setError(null);
        });
    }

    private void attemptPasswordReset() {
        String email = emailEditText.getText().toString().trim();

        // Validate email
        ValidationUtils.ValidationResult emailValidation = ValidationUtils.validateEmail(email);
        if (!emailValidation.isValid) {
            emailLayout.setError(emailValidation.errorMessage);
            return;
        }

        resetButton.setEnabled(false);
        UIUtils.showLoading(this, "Sending reset link...");

        // Directly send reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    UIUtils.hideLoading();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Reset Link Sent")
                            .setMessage("If an account exists with this email address, " +
                                    "a password reset link will be sent. Please check your inbox and spam folder.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    UIUtils.hideLoading();
                    resetButton.setEnabled(true);
                    Toast.makeText(this, "Failed to send reset link. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }
}