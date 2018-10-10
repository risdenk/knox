/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.dispatch;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.KerberosConfig;
import org.apache.hc.client5.http.impl.auth.SPNegoSchemeFactory;
import org.apache.hc.core5.http.protocol.HttpContext;

public class KnoxSpnegoAuthSchemeFactory extends SPNegoSchemeFactory {

  private final KerberosConfig config;
  private final DnsResolver dnsResolver;

  public KnoxSpnegoAuthSchemeFactory(KerberosConfig config, DnsResolver dnsResolver) {
    super(config, dnsResolver);
    this.config = config;
    this.dnsResolver = dnsResolver;
  }

  @SuppressWarnings("deprecation")
  @Override
  public AuthScheme create(HttpContext context) {
    return new KnoxSpnegoAuthScheme(this.config, this.dnsResolver);
  }
}
