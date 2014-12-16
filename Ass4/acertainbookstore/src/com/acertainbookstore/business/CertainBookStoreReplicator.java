package com.acertainbookstore.business;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.acertainbookstore.interfaces.Replicator;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {
    //protected HttpClient client;

    public CertainBookStoreReplicator(int maxReplicatorThreads) {
        /*
        // TODO:Implement this constructor
        //setServerAddress(serverAddress);
        client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS); // max
        // concurrent
        // connections
        // to
        // every
        // address
        //client.setThreadPool(new QueuedThreadPool(BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS)); // max
        client.setThreadPool(new QueuedThreadPool(maxReplicatorThreads)); // max
        // threads
        client.setTimeout(BookStoreClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS); // seconds
        // timeout;
        // if
        // no
        // server
        // reply,
        // the
        // request
        // expires
        client.start();
        */
    }

    public List<Future<ReplicationResult>> replicate(Set<String> slaveServers,
            ReplicationRequest request) {
        // TODO: Implement this method
        for(String slave : slaveServers) {
            /*
            ContentExchange exchange = new ContentExchange();
            String urlString = slave + "/" + request.messageType;

            String xmlString = BookStoreUtility.serializeObjectToXMLString(request.dataSet);
            exchange.setMethod("POST");
            exchange.setURL(urlString);
            Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);
            exchange.setRequestContent(requestContent);

            BookStoreUtility.SendAndRecv(this.client, exchange);
            */
        }
        return null;
    }

    /*
    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    */

}
