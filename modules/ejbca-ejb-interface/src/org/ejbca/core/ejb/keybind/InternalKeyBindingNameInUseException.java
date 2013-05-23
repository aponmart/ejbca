/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb.keybind;

import org.cesecore.ErrorCode;
import org.ejbca.core.EjbcaException;

/**
 * An exception thrown when someone tries to create an InternalKeyBinding with an existing name.
 *
 * @version $Id$
 */
public class InternalKeyBindingNameInUseException extends EjbcaException {

    private static final long serialVersionUID = 1L;
    private static final String _INTERNAL_KEY_BINDING_NAME_IN_USE = "INTERNAL_KEY_BINDING_NAME_IN_USE";
    public static final ErrorCode INTERNAL_KEY_BINDING_NAME_IN_USE = ErrorCode.NOT_SPECIFIED;
    {
        // Work-around for a not so convenient API
        INTERNAL_KEY_BINDING_NAME_IN_USE.setInternalErrorCode(_INTERNAL_KEY_BINDING_NAME_IN_USE);
    }

    /**
     * Creates a new instance of <code>InternalKeyBindingNameInUseException</code> without detail message.
     */
    public InternalKeyBindingNameInUseException() {
        super(INTERNAL_KEY_BINDING_NAME_IN_USE);
    }

    /**
     * Constructs an instance of <code>InternalKeyBindingNameInUseException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public InternalKeyBindingNameInUseException(String msg) {
        super(INTERNAL_KEY_BINDING_NAME_IN_USE, msg);
    }

    /**
     * Constructs an instance of <code>InternalKeyBindingNameInUseException</code> with the specified detail message.
     * @param exception the exception that caused this
     */
    public InternalKeyBindingNameInUseException(Exception exception) {
        super(INTERNAL_KEY_BINDING_NAME_IN_USE, exception);
    }

    /**
     * Constructs an instance of <code>InternalKeyBindingNameInUseException</code> with the specified detail message.
     * @param msg the detail message.
     * @param exception the exception that caused this
     */
    public InternalKeyBindingNameInUseException(String msg, Exception e) {
        super(INTERNAL_KEY_BINDING_NAME_IN_USE, msg, e);
    }
}