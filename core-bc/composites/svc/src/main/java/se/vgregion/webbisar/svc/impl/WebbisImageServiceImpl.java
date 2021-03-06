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

package se.vgregion.webbisar.svc.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.vgregion.webbisar.svc.Configuration;
import se.vgregion.webbisar.svc.WebbisImageService;

@Service("webbisImageService")
@Transactional
public class WebbisImageServiceImpl implements WebbisImageService {

    private static final Log LOGGER = LogFactory.getLog(WebbisImageServiceImpl.class);

    private Configuration cfg;

    @Autowired
    void setConfiguration(Configuration cfg) {
        this.cfg = cfg;
    }

    /**
     * {@inheritDoc}
     */
    public void resize(List<String> images) {
        for (String imagePath : images) {
            File imageFile = new File(cfg.getMultimediaFileBaseDir(), imagePath);
            try {
                LOGGER.info("resize: " + imageFile.getAbsolutePath());
                ImageUtil.scaleImage(imageFile, cfg.getImageSize(), cfg.getImageQuality());
            } catch (IOException e) {
                LOGGER.error("Failed to resize image " + imagePath, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteImages(List<String> toBeDeletedList) {
        for (String imagePath : toBeDeletedList) {
            File imageFile = new File(cfg.getMultimediaFileBaseDir(), imagePath);
            imageFile.delete();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cleanUpTempDir(String dir) {
        File tempDir = new File(new File(cfg.getMultimediaFileBaseDir(), "temp"), dir);
        ImageUtil.removeDir(tempDir);
    }

}
