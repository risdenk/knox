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

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.apache.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PartiallyRepeatableHttpEntity extends HttpEntityWrapper {

  public static final int DEFAULT_BUFFER_SIZE = 4096;

  private final HttpEntity wrappedEntity;
  private int replayWriteIndex;
  private int replayWriteLimit;
  private byte[] replayBuffer;
  private ReplayStream finalStream;
  private InputStream wrappedStream;

  public PartiallyRepeatableHttpEntity(final HttpEntity entity, int bufferSize) throws IOException {
    super( entity );
    this.wrappedEntity = Args.notNull(entity, "Wrapped entity");
    this.wrappedStream = null;
    this.finalStream = null;
    this.replayWriteIndex = -1;
    if( !entity.isRepeatable() ) {
      this.replayBuffer = new byte[ bufferSize ];
      this.replayWriteLimit = bufferSize-1;
    } else {
      this.replayBuffer = null;
    }
  }

  public PartiallyRepeatableHttpEntity(final HttpEntity entity) throws IOException {
    this( entity, DEFAULT_BUFFER_SIZE );
  }

  @Override
  public boolean isRepeatable() {
    return finalStream == null;
  }

  @Override
  public boolean isStreaming() {
    return wrappedEntity.isStreaming();
  }

  @Override
  public boolean isChunked() {
    return wrappedEntity.isChunked();
  }

  @Override
  public long getContentLength() {
    return wrappedEntity.getContentLength();
  }

  // This will throw an IOException if an attempt is made to getContent a second time after
  // more bytes than the buffer can hold has been read on the first stream.
  @Override
  public InputStream getContent() throws IOException {
    // If the wrapped stream is repeatable return it directly.
    if( replayBuffer == null ) {
      return wrappedEntity.getContent();
    // Else if the buffer has overflowed
    } else if( finalStream != null ) {
      throw new IOException( "Existing stream already past replay buffer capacity." );
    } else {
      if( wrappedStream == null ) {
         wrappedStream = wrappedEntity.getContent();
      }
      return new ReplayStream();
    }
  }

  @Override
  public void writeTo( final OutputStream stream ) throws IOException {
    IOUtils.copy( getContent(), stream );
  }

  private class ReplayStream extends InputStream {

    private int replayReadIndex = -1;

    @Override
    public int read() throws IOException {
      int b;
      if( finalStream != null && finalStream != this ) {
        throw new IOException( "Replay stream taken over by another consumer." );
      }
      // If we can read from the buffer do so.
      if( replayReadIndex < replayWriteIndex ) {
        b = replayBuffer[ ++replayReadIndex ];
      } else {
        b = wrappedStream.read();
        // If the underlying stream is not closed.
        if( b > -1 ) {
          if( replayWriteIndex < replayWriteLimit ) {
            replayBuffer[ ++replayWriteIndex ] = (byte)b;
            replayReadIndex++;
          } else {
            finalStream = this;
          }
        }
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int limit ) throws IOException {
      int count;
      if( finalStream != null && finalStream != this ) {
        throw new IOException( "Replay stream taken over by another consumer." );
      }
      // If we can read from the buffer do so.
      if( replayReadIndex < replayWriteIndex ) {
        count = replayWriteIndex - replayReadIndex;
        count = Math.min( limit, count );
        System.arraycopy( replayBuffer, replayReadIndex+1, buffer, offset, count );
        replayReadIndex += count;
      } else {
        count = wrappedStream.read( buffer, offset, limit );
        // If the underlying stream is not closed.
        if( count > -1 ) {
          if( replayWriteIndex+count < replayWriteLimit ) {
            System.arraycopy( buffer, offset, replayBuffer, replayWriteIndex+1, count );
            replayReadIndex += count;
            replayWriteIndex += count;
          } else {
            finalStream = this;
          }
        }
      }
      return count;
    }

  }

}
