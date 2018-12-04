/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.knox.gateway.topology.discovery;

import org.apache.knox.gateway.services.security.AliasService;
import org.apache.knox.gateway.services.security.impl.DefaultAliasService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceDiscoveryFactoryTest {
    @Test
    void testGetDummyImpl() {
        String TYPE = "DUMMY";
        ServiceDiscovery sd = ServiceDiscoveryFactory.get(TYPE);
        assertNotNull(sd, "Expected to get a ServiceDiscovery object.");
        assertEquals(TYPE, sd.getType(), "Unexpected ServiceDiscovery type.");
    }

    @Test
    void testGetDummyImplWithMismatchedCase() {
        String TYPE = "dUmmY";
        ServiceDiscovery sd = ServiceDiscoveryFactory.get(TYPE);
        assertNotNull(sd, "Expected to get a ServiceDiscovery object.");
        assertEquals(TYPE.toUpperCase(Locale.ROOT), sd.getType(), "Unexpected ServiceDiscovery type.");
    }

    @Test
    void testGetInvalidImpl() {
        String TYPE = "InValID";
        ServiceDiscovery sd = ServiceDiscoveryFactory.get(TYPE);
        assertNull(sd, "Unexpected ServiceDiscovery object.");
    }

    @Test
    void testGetImplWithMismatchedType() {
        String TYPE = "DeclaredType";
        ServiceDiscovery sd = ServiceDiscoveryFactory.get(TYPE);
        assertNull(sd, "Unexpected ServiceDiscovery object.");
    }

    @Test
    void testGetPropertiesFileImplWithAliasServiceInjection() throws Exception {
        String TYPE = "PROPERTIES_FILE";
        ServiceDiscovery sd = ServiceDiscoveryFactory.get(TYPE, new DefaultAliasService());
        assertNotNull(sd, "Expected to get a ServiceDiscovery object.");
        assertEquals(TYPE, sd.getType(), "Unexpected ServiceDiscovery type.");

        // Verify that the AliasService was injected as expected
        Field aliasServiceField = sd.getClass().getDeclaredField("aliasService");
        aliasServiceField.setAccessible(true);
        Object fieldValue = aliasServiceField.get(sd);
        assertNotNull(fieldValue);
        assertTrue(AliasService.class.isAssignableFrom(fieldValue.getClass()));
    }
}
