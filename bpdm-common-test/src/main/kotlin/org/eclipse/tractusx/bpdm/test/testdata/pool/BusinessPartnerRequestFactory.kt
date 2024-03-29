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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random


/**
 * This class provides functions for generating business partner requests
 * Since business partner data is quite complex its creation is centrally contained in this class
 * Other request data should be handled directly inside the test classes
 */
class BusinessPartnerRequestFactory(
    availableMetadata: TestMetadata
) {
    private val availableLegalForms = availableMetadata.legalForms.map { it.technicalKey }
    private val availableLegalEntityIdentifiers = availableMetadata.legalEntityIdentifierTypes.map { it.technicalKey }
    private val availableAddressIdentifiers = availableMetadata.addressIdentifierTypes.map { it.technicalKey }
    private val availableAdminAreas = availableMetadata.adminAreas.map { it.code }

    fun createLegalEntityRequest(
        seed: String,
        isCatenaXMemberData: Boolean = true
    ): LegalEntityPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityPartnerCreateRequest(
            legalEntity = LegalEntityDto(
                legalName = "Legal Name $seed",
                legalShortName = "Legal Short Name $seed",
                legalForm = availableLegalForms.random(random),
                identifiers = listOf(availableLegalEntityIdentifiers.random(random), availableLegalEntityIdentifiers.random(random))
                    .mapIndexed { index, idKey -> LegalEntityIdentifierDto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed") },
                states = listOf(
                    LegalEntityStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                    LegalEntityStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
                ),
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = true,
                    checkedByExternalDataSource = false,
                    numberOfSharingMembers = 1,
                    lastConfidenceCheckAt = timeStamp,
                    nextConfidenceCheckAt = timeStamp.plusDays(7),
                    confidenceLevel = 5
                ),
                isCatenaXMemberData = isCatenaXMemberData
            ),
            legalAddress = createAddressDto(seed, random),
            index = seed
        )
    }

    fun createSiteRequest(seed: String, bpnlParent: String): SitePartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return SitePartnerCreateRequest(
            bpnlParent = bpnlParent,
            index = seed,
            site = SiteDto(
                name = "Site Name $seed",
                states = listOf(
                    SiteStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                    SiteStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
                ),
                mainAddress = createAddressDto(seed, random),
                confidenceCriteria = ConfidenceCriteriaDto(
                    sharedByOwner = true,
                    checkedByExternalDataSource = false,
                    numberOfSharingMembers = 2,
                    lastConfidenceCheckAt = timeStamp.plusDays(10),
                    nextConfidenceCheckAt = timeStamp.plusDays(20),
                    confidenceLevel = 4
                )
            )
        )
    }

    fun createAddressRequest(seed: String, bpnParent: String): AddressPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return AddressPartnerCreateRequest(
            bpnParent = bpnParent,
            index = seed,
            address = createAddressDto(seed, random)
        )
    }

    private fun createAddressDto(seed: String, random: Random): LogisticAddressDto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)
        return LogisticAddressDto(
            name = "Address Name $seed",
            states = listOf(
                AddressStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                AddressStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            identifiers = listOf(availableAddressIdentifiers.random(random), availableAddressIdentifiers.random(random))
                .mapIndexed { index, idKey -> AddressIdentifierDto("$idKey Value $seed $index", idKey) },
            physicalPostalAddress = PhysicalPostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextFloat(), latitude = random.nextFloat(), altitude = random.nextFloat()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.random(random),
                administrativeAreaLevel2 = "Admin Level 2 $seed",
                administrativeAreaLevel3 = "Admin Level 3 $seed",
                postalCode = "Postal Code $seed",
                city = "City $seed",
                district = "District $seed",
                street = StreetDto(
                    name = "Street Name $seed",
                    houseNumber = "House Number $seed",
                    houseNumberSupplement = "House Number Supplement $seed",
                    milestone = "Milestone $seed",
                    direction = "Direction $seed",
                    namePrefix = "Name Prefix $seed",
                    nameSuffix = "Name Suffix $seed",
                    additionalNamePrefix = "Additional Name Prefix $seed",
                    additionalNameSuffix = "Additional Name Suffix $seed"
                ),
                companyPostalCode = "Company Postal Code $seed",
                industrialZone = "Industrial Zone $seed",
                building = "Building $seed",
                floor = "Floor $seed",
                door = "Door $seed"
            ),
            alternativePostalAddress = AlternativePostalAddressDto(
                geographicCoordinates = GeoCoordinateDto(longitude = random.nextFloat(), latitude = random.nextFloat(), altitude = random.nextFloat()),
                country = CountryCode.entries.random(random),
                administrativeAreaLevel1 = availableAdminAreas.random(random),
                postalCode = "Postal Code $seed",
                city = "City $seed",
                deliveryServiceNumber = "Delivery Service Number $seed",
                deliveryServiceType = DeliveryServiceType.entries.random(random),
                deliveryServiceQualifier = "Delivery Service Qualifier $seed"
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 2,
                lastConfidenceCheckAt = timeStamp.plusDays(10),
                nextConfidenceCheckAt = timeStamp.plusDays(20),
                confidenceLevel = 4
            )
        )
    }
}

