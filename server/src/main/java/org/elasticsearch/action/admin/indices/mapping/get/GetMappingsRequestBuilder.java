/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.action.admin.indices.mapping.get;

import org.elasticsearch.action.support.master.info.ClusterInfoRequestBuilder;
import org.elasticsearch.client.internal.ElasticsearchClient;
import org.elasticsearch.core.TimeValue;

public class GetMappingsRequestBuilder extends ClusterInfoRequestBuilder<
    GetMappingsRequest,
    GetMappingsResponse,
    GetMappingsRequestBuilder> {

    public GetMappingsRequestBuilder(ElasticsearchClient client, TimeValue masterTimeout, String... indices) {
        super(client, GetMappingsAction.INSTANCE, new GetMappingsRequest(masterTimeout).indices(indices));
    }
}
