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

package org.eclipse.tractusx.bpdm.gate.auth

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.*
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Paths

abstract class AuthTestBase(
    private val gateClient: GateClient,
    private val authAssertions: AuthAssertionHelper,
    private val authExpectations: GateAuthExpectations
) {
    @Test
    fun `GET Partner Input`() {
        authAssertions.assert(authExpectations.businessPartner.getInput) { gateClient.businessParters.getBusinessPartnersInput() }
    }

    @Test
    fun `GET Partner Output`() {
        authAssertions.assert(authExpectations.businessPartner.getOutput) { gateClient.businessParters.getBusinessPartnersOutput() }
    }

    @Test
    fun `PUT Partner Input`() {
        authAssertions.assert(authExpectations.businessPartner.putInput) { gateClient.businessParters.upsertBusinessPartnersInput(listOf(
            BusinessPartnerInputRequest("externalId")
        )) }
    }

    @Test
    fun `GET Changelog Input`() {
        authAssertions.assert(authExpectations.changelog.getInput) { gateClient.changelog.getInputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    @Test
    fun `GET Changelog Output`() {
        authAssertions.assert(authExpectations.changelog.getOutput) { gateClient.changelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest()) }
    }

    @Test
    fun `GET Sharing State`() {
        authAssertions.assert(authExpectations.sharingState.getSharingState) { gateClient.sharingState.getSharingStates(PaginationRequest(), null) }
    }

    @Test
    fun `POST Sharing State Ready`() {
        authAssertions.assert(authExpectations.sharingState.postReady) { gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(emptyList())) }
    }

    @Test
    fun `GET Sharing State Stats`() {
        authAssertions.assert(authExpectations.stats.getSharingState) { gateClient.stats.countPartnersBySharingState() }
    }

    @Test
    fun `GET Stage Stats`() {
        authAssertions.assert(authExpectations.stats.getStage) { gateClient.stats.countPartnersPerStage() }
    }

    @Test
    fun `GET Address Type Stats`() {
        authAssertions.assert(authExpectations.stats.getAddressType) { gateClient.stats.countAddressTypes(StageType.Input) }
    }

    @Test
    fun `GET Confidence Criteria Stats`() {
        authAssertions.assert(authExpectations.stats.getConfidenceCriteria) { gateClient.stats.getConfidenceCriteriaStats() }
    }

    @Test
    fun `POST Partner Input`() {
        val bytes = Files.readAllBytes(Paths.get("src/test/resources/testData/valid_partner_data.csv"))
        val uploadedFile = MockMultipartFile("valid_partner_data.csv", "valid_partner_data.csv", "text/csv", bytes)
        authAssertions.assert(authExpectations.uploadPartner.postInput) { gateClient.partnerUpload.uploadPartnerCsvFile(uploadedFile) }
    }

    @Test
    fun `GET Partner Upload Template`() {
        authAssertions.assert(authExpectations.uploadPartner.getInputTemplate) { gateClient.partnerUpload.getPartnerCsvTemplate() }
    }

    @Test
    fun `GET Relations`() {
        authAssertions.assert(authExpectations.relation.get) { gateClient.relation.get() }
    }

    @Test
    fun `POST Relations`() {
        authAssertions.assert(authExpectations.relation.post) { gateClient.relation.post(RelationPostRequest(null, RelationType.IsManagedBy, "", "")) }
    }

    @Test
    fun `PUT Relations`() {
        authAssertions.assert(authExpectations.relation.put) { gateClient.relation.put(true, RelationPutRequest("", RelationType.IsManagedBy, "", "")) }
    }

    @Test
    fun `DELETE Relations`() {
        authAssertions.assert(authExpectations.relation.delete) { gateClient.relation.delete( "") }
    }

}

data class GateAuthExpectations(
    val businessPartner: BusinessPartnerAuthExpectations,
    val changelog: ChangelogAuthExpectations,
    val sharingState: SharingStateAuthExpectations,
    val stats: StatsAuthExpectations,
    val uploadPartner: UploadPartnerAuthExpections,
    val relation: RelationAuthExpectations
)

data class BusinessPartnerAuthExpectations(
    val getInput: AuthExpectationType,
    val getOutput: AuthExpectationType,
    val putInput: AuthExpectationType
)

data class ChangelogAuthExpectations(
    val getInput: AuthExpectationType,
    val getOutput: AuthExpectationType
)

data class SharingStateAuthExpectations(
    val getSharingState: AuthExpectationType,
    val postReady: AuthExpectationType
)

data class StatsAuthExpectations(
    val getSharingState: AuthExpectationType,
    val getStage: AuthExpectationType,
    val getAddressType: AuthExpectationType,
    val getConfidenceCriteria: AuthExpectationType
)

data class UploadPartnerAuthExpections(
    val postInput: AuthExpectationType,
    val getInputTemplate: AuthExpectationType
)

data class RelationAuthExpectations(
    val get: AuthExpectationType,
    val post: AuthExpectationType,
    val put: AuthExpectationType,
    val delete: AuthExpectationType
)
