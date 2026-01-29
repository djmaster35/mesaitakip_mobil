package com.example.mesaitakip.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mesaitakip.data.entities.Week
import com.example.mesaitakip.databinding.ItemWeekBinding

class WeekAdapter(
    private val onClick: (Week) -> Unit,
    private val onDelete: (Week) -> Unit
) : ListAdapter<Week, WeekAdapter.WeekViewHolder>(WeekDiffCallback) {

    class WeekViewHolder(private val binding: ItemWeekBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(week: Week, onClick: (Week) -> Unit, onDelete: (Week) -> Unit) {
            binding.tvWeekRange.text = week.hafta_araligi
            binding.tvEmployeeName.text = week.calisan_adi
            binding.root.setOnClickListener { onClick(week) }
            binding.btnDelete.setOnClickListener { onDelete(week) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val binding = ItemWeekBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeekViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onDelete)
    }

    object WeekDiffCallback : DiffUtil.ItemCallback<Week>() {
        override fun areItemsTheSame(oldItem: Week, newItem: Week): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Week, newItem: Week): Boolean = oldItem == newItem
    }
}
