package com.couchbase.workloads.impl;

import java.util.HashMap;
import java.util.Map;

import org.ektorp.UpdateConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.WorkloadHelper;

public class CRUDDocuments extends CouchbaseWorkload {

    private final static Logger LOG = LoggerFactory
            .getLogger(CRUDDocuments.class);

	@Override
	protected String performWork() {

		int documentsCreated = 0;
		while(!thread.isCancelled()) {

			//create
			Map<String, Object> document = documentTemplate();
			couchDbConnector.create(document);
			workloadRunner.publishedWorkloadDocumentWithIdandRevision((String)document.get("_id"), (String)document.get("_rev"));
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
			workloadRunner.publishedWorkloadDocumentWithIdandRevision((String)documentRead.get("_id"), (String)documentRead.get("_rev"));

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

	protected Map<String, Object> documentTemplate() {
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("type", "sample");
        result.put("author", extras.get(WorkloadHelper.EXTRA_NODE_ID));
        result.put("friends", workloadRunner.getRandomFriends(2));
		return result;
	}

	@Override
	public String getName() {
		return "CRUD Documents";
	}

}
