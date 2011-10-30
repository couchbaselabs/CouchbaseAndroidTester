package com.couchbase.workloads.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.DesignDocument;

import com.couchbase.workloads.WorkloadHelper;

public class ContinuousFriendFilterReplication extends ContinuousReplication {

    public static final String DDOC_PREFIX = "_design";
    public static final String DDOC_NAME = "friendFilter";
    public static final String DDOC_FILTER_NAME = "onlyFriends";


//    public static final String DDOC_LANGUAGE = "erlang";
//    private static final String FRIEND_FILTER_FUNCTION = "fun(Doc, Req) ->"
//            + "    GetFrom = fun(Key, {Plist}) -> proplists:get_value(Key, Plist, null) end,"
//            + "    case GetFrom(<<\"friends\">>, Doc) of"
//            + "    null -> false; % don't send untagged docs?"
//            + "    Friends ->"
//            + "        case GetFrom(<<\"query\">>, Req) of"
//            + "        null -> false;"
//            + "        Query ->"
//            + "            case GetFrom(<<\"target\">>, Query) of"
//            + "            null -> false;"
//            + "            Target ->"
//            + "                case [true || Friend <- Friends, Friend == Target] of"
//            + "                    [true] -> true;"
//            + "                    [] -> false"
//            + "                end"
//            + "            end"
//            + "        end"
//            + "    end"
//            + "end.";

    public static final String DDOC_LANGUAGE = "javascript";
    private static final String FRIEND_FILTER_FUNCTION = "function(doc, req) {"
            + "    if (req.query && req.query.target && doc.friends) {"
            + "        return (doc.friends.indexOf(req.query.target) !== -1);"
            + "    } else {"
            + "        return false;"
            + "    }"
            + "};";

    @Override
    protected void setupReplicationFilters() {
        super.setupReplicationFilters();

        // try and install a replication filter to pull your friends documents

        //we need to decompose the replication url and build a connector
        String urlString = workloadRunner.getWorkloadReplicationUrl();
        URL url;
        try {
            url = new URL(urlString);
            String host = url.getHost();
            int port = url.getPort();
            if(port < 0) {
                port = 80;
            }
            String path = url.getPath();
            if(path.startsWith("/")) {
                path = path.substring(1);
            }

            //FIXME this wont work on android now, need to move this logic to WorkloadRunner
            HttpClient httpClient = new StdHttpClient.Builder().host(host).port(port).build();
            CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClient);
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(path, false);


            try {
                DesignDocument dDoc = couchDbConnector.get(DesignDocument.class, DDOC_PREFIX + "/" + DDOC_NAME);
                dDoc.setLanguage(DDOC_LANGUAGE);
                dDoc.addFilter(DDOC_FILTER_NAME, FRIEND_FILTER_FUNCTION);
                try {
                    couchDbConnector.update(dDoc);
                } catch (UpdateConflictException uce) {
                    //ignore - these happen by design because multiple threads all
                    //try to create the design doc if it doesn't exist
                }
            } catch (DocumentNotFoundException ndfe) {
                DesignDocument dDoc = new DesignDocument(DDOC_PREFIX + "/" + DDOC_NAME);
                dDoc.setLanguage(DDOC_LANGUAGE);
                dDoc.addFilter(DDOC_FILTER_NAME, FRIEND_FILTER_FUNCTION);
                try {
                    couchDbConnector.create(dDoc);
                } catch (UpdateConflictException uce) {
                    //ignore - these happen by design because multiple threads all
                    //try to create the design doc if it doesn't exist
                }
            }


        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected String pullFilterFunction() {
        return DDOC_NAME + "/" + DDOC_FILTER_NAME;
    }

    @Override
    protected Object pullQueryParams() {
        Map<String, Object> result = new HashMap<String,Object>();
        result.put("target", extras.get(WorkloadHelper.EXTRA_NODE_ID));
        return result;
    }

}
