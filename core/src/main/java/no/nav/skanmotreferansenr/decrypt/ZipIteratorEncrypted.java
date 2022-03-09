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

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ZipIteratorEncrypted implements Iterator<Message>, Closeable {
    static final Logger LOGGER = LoggerFactory.getLogger(org.apache.camel.dataformat.zipfile.ZipIterator.class);

    private final Exchange exchange;
    private boolean allowEmptyDirectory;
    private volatile ZipInputStream zipInputStream;
    private volatile Message parent;

    public ZipIteratorEncrypted(Exchange exchange, InputStream inputStream) {
        this.exchange = exchange;
        this.allowEmptyDirectory = false;
        if (inputStream instanceof ZipInputStream) {
            zipInputStream = (ZipInputStream) inputStream;
        } else {
            zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
        }
        parent = null;
    }

    @Override
    public boolean hasNext() {
        try {
            if (zipInputStream == null) {
                return false;
            }
            boolean availableDataInCurrentEntry = zipInputStream.available() >= 1;
            if (!availableDataInCurrentEntry) {
                // advance to the next entry.
                parent = getNextElement();
                if (parent == null) {
                    zipInputStream.close();
                    availableDataInCurrentEntry = false;
                } else {
                    availableDataInCurrentEntry = true;
                }
            }
            return availableDataInCurrentEntry;
        } catch (IOException exception) {
            throw new RuntimeCamelException(exception);
        }
    }

    @Override
    public Message next() {
        if (parent == null) {
            parent = getNextElement();
        }
        Message answer = parent;
        parent = null;
        checkNullAnswer(answer);

        return answer;
    }

    private Message getNextElement() {
        if (zipInputStream == null) {
            return null;
        }

        try {
            LocalFileHeader current = getNextEntry();

            if (current != null) {

                //we only want to accept files with AES encryption
                if (current.getEncryptionMethod() == EncryptionMethod.NONE) {
                    throw new ZipException("En enc.zip kom inn men filene er ukrypterte");
                }
                if (current.getEncryptionMethod() != EncryptionMethod.AES) {
                    throw new ZipException("Filene er ikke kryptert med AES men en annen krypteringsmetode.");
                }

                LOGGER.debug("read zipEntry {}", current.getFileName());
                Message answer = new DefaultMessage(exchange.getContext());
                answer.getHeaders().putAll(exchange.getIn().getHeaders());
                answer.setHeader("zipFileName", current.getFileName());
                answer.setHeader(Exchange.FILE_NAME, current.getFileName());
                answer.setBody(new ZipInputStreamWrapperEncrypted(zipInputStream));
                return answer;
            } else {
                LOGGER.trace("close zipInputStream");
                return null;
            }
        } catch (IOException exception) {
            throw new RuntimeCamelException(exception);
        }
    }

    public void checkNullAnswer(Message answer) {
        if (answer == null && zipInputStream != null) {
            IOHelper.close(zipInputStream);
            zipInputStream = null;
        }
    }

    private LocalFileHeader getNextEntry() throws IOException {
        LocalFileHeader entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                return entry;
            } else {
                if (allowEmptyDirectory) {
                    return entry;
                }
            }
        }

        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        IOHelper.close(zipInputStream);
        zipInputStream = null;
    }

    public boolean isSupportIteratorForEmptyDirectory() {
        return allowEmptyDirectory;
    }

    public void setAllowEmptyDirectory(boolean allowEmptyDirectory) {
        this.allowEmptyDirectory = allowEmptyDirectory;
    }
}