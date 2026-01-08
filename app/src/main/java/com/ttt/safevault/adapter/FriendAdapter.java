package com.ttt.safevault.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ttt.safevault.R;
import com.ttt.safevault.model.Friend;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 好友列表适配器
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<Friend> friendList = new ArrayList<>();
    private OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onRemoveFriend(Friend friend);
    }

    public void setOnFriendActionListener(OnFriendActionListener listener) {
        this.listener = listener;
    }

    public void setFriendList(List<Friend> friends) {
        this.friendList = friends != null ? friends : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textDisplayName;
        private final TextView textFriendId;
        private final TextView textAddedTime;
        private final MaterialButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDisplayName = itemView.findViewById(R.id.text_display_name);
            textFriendId = itemView.findViewById(R.id.text_friend_id);
            textAddedTime = itemView.findViewById(R.id.text_added_time);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(Friend friend) {
            // 显示昵称
            textDisplayName.setText(friend.getDisplayName());

            // 显示好友ID
            textFriendId.setText(friend.getFriendId());

            // 显示添加时间
            if (friend.getAddedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                textAddedTime.setText(sdf.format(new Date(friend.getAddedAt())));
            } else {
                textAddedTime.setText("");
            }

            // 删除按钮点击事件
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFriend(friend);
                }
            });
        }
    }
}
