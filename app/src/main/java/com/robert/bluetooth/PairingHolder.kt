package com.robert.bluetooth

import androidx.recyclerview.widget.RecyclerView
import com.robert.bluetooth.databinding.ItemPairingDeviceBinding

class PairingHolder(
    val binding: ItemPairingDeviceBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(device: Device) {
        binding.itemName.text = device.name
        binding.itemMac.text = device.address
    }
}

data class Device(
    val name: String,
    val address: String
)
