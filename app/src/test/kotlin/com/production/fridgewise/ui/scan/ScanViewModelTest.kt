package com.production.fridgewise.ui.scan

import org.junit.Assert.*
import org.junit.Test

class ScanViewModelTest {
    @Test fun newScanClearsPreviousReviewSession() {
        val model = ScanViewModel()
        var cleared = false
        val review = object : androidx.lifecycle.ViewModel() {
            override fun onCleared() { cleared = true }
        }
        model.viewModelStore.put("review", review)
        assertSame(review, model.viewModelStore.get("review"))
        model.retake()
        assertTrue(cleared)
        assertNull(model.viewModelStore.get("review"))
    }

    @Test fun lateCaptureCannotRestorePhotoOrErrorAfterClosingAndReopening() {
        val model = ScanViewModel()
        model.beginCapture()
        val oldGeneration = model.captureGeneration
        model.retake()
        model.beginCapture()
        assertFalse(model.completeCapture(oldGeneration, "file:///old.jpg"))
        model.failCapture(oldGeneration, "Old error")
        assertNull(model.uiState.value.photoUri)
        assertNull(model.uiState.value.error)
        assertTrue(model.uiState.value.isCapturing)
        assertTrue(model.completeCapture(model.captureGeneration, "file:///new.jpg"))
        assertEquals("file:///new.jpg", model.uiState.value.photoUri)
    }
    @Test fun captureCannotBeSubmittedTwiceAndReviewResetsTorch() {
        val model = ScanViewModel()
        model.setTorch(true)
        assertTrue(model.beginCapture())
        assertFalse(model.beginCapture())
        model.showPhoto("file:///receipt.jpg")
        assertEquals("file:///receipt.jpg", model.uiState.value.photoUri)
        assertFalse(model.uiState.value.isCapturing)
        assertFalse(model.uiState.value.torch)
        assertFalse(model.beginCapture())
        model.retake()
        assertNull(model.uiState.value.photoUri)
        assertTrue(model.beginCapture())
    }
    @Test fun failedCaptureAllowsRetry() {
        val model = ScanViewModel()
        model.beginCapture()
        model.fail("Camera unavailable")
        assertFalse(model.uiState.value.isCapturing)
        assertEquals("Camera unavailable", model.uiState.value.error)
        assertTrue(model.beginCapture())
        assertNull(model.uiState.value.error)
    }
}
