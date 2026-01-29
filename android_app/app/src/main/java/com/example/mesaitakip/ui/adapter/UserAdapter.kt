package com.example.mesaitakip.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mesaitakip.data.entities.User
import com.example.mesaitakip.databinding.ItemUserBinding

class UserAdapter(
    private val onBanToggle: (User) -> Unit,
    private val onAdminToggle: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback) {

    class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            user: User,
            onBanToggle: (User) -> Unit,
            onAdminToggle: (User) -> Unit,
            onDelete: (User) -> Unit
        ) {
            binding.tvUserInfo.text = "${user.adsoyad} (@${user.username})"

            binding.btnToggleBan.text = if (user.is_banned == 1) "Unban" else "Ban"
            binding.btnToggleAdmin.text = if (user.is_admin == 1) "Revoke Admin" else "Make Admin"

            binding.btnToggleBan.setOnClickListener { onBanToggle(user) }
            binding.btnToggleAdmin.setOnClickListener { onAdminToggle(user) }
            binding.btnDeleteUser.setOnClickListener { onDelete(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position), onBanToggle, onAdminToggle, onDelete)
    }

    object UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
