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
package org.slim3.controller.upload;

/**
 * This exception is thrown to indicate that the request size exceeds the
 * configured maximum.
 * 
 * @author higa
 * @since 3.0
 * 
 */
public class SizeLimitExceededException extends FileUploadException {

    private static final long serialVersionUID = 1L;

    /**
     * The actual size of the request.
     */
    protected long actual;

    /**
     * The maximum permitted size of the request.
     */
    protected long permitted;

    /**
     * Constructor.
     * 
     * @param message
     *            the message.
     * @param actual
     *            the actual number(bytes).
     * @param permitted
     *            the permitted size limit(bytes).
     */
    public SizeLimitExceededException(String message, long actual,
            long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Retrieves the actual size of the request.
     * 
     * @return The actual size of the request.
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Retrieves the permitted size of the request.
     * 
     * @return The permitted size of the request.
     */
    public long getPermittedSize() {
        return permitted;
    }
}