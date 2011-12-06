package com.couchbase.androidtester.workloads.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.couchbase.androidtester.workloads.CouchbaseWorkload;

public class Calendar extends CouchbaseWorkload {

    private static int numberOfEvents = 100;

    public Calendar() {
        indeterminate = false;
        total = numberOfEvents;
    }

    @Override
    protected String performWork() {

        int calendarEventsCreated = 0;

        while(!task.isCancelled() && (calendarEventsCreated < numberOfEvents)) {

            //create a calendar item
            String id = UUID.randomUUID().toString();
            Map<String,Object> document = documentTemplate(id);
            couchDbConnector.create(document);
            calendarEventsCreated++;

            //update the calendar item
            document = cancelEvent(document);
            couchDbConnector.update(document);

            progress++;
            task.publishWorkProgress("Processed Even " + calendarEventsCreated);

        }


        String resultMessage = "" +  calendarEventsCreated + " events created";

        if(task.isCancelled()) {
            resultMessage = "Cancelled Calendar Usage after " + resultMessage;
        }
        else {
            resultMessage = "Finished Calendar Usage " + resultMessage;
        }
        progress = 0;
        return resultMessage;

    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> cancelEvent(Map<String, Object> document) {
        Map<String, Object> data = (Map<String,Object>)document.get("data");
        data.put("status", "canceled");
        document.put("data", data);
        return document;
    }

    protected HashMap<String, Object> documentTemplate(String id) {
        HashMap<String, Object> when = new HashMap<String, Object>();
        when.put("start", "2010-04-17T15:00:00.000Z");
        when.put("end", "2010-04-17T17:00:00.000Z");


        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("title", "CouchConf");
        data.put("details", "Join us at a CouchConf near you!");
        data.put("transparency", "opaque");
        data.put("status", "confirmed");
        data.put("location", "New York, NY");
        data.put("when", when);

        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("_id", id);
        result.put("data", data);
        return result;
    }

    @Override
    public String getName() {
        return "Calendar Events";
    }

}