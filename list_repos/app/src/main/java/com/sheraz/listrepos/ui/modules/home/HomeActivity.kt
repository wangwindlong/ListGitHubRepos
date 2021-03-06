package com.sheraz.listrepos.ui.modules.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import com.sheraz.listrepos.BR
import com.sheraz.listrepos.Injector
import com.sheraz.listrepos.R
import com.sheraz.listrepos.databinding.ActivityHomeBinding
import com.sheraz.listrepos.internal.bindViewModel
import com.sheraz.listrepos.ui.models.GitHubRepoItem
import com.sheraz.listrepos.ui.modules.adapters.HomeAdapter
import com.sheraz.listrepos.ui.modules.base.BaseActivity
import com.sheraz.listrepos.utils.Logger
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>() {

    private lateinit var activityHomeBinding: ActivityHomeBinding
    private val homeAdapter: HomeAdapter
    private val homeViewModel by bindViewModel<HomeViewModel>(viewModelFactory)

    private var smoothScrollNeeded = false

    init {

        logger.d(TAG, "init(): ")
        homeAdapter = Injector.get().homeAdapter()

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        logger.d(TAG, "onCreate(): ")
        super.onCreate(savedInstanceState)
        activityHomeBinding = getViewDataBinding()
        initUI()
        setUpListeners()
        subscribeUi()

    }

    override fun initUI() {

        logger.d(TAG, "initUI(): ")

        setUpActionBar()
        fab.hide()
        rvGitHubRepoList.layoutManager = LinearLayoutManager(this)
        rvGitHubRepoList.adapter = homeAdapter

    }

    private fun setUpActionBar() {

        logger.d(TAG, "setUpActionBar(): ")

        setSupportActionBar(toolbar)

        // For the immersive-window behavior, we do the following
        // toolbar gets cut in half due to "windowTranslucentStatus = true" &
        // "windowTranslucentNavigation = true" set in HomeActivityTheme,
        // so we have to handle insets ourselves
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {

            toolbar.setOnApplyWindowInsetsListener { v, insets ->

                // inset the toolbar down by the status bar height
                val lpToolbar = v.layoutParams as ViewGroup.MarginLayoutParams
                lpToolbar.topMargin += insets.systemWindowInsetTop
                lpToolbar.leftMargin += insets.systemWindowInsetLeft
                lpToolbar.rightMargin += insets.systemWindowInsetRight
                v.layoutParams = lpToolbar

                // inset the fab for the navbar at the bottom
                val lpFab = fab.layoutParams as ViewGroup.MarginLayoutParams
                lpFab.bottomMargin += insets.systemWindowInsetBottom // portrait
                lpFab.rightMargin += insets.systemWindowInsetRight // landscape
                fab.layoutParams = lpFab

                // clear this listener so insets aren't re-applied
                v.setOnApplyWindowInsetsListener(null)

                insets.consumeSystemWindowInsets()
            }
        }

    }

    private fun setUpListeners() {

        logger.d(TAG, "setUpListeners(): ")

        homeAdapter.setListener(View.OnClickListener {
            if (it.tag != null) {
                openGoToUrlBottomSheet(it.tag as GitHubRepoItem)
            }
        })

        swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.onRefresh()
        }

        rvGitHubRepoList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // When user scrolls up, we show fab so user can easily scroll
                // to top/zero index of the list by just clicking this fab
                if (dy < 0 && !fab.isShown) {
                    fab.show()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // We need to hide this fab but only when user clicks on
                    // fab button, so we check for "smoothScrollNeeded" boolean
                    // if it's true then we know user clicked on fab
                    // and now if fab.isShown then hide the fab as recyclerView's
                    // scrollState == SCROLL_STATE_IDLE
                    if (smoothScrollNeeded && fab.isShown) {
                        fab.hide()
                        smoothScrollNeeded = false
                    }
                }

                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        fab.setOnClickListener {
            smoothScrollNeeded = true
            rvGitHubRepoList.smoothScrollToPosition(0).also { appBar.setExpanded(true, true) } }

    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_home
    }

    override fun getBindingVariable(): Int {
        return BR.homeViewModel
    }

    override fun getViewModel(): HomeViewModel {

        logger.d(TAG, "getViewModel(): ")
        return homeViewModel

    }

    override fun subscribeUi() {

        logger.d(TAG, "subscribeUi(): ")

        homeViewModel.getPagedListAsLiveData().observe(this, Observer { pagedList ->
            logger.i(TAG, "pagedList.Observer(): pagedList.size: ${pagedList?.size}")
            logger.i(TAG, "pagedList.Observer(): pagedList: ${pagedList?.toString()}")
            submitList(pagedList, false)
        })

        homeViewModel.getLoadingLiveData().observe(this, Observer { isFetchInProgress ->
            logger.d(TAG, "loading.Observer(): isFetchInProgress: $isFetchInProgress")
            handleFetchInProgress(isFetchInProgress)
        })

        homeViewModel.getNetworkErrorLiveData().observe(this, Observer { exception ->
            logger.d(TAG, "networkError.Observer(): exception: $exception")
            handleNetworkError(exception)
        })

    }

    override fun onChooseUrl(chosenUrl: String) {

        logger.d(TAG, "onChooseUrl(): chosenUrl: $chosenUrl")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(chosenUrl)))
        } catch (e: Exception) {
            logger.e(TAG, "onChooseUrl(): chosenUrl: $chosenUrl, Exception occurred while parsing, Error => ${e.message}")
        }

    }

    private fun submitList(pagedList: PagedList<GitHubRepoItem>?, isRefreshing: Boolean) {

        logger.d(TAG, "submitList(): pagedList: ${pagedList?.size}, isRefreshing: $isRefreshing")
        homeAdapter.submitList(pagedList)
        swipeRefreshLayout.isRefreshing = isRefreshing

    }

    private fun handleFetchInProgress(isFetchInProgress: Boolean) {

        logger.d(TAG, "handleFetchInProgress(): isFetchInProgress: $isFetchInProgress")
        homeViewModel.setIsLoading(isFetchInProgress)
        swipeRefreshLayout.isRefreshing = false

    }

    private fun handleNetworkError(exception: Exception) {

        logger.d(TAG, "handleNetworkError(): exception: $exception")
        swipeRefreshLayout.isRefreshing = false
        Snackbar.make(activityHomeBinding.root, exception.message.toString(), LENGTH_LONG).show()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.clearLocalDbCache -> homeViewModel.onClearCache()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = HomeActivity::class.java.simpleName
    }
}
