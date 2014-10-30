/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;

import javax.json.JsonException;

final class RFC4627AwareInputStreamReader extends InputStreamReader {

    RFC4627AwareInputStreamReader(final InputStream in) {
        this(new PushbackInputStream(in,4));
    }
    
    private RFC4627AwareInputStreamReader(final PushbackInputStream in) {
        super(in, getCharset(in).newDecoder());
       
    }

    /**
     * According to the Java API "An attempt is made to read as many as len bytes, but a smaller number may be read".
     * [http://docs.oracle.com/javase/7/docs/api/java/io/InputStream.html#read(byte[],%20int,%20int)]
     * For this reason we need to ensure that we've read all the bytes that we need out of this stream.
     */
    private static byte[] readAllBytes(final PushbackInputStream inputStream) throws IOException {
        final int first = inputStream.read();
        final int second = inputStream.read();
        if(first == -1|| second == -1) {
            throw new JsonException("Invalid Json. Valid Json has at least 2 bytes");
        }
        final int third = inputStream.read();
        final int fourth = inputStream.read();
        if(third == -1) {
            return new byte[] { (byte) first, (byte) second };
        } else if(fourth == -1) {
            return new byte[] { (byte) first, (byte) second, (byte) third };
        } else {
            return new byte[] { (byte) first, (byte) second, (byte) third, (byte) fourth };
        }
    }

    /*
        * RFC 4627

          JSON text SHALL be encoded in Unicode.  The default encoding is
          UTF-8.
       
          Since the first two characters of a JSON text will always be ASCII
          characters [RFC0020], it is possible to determine whether an octet
          stream is UTF-8, UTF-16 (BE or LE), or UTF-32 (BE or LE) by looking
          at the pattern of nulls in the first four octets.

          00 00 00 xx  UTF-32BE
          00 xx 00 xx  UTF-16BE
          xx 00 00 00  UTF-32LE
          xx 00 xx 00  UTF-16LE
          xx xx xx xx  UTF-8

        */

    private static Charset getCharset(final PushbackInputStream inputStream) {
        Charset charset = Charset.forName("UTF-8");
        int bomLength=0;
        try {
            final byte[] utfBytes = readAllBytes(inputStream);
            int first = (utfBytes[0] & 0xFF);
            int second = (utfBytes[1] & 0xFF);
            if (first == 0x00) {
                charset = (second == 0x00) ? Charset.forName("UTF-32BE") : Charset.forName("UTF-16BE");
            } else if (utfBytes.length > 2 && second == 0x00) {
                int third = (utfBytes[2] & 0xFF);
                charset = (third  == 0x00) ? Charset.forName("UTF-32LE") : Charset.forName("UTF-16LE");
            } else {

                    /*check BOM

                    Encoding       hex byte order mark
                    UTF-8          EF BB BF
                    UTF-16 (BE)    FE FF
                    UTF-16 (LE)    FF FE
                    UTF-32 (BE)    00 00 FE FF
                    UTF-32 (LE)    FF FE 00 00
                    */

                //We do not check for UTF-32BE because that is already covered above and we
                //do not to unread anything.

                if(first == 0xFE && second == 0xFF) {
                    charset = Charset.forName("UTF-16BE");
                    bomLength=2;
                } else if(first == 0xFF && second == 0xFE) {
                    if(utfBytes.length > 3 && (utfBytes[2]&0xff) == 0x00 && (utfBytes[3]&0xff) == 0x00) {
                        charset = Charset.forName("UTF-32LE");
                        bomLength=4;
                    }else {
                        charset = Charset.forName("UTF-16LE");
                        bomLength=2;
                    }
                } else if (utfBytes.length > 2 && first == 0xEF && second == 0xBB && (utfBytes[2]&0xff) == 0xBF) {
                    //UTF-8 with BOM
                    bomLength=3;
                }
            }
            //assume UTF8
            if(bomLength > 0 && bomLength < 4) {             
                //do not unread BOM, only bytes after BOM        
                inputStream.unread(utfBytes,bomLength,utfBytes.length - bomLength);
            } else {             
                //no BOM, unread all read bytes
                inputStream.unread(utfBytes);
            }
          

        } catch (final IOException e) {
            throw new JsonException("Unable to detect charset due to "+e.getMessage(), e);
        }

        return charset;
    }

}
