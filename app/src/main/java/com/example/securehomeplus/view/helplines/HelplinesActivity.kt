package com.example.securehomeplus.view.helplines

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securehomeplus.R
import com.example.securehomeplus.databinding.ActivityHelplinesBinding
import com.example.securehomeplus.databinding.ItemHelplineBinding

data class Helpline(val name: String, val number: String, val iconRes: Int)

class HelplinesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelplinesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelplinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarHelplines)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarHelplines.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val helplines = listOf(
            Helpline("National Emergency", "112", R.drawable.ic_emergency),
            Helpline("Police", "100", R.drawable.ic_shield),
            Helpline("Fire", "101", R.drawable.ic_emergency),
            Helpline("Ambulance", "102", R.drawable.ic_family),
            Helpline("Women Helpline", "1091", R.drawable.ic_person),
            Helpline("Domestic Abuse Support", "181", R.drawable.ic_person),
            Helpline("Senior Citizen (Elder Line)", "14567", R.drawable.ic_person),
            Helpline("Child Helpline", "1098", R.drawable.ic_family),
            Helpline("Cyber Crime Helpline", "1930", R.drawable.ic_lock),
            Helpline("Disaster Management", "108", R.drawable.ic_location)
        )

        binding.rvHelplines.layoutManager = LinearLayoutManager(this)
        binding.rvHelplines.adapter = HelplinesAdapter(helplines) { number ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
        }
    }

    class HelplinesAdapter(
        private val items: List<Helpline>,
        private val onCallClick: (String) -> Unit
    ) : RecyclerView.Adapter<HelplinesAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemHelplineBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHelplineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvHelplineName.text = item.name
            holder.binding.tvHelplineNumber.text = item.number
            holder.binding.ivHelplineIcon.setImageResource(item.iconRes)
            holder.binding.btnCall.setOnClickListener { onCallClick(item.number) }
            holder.binding.root.setOnClickListener { onCallClick(item.number) }
        }

        override fun getItemCount() = items.size
    }
}
