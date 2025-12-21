package com.ttt.safevault.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.ttt.safevault.R;
import com.ttt.safevault.model.PasswordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码列表的RecyclerView适配器
 * 支持点击动画、快捷操作等功能
 */
public class PasswordListAdapter extends ListAdapter<PasswordItem, PasswordListAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private boolean showAnimations = true;

    public PasswordListAdapter(OnItemClickListener listener) {
        super(DiffCallback);
        this.listener = listener;
    }

    public PasswordListAdapter(OnItemClickListener listener, boolean showAnimations) {
        super(DiffCallback);
        this.listener = listener;
        this.showAnimations = showAnimations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PasswordItem item = getItem(position);
        holder.bind(item, listener, showAnimations);
    }

    /**
     * ViewHolder for password items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleText;
        private final TextView usernameText;
        private final TextView urlText;
        private final ImageButton copyButton;
        private final ImageButton editButton;
        private final ImageButton deleteButton;
        private final ImageView faviconView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_view);
            titleText = itemView.findViewById(R.id.title_text);
            usernameText = itemView.findViewById(R.id.username_text);
            urlText = itemView.findViewById(R.id.url_text);
            copyButton = itemView.findViewById(R.id.btn_copy);
            editButton = itemView.findViewById(R.id.btn_edit);
            deleteButton = itemView.findViewById(R.id.btn_delete);
            faviconView = itemView.findViewById(R.id.favicon_image);
        }

        public void bind(PasswordItem item, OnItemClickListener listener, boolean animate) {
            // 设置标题
            titleText.setText(item.getDisplayName());

            // 设置用户名
            if (item.getUsername() != null && !item.getUsername().isEmpty()) {
                usernameText.setText(item.getUsername());
                usernameText.setVisibility(View.VISIBLE);
            } else {
                usernameText.setVisibility(View.GONE);
            }

            // 设置URL
            if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                String displayUrl = formatUrl(item.getUrl());
                urlText.setText(displayUrl);
                urlText.setVisibility(View.VISIBLE);

                // 设置网站图标
                if (faviconView != null) {
                    faviconView.setVisibility(View.VISIBLE);
                    faviconView.setImageResource(R.drawable.ic_public);
                }
            } else {
                urlText.setVisibility(View.GONE);
                if (faviconView != null) {
                    faviconView.setVisibility(View.GONE);
                }
            }

            // 如果没有URL，显示默认图标
            if (faviconView != null && (item.getUrl() == null || item.getUrl().isEmpty())) {
                faviconView.setVisibility(View.VISIBLE);
                faviconView.setImageResource(R.drawable.ic_password);
            }

            // 设置点击事件
            cardView.setOnClickListener(v -> {
                if (animate) {
                    animateCardClick(cardView);
                }
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            // 复制按钮
            copyButton.setOnClickListener(v -> {
                if (animate) {
                    animateButtonClick(copyButton);
                }
                if (listener != null) {
                    listener.onItemCopyClick(item);
                }
            });

            // 编辑按钮
            editButton.setOnClickListener(v -> {
                if (animate) {
                    animateButtonClick(editButton);
                }
                if (listener != null) {
                    listener.onItemEditClick(item);
                }
            });

            // 删除按钮 - 修复原来的错误（之前错误地使用了editButton）
            deleteButton.setOnClickListener(v -> {
                if (animate) {
                    animateButtonClick(deleteButton);
                }
                if (listener != null) {
                    listener.onItemDeleteClick(item);
                }
            });

            // 设置过渡动画名称
            ViewCompat.setTransitionName(cardView, "password_item_" + item.getId());
        }

        private String formatUrl(String url) {
            if (url == null) return "";

            // 移除协议前缀
            String formattedUrl = url.replace("https://", "")
                                     .replace("http://", "")
                                     .replace("www.", "");

            // 移除路径
            int slashIndex = formattedUrl.indexOf('/');
            if (slashIndex > 0) {
                formattedUrl = formattedUrl.substring(0, slashIndex);
            }

            return formattedUrl;
        }

        private void animateCardClick(MaterialCardView card) {
            AnimatorSet animatorSet = new AnimatorSet();

            // 缩放动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.95f, 1f);

            // 背景颜色动画
            int currentColor = MaterialColors.getColor(card, android.R.attr.colorBackground);
            int pressedColor = MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimary);

            ObjectAnimator backgroundColor = ObjectAnimator.ofArgb(
                    card, "cardBackgroundColor", currentColor, pressedColor, currentColor);

            animatorSet.playTogether(scaleX, scaleY, backgroundColor);
            animatorSet.setDuration(150);
            animatorSet.start();
        }

        private void animateButtonClick(ImageButton button) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.8f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.8f, 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(100);
            animatorSet.start();
        }
    }

    /**
     * Item点击监听接口
     */
    public interface OnItemClickListener {
        void onItemClick(PasswordItem item);
        void onItemCopyClick(PasswordItem item);
        void onItemEditClick(PasswordItem item);
        void onItemDeleteClick(PasswordItem item);
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private static final DiffUtil.ItemCallback<PasswordItem> DiffCallback = new DiffUtil.ItemCallback<PasswordItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull PasswordItem oldItem, @NonNull PasswordItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PasswordItem oldItem, @NonNull PasswordItem newItem) {
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull PasswordItem oldItem, @NonNull PasswordItem newItem) {
            // 返回哪些字段发生了变化，用于局部更新
            Bundle diff = new Bundle();

            if (!oldItem.getTitle().equals(newItem.getTitle())) {
                diff.putString("title", newItem.getTitle());
            }
            if (!oldItem.getUsername().equals(newItem.getUsername())) {
                diff.putString("username", newItem.getUsername());
            }
            if (!oldItem.getUrl().equals(newItem.getUrl())) {
                diff.putString("url", newItem.getUrl());
            }

            return diff.isEmpty() ? null : diff;
        }
    };

    /**
     * 设置是否显示动画
     */
    public void setShowAnimations(boolean showAnimations) {
        this.showAnimations = showAnimations;
    }

    /**
     * 获取指定位置的密码项
     */
    public PasswordItem getItemAt(int position) {
        return getItem(position);
    }

    /**
     * 清空列表
     */
    public void clearList() {
        submitList(null);
    }

    /**
     * 更新单个项
     */
    public void updateItem(PasswordItem item) {
        List<PasswordItem> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId() == item.getId()) {
                currentList.set(i, item);
                submitList(new ArrayList<>(currentList));
                break;
            }
        }
    }
}