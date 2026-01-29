package com.example.mesaitakip.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.databinding.ItemRecordBinding

class RecordAdapter(
    private val onSaveClicked: (OvertimeRecord) -> Unit
) : ListAdapter<OvertimeRecord, RecordAdapter.RecordViewHolder>(RecordDiffCallback) {

    class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: OvertimeRecord, onSaveClicked: (OvertimeRecord) -> Unit) {
            binding.tvDate.text = record.tarih
            binding.etHours.setText(record.saat)
            binding.etDescription.setText(record.aciklama)
            binding.cbIsHoliday.isChecked = record.is_resmi_tatil == 1

            binding.btnSave.setOnClickListener {
                val updatedRecord = record.copy(
                    saat = binding.etHours.text.toString(),
                    aciklama = binding.etDescription.text.toString(),
                    is_resmi_tatil = if (binding.cbIsHoliday.isChecked) 1 else 0
                )
                onSaveClicked(updatedRecord)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position), onSaveClicked)
    }

    object RecordDiffCallback : DiffUtil.ItemCallback<OvertimeRecord>() {
        override fun areItemsTheSame(oldItem: OvertimeRecord, newItem: OvertimeRecord): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OvertimeRecord, newItem: OvertimeRecord): Boolean {
            // We want to avoid rebinding if only the inputs (which the user might be editing) changed,
            // but since we added a Save button, it's safer to just check everything.
            return oldItem == newItem
        }
    }
}
