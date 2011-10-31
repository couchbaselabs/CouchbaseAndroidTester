package com.couchbase.workloads.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.ektorp.UpdateConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.WorkloadHelper;

public class CRUDDocuments extends CouchbaseWorkload {

    private int numFriends = 2;

    private final static Logger LOG = LoggerFactory
            .getLogger(CRUDDocuments.class);

	@Override
	protected String performWork() {

        if(extras.containsKey(WorkloadHelper.EXTRA_NUM_FRIENDS)) {
            numFriends = (Integer)extras.get(WorkloadHelper.EXTRA_NUM_FRIENDS);
        }

		int documentsCreated = 0;
		while(!thread.isCancelled()) {

			//create
		    String id = UUID.randomUUID().toString();
			Map<String, Object> document = documentTemplate(id);
			workloadRunner.publishedWorkloadDocumentWithIdandRevision(id, "1");
			couchDbConnector.create(document);
			documentsCreated++;

			String documentId = (String)document.get("_id");
			LOG.debug("Document created got id " + documentId);

			//read
			@SuppressWarnings("unchecked")
			Map<String, Object> documentRead = couchDbConnector.get(Map.class, documentId);

			//update
			documentRead.put("updated", "true");
			try {
				couchDbConnector.update(documentRead);
			} catch (UpdateConflictException e) {
			    LOG.debug("Update Conflict", e);
			}
			workloadRunner.publishedWorkloadDocumentWithIdandRevision((String)documentRead.get("_id"), "2");

			//delete
			couchDbConnector.delete(documentRead);
		}

		String resultMessage = "" +  documentsCreated + " documents";

		if(thread.isCancelled()) {
			resultMessage = "Cancelled CRUD after " + resultMessage;
		}
		else {
			resultMessage = "Finished CRUD " + resultMessage;
		}
		progress = 0;
		return resultMessage;
	}

	protected Map<String, Object> documentTemplate(String id) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("_id", id);
		result.put("type", "sample");
        result.put("author", extras.get(WorkloadHelper.EXTRA_NODE_ID));
        result.put("friends", workloadRunner.getRandomFriends((String)extras.get(WorkloadHelper.EXTRA_NODE_ID), numFriends));
		return result;
	}

	@Override
	public String getName() {
		return "CRUD Documents";
	}

}
