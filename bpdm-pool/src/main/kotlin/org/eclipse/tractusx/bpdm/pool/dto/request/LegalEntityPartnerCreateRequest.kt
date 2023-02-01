/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.dto.request

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto

@JsonDeserialize(using = LegalEntityPartnerCreateRequest.CustomDeserializer::class)
@Schema(name = "LegalEntityPartnerCreateRequest", description = "Request for creating new business partner record of type legal entity")
data class LegalEntityPartnerCreateRequest(
    @field:JsonUnwrapped
    val properties: LegalEntityDto,
    @Schema(description = "User defined index to conveniently match this entry to the corresponding entry in the response")
    val index: String?
) {
    class CustomDeserializer(vc: Class<LegalEntityPartnerCreateRequest>?) : StdDeserializer<LegalEntityPartnerCreateRequest>(vc) {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): LegalEntityPartnerCreateRequest {
            val node = parser.codec.readTree<JsonNode>(parser)
            return LegalEntityPartnerCreateRequest(
                ctxt.readTreeAsValue(node, LegalEntityDto::class.java),
                node.get(LegalEntityPartnerCreateRequest::index.name)?.textValue(),
            )
        }
    }
}

