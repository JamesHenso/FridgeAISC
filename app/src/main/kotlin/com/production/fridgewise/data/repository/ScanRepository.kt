package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.ApiService
import com.production.fridgewise.data.remote.dto.BulkUpdateRequest
import com.production.fridgewise.data.remote.dto.CommitResponse
import com.production.fridgewise.data.remote.dto.CreateScanRequest
import com.production.fridgewise.data.remote.dto.CreateScanResponse
import com.production.fridgewise.data.remote.dto.LocationAssignment
import com.production.fridgewise.data.remote.dto.LocationsRequest
import com.production.fridgewise.data.remote.dto.ScanItemRequest
import com.production.fridgewise.data.remote.dto.UpdateScanItemRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(private val api: ApiService) {
    suspend fun createScan(fridgeId: String, items: List<ScanItemRequest>): Result<CreateScanResponse> =
        runCatching { api.createScan(CreateScanRequest(fridgeId, items)) }
    suspend fun updateItems(scanId: String, items: List<UpdateScanItemRequest>): Result<Unit> =
        runCatching { api.updateScanItems(scanId, BulkUpdateRequest(items)); Unit }
    suspend fun updateLocations(scanId: String, items: List<LocationAssignment>): Result<Unit> =
        runCatching { api.updateScanLocations(scanId, LocationsRequest(items)); Unit }
    suspend fun commitScan(scanId: String): Result<CommitResponse> = runCatching { api.commitScan(scanId) }
}
