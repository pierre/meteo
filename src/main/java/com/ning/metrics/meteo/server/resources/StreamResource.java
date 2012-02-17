/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.meteo.server.resources;

import com.google.common.cache.Cache;
import com.google.inject.Singleton;
import com.ning.metrics.meteo.binder.StreamConfig;
import com.ning.metrics.meteo.publishers.PublishersCompiler;
import com.ning.metrics.meteo.publishers.ResourceListener;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONPObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Path("/rest/1.0")
public class StreamResource
{
    private static final Logger log = LoggerFactory.getLogger(StreamResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ResourceListener resourceListener;
    private final PublishersCompiler compiler;

    @Inject
    public StreamResource(final PublishersCompiler compiler)
    {
        this.compiler = compiler;
        this.resourceListener = (ResourceListener) this.compiler.getPublisherInstances().get(ResourceListener.class.getName());
    }

    /**
     * Get the data points associated with a field in an Esper query
     *
     * @param callback  Javascript callback
     * @param attribute the SQL alias of an Esper query
     * @return jsonp representation of the data points in memory
     */
    @GET
    @Path("/{attribute}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSamplesByRoute(@QueryParam("callback") @DefaultValue("callback") final String callback,
                                      @PathParam("attribute") final String attribute)
    {
        final Map<String, Cache<Object, Object>> samples = resourceListener.getSamplesCache();
        return buildJsonpResponse(attribute, samples.get(attribute), callback);
    }

    /**
     * Add a new stream
     *
     * @param streamConfig the new stream configuration
     * @return 201 Created on success, 500 on error (exception in the 199 Warning header)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addStream(final StreamConfig streamConfig)
    {
        try {
            compiler.addStream(streamConfig);
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header("Warning", "199 " + e.toString()).build();
        }

        return Response.status(Response.Status.CREATED).build();
    }

    private Response buildJsonpResponse(final String attribute, final Cache<Object, Object> samples, final String callback)
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("attribute");
            generator.writeString(attribute);

            generator.writeFieldName("samples");
            generator.writeStartArray();

            if (samples != null) {
                final ConcurrentMap<Object, Object> samplesForType = samples.asMap();

                final List<DateTime> timestamps = new ArrayList<DateTime>();
                for (final Object timestamp : samplesForType.keySet()) {
                    timestamps.add((DateTime) timestamp);
                }
                Collections.sort(timestamps);

                for (final DateTime timestamp : timestamps) {
                    final Object dataPoint = samplesForType.get(timestamp);
                    // Might have been evicted already
                    if (dataPoint != null) {
                        generator.writeNumber(unixSeconds(timestamp));
                        generator.writeObject(dataPoint);
                    }
                }
            }

            generator.writeEndArray();

            generator.writeEndObject();
            generator.close();

            final JSONPObject object = new JSONPObject(callback, out.toString());
            return Response.ok(object).build();
        }
        catch (IOException e) {
            log.error("Error", e);
            return Response.serverError().build();
        }
    }

    public static int unixSeconds(final DateTime dateTime)
    {
        final long millis = dateTime.toDateTime(DateTimeZone.UTC).getMillis();
        return (int) (millis / 1000L);
    }
}