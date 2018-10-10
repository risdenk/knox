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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;

/**
 * Factory for {@link ManagedHttpClientConnection} instances.
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public class KnoxManagedHttpClientConnectionFactory
    implements HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> {

  private static final AtomicLong COUNTER = new AtomicLong();

  private static final Log log = LogFactory.getLog(DefaultManagedHttpClientConnection.class); //NOPMD
  private static final Log headerlog = LogFactory.getLog("org.apache.http.headers"); //NOPMD
  private static final Log wirelog = LogFactory.getLog("org.apache.http.wire"); //NOPMD

  private final HttpMessageWriterFactory<HttpRequest> requestWriterFactory;
  private final HttpMessageParserFactory<HttpResponse> responseParserFactory;
  private final ContentLengthStrategy incomingContentStrategy;
  private final ContentLengthStrategy outgoingContentStrategy;

  /**
   * @since 4.4
   */
  public KnoxManagedHttpClientConnectionFactory(
      final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
      final HttpMessageParserFactory<HttpResponse> responseParserFactory,
      final ContentLengthStrategy incomingContentStrategy,
      final ContentLengthStrategy outgoingContentStrategy) {
    super();
    this.requestWriterFactory = requestWriterFactory != null ? requestWriterFactory :
                                    DefaultHttpRequestWriterFactory.INSTANCE;
    this.responseParserFactory = responseParserFactory != null ? responseParserFactory :
                                     DefaultHttpResponseParserFactory.INSTANCE;
    this.incomingContentStrategy = incomingContentStrategy != null ? incomingContentStrategy :
                                       LaxContentLengthStrategy.INSTANCE;
    this.outgoingContentStrategy = outgoingContentStrategy != null ? outgoingContentStrategy :
                                       StrictContentLengthStrategy.INSTANCE;
  }

  public KnoxManagedHttpClientConnectionFactory(
      final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
      final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
    this(requestWriterFactory, responseParserFactory, null, null);
  }

  public KnoxManagedHttpClientConnectionFactory(
      final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
    this(null, responseParserFactory);
  }

  public KnoxManagedHttpClientConnectionFactory() {
    this(null, null);
  }

  @Override
  public ManagedHttpClientConnection create(final HttpRoute route, final ConnectionConfig config) {
    final ConnectionConfig cconfig = config != null ? config : ConnectionConfig.DEFAULT;
    CharsetDecoder chardecoder = null;
    CharsetEncoder charencoder = null;
    final Charset charset = cconfig.getCharset();
    final CodingErrorAction malformedInputAction = cconfig.getMalformedInputAction() != null ?
                                                       cconfig.getMalformedInputAction() : CodingErrorAction.REPORT;
    final CodingErrorAction unmappableInputAction = cconfig.getUnmappableInputAction() != null ?
                                                        cconfig.getUnmappableInputAction() : CodingErrorAction.REPORT;
    if (charset != null) {
      chardecoder = charset.newDecoder();
      chardecoder.onMalformedInput(malformedInputAction);
      chardecoder.onUnmappableCharacter(unmappableInputAction);
      charencoder = charset.newEncoder();
      charencoder.onMalformedInput(malformedInputAction);
      charencoder.onUnmappableCharacter(unmappableInputAction);
    }
    final String id = "http-outgoing-" + Long.toString(COUNTER.getAndIncrement());
    return new KnoxLoggingManagedHttpClientConnection(
        id,
        log,
        headerlog,
        wirelog,
        Integer.parseInt(System.getProperty("KNOX_BUFFERSIZE", String.valueOf(cconfig.getBufferSize()))),
        cconfig.getFragmentSizeHint(),
        chardecoder,
        charencoder,
        cconfig.getMessageConstraints(),
        incomingContentStrategy,
        outgoingContentStrategy,
        requestWriterFactory,
        responseParserFactory);
  }

}
