/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationOutputDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.exception.*
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RelationService(
    private val relationRepository: RelationRepository,
    private val relationStageRepository: RelationStageRepository,
    private val sharingStateRepository: SharingStateRepository,
    private val relationSharingStateService: RelationSharingStateService
): IRelationService {

    override fun findInputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String>,
        relationType: RelationType?,
        sourceBusinessPartnerExternalIds: List<String>,
        targetBusinessPartnerExternalIds: List<String>,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationDto>{

        val querySpecs = Specification.allOf(
            RelationStageRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationStageRepository.Specs.byStage(StageType.Input),
            RelationStageRepository.Specs.byExternalIds(externalIds),
            RelationStageRepository.Specs.byRelationshipType(relationType),
            RelationStageRepository.Specs.bySourceExternalIds(sourceBusinessPartnerExternalIds),
            RelationStageRepository.Specs.byTargetExternalIds(targetBusinessPartnerExternalIds),
            RelationStageRepository.Specs.byUpdatedAfter(updatedAtFrom)
        )

        return relationStageRepository.findAll(querySpecs, paginationRequest.toPageRequest())
            .toPageDto { toDto(it) }
    }

    override fun findOutputRelations(
        tenantBpnL: BpnLString,
        externalIds: List<String>,
        relationType: SharableRelationType?,
        sourceBpnLs: List<String>,
        targetBpnLs: List<String>,
        updatedAtFrom: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationOutputDto> {
        val querySpecs = Specification.allOf(
            RelationRepository.Specs.byOutputIsNotNull(),
            RelationRepository.Specs.byTenantBpnL(tenantBpnL.value),
            RelationRepository.Specs.byExternalIds(externalIds),
            RelationRepository.Specs.byOutputRelationType(relationType),
            RelationRepository.Specs.byOutputSourceBpnLs(sourceBpnLs),
            RelationRepository.Specs.byOutputTargetBpnLs(targetBpnLs),
            RelationRepository.Specs.byOutputUpdatedAfter(updatedAtFrom)
        )

        return relationRepository.findAll(querySpecs, paginationRequest.toPageRequest())
            .toPageDto { toOutputDto(it) }
    }

    @Transactional
    fun createInputRelations(
        tenantBpnL: BpnLString,
        externalId: String?,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        if (externalId != null) {
            val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId)
            if (existingRelationship != null) throw BpdmRelationAlreadyExistsException(externalId)
        }
        return toDto(createInputStage(tenantBpnL, externalId, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId))
    }

    @Transactional
    fun deleteRelation(tenantBpnL: BpnLString, externalId: String) {
        val relations = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        val relationStage = relationStageRepository.findByRelationAndStage(relations, stage = StageType.Input) ?: throw BpdmMissingRelationException(externalId)
        relationStageRepository.delete(relationStage)
        relationRepository.delete(relations)
    }

    @Transactional
    override fun upsertInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto> {
        return relations.map {
                with(it){
                    upsertInputStage(
                        tenantBpnL = tenantBpnL,
                        externalId = externalId,
                        relationType = relationType,
                        sourceBusinessPartnerExternalId = businessPartnerSourceExternalId,
                        targetBusinessPartnerExternalId = businessPartnerTargetExternalId
                    )
                }
            }
    }

    @Transactional
    override fun updateInputRelations(tenantBpnL: BpnLString, relations: List<RelationPutEntry>): List<RelationDto> {
        return relations.map {
                with(it){
                    updateInputStage(
                        tenantBpnL = tenantBpnL,
                        externalId = externalId,
                        relationType = relationType,
                        sourceBusinessPartnerExternalId = businessPartnerSourceExternalId,
                        targetBusinessPartnerExternalId = businessPartnerTargetExternalId
                    )
                }
            }.map(::toDto)
    }

    @Transactional
    override fun upsertOutputRelations(relations: List<IRelationService.RelationUpsertRequest>): List<RelationDb> {
        return relations.map { upsertOutput(it.relation, it.relationType, it.businessPartnerSourceExternalId, it.businessPartnerTargetExternalId) }
    }

    private fun upsertInputStage(
        tenantBpnL: BpnLString,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationDto {
        val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId)
        val upsertedRelationStage = if(existingRelationship == null)
            createInputStage(tenantBpnL, externalId, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId)
        else
            updateInputStage(existingRelationship, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId)

        return toDto(upsertedRelationStage)
    }

    private fun upsertOutput(
        relation: RelationDb,
        relationType: SharableRelationType,
        sourceBpnL: String,
        targetBpnL: String
    ): RelationDb{
        if(sourceBpnL == targetBpnL)
            throw BpdmInvalidRelationException("Source and target should not be the same")

        relation.output = RelationOutputDb(relationType, sourceBpnL, targetBpnL, Instant.now())
        relationSharingStateService.setSuccess(relation)

        return relationRepository.save(relation)
    }

    private fun createInputStage(
        tenantBpnL: BpnLString,
        externalId: String?,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        if(sourceBusinessPartnerExternalId == targetBusinessPartnerExternalId)
            throw BpdmInvalidRelationException("Source and target '$sourceBusinessPartnerExternalId' should not be equal.")

        val relation = RelationDb(
            externalId = externalId ?: UUID.randomUUID().toString(),
            tenantBpnL = tenantBpnL.value,
            sharingState = null,
            output = null
        )
        relationSharingStateService.setInitial(relation, relationType)

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val relationStage = RelationStageDb(
            relation = relation,
            relationType = relationType,
            stage = StageType.Input,
            source = source,
            target = target
        )

        relationStageRepository.save(relationStage)

        return relationStage
    }

    private fun updateInputStage(
        tenantBpnL: BpnLString,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        return updateInputStage(existingRelationship, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId)
    }

    private fun updateInputStage(
        relation: RelationDb,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        if(sourceBusinessPartnerExternalId == targetBusinessPartnerExternalId)
            throw BpdmInvalidRelationException("Source and target '$sourceBusinessPartnerExternalId' should not be equal.")

        val existingStage = relationStageRepository.findByRelationAndStage(relation, StageType.Input) ?: throw BpdmMissingRelationException(relation.externalId)

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val newValues = RelationUpdateComparison(relationType, source, target)
        val oldValues = RelationUpdateComparison(existingStage.relationType, existingStage.source, existingStage.target)
        val hasChanges = newValues != oldValues
        val isInErrorState = relation.sharingState?.sharingStateType == RelationSharingStateType.Error

        if(hasChanges){
            existingStage.relationType = relationType
            existingStage.source = source
            existingStage.target = target
            existingStage.updatedAt = Instant.now()

            relationStageRepository.save(existingStage)
        }

        if(hasChanges || isInErrorState){
            relationSharingStateService.setInitial(relation, relationType)
        }

        return existingStage
    }

    private fun createOutputStage(
        relation: RelationDb,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val relationStage = RelationStageDb(
            relation = relation,
            relationType = relationType,
            stage = StageType.Output,
            source = source,
            target = target
        )

        relationSharingStateService.setSuccess(relation)

        relationStageRepository.save(relationStage)
        return relationStage
    }

    private fun updateOutputStage(
        tenantBpnL: BpnLString,
        externalId: String,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        val existingRelationship = relationRepository.findByTenantBpnLAndExternalId(tenantBpnL.value, externalId) ?: throw BpdmMissingRelationException(externalId)
        val existingStage = relationStageRepository.findByRelationAndStage(existingRelationship, StageType.Input) ?: throw BpdmMissingRelationException(externalId)
        return updateOutputStage(existingStage, relationType, sourceBusinessPartnerExternalId, targetBusinessPartnerExternalId)
    }

    private fun updateOutputStage(
        relationStage: RelationStageDb,
        relationType: RelationType,
        sourceBusinessPartnerExternalId: String,
        targetBusinessPartnerExternalId: String
    ): RelationStageDb{
        val relation = relationStage.relation

        val source = sharingStateRepository.findByExternalIdAndTenantBpnl(sourceBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?: throw BpdmRelationSourceNotFoundException(sourceBusinessPartnerExternalId, relation.tenantBpnL)
        val target = sharingStateRepository.findByExternalIdAndTenantBpnl(targetBusinessPartnerExternalId, relation.tenantBpnL).singleOrNull()
            ?:  throw BpdmRelationTargetNotFoundException(targetBusinessPartnerExternalId, relation.tenantBpnL)

        val newValues = RelationUpdateComparison(relationType, source, target)
        val oldValues = RelationUpdateComparison(relationStage.relationType, relationStage.source, relationStage.target)
        val hasChanges = newValues == oldValues

        if(hasChanges){
            relationStage.relationType = relationType
            relationStage.source = source
            relationStage.target = target
            relationStage.updatedAt = Instant.now()

            relationStageRepository.save(relationStage)
        }

        relationSharingStateService.setSuccess(relation)

        return relationStage
    }


    private fun toDto(entity: RelationStageDb): RelationDto{
        return RelationDto(
            externalId = entity.relation.externalId,
            relationType = entity.relationType,
            businessPartnerSourceExternalId = entity.source.externalId,
            businessPartnerTargetExternalId = entity.target.externalId,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt
        )
    }

    private fun toOutputDto(entity: RelationDb): RelationOutputDto{
        return RelationOutputDto(
            externalId = entity.externalId,
            relationType = entity.output!!.relationType,
            sourceBpnL = entity.output!!.sourceBpnL,
            targetBpnL = entity.output!!.targetBpnL,
            updatedAt = entity.output!!.updatedAt
        )
    }

    data class RelationUpdateComparison(
        val relationType: RelationType,
        val source: SharingStateDb,
        val target: SharingStateDb
    )
}