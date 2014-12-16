package com.acertainbookstore.business;

import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import com.acertainbookstore.interfaces.Replicator;
import com.acertainbookstore.client.BookStoreClientConstants;
import com.acertainbookstore.utils.BookStoreUtility;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements
        Callable<ReplicationResult> {

    protected HttpClient client;
    protected String slave;
    protected ReplicationRequest request;

    public CertainBookStoreReplicationTask(String s, ReplicationRequest r) {
        //setServerAddress(serverAddress);
        slave = s;
        request = r;
        client = new HttpClient();
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS); // max
        // concurrent connections to every address
        //client.setThreadPool(new QueuedThreadPool(BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS)); // max
        client.setThreadPool(new QueuedThreadPool(1)); // max
        // threads
        client.setTimeout(BookStoreClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS); // seconds
        // timeout; if no server reply, the request expires
        try {
            client.start();
        } catch (Exception e) {
            ;
        }
    }

    @Override
    public ReplicationResult call() throws Exception {
        // TODO Auto-generated method stub
        BookStoreResult bookRes;
        Boolean success;

        ContentExchange exchange = new ContentExchange();
        String urlString = slave + "/" + request.getMessageType();

        String xmlString = BookStoreUtility.serializeObjectToXMLString(request.getDataSet());
        exchange.setMethod("POST");
        exchange.setURL(urlString);
        Buffer requestContent = new ByteArrayBuffer(xmlString);
        exchange.setRequestContent(requestContent);

        try {
            BookStoreUtility.SendAndRecv(this.client, exchange);
            success = true;
        } catch (BookStoreException e) {
            success = false;
        }
        return new ReplicationResult(slave, success);

    }

    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
