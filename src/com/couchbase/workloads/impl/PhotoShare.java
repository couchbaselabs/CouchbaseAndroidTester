package com.couchbase.workloads.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.ektorp.AttachmentInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.WorkloadHelper;

public class PhotoShare extends CouchbaseWorkload {

    private int minimumDelay = 500;
    private int numFriends = 2;

    private final static Logger LOG = LoggerFactory
            .getLogger(PhotoShare.class);

    @Override
    protected String performWork() {

        if(extras.containsKey(WorkloadHelper.EXTRA_MINIMUM_DELAY)) {
            minimumDelay = (Integer)extras.get(WorkloadHelper.EXTRA_MINIMUM_DELAY);
        }

        if(extras.containsKey(WorkloadHelper.EXTRA_NUM_FRIENDS)) {
            numFriends = (Integer)extras.get(WorkloadHelper.EXTRA_NUM_FRIENDS);
        }

        int photosUploaded = 0;
        while(!thread.isCancelled()) {

            String id = UUID.randomUUID().toString();
            Map<String,Object> document = documentTemplate(id);
            workloadRunner.publishedWorkloadDocumentWithIdandRevision(id, "1");
            couchDbConnector.create(document);
//            String id = (String)document.get("_id");
            String rev = (String)document.get("_rev");


            String filename = "small.jpg";
            if(photosUploaded % 2 == 0) {
                filename = "large.jpg";
            }

            try {
                AttachmentInputStream ais = new AttachmentInputStream(filename, workloadRunner.openResource("attachments/images/" + filename), "image/jpeg");
                workloadRunner.publishedWorkloadDocumentWithIdandRevision(id, "2");
                rev = couchDbConnector.createAttachment(id, rev, ais);
                photosUploaded++;
                //task.publishWorkProgress("Uploaded Photo " + photosUploaded);
            } catch (IOException e) {
                LOG.error("Error reading attachment", e);
            }

            try {
                int delayBetweenPosts = (minimumDelay + new Random().nextInt(minimumDelay));
                Thread.sleep(delayBetweenPosts);
            } catch (InterruptedException e) {
                //ignore
            }

        }


        String resultMessage = "" +  photosUploaded + " photos uploaded";

        if(thread.isCancelled()) {
            resultMessage = "Cancelled PhotoShare after " + resultMessage;
        }
        else {
            resultMessage = "Finished PhotoShare " + resultMessage;
        }
        progress = 0;
        return resultMessage;

    }


    protected HashMap<String, Object> documentTemplate(String id) {
        HashMap<String, Object> exif = new HashMap<String, Object>();
        exif.put("ColorSpace", 1);
        exif.put("PixelYDimension", 2048);
        exif.put("DateTimeOriginal", "2011:07:03 12:10:49");
        exif.put("DateTimeDigitized", "2011:07:03 12:10:49");
        exif.put("PixelXDimension", 1536);

        HashMap<String, Object> tiff = new HashMap<String, Object>();
        tiff.put("Make", "Apple");
        tiff.put("Model", "iPhone 3GS");
        tiff.put("YResolution", 72);
        tiff.put("DateTime", "2011:07:03 12:10:49");
        tiff.put("Software", "4.3.3");
        tiff.put("XResolution", 72);

        HashMap<String, Object> gyro = new HashMap<String, Object>();
        gyro.put("x", 4.301908592578078e-78);
        gyro.put("y", 7.065152736643451e-307);
        gyro.put("z", -7.382861389261252e+306);

        HashMap<String, Object> attitude = new HashMap<String, Object>();
        attitude.put("pitch", 0);
        attitude.put("roll", 0);
        attitude.put("yaw", 0);

        HashMap<String, Object> accel = new HashMap<String, Object>();
        accel.put("x", -0.0543365478515625);
        accel.put("y", -0.8150482177734375);
        accel.put("z", -0.6339263916015625);

        HashMap<String, Object> motionData = new HashMap<String, Object>();
        motionData.put("gyro", gyro);
        motionData.put("attitude", attitude);
        motionData.put("accel", accel);

        HashMap<String, Object> media = new HashMap<String, Object>();
        media.put("DPIHeight", 72);
        media.put("{Exif}", exif);
        media.put("DPIWidth",  72);
        media.put("Orientation", 6);
        media.put("{TIFF}", tiff);

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("_id", id);
        result.put("mediaMetaData", media);
        result.put("motionData", motionData);
        result.put("author", extras.get(WorkloadHelper.EXTRA_NODE_ID));
        result.put("friends", workloadRunner.getRandomFriends((String)extras.get(WorkloadHelper.EXTRA_NODE_ID), numFriends));
        return result;
    }

    @Override
    public String getName() {
        return "Upload Photos";
    }

}
