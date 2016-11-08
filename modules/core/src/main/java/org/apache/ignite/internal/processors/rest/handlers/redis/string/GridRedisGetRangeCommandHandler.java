/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.rest.handlers.redis.string;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.rest.GridRestProtocolHandler;
import org.apache.ignite.internal.processors.rest.GridRestResponse;
import org.apache.ignite.internal.processors.rest.handlers.redis.GridRedisThruRestCommandHandler;
import org.apache.ignite.internal.processors.rest.handlers.redis.exception.GridRedisGenericException;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisCommand;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisMessage;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisProtocolParser;
import org.apache.ignite.internal.processors.rest.request.GridRestCacheRequest;
import org.apache.ignite.internal.processors.rest.request.GridRestRequest;
import org.apache.ignite.internal.util.typedef.internal.U;

import static org.apache.ignite.internal.processors.rest.GridRestCommand.CACHE_GET;
import static org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisCommand.GETRANGE;

/**
 * Redis SETRANGE command handler.
 */
public class GridRedisGetRangeCommandHandler extends GridRedisThruRestCommandHandler {
    /** Supported commands. */
    private static final Collection<GridRedisCommand> SUPPORTED_COMMANDS = U.sealList(
        GETRANGE
    );

    /** Start offset position in Redis message parameters. */
    private static final int START_OFFSET_POS = 1;

    /** End offset position in Redis message parameters. */
    private static final int END_OFFSET_POS = 2;

    /** {@inheritDoc} */
    public GridRedisGetRangeCommandHandler(final GridRestProtocolHandler hnd) {
        super(hnd);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridRedisCommand> supportedCommands() {
        return SUPPORTED_COMMANDS;
    }

    /** {@inheritDoc} */
    @Override public GridRestRequest asRestRequest(GridRedisMessage msg) throws IgniteCheckedException {
        assert msg != null;

        if (msg.messageSize() < 4)
            throw new GridRedisGenericException("Wrong number of arguments");

        GridRestCacheRequest getReq = new GridRestCacheRequest();

        getReq.clientId(msg.clientId());
        getReq.key(msg.key());
        getReq.command(CACHE_GET);

        return getReq;
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer makeResponse(final GridRestResponse restRes, List<String> params) {
        if (restRes.getResponse() == null)
            return GridRedisProtocolParser.toBulkString("");
        else {
            String res = String.valueOf(restRes.getResponse());
            int startOffset;
            int endOffset;

            try {
                startOffset = boundedStartOffset(Integer.parseInt(params.get(START_OFFSET_POS)), res.length());
                endOffset = boundedEndOffset(Integer.parseInt(params.get(END_OFFSET_POS)), res.length());
            }
            catch (NumberFormatException ignored) {
                return GridRedisProtocolParser.toGenericError("Offset is not an integer!");
            }

            return GridRedisProtocolParser.toBulkString(res.substring(startOffset, endOffset));
        }
    }

    /**
     * @param idx Index.
     * @param size Bounds.
     * @return Offset within the bounds.
     */
    private int boundedStartOffset(int idx, int size) {
        return idx >= 0 ? Math.min(idx, size) : size + idx;
    }

    /**
     * @param idx Index.
     * @param size Bounds.
     * @return Offset within the bounds.
     */
    private int boundedEndOffset(int idx, int size) {
        return idx >= 0 ? Math.min(idx + 1, size) : size + idx + 1;
    }
}
