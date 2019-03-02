package com.sheraz.listrepos.data.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sheraz.listrepos.data.db.entity.GitHubRepoEntity
import com.sheraz.listrepos.utils.Logger
import java.io.IOException

class GitHubNetworkDataSourceImpl(
    private val gitHubApiService: GitHubApiService
) : GitHubNetworkDataSource {


    init {
        Logger.d(TAG, "init(): ")
    }

    /**
     * Encapsulating Mutable value so that it can only be changed within the "fetchGitHubRepos()" method
     */
    private val _downloadedGitHubRepoList = MutableLiveData<Result<List<GitHubRepoEntity>>>()
    override val downloadedGitHubRepoList: LiveData<Result<List<GitHubRepoEntity>>>
        get() = _downloadedGitHubRepoList


    /**
     * Method to get Repos using gitHubApiService and post
     * the received list of repos on the MutableLiveData
     * Since, we are doing IO operations with API
     * service (GitHubApiService) using Retrofit, this method
     * be a suspend function and called from a coroutine with
     * IO Dispatcher.
     */
    override suspend fun fetchGitHubRepos(page: Int, per_page: Int) {

        Logger.d(TAG, "fetchGitHubRepos(): page: $page, per_page: $per_page")

        try {

            val response = gitHubApiService
                .getReposWithPageAsync(page, per_page)
                .await()

            Logger.v(TAG, "fetchGitHubRepos(): response: $response")

            if (response.isSuccessful){
                // MutableLiveData.postValue will post a task on
                // main thread to set the given value
                // We cannot use MutableLiveData.setValue here
                _downloadedGitHubRepoList.postValue(Result.success(response.body()!!))
            } else {
                _downloadedGitHubRepoList.postValue(Result.failure(IOException(response.message())))
            }

        } catch (e: Exception) {
            Logger.e(TAG, "fetchGitHubRepos(): Exception occurred, Error => " + e.message)
            _downloadedGitHubRepoList.postValue(Result.failure(e))
        }
    }


    /**
     * Companion object, common to all instances of this class
     * Similar to static fields in Java
     */
    companion object {
        private val TAG: String = GitHubNetworkDataSourceImpl::class.java.simpleName
    }
}