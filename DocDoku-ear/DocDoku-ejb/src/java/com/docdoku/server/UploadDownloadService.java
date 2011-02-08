/*
 * DocDoku, Professional Open Source
 * Copyright 2006, 2007, 2008, 2009, 2010 DocDoku SARL
 *
 * This file is part of DocDoku.
 *
 * DocDoku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDoku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DocDoku.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.docdoku.server;

import com.docdoku.core.services.CreationException;
import com.docdoku.core.services.FileAlreadyExistsException;
import com.docdoku.core.services.FileNotFoundException;
import com.docdoku.core.services.ICommandLocal;
import com.docdoku.core.services.IUploadDownloadWS;
import com.docdoku.core.services.MasterDocumentNotFoundException;
import com.docdoku.core.services.MasterDocumentTemplateNotFoundException;
import com.docdoku.core.services.NotAllowedException;
import com.docdoku.core.services.UserNotActiveException;
import com.docdoku.core.services.UserNotFoundException;
import com.docdoku.core.services.WorkspaceNotFoundException;
import com.docdoku.core.common.BasicElementKey;
import com.docdoku.core.document.DocumentKey;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.MTOM;

/**
 *
 * @author Florent GARIN
 */
@MTOM
@Local(IUploadDownloadWS.class)
@Stateless(name = "UploadDownloadService")
@WebService(serviceName = "UploadDownloadService", endpointInterface = "com.docdoku.core.IUploadDownloadWS")
public class UploadDownloadService implements IUploadDownloadWS {

    @EJB
    private ICommandLocal commandService;

    @RolesAllowed("users")
    public
    @XmlMimeType("application/octet-stream")
    @Override
    DataHandler downloadFromDocument(String workspaceId, String mdocID, String mdocVersion, int iteration, String fileName) throws NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        String fullName = workspaceId + "/documents/" + mdocID + "/" + mdocVersion + "/" + iteration + "/" + fileName;
        File dataFile = commandService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }

    @RolesAllowed("users")
    public
    @XmlMimeType("application/octet-stream")
    @Override
    DataHandler downloadFromTemplate(String workspaceId, String templateID, String fileName) throws NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        String fullName = workspaceId + "/templates/" + templateID + "/" + fileName;
        File dataFile = commandService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }

    @RolesAllowed("users")
    @Override
    public void uploadToDocument(String workspaceId, String mdocID, String mdocVersion, int iteration, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws IOException, CreationException, WorkspaceNotFoundException, NotAllowedException, MasterDocumentNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException {
        DocumentKey docPK = null;
        File vaultFile = null;

        docPK = new DocumentKey(workspaceId, mdocID, mdocVersion, iteration);

        vaultFile = commandService.saveFileInDocument(docPK, fileName, 0);

        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();
        //StreamingDataHandler dh = (StreamingDataHandler) data;
        //dh.moveTo(vaultFile);
        //dh.close();
        commandService.saveFileInDocument(docPK, fileName, vaultFile.length());

    }

    @RolesAllowed("users")
    @Override
    public void uploadToTemplate(String workspaceId, String templateID, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws IOException, CreationException, WorkspaceNotFoundException, NotAllowedException, MasterDocumentTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException {
        BasicElementKey templatePK = null;
        File vaultFile = null;

        templatePK = new BasicElementKey(workspaceId, templateID);
        vaultFile = commandService.saveFileInTemplate(templatePK, fileName, 0);
        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();

        //StreamingDataHandler dh = (StreamingDataHandler) data;
        //dh.moveTo(vaultFile);
        //dh.close();
        commandService.saveFileInTemplate(templatePK, fileName, vaultFile.length());

    }
}
