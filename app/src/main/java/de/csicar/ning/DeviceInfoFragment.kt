package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceInfoFragment.OnListFragmentInteractionListener] interface.
 */
class DeviceInfoFragment : Fragment() {
    lateinit var viewModel: ScanViewModel
    lateinit var adapter : DeviceInfoRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deviceinfo_list, container, false)
        viewModel = ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)
        viewModel.deviceDao.getById(arguments?.getLong("deviceId")!!).observe(this, Observer {
            fetchInfo(it)

            viewModel.portDao.getAllForDevice(it.deviceId).observe(this, Observer {
                adapter.updateData(it)
            })
        })
        activity?.toolbar?.findViewById<TextView>(R.id.title_detail)?.text = arguments?.getString("deviceIp")
        // Set the adapter
        adapter = DeviceInfoRecyclerViewAdapter(listOf())
        if (view is RecyclerView) {
            view.layoutManager = LinearLayoutManager(context)
            view.adapter = adapter
        }
        return view
    }

    fun fetchInfo(device: Device) {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                device.scanPorts().forEach {
                    launch {
                        val (port, isOpen) = it.await()
                        if (isOpen) {
                            viewModel.portDao.insert(Port(0, port, Protocol.TCP, device.deviceId))
                        }
                    }
                }
            }
        }
    }
}