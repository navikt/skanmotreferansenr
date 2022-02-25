/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.nav.skanmotreferansenr.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;


@Slf4j
public class ZipSplitterEncrypted extends ZipSplitter {

    private final String passphrase;

    @Autowired
    public ZipSplitterEncrypted(@Value("${passphrase}") String passphrase) {
        this.passphrase = passphrase;
    }

    @Override
    public ZipIteratorEncrypted evaluate(Exchange exchange) {
        Message inputMessage = exchange.getIn();
        ZipInputStream zip = new ZipInputStream(inputMessage.getBody(InputStream.class), passphrase.toCharArray());
        return new ZipIteratorEncrypted(exchange, zip);
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        Object result = this.evaluate(exchange);
        return exchange.getContext().getTypeConverter().convertTo(type, exchange, result);
    }

}
