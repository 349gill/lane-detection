package com.example.lane_detection

import PipelineViewModel
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.fragment.app.activityViewModels

class PipelineFragment : Fragment() {
    private val viewModel: PipelineViewModel by activityViewModels()
    private lateinit var adapter: PipelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pipeline, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        adapter = PipelineAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.pipelineImages.observe(viewLifecycleOwner) { images ->
            adapter.updateImages(images)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.pipelineImages.removeObservers(viewLifecycleOwner)
    }
}