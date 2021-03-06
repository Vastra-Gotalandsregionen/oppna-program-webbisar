/**
 * Copyright 2010 Västra Götalandsregionen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 *
 */

package se.vgregion.webbisar.portlet;

import org.apache.commons.fileupload.portlet.PortletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.vgregion.webbisar.beans.MainWebbisBean;
import se.vgregion.webbisar.helpers.FileHandler;
import se.vgregion.webbisar.helpers.WebbisPortletHelper;
import se.vgregion.webbisar.helpers.WebbisPortletHelper.WebbisValidationException;
import se.vgregion.webbisar.helpers.WebbisServiceProxy;
import se.vgregion.webbisar.types.Hospital;
import se.vgregion.webbisar.types.Webbis;
import se.vgregion.webbisar.util.CallContext;
import se.vgregion.webbisar.util.CallContextUtil;

import javax.portlet.*;
import java.io.IOException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * EditWebbis Portlet Class
 *
 * @author sofiajonsson
 */
public class EditWebbisPortlet extends GenericPortlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditWebbisPortlet.class);

    // these parameters are set in the action phase. They help decide which view
    // to redirect to in the render phase
    private static final String VIEW = "VIEW";
    private static final String MAIN_VIEW = "MAIN_VIEW";
    private static final String ADD_IMAGES_VIEW = "ADD_IMAGES_VIEW";
    private static final String CONFIRMATION_VIEW = "CONFIRMATION_VIEW";
    private static final String SHOW_WEBBIS_LIST_VIEW = "SHOW_WEBBIS_LIST_VIEW";
    private static final String CONFIRM_DELETE_WEBBIS_VIEW = "CONFIRM_DELETE_WEBBIS_VIEW";
    private static final String PREVIEW_VIEW = "PREVIEW_VIEW";

    private static final int SUPPORTED_MULTIPLE_BIRTH_SIBLINGS = 3; // Triplets
    private static final int SUPPORTED_NO_OF_MEDIAFILES = 4;

    private WebbisPortletHelper helper = null;
    private WebbisServiceProxy webbisServiceProxy = null;

    @Override
    public void init(PortletConfig config) throws PortletException {
        super.init(config);
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {

        // Moved proxy init out of portlet init() to enable easy localhost deploy
        initServiceProxyAndHelper();

        response.setContentType("text/html");
        PortletRequestDispatcher dispatcher = null;
        Object view = request.getParameter(VIEW);

        String userId = null;
        try {
            userId = helper.getUserId(request);
        } catch (RuntimeException re) {
            // userId was probably not found in request
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/UserIdNotFound.jsp");
            dispatcher.include(request, response);
            return;
        }

        if ((view == null) || (view.equals(SHOW_WEBBIS_LIST_VIEW))) {
            // CleanUp so the session is empty
            helper.cleanUp(request.getPortletSession(true));

            List<MainWebbisBean> webbisar = webbisServiceProxy.getWebbisarForAuthorId(userId);
            if (webbisar != null && webbisar.size() > 0) {
                // helper.storeMyWebbisarInSession(request, webbisar);
                request.setAttribute("webbisar", webbisar);
                request.setAttribute("userId", userId);
                dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ShowWebbisList.jsp");
            } else {
                // show an empty main edit webbis page - for adding a new webbis
                helper.populateDefaults(request.getPortletSession(true), null);
                // this is used in the jsp
                request.setAttribute("currentYear", new GregorianCalendar().get(Calendar.YEAR));
                request.setAttribute("hospitals", Hospital.values());
                request.setAttribute("userId", userId);
                dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/EditWebbis.jsp");
            }
        } else if (view.equals(ADD_IMAGES_VIEW)) {// show add picture page
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/AddImages.jsp");
        } else if (view.equals(PREVIEW_VIEW)) {// show preview page
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/PreviewWebbis.jsp");
        } else if (view.equals(CONFIRMATION_VIEW)) {// show confirmation page
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/Confirmation.jsp");
        } else if (view.equals(CONFIRM_DELETE_WEBBIS_VIEW)) {// show confirm delete page
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/ConfirmDelete.jsp");
        } else {
            // MAIN_VIEW and any other (unexpected/undefined) view
            helper.populateDefaults(request.getPortletSession(true), request.getParameter("noOfSiblings"));

            // this is used in the jsp
            request.setAttribute("currentYear", new GregorianCalendar().get(Calendar.YEAR));
            request.setAttribute("hospitals", Hospital.values());
            request.setAttribute("userId", userId);
            // show the main edit webbis page
            dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/EditWebbis.jsp");
        }

        dispatcher.include(request, response);
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException,
            IOException {

        // Moved proxy init out of portlet init() to enable easy localhost deploy
        initServiceProxyAndHelper();

        try {
            String userId = null;
            try {
                userId = helper.getUserId(request);
            } catch (RuntimeException re) {
                // userId was probably not found in request
                response.setRenderParameter(VIEW, SHOW_WEBBIS_LIST_VIEW);
                return;
            }

            CallContextUtil.setContext(new CallContext(userId));

            // Check if we have a file upload request
            if (PortletFileUpload.isMultipartContent(request)) {
                try {
                    // Try to save the images/video using FTP.
                    List<String> imageFiles = helper.parseAndSaveMultipartFiles(request);
                    // If null then user probably pressed cancel
                    if (imageFiles != null) {
                        // Now tell the server to resize them.
                        webbisServiceProxy.resize(imageFiles);
                    }
                    response.setRenderParameter(VIEW, MAIN_VIEW);
                } catch (WebbisValidationException e) {
                    request.setAttribute("validationMessages", e.getValidationMessages());
                    response.setRenderParameter(VIEW, ADD_IMAGES_VIEW);
                }
            } else {
                // check which button was pressed and handle each request accordingly
                if (request.getParameter("preview") != null) {
                    helper.saveWebbisFormInSession(request, null);
                    Webbis webbis;
                    try {
                        webbis = helper.createWebbis(request);
                        helper.saveWebbisInSession(request.getPortletSession(), webbis);
                        request.setAttribute("previewWebbis", helper.createPreviewWebbisBean(request, webbis));
                        response.setRenderParameter(VIEW, PREVIEW_VIEW);
                    } catch (WebbisValidationException wve) {
                        request.setAttribute("validationMessages", wve.getValidationMessages());
                        response.setRenderParameter(VIEW, MAIN_VIEW);
                    }

                } else if (request.getParameter("publish") != null) {
                    // has the user accepted the terms?
                    if (request.getParameter("accept") == null) {
                        request.setAttribute("validationMessages", "Villkoren måste accepteras");
                        request.setAttribute("previewWebbis", helper.createPreviewWebbisBean(request, helper
                                .getWebbisFromSession(request.getPortletSession())));
                        response.setRenderParameter(VIEW, PREVIEW_VIEW);
                    } else {
                        webbisServiceProxy.saveWebbis(request.getPortletSession(true).getId(), helper
                                .getWebbisFromSession(request.getPortletSession()));
                        helper.cleanUp(request.getPortletSession(true));
                        response.setRenderParameter(VIEW, CONFIRMATION_VIEW);
                    }
                } else if (request.getParameter("cancelPreview") != null) {
                    response.setRenderParameter(VIEW, MAIN_VIEW);
                } else if (request.getParameter("backFromConfirm") != null) {
                    response.setRenderParameter(VIEW, SHOW_WEBBIS_LIST_VIEW);
                } else if (request.getParameter("editWebbisId") != null) {
                    Webbis webbis = webbisServiceProxy.prepareForEditing(request.getPortletSession(true).getId(),
                            request.getParameter("editWebbisId"));
                    helper.putWebbisDataInSession(request.getPortletSession(true), helper
                            .getMainWebbisBeanForWebbis(webbis));
                    request.setAttribute("editWebbisId", request.getParameter("editWebbisId"));
                    response.setRenderParameter(VIEW, MAIN_VIEW);
                } else if (request.getParameter("deleteWebbis") != null) {
                    // show the confirmDelete Page
                    request.setAttribute("webbisId", request.getParameter("w0_webbisId"));
                    response.setRenderParameter(VIEW, CONFIRM_DELETE_WEBBIS_VIEW);
                } else if (request.getParameter("cancelDelete") != null) {
                    response.setRenderParameter(VIEW, MAIN_VIEW);
                } else if (request.getParameter("confirmDelete") != null) {
                    webbisServiceProxy.deleteWebbis(request.getParameter("webbisId"));
                    webbisServiceProxy.cleanUpTempDir(request.getPortletSession(true).getId());
                    response.setRenderParameter(VIEW, CONFIRMATION_VIEW);
                } else if (request.getParameter("cancel") != null) {
                    helper.cleanUp(request.getPortletSession(true));
                    webbisServiceProxy.cleanUpTempDir(request.getPortletSession(true).getId());
                    response.setRenderParameter(VIEW, SHOW_WEBBIS_LIST_VIEW);
                } else {
                    // MultimediaFile: Add, remove or change main
                    // Note: because we cannot rely on javascript we have to handle this on the server.

                    // Check if user wants to add image
                    boolean addingNewImage = false;
                    Enumeration<String> paramNames = request.getParameterNames();
                    while (paramNames.hasMoreElements()) {
                        String paramName = paramNames.nextElement();
                        if (paramName.startsWith("addImages")) {
                            addingNewImage = true;
                            String webbisIndex = paramName.replace("addImages_w", "");
                            helper.saveWebbisFormInSession(request, Integer.valueOf(webbisIndex));
                            response.setRenderParameter(VIEW, ADD_IMAGES_VIEW);
                            break;
                        }
                    }

                    // Not adding new image, handle remove image or setting of main image
                    if (!addingNewImage) {
                        helper.saveWebbisFormInSession(request, null);
                        response.setRenderParameter(VIEW, MAIN_VIEW);
                        // Check which image operation that should be performed
                        handleImageOperations(request);
                    }
                }
            }
        } finally {
            CallContextUtil.clear();
        }
    }

    private void initServiceProxyAndHelper() {
        if (webbisServiceProxy == null) {
            try {
                webbisServiceProxy = new WebbisServiceProxy();
                String baseUrl = webbisServiceProxy.getImageBaseUrl();
                String ftpCfg = webbisServiceProxy.getFtpConfig();
                Boolean testMode = webbisServiceProxy.isTestMode();
                int maxNoOfVideoFiles = webbisServiceProxy.getMaxNoOfVideoFiles();
                int maxVideoFileSize = webbisServiceProxy.getMaxVideoFileSize();

                FileHandler fileHandler = new FileHandler(ftpCfg);

                helper = new WebbisPortletHelper(baseUrl, fileHandler, testMode, maxNoOfVideoFiles,
                        maxVideoFileSize);
            } catch (Exception ex) {
                LOGGER.error("The configuration is broken", ex);
                webbisServiceProxy = null;
                throw new RuntimeException(ex);
            }
        }
    }

    private void handleImageOperations(ActionRequest request) {
        // For all multiple birth siblings, check if operation was performed
        siblingLoop:
        for (int i = 0; i < SUPPORTED_MULTIPLE_BIRTH_SIBLINGS; i++) {
            // Check if we have a remove image operation in request
            for (int x = 0; x < SUPPORTED_NO_OF_MEDIAFILES; x++) {
                if (request.getParameter("w" + i + "_remove-mediaFile" + x) != null) {
                    helper.removeMediaFile(i, x, request);
                    break siblingLoop;
                }
            }
            // Check if we have a set main image operation in request
            for (int x = 0; x < SUPPORTED_NO_OF_MEDIAFILES; x++) {
                if (request.getParameter("w" + i + "_mediaFile" + x + "-main-image") != null) {
                    helper.setMainImage(i, x, request);
                    break siblingLoop;
                }
            }
        }
    }
}
