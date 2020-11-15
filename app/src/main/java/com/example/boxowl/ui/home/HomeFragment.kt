package com.example.boxowl.ui.home

import android.app.AlertDialog
import com.example.boxowl.bases.FragmentInteractionListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.boxowl.R
import com.example.boxowl.bases.BaseFragment
import com.example.boxowl.databinding.FragmentHomeBinding
import com.example.boxowl.databinding.FragmentProfileBinding
import com.example.boxowl.models.Order
import com.example.boxowl.presentation.home.HomeAdapter
import com.example.boxowl.presentation.home.HomeContract
import com.example.boxowl.presentation.home.HomePresenter
import com.example.boxowl.utils.dismissLoadingDialog
import com.example.boxowl.utils.loadingSpotsDialog
import com.example.boxowl.utils.showLoadingDialog
import com.wada811.viewbinding.viewBinding


/**
 * Created by Andrey Morgunov on 27/10/2020.
 */

class HomeFragment : Fragment(R.layout.fragment_home), HomeContract.View {

    interface OnHomeFragmentInteractionListener : FragmentInteractionListener

    private val binding: FragmentHomeBinding by viewBinding()
    private lateinit var recyclerView: RecyclerView
    private lateinit var homePresenter: HomePresenter
    private lateinit var loadingDialog: AlertDialog
    lateinit var listener: OnHomeFragmentInteractionListener

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadView()
    }

    private fun loadView() {
        homePresenter = HomePresenter(this)
        loadingDialog = loadingSpotsDialog(requireContext())
        showLoadingDialog(loadingDialog)
        homePresenter.loadOrders()
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(
                true
            ) {
                override fun handleOnBackPressed() {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            })
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnHomeFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(requireContext().toString() + " must implement OnHomeFragmentInteractionListener")
        }
    }

    override fun onStart() {
        super.onStart()
        listener.setBottomNavigation(true, R.id.navigation_home)
        listener.setToolbarTitle(resources.getString(R.string.title_home))
    }

    override fun onDestroy() {
        homePresenter.onDestroy()
        super.onDestroy()
    }

    override fun onSuccess(dataset: List<Order>) {
        recyclerView = binding.stockRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = HomeAdapter(dataset)
        dismissLoadingDialog(loadingDialog)
    }

    override fun onError(error: String) {
        dismissLoadingDialog(loadingDialog)
    }
}