package org.hammock.sync.sample.task;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.hammock.sync.sample.R;
import org.hammock.sync.sample.task.model.Task;
import org.hammock.sync.sample.task.model.TasksModel;

public class NewTaskFragment extends Fragment {

    public NewTaskFragment() {
        super(R.layout.fragment_new_task);
    }

    private TasksModel tasksModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tasksModel = new TasksModel(getContext());

        MaterialButton btn = view.findViewById(R.id.button);
        TextInputEditText et = view.findViewById(R.id.textInput);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btn.setEnabled(!s.toString().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        btn.setOnClickListener((v) -> {
            Editable field = et.getText();
            if (field != null) {
                String label = field.toString();
                if (!label.isEmpty()) {
                    tasksModel.createDocument(new Task(label));
                    Navigation.findNavController(view).navigateUp();
                }
            }
        });
    }
}
