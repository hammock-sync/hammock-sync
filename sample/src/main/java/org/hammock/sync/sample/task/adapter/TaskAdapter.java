package org.hammock.sync.sample.task.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.hammock.sync.sample.R;
import org.hammock.sync.sample.task.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {

    private static final String TAG = TaskAdapter.class.getSimpleName();
    private final TaskItemListener listener;
    private final List<Task> taskList = new ArrayList<>();

    public TaskAdapter(TaskItemListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.task_item, parent, false);
        return new TaskHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, int position) {
        Task task = getItem(position);
        holder.onBind(task);
    }

    public Task getItem(int position) {
        if(taskList.size() > position) {
            return taskList.get(position);
        } else {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> newList) {
        final TaskDiffCallback diffCallback = new TaskDiffCallback(taskList, newList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        taskList.clear();
        taskList.addAll(newList);

        diffResult.dispatchUpdatesTo(this);
    }

    public interface TaskItemListener {
        void onTaskChecked(Task task);
    }

    public class TaskHolder extends RecyclerView.ViewHolder {

        private final View view;
        public TaskHolder(@NonNull View itemView) {
            super(itemView);
            this.view = itemView;
        }

        public void onBind(Task task) {
            if(task != null){
                TextView desc = view.findViewById(R.id.task_description);
                desc.setText(task.getDescription());
                MaterialCheckBox check = view.findViewById(R.id.checkbox);
                check.setChecked(task.isCompleted());
                check.setOnClickListener((v) -> {
                    if(listener != null) {
                        listener.onTaskChecked(task);
                    }
                });
            }
        }
    }
}