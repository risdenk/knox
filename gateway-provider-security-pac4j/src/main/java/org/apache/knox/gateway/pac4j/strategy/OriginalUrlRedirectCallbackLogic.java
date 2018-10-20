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
package org.apache.knox.gateway.pac4j.strategy;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.HttpAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.pac4j.core.util.CommonHelper.isNotBlank;

public class OriginalUrlRedirectCallbackLogic extends DefaultCallbackLogic<Object, WebContext> {
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  protected HttpAction redirectToOriginallyRequestedUrl(WebContext context, String defaultUrl) {
    SessionStore sessionStore = context.getSessionStore();
    final String requestedUrl = (String) sessionStore.get(context, Pac4jConstants.REQUESTED_URL);
    String redirectUrl = defaultUrl;
    if (isNotBlank(requestedUrl)) {
      sessionStore.set(context, Pac4jConstants.REQUESTED_URL, null);
      Optional<String> originalUrlOption = getOriginalUrl(requestedUrl);
      if (!originalUrlOption.isPresent()) {
        logger.debug("'originalUrl' query parameter not present. Returning the requested URL.");
      }
      redirectUrl = originalUrlOption.orElse(requestedUrl);

    }
    logger.debug("redirectUrl: {}", redirectUrl);
    return HttpAction.redirect(context, redirectUrl);
  }

  private Optional<String> getOriginalUrl(String requestedUrl) {
    try {
      URL url = new URL(requestedUrl);
      List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(url.getQuery(), UTF_8);
      return nameValuePairs.stream()
                 .filter(pair -> pair.getName().equals("originalUrl"))
                 .map(NameValuePair::getValue)
                 .findFirst();
    } catch (MalformedURLException e) {
      return Optional.empty();
    }

  }
}
