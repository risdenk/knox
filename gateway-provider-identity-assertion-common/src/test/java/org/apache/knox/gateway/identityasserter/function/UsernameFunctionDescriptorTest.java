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
package org.apache.knox.gateway.identityasserter.function;

import org.apache.knox.gateway.filter.rewrite.api.UrlRewriteFunctionDescriptor;
import org.apache.knox.gateway.identityasserter.common.function.UsernameFunctionDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class UsernameFunctionDescriptorTest {
  @Test
  void testName() {
    UsernameFunctionDescriptor descriptor = new UsernameFunctionDescriptor();
    assertThat( descriptor.name(), is( "username" ) );
  }

  @Test
  void testServiceLoader() {
    ServiceLoader loader = ServiceLoader.load( UrlRewriteFunctionDescriptor.class );
    for (Object object : loader) {
      if (object instanceof UsernameFunctionDescriptor) {
        return;
      }
    }
    fail( "Failed to find UsernameFunctionDescriptor via service loader." );
  }
}
