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
package org.apache.knox.gateway.security.principal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PrincipalMapperTest {
  private PrincipalMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new SimplePrincipalMapper();
  }

  @Test
  void testSimplePrincipalMappingWithWildcardGroups() {
    String principalMapping = "";
    String groupMapping = "*=users";
    try {
      mapper.loadMappingTable(principalMapping, groupMapping);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("lmccay", mapper.mapUserPrincipal("lmccay"));
    assertEquals("users", mapper.mapGroupPrincipal("hdfs")[0]);
    assertEquals("users", mapper.mapGroupPrincipal("lmccay")[0]);
  }

  @Test
  void testSimplePrincipalMappingWithWildcardAndExplicitGroups() {
    String principalMapping = "";
    String groupMapping = "*=users;lmccay=mrgroup";
    try {
      mapper.loadMappingTable(principalMapping, groupMapping);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("lmccay", mapper.mapUserPrincipal("lmccay"));
    assertEquals("users", mapper.mapGroupPrincipal("hdfs")[0]);
    String group = mapper.mapGroupPrincipal("lmccay")[0];
    assertTrue("users".equals(group) || "mrgroup".equals(group));
    group = mapper.mapGroupPrincipal("lmccay")[1];
    assertTrue("users".equals(group) || "mrgroup".equals(group));
  }

  @Test
  void testSimplePrincipalMappingWithUserAndWildcardAndExplicitGroups() {
    String principalMapping = "guest=lmccay";
    String groupMapping = "*=users;lmccay=mrgroup";
    try {
      mapper.loadMappingTable(principalMapping, groupMapping);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("lmccay", mapper.mapUserPrincipal("guest"));
    assertEquals(1, mapper.mapGroupPrincipal("hdfs").length);
    assertEquals("users", mapper.mapGroupPrincipal("hdfs")[0]);
    assertEquals(2, mapper.mapGroupPrincipal("lmccay").length);
    String group = mapper.mapGroupPrincipal("lmccay")[0];
    assertTrue("users".equals(group) || "mrgroup".equals(group));
    group = mapper.mapGroupPrincipal("lmccay")[1];
    assertTrue("users".equals(group) || "mrgroup".equals(group));
  }

  @Test
  void testNonNullSimplePrincipalMappingWithGroups() {
    String principalMapping = "lmccay,kminder=hdfs;newuser=mapred";
    String groupMapping = "hdfs=group1;mapred=mrgroup,mrducks";
    try {
      mapper.loadMappingTable(principalMapping, groupMapping);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("hdfs", mapper.mapUserPrincipal("lmccay"));
    assertEquals("group1", mapper.mapGroupPrincipal("hdfs")[0]);

    assertEquals("hdfs", mapper.mapUserPrincipal("kminder"));

    assertEquals("mapred", mapper.mapUserPrincipal("newuser"));
    assertEquals("mrgroup", mapper.mapGroupPrincipal("mapred")[0]);
    assertEquals("mrducks", mapper.mapGroupPrincipal("mapred")[1]);

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }

  @Test
  void testNonNullSimplePrincipalMapping() {
    String principalMapping = "lmccay,kminder=hdfs;newuser=mapred";
    try {
      mapper.loadMappingTable(principalMapping, null);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("hdfs", mapper.mapUserPrincipal("lmccay"));
    assertEquals("hdfs", mapper.mapUserPrincipal("kminder"));

    assertEquals("mapred", mapper.mapUserPrincipal("newuser"));

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }

  @Test
  void testNonNullEndingSemiColonSimplePrincipalMapping() {
    String principalMapping = "lmccay,kminder=hdfs;newuser=mapred;";
    try {
      mapper.loadMappingTable(principalMapping, null);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("hdfs", mapper.mapUserPrincipal("lmccay"));
    assertEquals("hdfs", mapper.mapUserPrincipal("kminder"));

    assertEquals("mapred", mapper.mapUserPrincipal("newuser"));

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }

  @Test
  void testNullSimplePrincipalMapping() {
    String principalMapping = null;
    try {
      mapper.loadMappingTable(principalMapping, null);
    } catch (PrincipalMappingException pme) {
      fail(pme);
    }

    assertEquals("lmccay", mapper.mapUserPrincipal("lmccay"));
    assertEquals("kminder", mapper.mapUserPrincipal("kminder"));

    assertEquals("newuser", mapper.mapUserPrincipal("newuser"));

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }

  @Test
  void testInvalidSimplePrincipalMapping() {
    String principalMapping = "ksdlhfjkdshf;kjdshf";
    Assertions.assertThrows(PrincipalMappingException.class,
        () -> mapper.loadMappingTable(principalMapping, null));

    assertEquals("lmccay", mapper.mapUserPrincipal("lmccay"));
    assertEquals("kminder", mapper.mapUserPrincipal("kminder"));

    assertEquals("newuser", mapper.mapUserPrincipal("newuser"));

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }

  @Test
  void testPartiallyInvalidSimplePrincipalMapping() {
    String principalMapping = "lmccay=hdfs;kjdshf";
    Assertions.assertThrows(PrincipalMappingException.class,
        () -> mapper.loadMappingTable(principalMapping, null));

    assertEquals("lmccay", mapper.mapUserPrincipal("lmccay"));
    assertEquals("kminder", mapper.mapUserPrincipal("kminder"));

    assertEquals("newuser", mapper.mapUserPrincipal("newuser"));

    assertEquals("hdfs", mapper.mapUserPrincipal("hdfs"));
    assertEquals("mapred", mapper.mapUserPrincipal("mapred"));

    assertEquals("stink", mapper.mapUserPrincipal("stink"));
  }
}