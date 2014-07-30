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
package org.apache.fleece.core;

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
        final byte[] utfBytes = new byte[4];
        int bomLength=0;
        try {
            final int read = inputStream.read(utfBytes);
            if (read < 2) {
                throw new JsonException("Invalid Json. Valid Json has at least 2 bytes");
            } else {

                int first = (utfBytes[0] & 0xFF);
                int second = (utfBytes[1] & 0xFF);
                
                if (first == 0x00) {
                    charset = (second == 0x00) ? Charset.forName("UTF-32BE") : Charset.forName("UTF-16BE");
                } else if (read > 2 && second == 0x00) {
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
                                        
                    
                    
                    
                    if(first == 0xFE && second == 0xFF) {
                        charset = Charset.forName("UTF-16BE");
                        bomLength=2;
                    } else if(read > 3 && first == 0x00 && second == 0x00 && (utfBytes[2]&0xff) == 0xFE && (utfBytes[3]&0xff) == 0xFF){
                        charset = Charset.forName("UTF-32BE");
                        bomLength=4;
                    } else if(first == 0xFF && second == 0xFE) {
                        
                        if(read > 3 && (utfBytes[2]&0xff) == 0x00 && (utfBytes[3]&0xff) == 0x00) {
                            charset = Charset.forName("UTF-32LE");
                            bomLength=4;
                        }else {
                            charset = Charset.forName("UTF-16LE");
                            bomLength=2;
                        }
                        
                    } 
                    
                    //assume UTF8
                    
                }

            }
            
            if(bomLength < 4) {
                inputStream.unread(utfBytes,bomLength==2?2:0,read-bomLength);
            }
            
            

        } catch (final IOException e) {
            throw new JsonException("Unable to detect charset due to "+e.getMessage(), e);
        }

        return charset;
    }

}
