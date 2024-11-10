package student.inti.goodneighbour.ui.opportunities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;
import student.inti.goodneighbour.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpportunitiesAdapter extends RecyclerView.Adapter<OpportunitiesAdapter.OpportunityViewHolder> {
    private List<Opportunity> opportunities = new ArrayList<>();
    private final OnOpportunityClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final String currentUserId;

    public interface OnOpportunityClickListener {
        void onApplyClick(Opportunity opportunity);
        void onItemClick(Opportunity opportunity);
    }

    public OpportunitiesAdapter(OnOpportunityClickListener listener, String currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("currentUserId cannot be null");
        }
        this.opportunities = new ArrayList<>();
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public OpportunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new OpportunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpportunityViewHolder holder, int position) {
        holder.bind(opportunities.get(position));
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    public void setOpportunities(List<Opportunity> opportunities) {
        this.opportunities.clear();
        this.opportunities.addAll(opportunities);
        notifyDataSetChanged();
    }

    class OpportunityViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView organizationText;
        private final TextView dateText;
        private final TextView locationText;
        private final TextView descriptionText;
        private final MaterialButton applyButton;
        private final ImageView imageView;

        OpportunityViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.opportunityTitle);
            organizationText = itemView.findViewById(R.id.organizationName);
            dateText = itemView.findViewById(R.id.opportunityDate);
            locationText = itemView.findViewById(R.id.opportunityLocation);
            descriptionText = itemView.findViewById(R.id.opportunityDescription);
            applyButton = itemView.findViewById(R.id.applyButton);
            imageView = itemView.findViewById(R.id.opportunityImage);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(opportunities.get(position));
                }
            });

            applyButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onApplyClick(opportunities.get(position));
                }
            });
        }

        private void checkApplicationStatus(Opportunity opportunity, MaterialButton applyButton) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) return;

            FirebaseFirestore.getInstance()
                    .collection("applications")
                    .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                    .whereEqualTo("opportunityId", opportunity.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            applyButton.setText("Applied");
                            applyButton.setEnabled(false);
                        }
                    });
        }

        void bind(Opportunity opportunity) {
            titleText.setText(opportunity.getTitle());
            organizationText.setText(opportunity.getOrganizationName());
            dateText.setText(String.format("%s at %s",
                    dateFormat.format(opportunity.getDate()),
                    opportunity.getTime()));
            locationText.setText(opportunity.getLocation());
            descriptionText.setText(opportunity.getDescription());
            checkApplicationStatus(opportunity, applyButton);

            // Handle image loading
            String imagePath = opportunity.getImageUrl();
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                ImageUtils.loadImage(imagePath, new ImageUtils.OnCompleteListener<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("OpportunitiesAdapter", "Failed to load image: " + error);
                        imageView.setImageResource(R.drawable.placeholder_image);
                    }
                });
            } else {
                imageView.setImageResource(R.drawable.placeholder_image);
            }


            // Handle user's own listings and owner indicator
            boolean isUserOwned = opportunity.getOrganizationId().equals(currentUserId);

            if (isUserOwned) {
                applyButton.setEnabled(false);
                applyButton.setText("Your Listing");
                applyButton.setIcon(itemView.getContext().getDrawable(R.drawable.ic_owner));
            } else {
                applyButton.setEnabled(true);
                applyButton.setText("Apply Now");
                applyButton.setIcon(null);
                checkApplicationStatus(opportunity, applyButton);
            }
        }
    }
}