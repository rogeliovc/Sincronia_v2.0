package com.example.sincronia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainShellFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_shell, container, false);

        ImageButton tabMusic = view.findViewById(R.id.tab_music);
        ImageButton tabHome = view.findViewById(R.id.tab_home);
        ImageButton tabCalendar = view.findViewById(R.id.tab_calendar);

        // Estado inicial: Home activo
        setTabActive(tabHome, true);
        setTabActive(tabMusic, false);
        setTabActive(tabCalendar, false);
        showInnerFragment(new HomeFragment());

        tabMusic.setOnClickListener(v -> {
            setTabActive(tabMusic, true);
            setTabActive(tabHome, false);
            setTabActive(tabCalendar, false);
            showInnerFragment(new PlayerFragment());
        });
        tabHome.setOnClickListener(v -> {
            setTabActive(tabMusic, false);
            setTabActive(tabHome, true);
            setTabActive(tabCalendar, false);
            showInnerFragment(new HomeFragment());
        });
        tabCalendar.setOnClickListener(v -> {
            setTabActive(tabMusic, false);
            setTabActive(tabHome, false);
            setTabActive(tabCalendar, true);
            showInnerFragment(new TasksFragment());
        });

        return view;
    }

    private void showInnerFragment(Fragment fragment) {
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction()
                .replace(R.id.shell_content_container, fragment)
                .commit();
    }

    private void setTabActive(ImageButton tab, boolean active) {
        if (active) {
            tab.setBackgroundResource(R.drawable.tab_selected_bg);
            tab.setColorFilter(android.graphics.Color.parseColor("#14395B"));
        } else {
            tab.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tab.setColorFilter(android.graphics.Color.WHITE);
        }
    }
}
