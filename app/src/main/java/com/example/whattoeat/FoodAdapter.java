package com.example.whattoeat;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whattoeat.databinding.ItemFoodBinding;

public class FoodAdapter extends ListAdapter<FoodItem, FoodAdapter.FoodViewHolder> {

    private final OnItemDeleteListener deleteListener;
    private final OnItemToggleListener toggleListener;
    private final OnItemClickListener clickListener;
    private final OnItemMapClickListener mapListener;

    public interface OnItemDeleteListener {
        void onDeleteClick(FoodItem item);
    }

    public interface OnItemToggleListener {
        void onToggle(FoodItem item, boolean isChecked);
    }

    public interface OnItemClickListener {
        void onItemClick(FoodItem item);
    }

    public interface OnItemMapClickListener {
        void onMapClick(FoodItem item);
    }

    protected FoodAdapter(OnItemDeleteListener deleteListener, OnItemToggleListener toggleListener, OnItemClickListener clickListener, OnItemMapClickListener mapListener) {
        super(new DiffUtil.ItemCallback<FoodItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull FoodItem oldItem, @NonNull FoodItem newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull FoodItem oldItem, @NonNull FoodItem newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.deleteListener = deleteListener;
        this.toggleListener = toggleListener;
        this.clickListener = clickListener;
        this.mapListener = mapListener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodBinding binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FoodViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = getItem(position);
        holder.bind(item);
    }

    class FoodViewHolder extends RecyclerView.ViewHolder {

        private final ItemFoodBinding binding;

        public FoodViewHolder(@NonNull ItemFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FoodItem item) {
            binding.tvFoodName.setText(item.getName());
            
            // Remove listener so programmatic changes don't trigger it during recycling
            binding.switchEnable.setOnCheckedChangeListener(null);
            binding.switchEnable.setChecked(item.isEnabled());
            
            // Alpha visual feedback for the entire card
            binding.getRoot().setAlpha(item.isEnabled() ? 1.0f : 0.5f);

            binding.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 实时更新当前卡片的半透明状态
                binding.getRoot().setAlpha(isChecked ? 1.0f : 0.5f);
                
                if (toggleListener != null) {
                    toggleListener.onToggle(item, isChecked);
                }
            });

            binding.ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(item);
                }
            });

            binding.ivMap.setOnClickListener(v -> {
                if (mapListener != null) {
                    mapListener.onMapClick(item);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            });
        }
    }
}