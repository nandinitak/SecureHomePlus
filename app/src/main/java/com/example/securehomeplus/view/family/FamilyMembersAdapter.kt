package com.example.securehomeplus.view.family

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.securehomeplus.data.database.entities.FamilyMember
import com.example.securehomeplus.databinding.ItemFamilyMemberBinding

class FamilyMembersAdapter(
    private val onDeleteClick: (FamilyMember) -> Unit
) : ListAdapter<FamilyMember, FamilyMembersAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemFamilyMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(member: FamilyMember) {
            binding.tvName.text = member.name
            binding.tvRelationship.text = member.relationship
            binding.tvPhone.text = "📱 ${member.phoneNumber}"

            binding.btnDelete.setOnClickListener {
                onDeleteClick(member)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFamilyMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<FamilyMember>() {
        override fun areItemsTheSame(oldItem: FamilyMember, newItem: FamilyMember): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FamilyMember, newItem: FamilyMember): Boolean {
            return oldItem == newItem
        }
    }
}
