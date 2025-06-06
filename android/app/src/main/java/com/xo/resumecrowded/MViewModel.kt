package com.xo.resumecrowded

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MViewModel : ViewModel() {
    private val _apiResponse = MutableStateFlow<String?>(null)
    val apiResponse: StateFlow<String?> = _apiResponse
    val isLoading = MutableStateFlow(false)

    fun uploadPdf(uri: Uri, context: Context) {
        isLoading.value = true
        viewModelScope.launch {
            val filePart = prepareFilePart(uri, context)
            val response = apiService.uploadPdf(filePart)

            if (response.isSuccessful) {
                val markdown = response.body()?.string()
                println("Markdown:\n$markdown")
                isLoading.value = false
                _apiResponse.value = markdown // ✅ Update StateFlow here
            } else {
                println("Upload failed: ${response.errorBody()?.string()}")
                isLoading.value = false
                _apiResponse.value = "Upload failed: ${response.errorBody()?.string()}"
            }
        }
    }
}