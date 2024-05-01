package org.hammock.sync.sample.task.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hammock.sync.documentstore.DocumentRevision;

import java.util.HashMap;
import java.util.Map;

public class Task {

    public static final String DOC_TYPE = "com.hammock.sync.example.task";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_COMPLETED = "completed";
    public static final String PARAM_DESCRIPTION = "description";
    private Task() {
        //Internal use only
    }

    public Task(String desc) {
        this.setDescription(desc);
        this.setCompleted(false);
        this.setType(DOC_TYPE);
    }

    // this is the revision in the database representing this task
    private DocumentRevision rev;
    public DocumentRevision getDocumentRevision() {
        return rev;
    }

    private String type = DOC_TYPE;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    private boolean completed;
    public boolean isCompleted() {
        return this.completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    private String description;
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String desc) {
        this.description = desc;
    }

    @NonNull
    @Override
    public String toString() {
        return "{ desc: " + getDescription() + ", completed: " + isCompleted() + "}";
    }

    public static Task fromRevision(DocumentRevision rev) {
        Task t = new Task();
        t.rev = rev;
        // this could also be done by a fancy object mapper
        Map<String, Object> map = rev.getBody().asMap();
        if(Task.DOC_TYPE.equals(map.get(PARAM_TYPE))) {
            t.setType((String) map.get(PARAM_TYPE));
            Boolean completed = (Boolean) map.get(PARAM_COMPLETED);
            t.setCompleted(completed != null ? completed : Boolean.FALSE);
            t.setDescription((String) map.get(PARAM_DESCRIPTION));
            return t;
        }
        return null;
    }

    public Map<String, Object> asMap() {
        // this could also be done by a fancy object mapper
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put(PARAM_COMPLETED, completed);
        map.put(PARAM_DESCRIPTION, description);
        return map;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Task))
            return false;
        Task other = (Task)obj;
        boolean typeEquals = (this.type == null && other.type == null)
                || (this.type != null && this.type.equals(other.type));
        boolean completedEquals = this.completed == other.completed;
        boolean descriptionEquals = (this.description == null && other.description == null)
                || (this.description != null && this.description.equals(other.description));
        boolean revisionEquals = (this.getDocumentRevision() != null && this.getDocumentRevision().equals(other.getDocumentRevision()));
        return typeEquals && completedEquals && descriptionEquals && revisionEquals;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (type != null) {
            result = 31 * result + type.hashCode();
        }
        if (description != null) {
            result = 31 * result + description.hashCode();
        }
        if(completed)
            result = 31 * result + 1;
        return result;
    }
}