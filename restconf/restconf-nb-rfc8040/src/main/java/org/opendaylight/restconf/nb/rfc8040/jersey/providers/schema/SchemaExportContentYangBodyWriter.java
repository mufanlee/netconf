/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.jersey.providers.schema;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.opendaylight.restconf.common.schema.SchemaExportContext;
import org.opendaylight.restconf.nb.rfc8040.Rfc8040;

@Provider
@Produces({ Rfc8040.MediaTypes.YANG })
public class SchemaExportContentYangBodyWriter implements MessageBodyWriter<SchemaExportContext> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return type.equals(SchemaExportContext.class);
    }

    @Override
    public long getSize(final SchemaExportContext context, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final SchemaExportContext context, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException,
            WebApplicationException {
        final PrintWriter writer = new PrintWriter(entityStream);
        writer.write(context.getModule().getSource());

    }
}