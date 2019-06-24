/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AclParserTest {
  @Test
  void testValidAcls() {
    AclParser p = new AclParser();
    p.parseAcls("test", "guest;*;*");
    assertTrue(p.users.contains("guest"));
    assertTrue(p.anyGroup);
    assertTrue(p.ipv.allowsAnyIP());

    p = new AclParser();
    p.parseAcls("test", "*;admins;*");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertTrue(p.ipv.allowsAnyIP());

    p = new AclParser();
    p.parseAcls("test", "*;*;127.0.0.1");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertTrue(p.anyGroup);
    assertFalse(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));

    p = new AclParser();
    p.parseAcls("test", "*;admins;127.0.0.1");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));

    p = new AclParser();
    p.parseAcls("test", "guest;admins;127.0.0.1");
    assertTrue(p.users.contains("guest"));
    assertFalse(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));

    p = new AclParser();
    p.parseAcls("test", "guest;*;127.0.0.1");
    assertTrue(p.users.contains("guest"));
    assertFalse(p.anyUser);
    assertTrue(p.anyGroup);
    assertFalse(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));

    p = new AclParser();
    p.parseAcls("test", "*;admins;127.0.0.1");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));
  }

  @Test
  void testValidMultiValuedAcls() {
    AclParser p = new AclParser();
    p.parseAcls("test", "*;admins;127.0.0.1,127.0.0.2");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.2"));
    assertFalse(p.ipv.getIPAddresses().contains("127.0.0.3"));

    p = new AclParser();
    p.parseAcls("test", "*;admins,users;127.0.0.1,127.0.0.2");
    assertFalse(p.users.contains("guest"));
    assertTrue(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertTrue(p.groups.contains("users"));
    assertFalse(p.groups.contains("hackers"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.2"));
    assertFalse(p.ipv.getIPAddresses().contains("127.0.0.3"));

    p = new AclParser();
    p.parseAcls("test", "guest,visitor;admins,users;127.0.0.1,127.0.0.2");
    assertTrue(p.users.contains("guest"));
    assertTrue(p.users.contains("visitor"));
    assertFalse(p.users.contains("missing-guy"));
    assertFalse(p.anyUser);
    assertFalse(p.anyGroup);
    assertTrue(p.groups.contains("admins"));
    assertTrue(p.groups.contains("users"));
    assertFalse(p.groups.contains("hackers"));
    assertFalse(p.ipv.allowsAnyIP());
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.1"));
    assertTrue(p.ipv.getIPAddresses().contains("127.0.0.2"));
    assertFalse(p.ipv.getIPAddresses().contains("127.0.0.3"));
  }

  @Test
  void testNullACL() {
    AclParser p = new AclParser();
    p.parseAcls("test", null);
  }

  @Test
  void testInvalidAcls() {
    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", "guest"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", "guest;;"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", ";;"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", ";"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", "guest;"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", ";admins"));

    Assertions.assertThrows(InvalidACLException.class,
        () -> new AclParser().parseAcls("test", ""));
  }
}
