package kz.patriotic.alarm.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.AlarmItem;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.util.AdManager;
import kz.patriotic.alarm.util.AlarmStorage;
import kz.patriotic.alarm.util.DonationManager;
import kz.patriotic.alarm.util.LanguageProgressionManager;
import kz.patriotic.alarm.util.LegalAgreementsDialog;
import kz.patriotic.alarm.util.StreakManager;

public class MainActivity extends AppCompatActivity implements AlarmsAdapter.OnAlarmListener {

    private static final int REQ_PERMISSIONS_CAMERA = 101;
    private static final int REQ_OVERLAY_PERMISSION = 102;

    // Header Views
    private TextView tvTopTitle, tvTopSubtitle;
    private ImageView btnTopAdd, btnOverflowMenu;

    // Tab Containers (5 Tabs)
    private NestedScrollView tabAlarmsView, tabStreakView, tabWidgetsView, tabDonationView, tabSettingsView;
    private ImageButton fabAddAlarm;

    // Bottom Navigation Elements (5 Tabs)
    private LinearLayout navTabAlarms, navTabStreak, navTabWidgets, navTabDonation, navTabSettings;
    private ImageView ivNavAlarms, ivNavStreak, ivNavWidgets, ivNavDonation, ivNavSettings;
    private TextView tvNavAlarms, tvNavStreak, tvNavWidgets, tvNavDonation, tvNavSettings;

    // Tab 1: Alarms Views
    private View bannerStreakMini;
    private TextView tvStreakBannerTitle, tvStreakBannerDesc, tvStreakBannerBadge;
    private TextView tvRuleTitle, tvRuleDesc;
    private TextView tvSectionMyAlarms, btnAddAlarmHeader;
    private RecyclerView recyclerAlarms;
    private LinearLayout layoutEmptyAlarms;
    private TextView tvEmptyText;
    private AlarmsAdapter alarmsAdapter;
    private List<AlarmItem> alarmList;

    // Tab 2: Streak Views & Recovery
    private TextView tvStreakHeroTitle, tvStreakNumber, tvStreakUnit;
    private TextView tvTierName, tvTierDesc, tvStreakMisses, tvRoadmapHeader;
    private TextView tvStreakRecoveryTitle, tvStreakRecoveryDesc;
    private MaterialButton btnRestoreMissesAds, btnRestoreStreakPaid, btnSimulateMiss;
    private RecyclerView recyclerRoadmapInline;

    // Tab 3: Widgets Views
    private TextView tvWidgetsHeader, tvW1Title, tvW1Desc, tvW2Title, tvW2Desc;
    private TextView previewW1Streak, previewW2Streak;
    private MaterialButton btnForceRefreshWidgets;
    private TextView tvWidgetGuideTitle, tvWidgetGuideSteps;

    // Tab 4: Dedicated Donation Tab Views
    private TextView tvDonTabTitle, tvDonTabDesc, tvDonSelectAmountLabel, tvDonPerksTitle, tvDonPerksDesc;
    private TextView tabBtnDon100, tabBtnDon200, tabBtnDon500, tabBtnDon1000, tabBtnDon2000, tabBtnDon5000, tabBtnDon10000;

    // Tab 5: Settings Views
    private TextView tvSettingLangTitle, btnLangKz, btnLangRu;
    private TextView tvImmersionTitle, tvImmersionDesc, tvLearningProgressText;
    private SwitchCompat switchImmersion;
    private ProgressBar progressLearningBar;
    private TextView tvAdRemovalTitle, tvAdStatus;
    private MaterialButton btnSettingsRemoveAds;
    private TextView tvLegalTitle, tvLegalDesc;
    private MaterialButton btnSettingsLegal;
    private TextView tvTutorialSettingTitle, tvTutorialSettingDesc;
    private MaterialButton btnSettingsTutorial;

    // Banner ad strip
    private View layoutBannerAdStrip;
    private TextView tvBannerAdText, btnBannerRemoveAd;

    private int currentTab = 0; // 0: Alarms, 1: Streak, 2: Widgets, 3: Donation, 4: Settings

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupAlarmsRecycler();
        setupRoadmapRecycler();
        updateStreakUI();
        updateLocalizationUI();
        updateAdUI();
        switchTab(0);

        // Check required permissions on startup
        checkAndPromptStartupPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStreakUI();
        updateAdUI();
        if (alarmsAdapter != null) {
            alarmList.clear();
            alarmList.addAll(AlarmStorage.getAlarms(this));
            alarmsAdapter.notifyDataSetChanged();
            checkEmptyState();
        }
    }

    private void initViews() {
        // Top Header
        tvTopTitle = findViewById(R.id.tv_top_title);
        tvTopSubtitle = findViewById(R.id.tv_top_subtitle);
        btnTopAdd = findViewById(R.id.btn_top_add);
        btnOverflowMenu = findViewById(R.id.btn_overflow_menu);

        btnTopAdd.setOnClickListener(v -> showEditAlarmDialog(null));
        btnOverflowMenu.setOnClickListener(this::showOverflowMenu);

        // 5 Tabs Containers
        tabAlarmsView = findViewById(R.id.tab_alarms_view);
        tabStreakView = findViewById(R.id.tab_streak_view);
        tabWidgetsView = findViewById(R.id.tab_widgets_view);
        tabDonationView = findViewById(R.id.tab_donation_view);
        tabSettingsView = findViewById(R.id.tab_settings_view);
        fabAddAlarm = findViewById(R.id.fab_add_alarm);
        fabAddAlarm.setOnClickListener(v -> showEditAlarmDialog(null));

        // 5 Bottom Navigation Items
        navTabAlarms = findViewById(R.id.nav_tab_alarms);
        navTabStreak = findViewById(R.id.nav_tab_streak);
        navTabWidgets = findViewById(R.id.nav_tab_widgets);
        navTabDonation = findViewById(R.id.nav_tab_donation);
        navTabSettings = findViewById(R.id.nav_tab_settings);

        ivNavAlarms = findViewById(R.id.iv_nav_alarms);
        ivNavStreak = findViewById(R.id.iv_nav_streak);
        ivNavWidgets = findViewById(R.id.iv_nav_widgets);
        ivNavDonation = findViewById(R.id.iv_nav_donation);
        ivNavSettings = findViewById(R.id.iv_nav_settings);

        tvNavAlarms = findViewById(R.id.tv_nav_alarms);
        tvNavStreak = findViewById(R.id.tv_nav_streak);
        tvNavWidgets = findViewById(R.id.tv_nav_widgets);
        tvNavDonation = findViewById(R.id.tv_nav_donation);
        tvNavSettings = findViewById(R.id.tv_nav_settings);

        navTabAlarms.setOnClickListener(v -> { switchTab(0); AdManager.triggerFrequentAd(this, null); });
        navTabStreak.setOnClickListener(v -> { switchTab(1); AdManager.triggerFrequentAd(this, null); });
        navTabWidgets.setOnClickListener(v -> { switchTab(2); AdManager.triggerFrequentAd(this, null); });
        navTabDonation.setOnClickListener(v -> { switchTab(3); AdManager.triggerFrequentAd(this, null); });
        navTabSettings.setOnClickListener(v -> { switchTab(4); AdManager.triggerFrequentAd(this, null); });

        // Tab 1: Alarms Views
        bannerStreakMini = findViewById(R.id.banner_streak_mini);
        bannerStreakMini.setOnClickListener(v -> switchTab(1));
        tvStreakBannerTitle = findViewById(R.id.tv_streak_banner_title);
        tvStreakBannerDesc = findViewById(R.id.tv_streak_banner_desc);
        tvStreakBannerBadge = findViewById(R.id.tv_streak_banner_badge);
        tvRuleTitle = findViewById(R.id.tv_rule_title);
        tvRuleDesc = findViewById(R.id.tv_rule_desc);
        tvSectionMyAlarms = findViewById(R.id.tv_section_my_alarms);
        btnAddAlarmHeader = findViewById(R.id.btn_add_alarm_header);
        btnAddAlarmHeader.setOnClickListener(v -> showEditAlarmDialog(null));
        recyclerAlarms = findViewById(R.id.recycler_alarms);
        layoutEmptyAlarms = findViewById(R.id.layout_empty_alarms);
        tvEmptyText = findViewById(R.id.tv_empty_text);

        // Tab 2: Streak Views & Recovery
        tvStreakHeroTitle = findViewById(R.id.tv_streak_hero_title);
        tvStreakNumber = findViewById(R.id.tv_streak_number);
        tvStreakUnit = findViewById(R.id.tv_streak_unit);
        tvTierName = findViewById(R.id.tv_tier_name);
        tvTierDesc = findViewById(R.id.tv_tier_desc);
        tvStreakMisses = findViewById(R.id.tv_streak_misses);
        tvRoadmapHeader = findViewById(R.id.tv_roadmap_header);
        recyclerRoadmapInline = findViewById(R.id.recycler_roadmap_inline);

        tvStreakRecoveryTitle = findViewById(R.id.tv_streak_recovery_title);
        tvStreakRecoveryDesc = findViewById(R.id.tv_streak_recovery_desc);
        btnRestoreMissesAds = findViewById(R.id.btn_restore_misses_ads);
        btnRestoreStreakPaid = findViewById(R.id.btn_restore_streak_paid);
        btnSimulateMiss = findViewById(R.id.btn_simulate_miss);

        btnRestoreMissesAds.setOnClickListener(v -> {
            AdManager.showAdToRestoreMiss(this, () -> {
                updateStreakUI();
                updateLocalizationUI();
            });
        });

        if (btnSimulateMiss != null) {
            btnSimulateMiss.setOnClickListener(v -> {
                StreakManager.consumeOneMiss(this);
                updateStreakUI();
                updateLocalizationUI();
                boolean isKz = LanguageProgressionManager.isKazakh(this);
                String msg = isKz ?
                        "1 өткізіп алу жұмсалды (тест). Енді сізде 1/2! 2/2 қалпына келтіру үшін 1 жарнама көріңіз." :
                        "Использован 1 пропуск (для теста). Теперь у вас 1/2! Посмотрите 1 рекламу для восстановления до 2/2.";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        btnRestoreStreakPaid.setOnClickListener(v -> {
            AdManager.showPurchaseStreakRestoreDialog(this, () -> {
                updateStreakUI();
                updateLocalizationUI();
            });
        });

        // Tab 3: Widgets Views
        tvWidgetsHeader = findViewById(R.id.tv_widgets_header);
        tvW1Title = findViewById(R.id.tv_w1_title);
        tvW1Desc = findViewById(R.id.tv_w1_desc);
        previewW1Streak = findViewById(R.id.preview_w1_streak);
        tvW2Title = findViewById(R.id.tv_w2_title);
        tvW2Desc = findViewById(R.id.tv_w2_desc);
        previewW2Streak = findViewById(R.id.preview_w2_streak);
        btnForceRefreshWidgets = findViewById(R.id.btn_force_refresh_widgets);
        tvWidgetGuideTitle = findViewById(R.id.tv_widget_guide_title);
        tvWidgetGuideSteps = findViewById(R.id.tv_widget_guide_steps);

        btnForceRefreshWidgets.setOnClickListener(v -> {
            StreakManager.updateWidgets(this);
            updateStreakUI();
            String msg = LanguageProgressionManager.isKazakh(this) ?
                    "Барлық виджеттер сәтті жаңартылды! (0 қате)" :
                    "Все виджеты успешно обновлены! (0 ошибок)";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            AdManager.triggerFrequentAd(this, null);
        });

        // Tab 4: Dedicated Donation Tab Views
        tvDonTabTitle = findViewById(R.id.tv_don_tab_title);
        tvDonTabDesc = findViewById(R.id.tv_don_tab_desc);
        tvDonSelectAmountLabel = findViewById(R.id.tv_don_select_amount_label);
        tvDonPerksTitle = findViewById(R.id.tv_don_perks_title);
        tvDonPerksDesc = findViewById(R.id.tv_don_perks_desc);

        tabBtnDon100 = findViewById(R.id.tab_btn_don_100);
        tabBtnDon200 = findViewById(R.id.tab_btn_don_200);
        tabBtnDon500 = findViewById(R.id.tab_btn_don_500);
        tabBtnDon1000 = findViewById(R.id.tab_btn_don_1000);
        tabBtnDon2000 = findViewById(R.id.tab_btn_don_2000);
        tabBtnDon5000 = findViewById(R.id.tab_btn_don_5000);
        tabBtnDon10000 = findViewById(R.id.tab_btn_don_10000);

        setupDonationButtons();

        // Tab 5: Settings Views
        tvSettingLangTitle = findViewById(R.id.tv_setting_lang_title);
        btnLangKz = findViewById(R.id.btn_lang_kz);
        btnLangRu = findViewById(R.id.btn_lang_ru);

        btnLangKz.setOnClickListener(v -> {
            LanguageProgressionManager.setLanguage(this, "kz");
            updateLocalizationUI();
            Toast.makeText(this, "Қазақ тілі орнатылды 🇰🇿", Toast.LENGTH_SHORT).show();
        });

        btnLangRu.setOnClickListener(v -> {
            LanguageProgressionManager.setLanguage(this, "ru");
            updateLocalizationUI();
            Toast.makeText(this, "Русский язык установлен 🇷🇺", Toast.LENGTH_SHORT).show();
        });

        tvImmersionTitle = findViewById(R.id.tv_immersion_title);
        tvImmersionDesc = findViewById(R.id.tv_immersion_desc);
        tvLearningProgressText = findViewById(R.id.tv_learning_progress_text);
        switchImmersion = findViewById(R.id.switch_immersion);
        progressLearningBar = findViewById(R.id.progress_learning_bar);

        switchImmersion.setChecked(LanguageProgressionManager.isProgressionEnabled(this));
        switchImmersion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LanguageProgressionManager.setProgressionEnabled(this, isChecked);
            updateLocalizationUI();
        });

        tvAdRemovalTitle = findViewById(R.id.tv_ad_removal_title);
        tvAdStatus = findViewById(R.id.tv_ad_status);
        btnSettingsRemoveAds = findViewById(R.id.btn_settings_remove_ads);
        btnSettingsRemoveAds.setOnClickListener(v -> AdManager.showPurchaseRemoveAdsDialog(this, this::updateAdUI));

        tvLegalTitle = findViewById(R.id.tv_legal_title);
        tvLegalDesc = findViewById(R.id.tv_legal_desc);
        btnSettingsLegal = findViewById(R.id.btn_settings_legal);
        btnSettingsLegal.setOnClickListener(v -> LegalAgreementsDialog.show(this));

        tvTutorialSettingTitle = findViewById(R.id.tv_tutorial_setting_title);
        tvTutorialSettingDesc = findViewById(R.id.tv_tutorial_setting_desc);
        btnSettingsTutorial = findViewById(R.id.btn_settings_tutorial);
        btnSettingsTutorial.setOnClickListener(v -> showTutorialDialog());

        // Banner ad strip
        layoutBannerAdStrip = findViewById(R.id.layout_banner_ad_strip);
        tvBannerAdText = findViewById(R.id.tv_banner_ad_text);
        btnBannerRemoveAd = findViewById(R.id.btn_banner_remove_ad);
        btnBannerRemoveAd.setOnClickListener(v -> AdManager.showPurchaseRemoveAdsDialog(this, this::updateAdUI));
    }

    private void setupDonationButtons() {
        int[] amounts = new int[]{100, 200, 500, 1000, 2000, 5000, 10000};
        TextView[] views = new TextView[]{
                tabBtnDon100, tabBtnDon200, tabBtnDon500,
                tabBtnDon1000, tabBtnDon2000, tabBtnDon5000, tabBtnDon10000
        };

        for (int i = 0; i < views.length; i++) {
            final int amt = amounts[i];
            views[i].setOnClickListener(v -> {
                boolean isKz = LanguageProgressionManager.isKazakh(this);
                String msg = isKz ?
                        ("Үлкен рақмет! Сіз отандық патриоттық жобаны " + amt + " ₸ көлемінде қолдадыңыз! 🇰🇿") :
                        ("Огромное спасибо! Вы поддержали казахстанский патриотический проект на " + amt + " ₸! 🇰🇿");
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        }
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;

        tabAlarmsView.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        tabStreakView.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        tabWidgetsView.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        tabDonationView.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
        tabSettingsView.setVisibility(tabIndex == 4 ? View.VISIBLE : View.GONE);

        fabAddAlarm.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);

        // Highlight Active Tab
        int activeColor = 0xFFFEC50C; // Gold
        int inactiveColor = 0xFF8E8E93; // Muted gray

        ivNavAlarms.setColorFilter(tabIndex == 0 ? activeColor : inactiveColor);
        tvNavAlarms.setTextColor(tabIndex == 0 ? activeColor : inactiveColor);

        ivNavStreak.setColorFilter(tabIndex == 1 ? activeColor : inactiveColor);
        tvNavStreak.setTextColor(tabIndex == 1 ? activeColor : inactiveColor);

        ivNavWidgets.setColorFilter(tabIndex == 2 ? activeColor : inactiveColor);
        tvNavWidgets.setTextColor(tabIndex == 2 ? activeColor : inactiveColor);

        ivNavDonation.setColorFilter(tabIndex == 3 ? activeColor : inactiveColor);
        tvNavDonation.setTextColor(tabIndex == 3 ? activeColor : inactiveColor);

        ivNavSettings.setColorFilter(tabIndex == 4 ? activeColor : inactiveColor);
        tvNavSettings.setTextColor(tabIndex == 4 ? activeColor : inactiveColor);

        // Header Title in active language
        boolean isKz = LanguageProgressionManager.isKazakh(this);

        if (tabIndex == 0) {
            tvTopTitle.setText(isKz ? "Оятарлар" : "Будильники");
            btnTopAdd.setVisibility(View.VISIBLE);
        } else if (tabIndex == 1) {
            tvTopTitle.setText(isKz ? "Серия 365" : "Серия 365 дней");
            btnTopAdd.setVisibility(View.GONE);
        } else if (tabIndex == 2) {
            tvTopTitle.setText(isKz ? "Виджеттер (0 қате)" : "Виджеты (0 ошибок)");
            btnTopAdd.setVisibility(View.GONE);
        } else if (tabIndex == 3) {
            tvTopTitle.setText(isKz ? "Әзірлеушіні қолдау" : "Поддержать разработчика");
            btnTopAdd.setVisibility(View.GONE);
        } else {
            tvTopTitle.setText(isKz ? "Баптаулар" : "Настройки");
            btnTopAdd.setVisibility(View.GONE);
        }
    }

    private void setupAlarmsRecycler() {
        alarmList = AlarmStorage.getAlarms(this);
        alarmsAdapter = new AlarmsAdapter(alarmList, this);
        recyclerAlarms.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlarms.setAdapter(alarmsAdapter);
        checkEmptyState();
    }

    private void setupRoadmapRecycler() {
        int currentStreak = StreakManager.getCurrentStreak(this);
        recyclerRoadmapInline.setLayoutManager(new LinearLayoutManager(this));
        RoadmapAdapter adapter = new RoadmapAdapter(StreakTier.getAllTiers(), currentStreak, tier -> {
            boolean isKz = LanguageProgressionManager.isKazakh(this);
            String title = isKz ? tier.getKzTitle() : tier.getRuTitle();
            String msg = title + " — " + tier.getDescription();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        recyclerRoadmapInline.setAdapter(adapter);
    }

    private void checkEmptyState() {
        if (layoutEmptyAlarms != null) {
            layoutEmptyAlarms.setVisibility((alarmList == null || alarmList.isEmpty()) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStreakUI() {
        int streak = StreakManager.getCurrentStreak(this);
        StreakTier tier = StreakManager.getCurrentTier(this);
        int missesRemaining = StreakManager.getMonthlyMissesRemaining(this);
        boolean isKz = LanguageProgressionManager.isKazakh(this);

        // Tab 1 Mini Banner
        String streakStr = String.valueOf(streak);
        String bannerTitle = isKz ?
                ("Патриоттық серия: " + streak + " күн қатарынан") :
                ("Патриотическая серия: " + streak + " дней подряд");
        tvStreakBannerTitle.setText(bannerTitle);

        String rankTitle = isKz ? tier.getKzTitle() : tier.getRuTitle();
        String bannerDesc = isKz ?
                (rankTitle + " дәрежесі • Толық шкаланы көру →") :
                (rankTitle + " • Посмотреть всю шкалу →");
        tvStreakBannerDesc.setText(bannerDesc);

        tvStreakBannerBadge.setText(missesRemaining + "/2 " + (isKz ? "запас" : "запас"));

        // Tab 2 Hero
        tvStreakNumber.setText(streakStr);
        String missesDesc = isKz ?
                ("Айына " + missesRemaining + " рет өткізіп алу запасы бар") :
                ("Запас: " + missesRemaining + " из 2 пропусков в этом месяце");
        tvStreakMisses.setText(missesDesc);

        tvTierName.setText((isKz ? "Ранг: " : "Ранг: ") + (isKz ? tier.getKzTitle() : (tier.getRuTitle() + " (" + tier.getKzTitle() + ")")));

        try {
            int accent = Color.parseColor(tier.getAccentColorHex());
            tvTierName.setTextColor(accent);
        } catch (Exception ignored) {}

        int nextTierDays = tier.getMaxDays() + 1;
        int daysLeft = Math.max(1, nextTierDays - streak);
        tvTierDesc.setText(tier.getDescription() + " • " + (isKz ? ("Келесі дәрежеге дейін: " + daysLeft + " күн") : ("До след. ранга: " + daysLeft + " дн.")));

        // Tab 3 Widgets Preview
        if (previewW1Streak != null) previewW1Streak.setText(streakStr);
        if (previewW2Streak != null) previewW2Streak.setText(streakStr);

        // Update Recovery Button text based on current misses remaining
        if (btnRestoreMissesAds != null) {
            if (missesRemaining >= 2) {
                btnRestoreMissesAds.setText(isKz ? "Өткізіп алу запасы толық (2/2) ✓" : "Запас пропусков полон (2/2) ✓");
                btnRestoreMissesAds.setAlpha(0.7f);
            } else if (missesRemaining == 1) {
                btnRestoreMissesAds.setText(isKz ? "1 жарнама көру (+1 запас → 2/2)" : "Смотреть 1 рекламу (+1 запас → 2/2)");
                btnRestoreMissesAds.setAlpha(1.0f);
            } else {
                btnRestoreMissesAds.setText(isKz ? "1 жарнама көру (+1 запас → 1/2)" : "Смотреть 1 рекламу (+1 запас → 1/2)");
                btnRestoreMissesAds.setAlpha(1.0f);
            }
        }
        if (btnSimulateMiss != null) {
            btnSimulateMiss.setText(isKz ? "Тест: 1 өткізіп алуды жұмсау (-1 запас)" : "Тест: потратить 1 пропуск (-1 запас)");
        }

        // Tab 5 Progress
        int learningProgress = LanguageProgressionManager.getLearningProgressPercent(this);
        if (progressLearningBar != null) progressLearningBar.setProgress(learningProgress);
        if (tvLearningProgressText != null) {
            String dailyWord = LanguageProgressionManager.getDailyImmersionPhrase(this);
            tvLearningProgressText.setText(isKz ?
                    ("Күннің жаңа сөзі: " + dailyWord + " (Прогресс: " + learningProgress + "/100)") :
                    ("Слово дня: " + dailyWord + " (Прогресс: " + learningProgress + "/100)"));
        }
    }

    private void updateAdUI() {
        boolean adFree = AdManager.isAdFree(this);
        boolean isKz = LanguageProgressionManager.isKazakh(this);

        if (layoutBannerAdStrip != null) {
            layoutBannerAdStrip.setVisibility(adFree ? View.GONE : View.VISIBLE);
        }

        if (adFree) {
            tvAdStatus.setText(isKz ? "Статус: Жарнама мәңгіге өшірілген ✓" : "Статус: Реклама навсегда отключена ✓");
            tvAdStatus.setTextColor(0xFF2ECC71);
            btnSettingsRemoveAds.setEnabled(false);
            btnSettingsRemoveAds.setText(isKz ? "Сатып алынды ✓ (Жарнама жоқ)" : "Приобретено ✓ (Рекламы нет)");
        } else {
            tvAdStatus.setText(isKz ? "Статус: Жарнама қосулы (Реклама активна)" : "Статус: Реклама активна");
            tvAdStatus.setTextColor(0xFF8EA9C1);
            btnSettingsRemoveAds.setEnabled(true);
            btnSettingsRemoveAds.setText(isKz ? "Жарнаманы өшіру — 2 000 ₸" : "Отключить рекламу навсегда — 2 000 ₸");
        }
    }

    private void updateLocalizationUI() {
        boolean isKz = LanguageProgressionManager.isKazakh(this);

        // Language Buttons highlight
        if (isKz) {
            btnLangKz.setBackgroundResource(R.drawable.bg_btn_kazakh_blue);
            btnLangKz.setTextColor(0xFF061320);
            btnLangRu.setBackgroundResource(R.drawable.bg_day_unselected);
            btnLangRu.setTextColor(0xFF8DB4D6);
        } else {
            btnLangRu.setBackgroundResource(R.drawable.bg_btn_kazakh_blue);
            btnLangRu.setTextColor(0xFF061320);
            btnLangKz.setBackgroundResource(R.drawable.bg_day_unselected);
            btnLangKz.setTextColor(0xFF8DB4D6);
        }

        // Header Subtitle
        tvTopSubtitle.setText(isKz ? "ҚАЗАҚСТАН ПАТРИОТЫ" : "РЕСПУБЛИКА КАЗАХСТАН");

        // Tab 1: Alarms
        tvRuleTitle.setText(isKz ? "СЕРИЯ ЕРЕЖЕСІ (ПРАВИЛО СЕРИИ)" : "ПРАВИЛО СЕРИИ ПАТРИОТА");
        tvRuleDesc.setText(isKz ?
                "• Жұмыс күндері (Пн-Пт): 00:00 - 10:00\n• Демалыс күндері (Сб-Вс): 00:00 - 12:00\nОсы уақыттар ғана серияны қолдайды. Ай сайын 2 рет өткізіп алуға рұқсат." :
                "• Будни (Пн-Пт): с 00:00 до 10:00\n• Выходные (Сб-Вс): с 00:00 до 12:00\nБудильники позже не засчитываются в серию. В месяц разрешено 2 пропуска без потери серии.");

        tvSectionMyAlarms.setText(isKz ? "МЕНІҢ ОЯТАРЛАРЫМ" : "МОИ БУДИЛЬНИКИ");
        btnAddAlarmHeader.setText(isKz ? "+ Қосу" : "+ Поставить");
        tvEmptyText.setText(isKz ?
                "Әзірге оятар жоқ.\nАлғашқы оятарыңызды орнатыңыз!" :
                "Нет активных будильников.\nПоставьте первый будильник для начала патриотической серии!");

        // Tab 2: Streak 365 & Recovery
        tvStreakHeroTitle.setText(isKz ? "ПАТРИОТТЫҚ 365 КҮН СЕРИЯСЫ" : "ПАТРИОТИЧЕСКАЯ 365-ДНЕВНАЯ СЕРИЯ");
        tvStreakUnit.setText(isKz ? "күн қатарынан" : "дней подряд");
        tvRoadmapHeader.setText(isKz ? "365 КҮН ШКАЛАСЫ ЖӘНЕ ДӘРЕЖЕЛЕР" : "ШКАЛА 365 ДНЕЙ И РАНГИ ПАТРИОТА");

        tvStreakRecoveryTitle.setText(isKz ? "СЕРИЯ ЖӘНЕ ЗАПАСТЫ ҚАЛПЫНА КЕЛТІРУ" : "ВОССТАНОВЛЕНИЕ СЕРИИ И ПРОПУСКОВ");
        tvStreakRecoveryDesc.setText(isKz ?
                "Будильникті өткізіп алдыңыз ба? Егер сізде 1/2 болса, 1 жарнама көру арқылы +1 запас (2/2-ге дейін) қайтарылады!\n• 1 жарнама көру: +1 өткізіп алу запасы (2/2-ге дейін)\n• Серияны толық қалпына келтіру: 500 ₸" :
                "Пропустили будильник? Если у вас 1/2, то просмотр 1 рекламы восстанавливает +1 запас (до 2/2)!\n• Просмотр 1 рекламы: +1 пропуск (до 2/2)\n• Полное восстановление серии: 500 ₸");

        int missesRem = StreakManager.getMonthlyMissesRemaining(this);
        if (btnRestoreMissesAds != null) {
            if (missesRem >= 2) {
                btnRestoreMissesAds.setText(isKz ? "Өткізіп алу запасы толық (2/2) ✓" : "Запас пропусков полон (2/2) ✓");
                btnRestoreMissesAds.setAlpha(0.7f);
            } else if (missesRem == 1) {
                btnRestoreMissesAds.setText(isKz ? "1 жарнама көру (+1 запас → 2/2)" : "Смотреть 1 рекламу (+1 запас → 2/2)");
                btnRestoreMissesAds.setAlpha(1.0f);
            } else {
                btnRestoreMissesAds.setText(isKz ? "1 жарнама көру (+1 запас → 1/2)" : "Смотреть 1 рекламу (+1 запас → 1/2)");
                btnRestoreMissesAds.setAlpha(1.0f);
            }
        }

        if (btnSimulateMiss != null) {
            btnSimulateMiss.setText(isKz ? "Тест: 1 өткізіп алуды жұмсау (-1 запас)" : "Тест: потратить 1 пропуск (-1 запас)");
        }

        btnRestoreStreakPaid.setText(isKz ?
                "Серияны 500 ₸-ге қалпына келтіру" :
                "Восстановить серию за 500 ₸");

        // Tab 3: Widgets
        tvWidgetsHeader.setText(isKz ? "РЕСМИ ҚАЗАҚСТАН ВИДЖЕТТЕРІ (0 ҚАТЕ)" : "ОФИЦИАЛЬНЫЕ ВИДЖЕТЫ КАЗАХСТАНА (0 ОШИБОК)");
        tvW1Title.setText(isKz ? "1. Горизонтальді виджет 4x2 (КҮН СЕРИЯСЫ)" : "1. Горизонтальный виджет 4x2 (СЕРИЯ ДНЕЙ)");
        tvW1Desc.setText(isKz ? "Бәйтерек, Қазақстан туы және жаңарту батырмасы" : "Байтерек, флаг Казахстана и кнопка моментального обновления");
        tvW2Title.setText(isKz ? "2. Шаршы виджет 2x2 (БІР ЕЛ — БІР ХАЛЫҚ!)" : "2. Квадратный виджет 2x2 (БІР ЕЛ — БІР ХАЛЫҚ!)");
        tvW2Desc.setText(isKz ? "Алтын оюлы жиектеме, Қыран мен Күн сақинасы" : "Золотой орнамент, кольцо солнца и орла со счетчиком серии");
        btnForceRefreshWidgets.setText(isKz ? "Виджеттерді мәжбүрлі жаңарту ↺" : "Принудительно обновить все виджеты ↺");
        tvWidgetGuideTitle.setText(isKz ? "ЖҰМЫС ҮСТЕЛІНЕ ҚАЛАЙ ҚОСУ КЕРЕК?" : "КАК ДОБАВИТЬ НА РАБОЧИЙ СТОЛ?");
        tvWidgetGuideSteps.setText(isKz ?
                "1. Экранның бос жерін ұзақ басып тұрыңыз\n2. Ашылған мәзірден «Виджеттер» (Widgets) таңдаңыз\n3. «Патриотический Будильник» тауып, 4x2 немесе 2x2 виджетін жұмыс үстеліне сүйреңіз!\n✓ Android жүйесінің барлық нұсқаларында 0 қатемен жұмыс істейді." :
                "1. Зажмите пустую область на главном экране телефона\n2. Выберите меню «Виджеты» (Widgets)\n3. Найдите «Патриотический Будильник» и перетащите 4x2 или 2x2 виджет!\n✓ Работает со 100% стабильностью и 0 ошибок на всех смартфонах.");

        // Tab 4: Dedicated Donation Section
        tvDonTabTitle.setText(isKz ? "ӘЗІРЛЕУШІНІ ҚОЛДАУ (DONATION)" : "ПОДДЕРЖАТЬ РАЗРАБОТЧИКА");
        tvDonTabDesc.setText(isKz ?
                "«Патриоттық Будильник» отандық тәуелсіз махаббатпен жасалған. Сіздің қолдауыңыз серверлерді қамтамасыз етуге, жаңа виджеттер шығаруға және қосымшаны тегін сақтауға көмектеседі!" :
                "«Патриотический Будильник» создан независимо с любовью к Казахстану. Ваша поддержка помогает оплачивать серверы, выпускать новые виджеты и сохранять проект бесплатным для всех граждан!");

        tvDonSelectAmountLabel.setText(isKz ? "ҚОЛДАУ СОМАСЫН ТАҢДАҢЫЗ:" : "ВЫБЕРИТЕ СУММУ ПОДДЕРЖКИ:");
        tvDonPerksTitle.setText(isKz ? "ПАТРИОТҚА АЛҒЫС:" : "БЛАГОДАРНОСТЬ ПАТРИОТУ:");
        tvDonPerksDesc.setText(isKz ?
                "⭐ Қолданба ішіндегі Алтын демеуші мәртебесі\n⭐ Жаңа функцияларды дауыс беру құқығы\n⭐ Әзірлеуші мен Қазақстан қауымдастығының шексіз алғысы!" :
                "⭐ Золотой статус спонсора в профиле приложения\n⭐ Участие в голосовании за будущие функции\n⭐ Вечная признательность разработчиков и сообщества Казахстана!");

        // Tab 5: Settings
        tvSettingLangTitle.setText(isKz ? "ТІЛДІ ТАҢДАУ / ВЫБОР ЯЗЫКА" : "ВЫБОР ЯЗЫКА ИНТЕРФЕЙСА");
        tvImmersionTitle.setText(isKz ? "100 күндік қазақ тілін үйрену" : "100-дневное изучение казахского языка");
        tvImmersionDesc.setText(isKz ? "Таңғы ояну сериясымен бірге күнделікті жаңа сөздерді меңгеріңіз" : "Изучайте казахские слова каждый день вместе с утренней серией");
        tvAdRemovalTitle.setText(isKz ? "ЖАРНАМАНЫ ӨШІРУ (2 000 ₸ МӘҢГІГЕ)" : "ОТКЛЮЧИТЬ РЕКЛАМУ НАВСЕГДА (2 000 ₸)");
        tvLegalTitle.setText(isKz ? "ҚҰҚЫҚТЫҚ КЕЛІСІМДЕР ЖӘНЕ ҚҰПИЯЛЫЛЫҚ" : "ПРАВОВЫЕ СОГЛАШЕНИЯ И КОНФИДЕНЦИАЛЬНОСТЬ");
        tvLegalDesc.setText(isKz ?
                "Google Play талаптарына толық сәйкестік: ML Kit бейнекамераны тек құрылғыда өңдейді, ешқандай дерек сыртқа жіберілмейді немесе сақталмайды." :
                "Соответствие Google Play: ML Kit обрабатывает камеру исключительно локально на вашем смартфоне, 0 байт данных передается вовне.");
        btnSettingsLegal.setText(isKz ? "Келісімді толық оқу (Terms & Privacy)" : "Читать соглашения (Terms & Privacy)");
        tvTutorialSettingTitle.setText(isKz ? "ҚОЛДАНУ НҰСҚАУЛЫҒЫ (ТУТОРИАЛ)" : "ИНСТРУКЦИЯ (ТУТОРИАЛ)");
        tvTutorialSettingDesc.setText(isKz ?
                "Оятар қалай жұмыс істейді? Қолды жүрекке қою және Тоқаевтың бейнеролигі туралы." :
                "Как работает будильник? Стойка с рукой на сердце и видеообращение Президента.");
        btnSettingsTutorial.setText(isKz ? "Нұсқаулықты қарау" : "Посмотреть инструкцию снова");

        // Bottom Navigation Bar items
        tvNavAlarms.setText(isKz ? "Оятарлар" : "Будильники");
        tvNavStreak.setText(isKz ? "Серия 365" : "Серия 365");
        tvNavWidgets.setText(isKz ? "Виджеттер" : "Виджеты");
        tvNavDonation.setText(isKz ? "Қолдау 🇰🇿" : "Донат 🇰🇿");
        tvNavSettings.setText(isKz ? "Баптаулар" : "Настройки");

        updateStreakUI();
        updateAdUI();
        switchTab(currentTab);
    }

    private void showOverflowMenu(View anchor) {
        boolean isKz = LanguageProgressionManager.isKazakh(this);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "📜 " + (isKz ? "Құқықтық келісімдер (Play Market)" : "Правовые соглашения (Play Market)"));
        popup.getMenu().add(0, 2, 1, "🇰🇿 " + (isKz ? "Әзірлеушіні қолдау (Донат)" : "Поддержать разработчика (Донат)"));
        popup.getMenu().add(0, 3, 2, "🚫 " + (isKz ? "Жарнаманы өшіру (2 000 ₸)" : "Отключить рекламу навсегда (2 000 ₸)"));
        popup.getMenu().add(0, 4, 3, "ℹ️ " + (isKz ? "Қолдану нұсқаулығы" : "Инструкция (Туториал)"));

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                LegalAgreementsDialog.show(this);
                return true;
            } else if (id == 2) {
                switchTab(3); // Navigate to dedicated donation tab
                return true;
            } else if (id == 3) {
                AdManager.showPurchaseRemoveAdsDialog(this, this::updateAdUI);
                return true;
            } else if (id == 4) {
                showTutorialDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ==================== Permissions ====================

    private void checkAndPromptStartupPermissions() {
        boolean needCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        boolean needOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this);
        boolean needNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;

        if (needCamera || needOverlay || needNotif) {
            showPermissionsDialog(needCamera, needOverlay, needNotif);
        }
    }

    private void showPermissionsDialog(boolean needCamera, boolean needOverlay, boolean needNotif) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_permissions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnGrantCamera = dialog.findViewById(R.id.btn_grant_camera);
        View btnGrantOverlay = dialog.findViewById(R.id.btn_grant_overlay);
        MaterialButton btnDismiss = dialog.findViewById(R.id.btn_dismiss_perms);

        btnGrantCamera.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS},
                    REQ_PERMISSIONS_CAMERA
            );
            btnGrantCamera.setVisibility(View.GONE);
        });

        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
            }
            btnGrantOverlay.setVisibility(View.GONE);
        });

        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS_CAMERA) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                boolean isKz = LanguageProgressionManager.isKazakh(this);
                Toast.makeText(this, isKz ? "Камераға рұқсат берілді" : "Камера разрешена для патриотической стойки", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== Tutorial ====================

    private void showTutorialDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tutorial);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnDismiss = dialog.findViewById(R.id.btn_dismiss_tutorial);
        btnDismiss.setOnClickListener(v -> {
            StreakManager.setTutorialShown(this, true);
            dialog.dismiss();
        });
        dialog.show();
    }

    // ==================== Alarm Edit & Tutorial Trigger ====================

    private void showEditAlarmDialog(AlarmItem alarmToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_alarm_edit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean isKz = LanguageProgressionManager.isKazakh(this);

        TimePicker timePicker = dialog.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        EditText etLabel = dialog.findViewById(R.id.et_alarm_label);

        TextView[] dayButtons = new TextView[]{
                dialog.findViewById(R.id.btn_day_mon),
                dialog.findViewById(R.id.btn_day_tue),
                dialog.findViewById(R.id.btn_day_wed),
                dialog.findViewById(R.id.btn_day_thu),
                dialog.findViewById(R.id.btn_day_fri),
                dialog.findViewById(R.id.btn_day_sat),
                dialog.findViewById(R.id.btn_day_sun)
        };

        TextView chipWeekdays = dialog.findViewById(R.id.chip_weekdays);
        TextView chipWeekends = dialog.findViewById(R.id.chip_weekends);
        TextView chipEveryday = dialog.findViewById(R.id.chip_everyday);
        TextView chipOnce = dialog.findViewById(R.id.chip_once);
        TextView tvSelectedDaysSummary = dialog.findViewById(R.id.tv_selected_days_summary);

        TextView tvNoticeDesc = dialog.findViewById(R.id.tv_streak_eligibility_desc);
        ImageView ivNoticeIcon = dialog.findViewById(R.id.iv_notice_icon);
        View layoutNotice = dialog.findViewById(R.id.layout_streak_eligibility_notice);

        boolean isNew = (alarmToEdit == null);
        AlarmItem alarm = isNew ? new AlarmItem() : alarmToEdit;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(alarm.getHour());
            timePicker.setMinute(alarm.getMinute());
        } else {
            timePicker.setCurrentHour(alarm.getHour());
            timePicker.setCurrentMinute(alarm.getMinute());
        }
        etLabel.setText(alarm.getLabel());

        boolean[] selectedDays = new boolean[7];
        boolean[] existingDays = alarm.getDaysOfWeek();
        if (existingDays != null && existingDays.length == 7) {
            System.arraycopy(existingDays, 0, selectedDays, 0, 7);
        } else {
            // Default: weekdays
            selectedDays[0] = true;
            selectedDays[1] = true;
            selectedDays[2] = true;
            selectedDays[3] = true;
            selectedDays[4] = true;
        }

        Runnable updateDaysUI = () -> {
            for (int i = 0; i < 7; i++) {
                boolean active = selectedDays[i];
                if (active) {
                    if (i >= 5) {
                        dayButtons[i].setBackgroundResource(R.drawable.bg_day_selected_weekend);
                        dayButtons[i].setTextColor(0xFF061320);
                    } else {
                        dayButtons[i].setBackgroundResource(R.drawable.bg_day_selected_weekday);
                        dayButtons[i].setTextColor(0xFF061320);
                    }
                } else {
                    dayButtons[i].setBackgroundResource(R.drawable.bg_day_unselected);
                    dayButtons[i].setTextColor(0xFF8DB4D6);
                }
            }

            // Check matching presets
            int count = 0;
            for (boolean b : selectedDays) if (b) count++;

            boolean isWeekdays = (count == 5 && selectedDays[0] && selectedDays[1] && selectedDays[2] && selectedDays[3] && selectedDays[4]);
            boolean isWeekends = (count == 2 && selectedDays[5] && selectedDays[6]);
            boolean isEveryday = (count == 7);
            boolean isOnce = (count == 0);

            chipWeekdays.setBackgroundResource(isWeekdays ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipWeekdays.setTextColor(isWeekdays ? 0xFF33C5DB : 0xFF8DB4D6);

            chipWeekends.setBackgroundResource(isWeekends ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipWeekends.setTextColor(isWeekends ? 0xFFFEC50C : 0xFF8DB4D6);

            chipEveryday.setBackgroundResource(isEveryday ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipEveryday.setTextColor(isEveryday ? 0xFF33C5DB : 0xFF8DB4D6);

            chipOnce.setBackgroundResource(isOnce ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipOnce.setTextColor(isOnce ? 0xFFFEC50C : 0xFF8DB4D6);

            // Summary text in active language
            if (isEveryday) {
                tvSelectedDaysSummary.setText(isKz ? "Қайталау: Күн сайын (апта бойы)" : "Повтор: Каждый день (7 дней в неделю)");
            } else if (isWeekdays) {
                tvSelectedDaysSummary.setText(isKz ? "Қайталау: Жұмыс күндері (Дс-Жм)" : "Повтор: Будни (Пн, Вт, Ср, Чт, Пт)");
            } else if (isWeekends) {
                tvSelectedDaysSummary.setText(isKz ? "Қайталау: Демалыс күндері (Сб-Жс)" : "Повтор: Выходные (Сб, Вс)");
            } else if (isOnce) {
                tvSelectedDaysSummary.setText(isKz ? "Қайталаусыз: Бір рет ойнайды" : "Без повтора: Сработает один раз");
            } else {
                String[] namesKz = {"Дс", "Сс", "Ср", "Бс", "Жм", "Сб", "Жс"};
                String[] namesRu = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                String[] names = isKz ? namesKz : namesRu;
                StringBuilder sb = new StringBuilder(isKz ? "Қайталау: " : "Повтор: ");
                for (int i = 0; i < 7; i++) {
                    if (selectedDays[i]) {
                        if (sb.length() > 10) sb.append(", ");
                        sb.append(names[i]);
                    }
                }
                tvSelectedDaysSummary.setText(sb.toString());
            }
        };

        Runnable checkEligibility = () -> {
            int h = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getHour() : timePicker.getCurrentHour();
            int m = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getMinute() : timePicker.getCurrentMinute();

            boolean hasWeekday = selectedDays[0] || selectedDays[1] || selectedDays[2] || selectedDays[3] || selectedDays[4];
            boolean hasWeekend = selectedDays[5] || selectedDays[6];

            boolean weekdayEligible = (h < 10) || (h == 10 && m == 0);
            boolean weekendEligible = (h < 12) || (h == 12 && m == 0);

            boolean fullyEligible = true;
            if (hasWeekday && !weekdayEligible) fullyEligible = false;
            if (hasWeekend && !weekendEligible) fullyEligible = false;
            if (!hasWeekday && !hasWeekend) {
                fullyEligible = StreakManager.isTimeEligible(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK), h, m);
            }

            if (fullyEligible) {
                layoutNotice.setBackgroundResource(R.drawable.bg_pill_eligible);
                ivNoticeIcon.setImageResource(R.drawable.ic_check_circle);
                tvNoticeDesc.setText(isKz ?
                        "Серияға есептеледі! ✓ (Будни: 00:00–10:00, Выходные: 00:00–12:00)" :
                        "Засчитывается в серию! ✓ (Будни: 00:00–10:00, Выходные: 00:00–12:00)");
                tvNoticeDesc.setTextColor(0xFF2ECC71);
            } else {
                layoutNotice.setBackgroundResource(R.drawable.bg_pill_ineligible);
                ivNoticeIcon.setImageResource(R.drawable.ic_calendar_streak);
                tvNoticeDesc.setText(isKz ?
                        "Серия үшін тым кеш! Будильник ойнайды, бірақ серия ұзармайды (будни >10:00, вых >12:00)." :
                        "Слишком поздно для серии! Будильник сработает, но серия не будет продлена (будни >10:00, вых >12:00).");
                tvNoticeDesc.setTextColor(0xFFF39C12);
            }
        };

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> checkEligibility.run());

        for (int i = 0; i < 7; i++) {
            final int dayIdx = i;
            dayButtons[dayIdx].setOnClickListener(v -> {
                selectedDays[dayIdx] = !selectedDays[dayIdx];
                updateDaysUI.run();
                checkEligibility.run();
            });
        }

        chipWeekdays.setOnClickListener(v -> {
            for (int i = 0; i < 5; i++) selectedDays[i] = true;
            selectedDays[5] = false;
            selectedDays[6] = false;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipWeekends.setOnClickListener(v -> {
            for (int i = 0; i < 5; i++) selectedDays[i] = false;
            selectedDays[5] = true;
            selectedDays[6] = true;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipEveryday.setOnClickListener(v -> {
            for (int i = 0; i < 7; i++) selectedDays[i] = true;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipOnce.setOnClickListener(v -> {
            for (int i = 0; i < 7; i++) selectedDays[i] = false;
            updateDaysUI.run();
            checkEligibility.run();
        });

        updateDaysUI.run();
        checkEligibility.run();

        dialog.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btn_save_alarm).setOnClickListener(v -> {
            int h = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getHour() : timePicker.getCurrentHour();
            int m = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getMinute() : timePicker.getCurrentMinute();

            alarm.setHour(h);
            alarm.setMinute(m);
            String defaultLabel = isKz ? "Патриоттық ояну" : "Патриотический подъем";
            alarm.setLabel(etLabel.getText().toString().trim().isEmpty() ? defaultLabel : etLabel.getText().toString().trim());
            alarm.setDaysOfWeek(Arrays.copyOf(selectedDays, 7));
            alarm.setEnabled(true);

            if (isNew) {
                AlarmStorage.addAlarm(this, alarm);
                alarmList.add(0, alarm);
                alarmsAdapter.notifyItemInserted(0);
                if (recyclerAlarms != null) recyclerAlarms.scrollToPosition(0);
            } else {
                AlarmStorage.updateAlarm(this, alarm);
                alarmsAdapter.notifyDataSetChanged();
            }

            checkEmptyState();
            updateStreakUI();
            dialog.dismiss();

            // Trigger frequent ad on alarm save
            AdManager.triggerFrequentAd(this, null);

            // Automatically show tutorial after setting an alarm
            showTutorialDialog();
        });

        dialog.show();
    }

    // ==================== AlarmsAdapter.OnAlarmListener ====================

    @Override
    public void onToggle(AlarmItem alarm, boolean enabled) {
        AlarmStorage.updateAlarm(this, alarm);
        updateStreakUI();
        AdManager.triggerFrequentAd(this, null);
    }

    @Override
    public void onDelete(AlarmItem alarm) {
        int idx = alarmList.indexOf(alarm);
        if (idx >= 0) {
            alarmList.remove(idx);
            alarmsAdapter.notifyItemRemoved(idx);
            AlarmStorage.deleteAlarm(this, alarm.getId());
            checkEmptyState();
            updateStreakUI();
            boolean isKz = LanguageProgressionManager.isKazakh(this);
            Toast.makeText(this, isKz ? "Оятар өшірілді" : "Будильник удален", Toast.LENGTH_SHORT).show();
            AdManager.triggerFrequentAd(this, null);
        }
    }

    @Override
    public void onEdit(AlarmItem alarm) {
        showEditAlarmDialog(alarm);
    }
}
