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

package se.vgregion.webbisar.beans;

import static org.apache.commons.lang.StringUtils.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.vgregion.webbisar.types.MultimediaFile;
import se.vgregion.webbisar.types.Sex;
import se.vgregion.webbisar.types.Webbis;

public class PreviewWebbisBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<WebbisPreview> webbisPreviews = new ArrayList<WebbisPreview>();

    public PreviewWebbisBean(String mediaFileBaseUrl, Webbis webbis, int selectedImage) {
        // "Main" webbis
        webbisPreviews.add(new WebbisPreview(mediaFileBaseUrl, webbis, selectedImage));
        // Multiple birth siblings, if any (for twins/triplets)
        if (webbis.getMultipleBirthSiblings() != null) {
            for (Webbis w : webbis.getMultipleBirthSiblings()) {
                webbisPreviews.add(new WebbisPreview(mediaFileBaseUrl, w, selectedImage));
            }
        }
    }

    public List<WebbisPreview> getWebbisPreviews() {
        return webbisPreviews;
    }

    public class WebbisPreview {
        private Long id;
        private String header;
        private String date;
        private String time;
        private String weight;
        private String length;
        private String parent1;
        private String parent2;
        private String siblings;
        private String home;
        private String hospital;
        private MultimediaFile[] mediaFiles;
        private String message;
        private String homePage;
        private int selectedImage;
        private String selectedImageComment;
        private String selectedImageMediaType;
        private String selectedImageContentType;
        private String mediaFileBaseUrl;

        protected WebbisPreview(String mediaFileBaseUrl, Webbis webbis, int selectedImage) {
            this.id = webbis.getId();
            this.header = generateHeader(webbis);
            this.time = webbis.getBirthTime().getTime();
            this.date = webbis.getBirthTime().getSmartTime(new Date());
            this.weight = webbis.getWeight() + "g";
            this.length = webbis.getLength() + "cm";
            this.parent1 = webbis.getParents().size() > 0 ? webbis.getParents().get(0).getFullName() : "";
            this.parent2 = webbis.getParents().size() > 1 ? webbis.getParents().get(1).getFullName() : "";
            this.siblings = webbis.getSiblings();
            this.home = webbis.getHome();
            this.hospital = webbis.getHospital().toLongString();
            this.mediaFiles = new MultimediaFile[webbis.getMediaFiles().size()];
            int cnt = 0;
            for (MultimediaFile file : webbis.getMediaFiles()) {
                file.setLocation(file.getLocation());
                this.mediaFiles[cnt++] = file;
            }
            this.selectedImage = selectedImage;
            if (webbis.getMediaFiles().size() > selectedImage) {
                MultimediaFile mediaFile = webbis.getMediaFiles().get(selectedImage);
                this.selectedImageComment = mediaFile.getText();
                this.selectedImageMediaType = mediaFile.getMediaType().toString();
                this.selectedImageContentType = mediaFile.getContentType();
            }
            this.homePage = webbis.getHomePage();
            this.message = webbis.getMessage();
            this.mediaFileBaseUrl = mediaFileBaseUrl;
        }

        public String getMediaFileBaseUrl() {
            return mediaFileBaseUrl;
        }

        private String generateHeader(Webbis webbis) {
            String name = isNotEmpty(webbis.getName()) ? webbis.getName()
                    : webbis.getSex() == Sex.Female ? "Flicka" : "Pojke";

            String date = webbis.getBirthTime().getSmartTime(new Date());

            String parents = getParentsNames(webbis);

            StringBuffer sb = new StringBuffer().append(name).append(" född ").append(date);
            if (isNotEmpty(parents)) {
                sb.append(", ").append(parents);
            }
            return sb.toString();
        }

        private String getParentsNames(Webbis webbis) {
            switch (webbis.getParentsFirstNames().size()) {
                case 1:
                    return webbis.getParentsFirstNames().get(0);
                case 2:
                    return webbis.getParentsFirstNames().get(0) + " och " + webbis.getParentsFirstNames().get(1);
                default:
                    return "";
            }
        }

        public Long getId() {
            return id;
        }

        public String getHeader() {
            return header;
        }

        public String getTime() {
            return time;
        }

        public String getDate() {
            return date;
        }

        public String getLongDate() {
            return date + " " + time;
        }

        public String getWeight() {
            return weight;
        }

        public String getLength() {
            return length;
        }

        public String getParent1() {
            return parent1;
        }

        public String getParent2() {
            return parent2;
        }

        public String getSiblings() {
            return siblings;
        }

        public String getHome() {
            return home;
        }

        public String getHospital() {
            return hospital;
        }

        public String getMessage() {
            return message;
        }

        public String getHomePage() {
            return homePage;
        }

        public String getSelectedImageUrl() {
            if (mediaFiles.length <= selectedImage) {
                return mediaFileBaseUrl + "/no-image.jpg";
            }
            return mediaFileBaseUrl + "/" + mediaFiles[selectedImage].getLocation();
        }

        public String getVideoThumbUrl() {
            return mediaFileBaseUrl + "/video-thumb.png";
        }

        public String getSelectedImageComment() {
            return selectedImageComment;
        }

        public String getSelectedImageMediaType() {
            return selectedImageMediaType;
        }

        public String getSelectedImageContentType() {
            return selectedImageContentType;
        }

        public MultimediaFile[] getMediaFiles() {
            return mediaFiles;
        }
    }
}
