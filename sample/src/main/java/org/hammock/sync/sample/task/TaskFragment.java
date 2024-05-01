package org.hammock.sync.sample.task;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.hammock.sync.documentstore.ConflictException;
import org.hammock.sync.documentstore.DocumentNotFoundException;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.sample.R;
import org.hammock.sync.sample.replication.ReplicationEventListener;
import org.hammock.sync.sample.task.adapter.TaskAdapter;
import org.hammock.sync.sample.task.model.Task;
import org.hammock.sync.sample.task.model.TasksModel;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TaskFragment extends Fragment implements ReplicationEventListener, TaskAdapter.TaskItemListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = TaskFragment.class.getSimpleName();
    private LinearProgressIndicator progress;
    private TaskAdapter taskAdapter;
    private RecyclerView recyclerView;

    private List<Task> taskList = new ArrayList<>();
    private TasksModel tasksModel;

    public TaskFragment(){
        super(R.layout.fragment_todo);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceManager.setDefaultValues(getContext(), R.xml.preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener((v) -> {
            NavDirections directions = TaskFragmentDirections.actionTaskToNewTask();
            Navigation.findNavController(view).navigate(directions);
        });

        progress = view.findViewById(R.id.progress);

        taskAdapter = new TaskAdapter(this);
        recyclerView = view.findViewById(R.id.list);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(taskAdapter);
        setupRecyclerviewTouchGestures();

        if (tasksModel == null) {
            // Model needs to stay in existence for lifetime of app.
            tasksModel = new TasksModel(getContext());
        }

        // Register this activity as the listener to replication updates
        // while its active.
        tasksModel.setReplicationListener(this);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.todo, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if(R.id.action_download == menuItem.getItemId()) {
                    progress.setVisibility(View.VISIBLE);
                    tasksModel.startPullReplication();
                    return true;
                }
                if(R.id.action_upload == menuItem.getItemId()) {
                    progress.setVisibility(View.VISIBLE);
                    tasksModel.startPushReplication();
                    return true;
                }
                if(R.id.action_settings == menuItem.getItemId()) {
                    NavDirections directions = TaskFragmentDirections.actionTaskToSettings();
                    Navigation.findNavController(view).navigate(directions);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        reloadTasksFromModel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tasksModel.setReplicationListener(null);
    }

    @Override
    public void onTaskChecked(Task task) {
        if(task != null) {
            try {
                int pos = taskList.indexOf(task);
                Log.d(TAG, "toggleTaskComplete: task " + task);
                if(pos >= 0) {
                    task.setCompleted(!task.isCompleted());
                    task = tasksModel.updateDocument(task);
                    taskList.set(pos, task);
                    forceAdapterUpdate();
                }
            } catch (ConflictException | DocumentStoreException e) {
                Log.e(TAG, "onTaskChecked: Error modifying document", e);
            }
        }
    }

    private void deleteTask(Task task) {
        try {
            tasksModel.deleteDocument(task);
            taskList.remove(task);
            forceAdapterUpdate();
            Toast.makeText(getContext(), "Deleted item : " + task.getDescription(), Toast.LENGTH_SHORT).show();
        } catch (ConflictException | DocumentNotFoundException | DocumentStoreException e) {
            Log.e(TAG, "deleteTask: Error deleting task", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void replicationComplete() {
        Log.d(TAG, "replicationComplete");
        reloadTasksFromModel();
        Toast.makeText(getContext(), R.string.replication_completed, Toast.LENGTH_LONG).show();
        progress.setVisibility(View.GONE);
    }

    @Override
    public void replicationError() {
        Log.e(TAG, "replicationError");
        reloadTasksFromModel();
        Toast.makeText(getContext(), R.string.replication_error, Toast.LENGTH_LONG).show();
        progress.setVisibility(View.GONE);
    }

    private void forceAdapterUpdate(){
        //Ensure UI thread
        recyclerView.post(() -> taskAdapter.updateTasks(taskList));
    }

    private void reloadTasksFromModel() {
        try {
            taskList = tasksModel.allTasks();
            forceAdapterUpdate();
        } catch (DocumentStoreException e) {
            Log.e(TAG, "reloadTasksFromModel: Error reloading tasks", e);
            throw new RuntimeException(e);
        }
    }

    private void setupRecyclerviewTouchGestures() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                deleteTask(taskAdapter.getItem(position));
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(simpleCallback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        try {
            this.tasksModel.reloadReplicationSettings();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Unable to construct remote URI from configuration", e);
            Toast.makeText(requireContext(), R.string.replication_error, Toast.LENGTH_LONG).show();
        }
    }
}
