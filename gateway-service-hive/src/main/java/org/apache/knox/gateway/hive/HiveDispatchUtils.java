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
package org.apache.knox.gateway.hive;

import org.apache.hc.client5.http.auth.AuthenticationException;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.knox.gateway.security.SubjectUtils;

class HiveDispatchUtils {
  private static final char[] PASSWORD_PLACEHOLDER = "*".toCharArray();

  static void addCredentialsToRequest(ClassicHttpRequest request) {
    String principal = SubjectUtils.getCurrentEffectivePrincipalName();
    if ( principal != null ) {
      UsernamePasswordCredentials credentials =
          new UsernamePasswordCredentials(principal, PASSWORD_PLACEHOLDER);
      BasicScheme basicScheme = new BasicScheme();
      basicScheme.initPreemptive(credentials);
      try {
        String authorizationHeaderContent = basicScheme.generateAuthResponse(null, request, new BasicHttpContext());
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderContent, true));
      } catch (AuthenticationException e) {
        // not possible
      }
    }
  }
}
