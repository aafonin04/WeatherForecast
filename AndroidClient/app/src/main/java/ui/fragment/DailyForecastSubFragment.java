package ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherforecast.R;

import ui.adapter.ForecastDayAdapter;
import ui.viewmodel.WeatherViewModel;

public class DailyForecastSubFragment extends Fragment {
    private RecyclerView recyclerView;
    private ForecastDayAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_daily, container, false);  // Создай layout с RecyclerView
        recyclerView = view.findViewById(R.id.recycler_daily);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ForecastDayAdapter();
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        WeatherViewModel viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        viewModel.getForecastData().observe(getViewLifecycleOwner(), forecast -> {
            if (forecast != null) {
                adapter.setItems(forecast.getForecast());
            }
        });
    }
}