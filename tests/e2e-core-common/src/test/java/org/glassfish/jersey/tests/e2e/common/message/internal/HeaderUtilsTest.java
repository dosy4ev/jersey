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

package org.glassfish.jersey.tests.e2e.common.message.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.message.internal.HeaderUtils;
import org.glassfish.jersey.tests.e2e.common.TestRuntimeDelegate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link HeaderUtils} unit tests.
 *
 * @author Marek Potociar
 */
public class HeaderUtilsTest {

    public HeaderUtilsTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testCreateInbound() throws Exception {
        final MultivaluedMap<String, String> inbound = HeaderUtils.createInbound();
        assertNotNull(inbound);

        // Test mutability.
        inbound.putSingle("key", "value");
        assertEquals("value", inbound.getFirst("key"));
    }

    @Test
    public void testEmpty() throws Exception {
        final MultivaluedMap<String, String> emptyStrings = HeaderUtils.empty();
        assertNotNull(emptyStrings);

        final MultivaluedMap<String, Object> emptyObjects = HeaderUtils.empty();
        assertNotNull(emptyObjects);

        // Test immutability.
        try {
            emptyStrings.putSingle("key", "value");
            fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
        try {
            emptyObjects.putSingle("key", "value");
            fail("UnsupportedOperationException expected.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    @Test
    public void testCreateOutbound() throws Exception {
        final MultivaluedMap<String, Object> outbound = HeaderUtils.createOutbound();
        assertNotNull(outbound);

        // Test mutability.
        outbound.putSingle("key", "value");
        assertEquals("value", outbound.getFirst("key"));

        final Object value = new Object();
        outbound.putSingle("key", value);
        assertEquals(value, outbound.getFirst("key"));
    }

    @Test
    public void testAsString() throws Exception {
        assertNull(HeaderUtils.asString(null, null));

        final String value = "value";
        assertSame(value, HeaderUtils.asString(value, null));

        final URI uri = new URI("test");
        assertEquals(uri.toASCIIString(), HeaderUtils.asString(uri, null));
    }

    @Test
    public void testAsStringList() throws Exception {
        assertNotNull(HeaderUtils.asStringList(null, null));
        assertTrue(HeaderUtils.asStringList(null, null).isEmpty());

        final URI uri = new URI("test");
        final List<Object> values = new LinkedList<Object>() {{
            add("value");
            add(null);
            add(uri);
        }};

        // test string values
        final List<String> stringList = HeaderUtils.asStringList(values, null);
        assertEquals(Arrays.asList("value", "[null]", uri.toASCIIString()),
                     stringList);

        // tests live view
        values.add("value2");
        assertEquals(Arrays.asList("value", "[null]", uri.toASCIIString(), "value2"),
                     stringList);
        values.remove(1);
        assertEquals(Arrays.asList("value", uri.toASCIIString(), "value2"),
                     stringList);
    }

    @Test
    public void testAsStringHeaders() throws Exception {
        assertNull(HeaderUtils.asStringHeaders(null, null));

        final AbstractMultivaluedMap<String, Object> headers = HeaderUtils.createOutbound();

        headers.putSingle("k1", "value");
        headers.add("k1", "value2");

        final URI uri = new URI("test");
        headers.putSingle("k2", uri);

        headers.putSingle("k3", "value3");

        final MultivaluedMap<String, String> stringHeaders = HeaderUtils.asStringHeaders(headers, null);

        // test string values
        assertEquals(Arrays.asList("value", "value2"),
                     stringHeaders.get("k1"));
        assertEquals(Collections.singletonList(uri.toASCIIString()),
                     stringHeaders.get("k2"));
        assertEquals(Collections.singletonList("value3"),
                     stringHeaders.get("k3"));

        // test live view
        headers.get("k1").remove(1);
        headers.add("k2", "value4");
        headers.remove("k3");

        assertEquals(Collections.singletonList("value"),
                     stringHeaders.get("k1"));
        assertEquals(Arrays.asList(uri.toASCIIString(), "value4"),
                     stringHeaders.get("k2"));
        assertFalse(stringHeaders.containsKey("k3"));
    }

    @Test
    public void testAsHeaderString() throws Exception {
        assertNull(HeaderUtils.asHeaderString(null, null));

        final URI uri = new URI("test");
        final List<Object> values = Arrays.asList("value", null, uri);

        // test string values
        final String result = HeaderUtils.asHeaderString(values, null);
        assertEquals("value,[null]," + uri.toASCIIString(), result);
    }

    @Test
    public void testGetPreferredCookie(){

        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, Calendar.JANUARY, 1);
        Date earlyDate = calendar.getTime();
        calendar.set(2000, Calendar.JANUARY, 2);
        Date laterDate = calendar.getTime();

        NewCookie earlyCookie = new NewCookie("fred", "valuestring", "pathstring", "domainstring",
                0, "commentstring", 0, earlyDate, false, false);
        NewCookie laterCookie = new NewCookie("fred", "valuestring", "pathstring", "domainstring",
                0, "commentstring", 0, laterDate, false, false);

        assertEquals(laterCookie, HeaderUtils.getPreferredCookie(earlyCookie, laterCookie));

        NewCookie one = new NewCookie("fred", "valuestring", "pathstring", "domainstring",
                0, "commentstring", 100, null, false, false);
        NewCookie second = new NewCookie("fred", "valuestring", "pathstring", "domainstring",
                0, "commentstring", 10, null, false, false);

        assertEquals(one, HeaderUtils.getPreferredCookie(one, second));

        NewCookie longPathNewCookie = new NewCookie("fred", "valuestring", "longestpathstring",
                "domainstring", 0, "commentstring", 0, null,
                false, false);
        NewCookie shortPathNewCookie = new NewCookie("fred", "valuestring", "shortestpath",
                "domainstring", 0, "commentstring", 0, null,
                false, false);

        assertEquals(longPathNewCookie, HeaderUtils.getPreferredCookie(longPathNewCookie, shortPathNewCookie));

        NewCookie identicalNewCookie = new NewCookie("fred", "valuestring", "pathstring",
                "domainstring", 0, "commentstring", 0, null,
                false, false);
        NewCookie identicalNewCookie1 = new NewCookie("fred", "valuestring", "pathstring",
                "domainstring", 0, "commentstring", 0, null,
                false, false);

        assertEquals(identicalNewCookie, HeaderUtils.getPreferredCookie(identicalNewCookie, identicalNewCookie1));

    }

    @Test
    public void testGetPreferredCookieWithNullPath() {
        NewCookie first = new NewCookie("NAME", "VALUE");
        NewCookie second = new NewCookie("NAME", "VALUE");
        NewCookie returnedCookie = HeaderUtils.getPreferredCookie(first, second);

        assertEquals(first, returnedCookie);
    }

    @Test
    public void testGetPreferredCookieWithNullInput() {
        assertNull(HeaderUtils.getPreferredCookie(null, null));
    }

}
