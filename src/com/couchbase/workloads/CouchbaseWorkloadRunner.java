package com.couchbase.workloads;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import org.ektorp.http.HttpClient;


public interface CouchbaseWorkloadRunner {

	public InputStream openResource(String path) throws IOException;

	public String getWorkloadReplicationUrl();

	public String getLogsReplicationUrl();

	public void publishedWorkloadDocumentWithIdandRevision(String id, String rev);

	public HttpClient buildHttpClientFromUrl(String url) throws MalformedURLException;

	public String getDatabaseNameFromUrl(String url) throws MalformedURLException;

	public List<String> getRandomFriends(int count);

}
