/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.dtgov.ui.server.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.DocumentArtifactType;
import org.overlord.dtgov.ui.server.services.sramp.SrampApiClientAccessor;
import org.overlord.sramp.atom.visitors.ArtifactContentTypeVisitor;
import org.overlord.sramp.client.SrampAtomApiClient;
import org.overlord.sramp.common.ArtifactType;
import org.overlord.sramp.common.visitors.ArtifactVisitorHelper;

/**
 * A standard servlet that makes it easy to download a deployment's binary content.
 *
 * @author eric.wittmann@redhat.com
 */
public class DeploymentDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = DeploymentDownloadServlet.class.hashCode();

    @Inject
    private SrampApiClientAccessor clientAccessor;

    /**
     * Constructor.
     */
    public DeploymentDownloadServlet() {
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {
        HttpServletResponse httpResponse = resp;
        try {
            SrampAtomApiClient client = clientAccessor.getClient();
            String uuid = req.getParameter("uuid"); //$NON-NLS-1$
            String type = req.getParameter("type"); //$NON-NLS-1$

            ArtifactType artyType = ArtifactType.valueOf(type);
            BaseArtifactType artifact = client.getArtifactMetaData(artyType, uuid);

            doDownloadContent(httpResponse, client, artyType, artifact);
        } catch (Exception e) {
            // TODO throw sensible errors (http responses - 404, 500, etc)
            throw new ServletException(e);
        }
    }

    /**
     * Downloads the content of the artifact.
     * @param httpResponse
     * @param client
     * @param artyType
     * @param artifact
     * @throws Exception
     */
    protected void doDownloadContent(HttpServletResponse httpResponse, SrampAtomApiClient client,
            ArtifactType artyType, BaseArtifactType artifact) throws Exception {
        InputStream artifactContent = null;
        try {
            // Set the content-disposition
            String artifactName = artifact.getName();
            String disposition = String.format("attachment; filename=\"%1$s\"", artifactName); //$NON-NLS-1$
            httpResponse.setHeader("Content-Disposition", disposition); //$NON-NLS-1$

            // Set the content-type
            ArtifactContentTypeVisitor ctVizzy = new ArtifactContentTypeVisitor();
            ArtifactVisitorHelper.visitArtifact(ctVizzy, artifact);
            String contentType = ctVizzy.getContentType().toString();
            httpResponse.setHeader("Content-Type", contentType); //$NON-NLS-1$

            // Set the content-size (if possible)
            if (artifact instanceof DocumentArtifactType) {
                DocumentArtifactType d = (DocumentArtifactType) artifact;
                long size = d.getContentSize();
                if (size != -1) {
                    httpResponse.setHeader("Content-Size", String.valueOf(size)); //$NON-NLS-1$
                }
            }

            // Make sure the browser doesn't cache it
            Date now = new Date();
            httpResponse.setDateHeader("Date", now.getTime()); //$NON-NLS-1$
            httpResponse.setDateHeader("Expires", now.getTime() - 86400000L); //$NON-NLS-1$
            httpResponse.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
            httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$

            artifactContent = client.getArtifactContent(artyType, artifact.getUuid());
            IOUtils.copy(artifactContent, httpResponse.getOutputStream());
        } finally {
            IOUtils.closeQuietly(artifactContent);
        }
    }
}
