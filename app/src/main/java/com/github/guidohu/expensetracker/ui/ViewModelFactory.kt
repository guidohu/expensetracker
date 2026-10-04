package com.github.guidohu.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Simple factory wrapping a creation lambda — avoids pulling in a DI framework. */
class SimpleViewModelFactory(
    private val create: () -> ViewModel,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
