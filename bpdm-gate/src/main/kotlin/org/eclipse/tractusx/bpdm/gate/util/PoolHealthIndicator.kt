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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component("poolHealth")
class PoolHealthIndicator(
    private val poolClient: PoolApiClient,
) : HealthIndicator {

    override fun health(): Health {

        return try {
            /*
            We can directly use actuator heath response from Pool service but that will not be an authenticated way.
            So, included changelog request to achieve the same for now and in future we can create separate REST api endpoint which will provide
            health of the service with readiness in authenticated way.
            */
            val response = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(), PaginationRequest(page = 0, size = 1))
            if (response.contentSize >= 0) {
                Health.up().withDetail("Pool Service", "Available").build()
            } else {
                Health.down().withDetail("Pool Service", "Unreachable").build()
            }
        } catch (e: Exception) {
            Health.down().withDetail("Pool Service", "Error: ${e.message}").build()
        }
    }
}
