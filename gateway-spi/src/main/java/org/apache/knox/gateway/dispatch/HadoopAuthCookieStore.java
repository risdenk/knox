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
package org.apache.knox.gateway.dispatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.knox.gateway.SpiGatewayMessages;
import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.i18n.messages.MessagesFactory;

public class HadoopAuthCookieStore extends BasicCookieStore {

  private static SpiGatewayMessages LOG = MessagesFactory.get( SpiGatewayMessages.class );

  private GatewayConfig gatewayConfig;

  HadoopAuthCookieStore(GatewayConfig config) {
    this.gatewayConfig = config;
  }

  @Override
  public void addCookie(Cookie cookie) {
    if (cookie.getName().equals("hadoop.auth") || cookie.getName().equals("hive.server2.auth")) {
      // Only add the cookie if it's Knox's cookie
      if (isKnoxCookie(gatewayConfig, cookie)) {
        LOG.acceptingServiceCookie(cookie);
        super.addCookie(cookie);
      }
    }
  }

  private static boolean isKnoxCookie(GatewayConfig config, Cookie cookie) {
    boolean result = false;

    if (cookie != null) {
      String value = cookie.getValue();
      if (value != null && !value.isEmpty()) {
        String principal = null;

        String[] cookieParts = value.split("&");
        if (cookieParts.length > 1) {
          String[] elementParts = cookieParts[1].split("=");
          if (elementParts.length == 2) {
            principal = elementParts[1];
          }

          if (principal != null) {
            String krb5Config = config.getKerberosLoginConfig();
            if (krb5Config != null && !krb5Config.isEmpty()) {
              Properties p = new Properties();
              try (InputStream in = Files.newInputStream(Paths.get(krb5Config))){
                p.load(in);
                String configuredPrincipal = p.getProperty("principal");
                // Strip off enclosing quotes, if present
                if (configuredPrincipal.startsWith("\"")) {
                  configuredPrincipal = configuredPrincipal.substring(1, configuredPrincipal.length() - 1);
                }
                // Check if they're the same principal
                result = principal.equals(configuredPrincipal);
              } catch (IOException e) {
                LOG.errorReadingKerberosLoginConfig(krb5Config, e);
              }
            }
          }
        }
      }
    }

    return result;
  }
}