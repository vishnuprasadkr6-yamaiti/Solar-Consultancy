package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.SolarRepository
import com.example.ui.screens.CreateQuoteViewModel
import com.example.ui.screens.CustomerDetailViewModel
import com.example.ui.screens.CustomersViewModel
import com.example.ui.screens.DashboardViewModel
import com.example.ui.screens.SettingsViewModel

class AppViewModelFactory(
    private val repository: SolarRepository,
    private val customerId: String? = null,
    private val projectId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(CustomersViewModel::class.java) ->
                CustomersViewModel(repository) as T
            modelClass.isAssignableFrom(CustomerDetailViewModel::class.java) ->
                CustomerDetailViewModel(
                    customerId = requireNotNull(customerId) { "customerId required for CustomerDetailViewModel" },
                    repository = repository
                ) as T
            modelClass.isAssignableFrom(CreateQuoteViewModel::class.java) ->
                CreateQuoteViewModel(
                    projectId = requireNotNull(projectId) { "projectId required for CreateQuoteViewModel" },
                    repository = repository
                ) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
