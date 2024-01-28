package org.hammock.sync.sample.task.adapter;

import androidx.recyclerview.widget.DiffUtil;

import org.hammock.sync.sample.task.model.Task;

import java.util.List;

public class TaskDiffCallback extends DiffUtil.Callback {

    private final List<Task> oldTaskList;
    private final List<Task> newTaskList;

    public TaskDiffCallback(List<Task> oldTaskList, List<Task> newTaskList) {
        this.oldTaskList = oldTaskList;
        this.newTaskList = newTaskList;
    }

    @Override
    public int getOldListSize() {
        return oldTaskList.size();
    }

    @Override
    public int getNewListSize() {
        return newTaskList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Task oldItem = oldTaskList.get(oldItemPosition);
        Task newItem = newTaskList.get(newItemPosition);
        return oldItem.equals(newItem);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Task oldItem = oldTaskList.get(oldItemPosition);
        Task newItem = newTaskList.get(newItemPosition);
        return oldItem.isCompleted() == newItem.isCompleted() &&
                oldItem.getDescription().equals(newItem.getDescription()) &&
                oldItem.getType().equals(newItem.getType()) &&
                oldItem.getDocumentRevision().equals(newItem.getDocumentRevision());
    }
}
