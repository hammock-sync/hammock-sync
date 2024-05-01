package org.hammock.sync.sample.task.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.hammock.sync.documentstore.ConflictException;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.DocumentException;
import org.hammock.sync.documentstore.DocumentNotFoundException;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.documentstore.DocumentStore;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.documentstore.DocumentStoreNotOpenedException;
import org.hammock.sync.event.Subscribe;
import org.hammock.sync.event.notifications.ReplicationCompleted;
import org.hammock.sync.event.notifications.ReplicationErrored;
import org.hammock.sync.replication.Replicator;
import org.hammock.sync.replication.ReplicatorBuilder;
import org.hammock.sync.sample.MainActivity;
import org.hammock.sync.sample.replication.ReplicationEventListener;
import org.hammock.sync.sample.task.model.Task;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TasksModel {

    private static final String LOG_TAG = "TasksModel";

    private static final String DOCUMENT_STORE_DIR = "data";
    private static final String DOCUMENT_STORE_NAME = "tasks";

    private DocumentStore mDocumentStore;

    private Replicator mPushReplicator;
    private Replicator mPullReplicator;

    private final Context mContext;
    private final Handler mHandler;
    private ReplicationEventListener mListener;

    public TasksModel(Context context) {

        this.mContext = context;

        // Set up our tasks DocumentStore within its own folder in the applications
        // data directory.
        File path = this.mContext.getApplicationContext().getDir(DOCUMENT_STORE_DIR, Context.MODE_PRIVATE);

        try {
            this.mDocumentStore = DocumentStore.getInstance(new File(path, DOCUMENT_STORE_NAME));
        } catch (DocumentStoreNotOpenedException e) {
            Log.e(LOG_TAG, "Unable to open DocumentStore", e);
        }

        Log.d(LOG_TAG, "Set up database at " + path.getAbsolutePath());

        // Set up the replicator objects from the app's settings.
        try {
            this.reloadReplicationSettings();
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Unable to construct remote URI from configuration", e);
        }

        // Allow us to switch code called by the ReplicationListener into
        // the main thread so the UI can update safely.
        this.mHandler = new Handler(Looper.getMainLooper());

        Log.d(LOG_TAG, "TasksModel set up " + path.getAbsolutePath());
    }

    //
    // GETTERS AND SETTERS
    //

    /**
     * Sets the listener for replication callbacks as a weak reference.
     * @param listener {@link MainActivity} to receive callbacks.
     */
    public void setReplicationListener(ReplicationEventListener listener) {
        this.mListener = listener;
    }

    //
    // DOCUMENT CRUD
    //

    /**
     * Creates a task, assigning an ID.
     * @param task task to create
     * @return new revision of the document
     */
    public Task createDocument(Task task) {
        DocumentRevision rev = new DocumentRevision();
        rev.setBody(DocumentBodyFactory.create(task.asMap()));
        try {
            DocumentRevision created = this.mDocumentStore.database().create(rev);
            return Task.fromRevision(created);
        } catch (DocumentException | DocumentStoreException de) {
            return null;
        }
    }

    /**
     * Updates a Task document within the DocumentStore.
     * @param task task to update
     * @return the updated revision of the Task
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the DocumentStore.
     * @throws DocumentStoreException if there was an error updating the rev for this task
     */
    public Task updateDocument(Task task) throws ConflictException, DocumentStoreException {
        DocumentRevision rev = task.getDocumentRevision();
        rev.setBody(DocumentBodyFactory.create(task.asMap()));
        try {
            DocumentRevision updated = this.mDocumentStore.database().update(rev);
            return Task.fromRevision(updated);
        } catch (DocumentException de) {
            Log.e(LOG_TAG, "updateDocument: failed", de);
            return null;
        }
    }

    /**
     * Deletes a Task document within the DocumentStore.
     * @param task task to delete
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the DocumentStore.
     * @throws DocumentNotFoundException if the rev for this task does not exist
     * @throws DocumentStoreException if there was an error deleting the rev for this task
     */
    public void deleteDocument(Task task) throws ConflictException, DocumentNotFoundException, DocumentStoreException {
        this.mDocumentStore.database().delete(task.getDocumentRevision());
    }

    /**
     * <p>Returns all {@code Task} documents in the DocumentStore.</p>
     */
    public List<Task> allTasks() throws DocumentStoreException {
        int nDocs = this.mDocumentStore.database().getDocumentCount();
        List<DocumentRevision> all = this.mDocumentStore.database().read(0, nDocs, true);
        List<Task> tasks = new ArrayList<>();

        // Filter all documents down to those of type Task.
        for(DocumentRevision rev : all) {
            Task t = Task.fromRevision(rev);
            if (t != null) {
                tasks.add(t);
            }
        }

        return tasks;
    }

    //
    // MANAGE REPLICATIONS
    //

    /**
     * <p>Stops running replications.</p>
     *
     * <p>The stop() methods stops the replications asynchronously, see the
     * replicator docs for more information.</p>
     */
    public void stopAllReplications() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.stop();
        }
        if (this.mPushReplicator != null) {
            this.mPushReplicator.stop();
        }
    }

    /**
     * <p>Starts the configured push replication.</p>
     */
    public void startPushReplication() {
        if (this.mPushReplicator != null) {
            this.mPushReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    /**
     * <p>Starts the configured pull replication.</p>
     */
    public void startPullReplication() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    /**
     * <p>Stops running replications and reloads the replication settings from
     * the app's preferences.</p>
     */
    public void reloadReplicationSettings()
            throws URISyntaxException {

        // Stop running replications before reloading the replication
        // settings.
        // The stop() method instructs the replicator to stop ongoing
        // processes, and to stop making changes to the DocumentStore. Therefore,
        // we don't clear the listeners because their complete() methods
        // still need to be called once the replications have stopped
        // for the UI to be updated correctly with any changes made before
        // the replication was stopped.
        this.stopAllReplications();

        // Set up the new replicator objects
        URI uri = this.createServerURI();

        mPullReplicator = ReplicatorBuilder.pull().to(mDocumentStore).from(uri).build();
        mPushReplicator = ReplicatorBuilder.push().from(mDocumentStore).to(uri).build();

        mPushReplicator.getEventBus().register(this);
        mPullReplicator.getEventBus().register(this);

        Log.d(LOG_TAG, "Set up replicators for URI:" + uri.toString());
    }

    /**
     * <p>Returns the URI for the remote database, based on the app's
     * configuration.</p>
     * @return the remote database's URI
     * @throws URISyntaxException if the settings give an invalid URI
     */
    private URI createServerURI()
            throws URISyntaxException {
        // We store this in plain text for the purposes of simple demonstration,
        // you might want to use something more secure.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        boolean https = sharedPref.getBoolean(MainActivity.SETTINGS_CLOUDANT_HTTPS, false);
        String scheme = https? "https" : "http";
        String host = sharedPref.getString(MainActivity.SETTINGS_CLOUDANT_HOST, "");
        String username = sharedPref.getString(MainActivity.SETTINGS_CLOUDANT_USER, "");
        String secret = sharedPref.getString(MainActivity.SETTINGS_CLOUDANT_SECRET, "");
        String dbName = sharedPref.getString(MainActivity.SETTINGS_CLOUDANT_DB, "");

        // We recommend always using HTTPS to talk to CouchDB.
        return new URI(scheme, username + ":" + secret, host, 5984, "/" + dbName, null, null);
    }

    //
    // REPLICATIONLISTENER IMPLEMENTATION
    //

    /**
     * Calls the TodoActivity's replicationComplete method on the main thread,
     * as the complete() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void complete(ReplicationCompleted rc) {
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.replicationComplete();
            }
        });
    }

    /**
     * Calls the TodoActivity's replicationComplete method on the main thread,
     * as the error() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void error(ReplicationErrored re) {
        Log.e(LOG_TAG, "Replication error:", re.errorInfo);
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.replicationError();
            }
        });
    }
}
