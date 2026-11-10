package kz.patriotic.alarm.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.StreakTier;

public class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.TierViewHolder> {

    public interface OnTierSelectListener {
        void onSelect(StreakTier tier);
    }

    private final List<StreakTier> tiers;
    private final int currentStreakDays;
    private final OnTierSelectListener listener;

    public RoadmapAdapter(List<StreakTier> tiers, int currentStreakDays, OnTierSelectListener listener) {
        this.tiers = tiers;
        this.currentStreakDays = currentStreakDays;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tier, parent, false);
        return new TierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TierViewHolder holder, int position) {
        StreakTier tier = tiers.get(position);

        String range = (tier.getMaxDays() > 365)
                ? (tier.getMinDays() + "+ дн.")
                : (tier.getMinDays() + "–" + tier.getMaxDays() + " дн.");
        holder.tvDaysRange.setText(range);
        holder.tvTitle.setText(tier.getKzTitle() + " (" + tier.getRuTitle() + ")");
        holder.tvDesc.setText(tier.getDescription());

        boolean isCurrent = currentStreakDays >= tier.getMinDays() && currentStreakDays <= tier.getMaxDays();
        if (isCurrent) {
            holder.card.setCardBackgroundColor(0xFF143B61);
            holder.tvTitle.setTextColor(0xFFFEC50C);
            holder.btnPreview.setText("Текущий ✓");
            holder.btnPreview.setTextColor(0xFF2ECC71);
        } else {
            holder.card.setCardBackgroundColor(0xFF0B2035);
            holder.tvTitle.setTextColor(0xFFFFFFFF);
            holder.btnPreview.setText("Уровень " + (tier.getTierIndex() + 1));
            holder.btnPreview.setTextColor(0xFF8EA9C1);
        }

        try {
            int accent = Color.parseColor(tier.getAccentColorHex());
            holder.tvDaysRange.setTextColor(accent);
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(tier);
        });
    }

    @Override
    public int getItemCount() {
        return tiers.size();
    }

    static class TierViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvDaysRange, tvTitle, tvDesc, btnPreview;

        public TierViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tier_card);
            tvDaysRange = itemView.findViewById(R.id.tv_tier_days_range);
            tvTitle = itemView.findViewById(R.id.tv_tier_title);
            tvDesc = itemView.findViewById(R.id.tv_tier_description);
            btnPreview = itemView.findViewById(R.id.btn_preview_tier);
        }
    }
}
