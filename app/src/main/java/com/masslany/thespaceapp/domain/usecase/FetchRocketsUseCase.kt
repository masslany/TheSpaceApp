package com.masslany.thespaceapp.domain.usecase

import com.masslany.thespaceapp.domain.model.RocketEntity
import com.masslany.thespaceapp.domain.repository.RocketsRepository
import com.masslany.thespaceapp.utils.Result
import javax.inject.Inject

class FetchRocketsUseCase @Inject constructor(
    private val rocketsRepository: RocketsRepository
) {
    suspend fun execute(): Result<List<RocketEntity>> {
        return rocketsRepository.fetchRocketsData()
    }
}