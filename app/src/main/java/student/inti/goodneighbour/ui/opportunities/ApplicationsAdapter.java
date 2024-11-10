package student.inti.goodneighbour.ui.opportunities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Application;
import student.inti.goodneighbour.utils.ImageUtils;

public class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder> {
    private List<Application> applications = new ArrayList<>();
    private final OnApplicationActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a",
            Locale.getDefault());

    public interface OnApplicationActionListener {
        void onApprove(Application application);
        void onReject(Application application);
    }

    public ApplicationsAdapter(OnApplicationActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        holder.bind(applications.get(position));
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
        notifyDataSetChanged();
    }

    class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView applicantImage;
        private final TextView applicantName;
        private final TextView applicantEmail;
        private final TextView applicantPhone;
        private final TextView applicationDate;
        private final TextView motivationText;
        private final TextView statusText;
        private final MaterialButton approveButton;
        private final MaterialButton rejectButton;

        ApplicationViewHolder(View itemView) {
            super(itemView);
            applicantImage = itemView.findViewById(R.id.applicantImage);
            applicantName = itemView.findViewById(R.id.applicantName);
            applicantEmail = itemView.findViewById(R.id.applicantEmail);
            applicantPhone = itemView.findViewById(R.id.applicantPhone);
            applicationDate = itemView.findViewById(R.id.applicationDate);
            motivationText = itemView.findViewById(R.id.motivationText);
            statusText = itemView.findViewById(R.id.statusText);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        void bind(Application application) {
            // Set user details
            applicantName.setText(application.getUserName());
            applicantEmail.setText(application.getUserEmail());
            applicantPhone.setText(application.getUserPhone());
            applicationDate.setText(dateFormat.format(application.getCreatedAt()));
            motivationText.setText(application.getMotivation());

            // Load profile image
            String profileImagePath = application.getUserImageUrl();
            Log.d("ApplicationsAdapter", "Loading profile image for " + application.getUserName() +
                    ", path: " + profileImagePath);

            if (profileImagePath != null && !profileImagePath.trim().isEmpty()) {
                // Show loading state
                applicantImage.setImageResource(R.drawable.default_profile);

                ImageUtils.loadImage(profileImagePath, new ImageUtils.OnCompleteListener<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        Log.d("ApplicationsAdapter", "Successfully loaded image for " +
                                application.getUserName());
                        if (bitmap != null) {
                            applicantImage.post(() -> {
                                applicantImage.setImageBitmap(bitmap);
                                Log.d("ApplicationsAdapter", "Image set to ImageView for " +
                                        application.getUserName());
                            });
                        } else {
                            Log.e("ApplicationsAdapter", "Received null bitmap for " +
                                    application.getUserName());
                            applicantImage.setImageResource(R.drawable.default_profile);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("ApplicationsAdapter", "Failed to load profile image for " +
                                application.getUserName() + ": " + error);
                        applicantImage.post(() ->
                                applicantImage.setImageResource(R.drawable.default_profile));
                    }
                });
            } else {
                Log.d("ApplicationsAdapter", "No profile image path for " + application.getUserName());
                applicantImage.setImageResource(R.drawable.default_profile);
            }

            // Handle status and buttons
            statusText.setText(application.getStatus().toUpperCase());
            boolean isPending = "pending".equals(application.getStatus());
            approveButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
            rejectButton.setVisibility(isPending ? View.VISIBLE : View.GONE);

            // Set status text color
            int statusColor;
            switch (application.getStatus()) {
                case "approved":
                    statusColor = R.color.status_approved;
                    break;
                case "rejected":
                    statusColor = R.color.status_rejected;
                    break;
                default:
                    statusColor = R.color.status_pending;
                    break;
            }
            statusText.setTextColor(itemView.getContext().getColor(statusColor));

            // Set button listeners
            if (isPending) {
                approveButton.setOnClickListener(v -> listener.onApprove(application));
                rejectButton.setOnClickListener(v -> listener.onReject(application));
            }
        }
    }
}