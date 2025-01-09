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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestDataConfiguration {

    @Bean
    fun testMetadata(poolClient: PoolApiClient): TestMetadata {
        val testMetadata = TestMetadata(
            identifierTypes = listOf("EU_VAT_ID_DE", "DUNS_ID"),
            legalForms = listOf("SCE1", "SGST"),
            adminAreas = listOf("DE-BW", "DE-BY")
        )

        return testMetadata
    }

    @Bean
    fun gateTestDataFactory(testMetadata: TestMetadata): GateInputFactory {
        return GateInputFactory(testMetadata, null)
    }

}