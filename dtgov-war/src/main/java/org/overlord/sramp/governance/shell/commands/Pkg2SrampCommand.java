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
package org.overlord.sramp.governance.shell.commands;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.ExtendedArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.Property;
import org.overlord.sramp.atom.services.brms.BrmsConstants;
import org.overlord.sramp.atom.services.brms.assets.Assets;
import org.overlord.sramp.atom.services.brms.packages.Packages;
import org.overlord.sramp.client.SrampAtomApiClient;
import org.overlord.sramp.common.ArtifactType;
import org.overlord.sramp.shell.api.AbstractShellCommand;
import org.overlord.sramp.shell.api.Arguments;
import org.overlord.sramp.shell.api.ShellContext;
import org.overlord.sramp.shell.api.SimpleShellContext;

/**
 * BRMS command that will copy a package out of BRMS and push it into the
 * S-RAMP repository.
 *
 * @author eric.wittmann@redhat.com
 */
public class Pkg2SrampCommand extends AbstractShellCommand {

    /**
     * Main entry point - for use outside the interactive shell.
     * @param args
     * @throws Exception
     */
    public static void main(String [ ] args) throws Exception {
        String brmsPackageName = "SRAMPPackage"; //$NON-NLS-1$
        String tag             = "LATEST"; //$NON-NLS-1$
        String brmsBaseUrl     = "http://localhost:8080/drools-guvnor"; //$NON-NLS-1$
        String brmsUserId      = "admin"; //$NON-NLS-1$
        String brmsPassword    = "admin"; //$NON-NLS-1$
        if (args.length > 0) brmsPackageName = args[0];
        if (args.length > 1) tag             = args[1];
        if (args.length > 2) brmsBaseUrl     = args[2];
        if (args.length > 3) brmsUserId      = args[3];
        if (args.length > 4) brmsPassword    = args[4];
        StringBuilder argLine = new StringBuilder();
        argLine.append(brmsPackageName)
                .append(" ").append(tag) //$NON-NLS-1$
                .append(" ").append(brmsBaseUrl) //$NON-NLS-1$
                .append(" ").append(brmsUserId) //$NON-NLS-1$
                .append(" ").append(brmsPassword); //$NON-NLS-1$

        SrampAtomApiClient client = new SrampAtomApiClient("http://localhost:8080/s-ramp-server"); //$NON-NLS-1$
        QName clientVarName = new QName("s-ramp", "client"); //$NON-NLS-1$ //$NON-NLS-2$
        Pkg2SrampCommand cmd = new Pkg2SrampCommand();
        ShellContext context = new SimpleShellContext();
        context.setVariable(clientVarName, client);
        cmd.setArguments(new Arguments(argLine.toString()));
        cmd.setContext(context);
        cmd.execute();
    }

    private ClientRequestFactory fac = null;

    /**
     * Constructor.
     */
    public Pkg2SrampCommand() {
    }

    /**
     * @see org.overlord.sramp.shell.api.shell.ShellCommand#printUsage()
     */
    @Override
    public void printUsage() {
        print("brms:pkg2sramp <brmsPackageName> <tag> <brmsBaseUrl> <brmsUserId> <brmsPassword>"); //$NON-NLS-1$
    }

    /**
     * @see org.overlord.sramp.shell.api.shell.ShellCommand#printHelp()
     */
    @Override
    public void printHelp() {
        print("The 'pkg2sramp' command copies a named BRMS package from the"); //$NON-NLS-1$
        print("BRMS system and uploads it to the S-RAMP repository."); //$NON-NLS-1$
        print(""); //$NON-NLS-1$
        print("Example usage:"); //$NON-NLS-1$
        print("> brms:pkg2sramp SRAMPPackage LATEST http://localhost:8080/drools-guvnor admin admin"); //$NON-NLS-1$
    }

    /**
     * @see org.overlord.sramp.shell.api.shell.ShellCommand#execute()
     */
    @Override
    public boolean execute() throws Exception {
        QName clientVarName = new QName("s-ramp", "client"); //$NON-NLS-1$ //$NON-NLS-2$
        SrampAtomApiClient client = (SrampAtomApiClient) getContext().getVariable(clientVarName);
        if (client == null) {
            print("No S-RAMP repository connection is currently open."); //$NON-NLS-1$
            return false;
        }

        String brmsPackageName = optionalArgument(0, "SRAMPPackage"); //$NON-NLS-1$
        String tag             = optionalArgument(1, "LATEST"); //$NON-NLS-1$
        String brmsBaseUrl     = optionalArgument(2, "http://localhost:8080/drools-guvnor"); //$NON-NLS-1$
        String brmsUserId      = optionalArgument(3, "admin"); //$NON-NLS-1$
        String brmsPassword    = optionalArgument(4, "admin"); //$NON-NLS-1$

        print("Copying BRMS package to S-RAMP using: "); //$NON-NLS-1$
        print("   brmsPackageName..: %1$s", brmsPackageName); //$NON-NLS-1$
        print("   tag .............: %1$s", tag); //$NON-NLS-1$
        print("   brmsBaseUrl......: %1$s", brmsBaseUrl); //$NON-NLS-1$
        print("   brmsUserId.......: %1$s", brmsUserId); //$NON-NLS-1$
        print("   brmsPassword.....: %1$s", brmsPassword); //$NON-NLS-1$

        String brmsURLStr = brmsBaseUrl + "/rest/packages/"; //$NON-NLS-1$
        boolean brmsExists = urlExists(brmsURLStr, brmsUserId, brmsPassword);
        if (!brmsExists) {
            print("Can't find BRMS endpoint: " + brmsURLStr); //$NON-NLS-1$
            return false;
        }

        try {
            uploadBrmsPackage(brmsBaseUrl, brmsPackageName, tag, brmsUserId, brmsPassword, client);
        } catch (Exception e) {
            print("FAILED to copy the BRMS package."); //$NON-NLS-1$
            print("\t" + e.getMessage()); //$NON-NLS-1$
            return false;
        }
        print("**********************************************************************"); //$NON-NLS-1$
        return true;
    }

    /**
     * Returns true if the given URL can be accessed.
     * @param checkUrl
     * @param user
     * @param password
     */
    public boolean urlExists(String checkUrl, String user, String password) {
        try {
            URL checkURL = new URL(checkUrl);
            HttpURLConnection checkConnection = (HttpURLConnection) checkURL.openConnection();
            checkConnection.setRequestMethod("GET"); //$NON-NLS-1$
            checkConnection.setRequestProperty("Accept", "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$
            checkConnection.setConnectTimeout(10000);
            checkConnection.setReadTimeout(10000);
            applyAuth(checkConnection, user, password);
            checkConnection.connect();
            return (checkConnection.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Applies basic auth to the given HTTP URL connection.
     * @param connection
     * @param user
     * @param password
     */
    protected void applyAuth(HttpURLConnection connection, String user, String password) {
        String auth = user + ":" + password; //$NON-NLS-1$
        connection.setRequestProperty("Authorization", "Basic " //$NON-NLS-1$ //$NON-NLS-2$
                + new String(Base64.encodeBase64(auth.getBytes())));
    }

    /**
     * Uploads the BMRS package.
     * @param brmsBaseUrl
     * @param pkgName
     * @param tag
     * @param userId
     * @param password
     * @param client
     * @throws Exception
     */
    public void uploadBrmsPackage(String brmsBaseUrl, String pkgName, String tag, String userId,
            String password, SrampAtomApiClient client) throws Exception {
        // http://localhost:8080/drools-guvnor/org.drools.guvnor.Guvnor/package/srampPackage/S_RAMP_0.0.3.0
        String urlStr = brmsBaseUrl + "/org.drools.guvnor.Guvnor/package/" + pkgName + "/" + tag; //$NON-NLS-1$ //$NON-NLS-2$

        Credentials credentials = new UsernamePasswordCredentials(userId, password);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
        ClientExecutor clientExecutor = new ApacheHttpClient4Executor(httpClient);
        fac = new ClientRequestFactory(clientExecutor, new URI(brmsBaseUrl));

        Map<String, Packages.Package> brmsPkgMap = getPkgsFromBrms(brmsBaseUrl);
        if (! brmsPkgMap.containsKey(pkgName)) {
            print("Brms contains the following BRMS Packages"); //$NON-NLS-1$
            for (String name : brmsPkgMap.keySet()) {
                print(" * " + name); //$NON-NLS-1$
            }
            throw new Exception ("Could not find package with name " + pkgName + " in BRMS"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        Packages.Package brmsPkg = brmsPkgMap.get(pkgName);

        print("Located BRMS package '" + pkgName + "' :"); //$NON-NLS-1$ //$NON-NLS-2$
        print("   UUID ............: " + brmsPkg.getMetadata().getUuid()); //$NON-NLS-1$
        print("   Version .........: " + brmsPkg.getMetadata().getVersionNumber()); //$NON-NLS-1$
        print("   Author ..........: " + brmsPkg.getAuthor()); //$NON-NLS-1$
        print("   Last published ..: " + brmsPkg.getPublished()); //$NON-NLS-1$
        print("   Description .....: " + brmsPkg.getDescription()); //$NON-NLS-1$

        // now uploading this into s-ramp
        @SuppressWarnings("deprecation")
        ExtendedArtifactType extendedArtifactType =
            (ExtendedArtifactType) ArtifactType.fromFileExtension("pkg").newArtifactInstance(); //$NON-NLS-1$
        extendedArtifactType.setUuid(brmsPkg.getMetadata().getUuid());
        extendedArtifactType.setName(pkgName + ".pkg"); //$NON-NLS-1$

        Property assetsProperty = new Property();
        assetsProperty.setPropertyName(BrmsConstants.ASSET_INFO_XML);
        String assetsXml = getAssetsStringFromBrms(brmsBaseUrl, pkgName);
        //update the links
        String srampUrl = client.getEndpoint().substring(0,client.getEndpoint().lastIndexOf("/")); //$NON-NLS-1$
        assetsXml = assetsXml.replaceAll(brmsBaseUrl, srampUrl + "/brms"); //$NON-NLS-1$
        assetsProperty.setPropertyValue(assetsXml);
        extendedArtifactType.getProperty().add(assetsProperty);

        print("Reading " + pkgName + " from url " + urlStr); //$NON-NLS-1$ //$NON-NLS-2$
        ClientResponse<InputStream> pkgResponse = getInputStream(urlStr);
        InputStream content = pkgResponse.getEntity();

        BaseArtifactType artifact = client.uploadArtifact(extendedArtifactType, content);
        IOUtils.closeQuietly(content);
        print("Uploaded " + pkgName + " UUID=" + artifact.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$

        // Now obtaining the assets in the this package, and upload those
        // TODO set relationship to parent pkg
        Assets assets = getAssetsFromBrms(brmsBaseUrl, pkgName);

        //Upload the process AND process-image, making sure the uuid is identical to the one mentioned
        for (Assets.Asset asset : assets.getAsset()) {
            if (!"package".equalsIgnoreCase(asset.getMetadata().getFormat())) { //$NON-NLS-1$
                //Upload the asset
                String fileName = asset.getTitle() + "." + asset.getMetadata().getFormat().toLowerCase(); //$NON-NLS-1$
                String uuid = asset.getMetadata().getUuid();
                //reading the asset from disk
                //http://localhost:8080/drools-guvnor/rest/packages/srampPackage/assets/
                String assetURLStr = brmsBaseUrl + "/rest/packages/" + pkgName + "/assets/" + asset.getTitle() + "/binary"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                //print("Reading asset " + asset.getTitle() + " from url " + assetURLStr );
                ClientResponse<InputStream> assetResponse = getInputStream(assetURLStr);
                InputStream assetInputStream = assetResponse.getEntity();

                //upload the asset using the uuid
                @SuppressWarnings("deprecation")
                ArtifactType artifactType = ArtifactType.fromFileExtension(asset.getMetadata().getFormat());
                BaseArtifactType baseArtifactType = artifactType.newArtifactInstance();
                baseArtifactType.setName(fileName);
                baseArtifactType.setUuid(uuid);

                BaseArtifactType assetArtifact = client.uploadArtifact(baseArtifactType, assetInputStream);
                IOUtils.closeQuietly(assetInputStream);
                print("Uploaded asset " + assetArtifact.getUuid() + " " + assetArtifact.getName()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        print("OK"); //$NON-NLS-1$

    }

    /**
     * Gets the packages from BRMS at the given URL.
     * @param brmsBaseUrl
     * @throws Exception
     */
    protected Map<String, Packages.Package> getPkgsFromBrms(String brmsBaseUrl) throws Exception {
        String pkgsUrl = brmsBaseUrl + "/rest/packages/"; //$NON-NLS-1$
        print("Reading from " + pkgsUrl + " to find all packages in BRMS.."); //$NON-NLS-1$ //$NON-NLS-2$
        ClientRequest pkgsRequest = fac.createRequest(pkgsUrl);
        pkgsRequest.accept(MediaType.APPLICATION_XML);
        ClientResponse<Packages> pkgsResponse = pkgsRequest.get(Packages.class);
        if (pkgsResponse.getStatus() != 200) {
            throw new Exception("Failed : HTTP error code : " //$NON-NLS-1$
                    + pkgsResponse.getStatus());
        }
        Packages packages = pkgsResponse.getEntity();
        Map<String, Packages.Package> brmsPkgMap = new HashMap<String, Packages.Package>();
        for (Packages.Package brmsPkg : packages.getPackage()) {
            brmsPkgMap.put(brmsPkg.getTitle(), brmsPkg);
        }
        return brmsPkgMap;
    }

    /**
     * Gets the assets XML as a string.
     * @param brmsBaseUrl
     * @param pkgName
     * @throws Exception
     */
    protected String getAssetsStringFromBrms(String brmsBaseUrl, String pkgName) throws Exception {
        String assetsUrl = brmsBaseUrl + "/rest/packages/" + pkgName + "/assets"; //$NON-NLS-1$ //$NON-NLS-2$
        print("Reading from " + assetsUrl + " to find all assets in package " + pkgName); //$NON-NLS-1$ //$NON-NLS-2$
        ClientRequest assetsRequest = fac.createRequest(assetsUrl);
        assetsRequest.accept(MediaType.APPLICATION_XML);
        ClientResponse<String> assetsResponse = assetsRequest.get(String.class);
        if (assetsResponse.getStatus() != 200) {
            throw new Exception("Failed : HTTP error code : " //$NON-NLS-1$
                    + assetsResponse.getStatus());
        }
        String assetsXml = assetsResponse.getEntity();
        return assetsXml;
    }

    /**
     * Gets the assets from BRMS.
     * @param brmsBaseUrl
     * @param pkgName
     * @throws Exception
     */
    protected Assets getAssetsFromBrms(String brmsBaseUrl, String pkgName) throws Exception {
        String assetsUrl = brmsBaseUrl + "/rest/packages/" + pkgName + "/assets"; //$NON-NLS-1$ //$NON-NLS-2$
        print("Reading from " + assetsUrl + " to find all assets in package " + pkgName); //$NON-NLS-1$ //$NON-NLS-2$
        ClientRequest assetsRequest = fac.createRequest(assetsUrl);
        assetsRequest.accept(MediaType.APPLICATION_XML);
        ClientResponse<Assets> assetsResponse = assetsRequest.get(Assets.class);
        if (assetsResponse.getStatus() != 200) {
            throw new Exception("Failed : HTTP error code : " //$NON-NLS-1$
                    + assetsResponse.getStatus());
        }
        Assets assets = assetsResponse.getEntity();
        return assets;
    }

    /**
     * Gets the input stream at the given URL.
     * @param url
     * @throws Exception
     */
    public ClientResponse<InputStream> getInputStream(String url) throws Exception {
        ClientRequest request = fac.createRequest(url);
        ClientResponse<InputStream> response = request.get(InputStream.class);
        if (response.getStatus() != 200) {
            throw new Exception("Failed : HTTP error code : " //$NON-NLS-1$
                    + response.getStatus());
        }
        return response;
    }

}
