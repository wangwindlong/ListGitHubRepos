package com.sheraz.listrepos.ui.modules.home

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sheraz.listrepos.BR
import com.sheraz.listrepos.Injector
import com.sheraz.listrepos.R
import com.sheraz.listrepos.databinding.ActivityHomeBinding
import com.sheraz.listrepos.ui.modules.adapters.HomeAdapter
import com.sheraz.listrepos.ui.modules.base.BaseActivity
import com.sheraz.listrepos.utils.Logger
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var activityHomeBinding: ActivityHomeBinding

    private val viewModelFactory: ViewModelProvider.Factory
    private val homeAdapter: HomeAdapter


    init {

        Logger.d(TAG, "init(): ")
        viewModelFactory = Injector.get().viewModelFactory()
        homeAdapter = Injector.get().homeAdapter()

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        Logger.d(TAG, "onCreate(): ")
        super.onCreate(savedInstanceState)
        activityHomeBinding = getViewDataBinding()
        initUI()
        initAdapter()
        subscribeUi()

    }

    override fun initUI() {

        Logger.d(TAG, "initUI(): ")
        rvGitHubRepoList.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout.setOnRefreshListener {
            homeAdapter.submitList(null)
            homeViewModel.onRefresh()
        }

    }

    private fun initAdapter() {

        rvGitHubRepoList.adapter = homeAdapter

    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_home
    }

    override fun getBindingVariable(): Int {
        return BR.homeViewModel
    }

    override fun getViewModel(): HomeViewModel {

        Logger.d(TAG, "getViewModel(): ")
        homeViewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)
        return homeViewModel

    }

    override fun subscribeUi() {

        Logger.d(TAG, "subscribeUi(): ")
        homeViewModel.getPagedListAsLiveData().observe(this, Observer { pagedList ->

            if (pagedList != null) {

                Logger.i(TAG, "pagedList.Observer(): pagedList.size: ${pagedList.size}")
                Logger.i(TAG, "pagedList.Observer(): pagedList: $pagedList")
            }

            homeAdapter.submitList(pagedList)
            swipeRefreshLayout.isRefreshing = false
        })

        homeViewModel.getLoadingLiveData().observe(this, Observer { isFetchInProgress ->
            Logger.d(TAG, "loading.Observer(): isFetchInProgress: $isFetchInProgress")
            homeViewModel.setIsLoading(isFetchInProgress)
        })

    }


    companion object {
        private val TAG = HomeActivity::class.java.simpleName
    }
}
