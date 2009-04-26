/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.slim3.gae.jdo;

import javax.jdo.JDOException;

/**
 * This exception is thrown when the query results is not unique.
 * 
 * @author higa
 * @since 3.0
 * 
 */
public class JDONonUniqueResultException extends JDOException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public JDONonUniqueResultException() {
    }

    /**
     * Constructor.
     * 
     * @param msg
     *            the message
     */
    public JDONonUniqueResultException(String msg) {
        super(msg);
    }
}