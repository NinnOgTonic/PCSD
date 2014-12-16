package com.acertainbookstore.business;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Future;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.acertainbookstore.interfaces.Replicator;


/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {
    protected int numThreads;

    public CertainBookStoreReplicator(int maxReplicatorThreads) {
        numThreads = maxReplicatorThreads;
    }

    public List<Future<ReplicationResult>> replicate(Set<String> slaveServers,
            ReplicationRequest request)  {
        List<Future<ReplicationResult>> futures = new ArrayList<Future<ReplicationResult>>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for(String slave : slaveServers) {
            futures.add(executor.submit(new CertainBookStoreReplicationTask(slave, request)));
        }

        return futures;
    }


}
