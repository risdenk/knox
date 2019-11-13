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
package org.apache.knox.gateway.shell;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

public class BasicResponse implements Closeable {
  private HttpResponse response;
  private boolean consumed;
  private String string;
  private InputStream stream;
  private byte[] bytes;

  public BasicResponse(HttpResponse response) {
    this.response = response;
  }

  public void consume() {
    if (!consumed) {
      EntityUtils.consumeQuietly(response.getEntity());
      consumed = true;
    }
  }

  @Override
  public void close() {
    consume();
  }

  protected HttpResponse response() {
    return response;
  }

  public boolean isConsumed() {
    return consumed;
  }

  public int getStatusCode() {
    return response.getStatusLine().getStatusCode();
  }

  public long getContentLength() {
    return response.getEntity().getContentLength();
  }

  public String getContentType() {
    return ContentType.getOrDefault(response.getEntity()).getMimeType();
  }

  public String getContentEncoding() {
    return ContentType.getOrDefault(response.getEntity()).getCharset().name();
  }

  public InputStream getStream() throws IOException {
    if (!consumed && stream == null) {
      stream = response.getEntity().getContent();
      consumed = true;
    }
    return stream;
  }

  public String getString() throws IOException {
    if (!consumed && string == null) {
      HttpEntity tempEntity = response.getEntity();
      if (tempEntity != null) {
        string = EntityUtils.toString(tempEntity);
        consumed = true;
      } else {
        string = "";
      }
    }
    return string;
  }

  public byte[] getBytes() throws IOException {
    if (!consumed && bytes == null) {
      bytes = EntityUtils.toByteArray(response.getEntity());
      consumed = true;
    }
    return bytes;
  }
}
