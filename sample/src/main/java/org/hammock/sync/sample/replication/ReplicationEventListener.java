package org.hammock.sync.sample.replication;

public interface ReplicationEventListener {
    void replicationComplete();
    void replicationError();
}
