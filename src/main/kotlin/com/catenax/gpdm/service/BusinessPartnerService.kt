package com.catenax.gpdm.service

import com.catenax.gpdm.dto.ChangelogEntryDto
import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.entity.ChangelogType
import com.catenax.gpdm.entity.IdentifierStatus
import com.catenax.gpdm.entity.IdentifierType
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import com.catenax.gpdm.repository.IdentifierStatusRepository
import com.catenax.gpdm.repository.IdentifierTypeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    val requestConversionService: RequestConversionService,
    val persistenceService: PersistenceService,
    val businessPartnerRepository: BusinessPartnerRepository,
    val identifierTypeRepository: IdentifierTypeRepository,
    val identifierStatusRepository: IdentifierStatusRepository,
    val partnerChangelogService: PartnerChangelogService
        ){

    @Transactional
    fun findPartner(bpn: String): BusinessPartnerResponse {
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

    @Transactional
    fun findPartnerByIdentifier(identifierType: String, identifierValue: String): BusinessPartnerResponse {
        val type = identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class, identifierType)
        return businessPartnerRepository.findByIdentifierTypeAndValue(type, identifierValue)?.toDto()
            ?: throw BpdmNotFoundException("Identifier Value", identifierValue)
    }

    @Transactional
    fun findPartnersByIdentifier(identifierType: String, identifierValues: Collection<String>): Collection<BusinessPartnerResponse> {
       return businessPartnerRepository.findByIdentifierTypeAndValues(identifierType, identifierValues).map { it.toDto() }
    }

    @Transactional
    fun findPartnersByIdentifier(typeKey: String, statusKey: String, pageable: Pageable = Pageable.unpaged()): Page<BusinessPartnerResponse> {
        val type = identifierTypeRepository.findByTechnicalKey(typeKey) ?: throw BpdmNotFoundException(IdentifierType::class, typeKey)
        val status = identifierStatusRepository.findByTechnicalKey(statusKey) ?: throw BpdmNotFoundException(IdentifierStatus::class, statusKey)

        return businessPartnerRepository.findByIdentifierTypeAndStatus(type, status, pageable).map { it.toDto() }
    }



    @Transactional
    fun createPartners(bpDtos: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse>{
        val bpEntities = requestConversionService.buildBusinessPartners(bpDtos)
        persistenceService.saveAll(bpEntities)
        partnerChangelogService.createChangelogEntries(bpEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE) })
        return bpEntities.map { it.toDto() }
    }


}