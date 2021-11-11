package com.robert.bluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.robert.bluetooth.databinding.ItemPairingDeviceBinding

class PairingAdapter(
    private var devices: List<Device>?
) : RecyclerView.Adapter<PairingHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PairingHolder(
            ItemPairingDeviceBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent, false
            )
        )

    override fun onBindViewHolder(holder: PairingHolder, position: Int) {
        holder.apply {
            devices?.get(position)?.let { bind(it) }
        }
    }

    override fun getItemCount(): Int = devices?.size ?: 0
}
