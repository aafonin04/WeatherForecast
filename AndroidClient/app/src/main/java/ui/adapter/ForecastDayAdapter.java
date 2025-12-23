package ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherforecast.R;

import java.util.List;

import data.model.ForecastData;

public class ForecastDayAdapter extends RecyclerView.Adapter<ForecastDayAdapter.ViewHolder> {
    private List<ForecastData.ForecastItem> items;

    public void setItems(List<ForecastData.ForecastItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastData.ForecastItem item = items.get(position);
        holder.textDate.setText(item.getDatetime());
        holder.textTempMinMax.setText(item.getTemperatureMin() + "/" + item.getTemperatureMax() + "°C");
        holder.textCondition.setText(item.getCondition());
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textTempMinMax, textCondition;

        ViewHolder(View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            textTempMinMax = itemView.findViewById(R.id.text_temp_min_max);
            textCondition = itemView.findViewById(R.id.text_condition);
        }
    }
}
