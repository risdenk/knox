/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.knox.gateway.shirorealm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnoxLdapRealmTest {
  @Test
  void setGetSearchBase() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setSearchBase("dc=hadoop,dc=apache,dc=org");
    assertEquals(realm.getSearchBase(), "dc=hadoop,dc=apache,dc=org");
  }

  @Test
  void setGetGroupObjectClass() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setGroupObjectClass("groupOfMembers");
    assertEquals(realm.getGroupObjectClass(), "groupOfMembers");
  }

  @Test
  void setGetUniqueMemberAttribute() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setMemberAttribute("member");
    assertEquals(realm.getMemberAttribute(), "member");
  }

  @Test
  void setGetUserSearchAttributeName() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setUserSearchAttributeName("uid");
    assertEquals(realm.getUserSearchAttributeName(), "uid");
  }

  @Test
  void setGetUserObjectClass() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setUserObjectClass("inetuser");
    assertEquals(realm.getUserObjectClass(), "inetuser");
  }

  @Test
  void setGetUserSearchBase() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setSearchBase("dc=example,dc=com");
    realm.setUserSearchBase("dc=knox,dc=example,dc=com");
    assertEquals(realm.getUserSearchBase(), "dc=knox,dc=example,dc=com");
  }

  @Test
  void setGetGroupSearchBase() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setSearchBase("dc=example,dc=com");
    realm.setGroupSearchBase("dc=knox,dc=example,dc=com");
    assertEquals(realm.getGroupSearchBase(), "dc=knox,dc=example,dc=com");
  }

  @Test
  void verifyDefaultUserSearchAttributeName() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    assertNull(realm.getUserSearchAttributeName());
  }

  @Test
  void verifyDefaultGetUserObjectClass() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    assertEquals(realm.getUserObjectClass(), "person");
  }

  @Test
  void verifyDefaultUserSearchBase() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setSearchBase("dc=knox,dc=example,dc=com");
    assertEquals(realm.getUserSearchBase(), "dc=knox,dc=example,dc=com");
  }

  @Test
  void verifyDefaultGroupSearchBase() {
    KnoxLdapRealm realm = new KnoxLdapRealm();
    realm.setSearchBase("dc=knox,dc=example,dc=com");
    assertEquals(realm.getGroupSearchBase(), "dc=knox,dc=example,dc=com");
  }
}
