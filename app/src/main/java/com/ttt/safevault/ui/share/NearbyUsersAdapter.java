package com.ttt.safevault.ui.share;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.ttt.safevault.R;
import com.ttt.safevault.dto.response.NearbyUserResponse;

import java.util.List;

/**
 * 附近用户列表适配器
 */
public class NearbyUsersAdapter extends RecyclerView.Adapter<NearbyUsersAdapter.ViewHolder> {

    private List<NearbyUserResponse> users;
    private final OnUserClickListener clickListener;

    public interface OnUserClickListener {
        void onUserClick(NearbyUserResponse user);
    }

    public NearbyUsersAdapter(List<NearbyUserResponse> users, OnUserClickListener clickListener) {
        this.users = users;
        this.clickListener = clickListener;
    }

    public void updateUsers(List<NearbyUserResponse> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_nearby_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyUserResponse user = users.get(position);
        holder.bind(user, clickListener);
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView displayNameText;
        private final TextView distanceText;
        private final TextView statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            displayNameText = itemView.findViewById(R.id.text_display_name);
            distanceText = itemView.findViewById(R.id.text_distance);
            statusText = itemView.findViewById(R.id.text_status);
        }

        public void bind(NearbyUserResponse user, OnUserClickListener clickListener) {
            // 显示用户名
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                displayNameText.setText(user.getDisplayName());
            } else {
                displayNameText.setText(user.getUserId());
            }

            // 显示距离
            if (user.getDistance() > 0) {
                double distanceKm = user.getDistance() / 1000.0;
                if (distanceKm < 1) {
                    distanceText.setText(String.format("%.0f米", user.getDistance()));
                } else {
                    distanceText.setText(String.format("%.1f公里", distanceKm));
                }
                distanceText.setVisibility(View.VISIBLE);
            } else {
                distanceText.setVisibility(View.GONE);
            }

            // 显示在线状态
            if (user.isOnline()) {
                statusText.setText("在线");
                statusText.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                statusText.setVisibility(View.VISIBLE);
            } else {
                statusText.setVisibility(View.GONE);
            }

            // 点击事件
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onUserClick(user);
                }
            });
        }
    }
}
