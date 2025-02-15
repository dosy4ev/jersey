/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.tests.e2e.client;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests chunk encoding and possibility of buffering the entity.
 *
 * @author Miroslav Fuksa
 */
public class BufferingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class, LoggingFeature.class);
    }

    @Path("resource")
    public static class MyResource {
        @POST
        public String getBuffered(@HeaderParam("content-length") String contentLenght,
                                  @HeaderParam("transfer-encoding") String transferEncoding) {
            if (transferEncoding != null && transferEncoding.equals("chunked")) {
                return "chunked";
            }
            return contentLenght;
        }
    }

    @Test
    public void testApacheConnector() {
        testWithBuffering(getApacheConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getApacheConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getApacheConnectorConfig());
        testWithChunkEncodingPerRequest(getApacheConnectorConfig());
        testDefaultOption(getApacheConnectorConfig(), RequestEntityProcessing.CHUNKED);
    }

    @Test
    public void testApache5Connector() {
        testWithBuffering(getApache5ConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getApache5ConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getApache5ConnectorConfig());
        testWithChunkEncodingPerRequest(getApache5ConnectorConfig());
        testDefaultOption(getApache5ConnectorConfig(), RequestEntityProcessing.CHUNKED);
    }

    @Test
    public void testGrizzlyConnector() {
        testWithBuffering(getGrizzlyConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getGrizzlyConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getGrizzlyConnectorConfig());
        testWithChunkEncodingPerRequest(getGrizzlyConnectorConfig());
        testDefaultOption(getGrizzlyConnectorConfig(), RequestEntityProcessing.CHUNKED);
    }

    @Test
    public void testHttpUrlConnector() {
        testWithBuffering(getHttpUrlConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getHttpUrlConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getHttpUrlConnectorConfig());
        testWithChunkEncodingPerRequest(getHttpUrlConnectorConfig());
        testDefaultOption(getHttpUrlConnectorConfig(), RequestEntityProcessing.BUFFERED);
    }

    private ClientConfig getApacheConnectorConfig() {
        return new ClientConfig().connectorProvider(new ApacheConnectorProvider());
    }

    private ClientConfig getApache5ConnectorConfig() {
        return new ClientConfig().connectorProvider(new Apache5ConnectorProvider());
    }

    private ClientConfig getGrizzlyConnectorConfig() {
        return new ClientConfig().connectorProvider(new GrizzlyConnectorProvider());
    }

    private ClientConfig getHttpUrlConnectorConfig() {
        return new ClientConfig();
    }

    private void testDefaultOption(ClientConfig cc, RequestEntityProcessing mode) {
        String entity = getVeryLongString();
        makeRequest(cc, entity, mode == RequestEntityProcessing.BUFFERED ? String.valueOf(entity.length())
                : "chunked");
    }

    private void testWithBuffering(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
        String entity = getVeryLongString();

        makeRequest(cc, entity, String.valueOf(entity.length()));
    }

    private void makeRequest(ClientConfig cc, String entity, String expected) {
        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(UriBuilder.fromUri(getBaseUri()).path("resource").build());

        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(expected, response.readEntity(String.class));
    }

    private void testWithChunkEncodingWithPropertyDefinition(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        cc.property(ClientProperties.CHUNKED_ENCODING_SIZE, 3000);
        String entity = getVeryLongString();

        makeRequest(cc, entity, "chunked");
    }

    private void testWithChunkEncodingWithoutPropertyDefinition(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        String entity = getVeryLongString();

        makeRequest(cc, entity, "chunked");
    }


    /**
     * Tests that {@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING} can be defined
     * per request with different values.
     */
    private void testWithChunkEncodingPerRequest(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        cc.property(ClientProperties.CHUNKED_ENCODING_SIZE, 3000);
        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(UriBuilder.fromUri(getBaseUri()).path("resource").build());

        String entity = getVeryLongString();
        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("chunked", response.readEntity(String.class));

        response = target.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED)
                .request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(String.valueOf(entity.length()), response.readEntity(String.class));
    }

    public String getVeryLongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1; i++) {
            sb.append("helllllloooooooooooooooooooooooooooooouuuuuuuuuuu.");
        }
        return sb.toString();
    }
}
