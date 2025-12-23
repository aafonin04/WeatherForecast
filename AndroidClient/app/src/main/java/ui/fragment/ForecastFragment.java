package ui.fragment;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.weatherforecast.databinding.FragmentForecastBinding;
import ui.viewmodel.WeatherViewModel;
import com.google.android.material.tabs.TabLayoutMediator;

public class ForecastFragment extends Fragment {
    private FragmentForecastBinding binding;
    private WeatherViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentForecastBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Adapter для ViewPager (2 таба)
        binding.viewPager.setAdapter(new ForecastPagerAdapter(this));

        // TabLayout
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "По дням" : "По часам");
        }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ForecastPagerAdapter extends FragmentStateAdapter {
        public ForecastPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new DailyForecastSubFragment();  // По дням
            } else {
                return new HourlyForecastSubFragment();  // По часам (placeholder)
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
