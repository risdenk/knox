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
package org.apache.http.impl.conn;

import org.apache.commons.logging.Log;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.io.ChunkedOutputStream;
import org.apache.http.impl.io.ContentLengthOutputStream;
import org.apache.http.impl.io.IdentityOutputStream;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionOutputBuffer;

import java.io.OutputStream;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

class KnoxLoggingManagedHttpClientConnection extends LoggingManagedHttpClientConnection {
  private int BUFFER_SIZE = Integer.parseInt(System.getProperty("KNOX_CHUNKSIZE", "2048"));

  @Override
  protected OutputStream createOutputStream(long len, SessionOutputBuffer outbuffer) {
    if (len == ContentLengthStrategy.CHUNKED) {
      return new ChunkedOutputStream(BUFFER_SIZE, outbuffer);
    } else if (len == ContentLengthStrategy.IDENTITY) {
      return new IdentityOutputStream(outbuffer);
    } else {
      return new ContentLengthOutputStream(outbuffer, len);
    }
  }

  KnoxLoggingManagedHttpClientConnection(String id, Log log, Log headerlog, Log wirelog, int buffersize, int fragmentSizeHint, CharsetDecoder chardecoder, CharsetEncoder charencoder, MessageConstraints constraints, ContentLengthStrategy incomingContentStrategy, ContentLengthStrategy outgoingContentStrategy, HttpMessageWriterFactory<HttpRequest> requestWriterFactory, HttpMessageParserFactory<HttpResponse> responseParserFactory) {
    super(id, log, headerlog, wirelog, buffersize, fragmentSizeHint, chardecoder, charencoder, constraints, incomingContentStrategy, outgoingContentStrategy, requestWriterFactory, responseParserFactory);
  }
}
