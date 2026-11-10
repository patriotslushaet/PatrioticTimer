package kz.patriotic.alarm.model;

import java.util.ArrayList;
import java.util.List;

public class StreakTier {
    private final int tierIndex;
    private final int minDays;
    private final int maxDays;
    private final String kzTitle;
    private final String ruTitle;
    private final String description;
    private final String accentColorHex;
    private final String bgStartHex;
    private final String bgEndHex;

    public StreakTier(int tierIndex, int minDays, int maxDays, String kzTitle, String ruTitle,
                      String description, String accentColorHex, String bgStartHex, String bgEndHex) {
        this.tierIndex = tierIndex;
        this.minDays = minDays;
        this.maxDays = maxDays;
        this.kzTitle = kzTitle;
        this.ruTitle = ruTitle;
        this.description = description;
        this.accentColorHex = accentColorHex;
        this.bgStartHex = bgStartHex;
        this.bgEndHex = bgEndHex;
    }

    public int getTierIndex() { return tierIndex; }
    public int getMinDays() { return minDays; }
    public int getMaxDays() { return maxDays; }
    public String getKzTitle() { return kzTitle; }
    public String getRuTitle() { return ruTitle; }
    public String getDescription() { return description; }
    public String getAccentColorHex() { return accentColorHex; }
    public String getBgStartHex() { return bgStartHex; }
    public String getBgEndHex() { return bgEndHex; }

    public static List<StreakTier> getAllTiers() {
        List<StreakTier> list = new ArrayList<>();
        // 37 Tiers spanning 365+ days (every 10 days)
        list.add(new StreakTier(0, 0, 9, "Бастау", "Начало Пути", "Первые шаги пробуждения настоящего патриота", "#00AFCA", "#071A2C", "#005662"));
        list.add(new StreakTier(1, 10, 19, "Тұлпар", "Степной Тулпар", "Неутомимый конь казахских бескрайних степей", "#00C0DE", "#072033", "#006B7A"));
        list.add(new StreakTier(2, 20, 29, "Қыран", "Гордый Бүркіт", "Зоркий сокол, парящий над вершинами Алатау", "#0099B8", "#08263D", "#084C61"));
        list.add(new StreakTier(3, 30, 39, "Батыр", "Степной Батыр", "Один месяц непоколебимой патриотической воли", "#C68B59", "#1B2836", "#6E4522"));
        list.add(new StreakTier(4, 40, 49, "Ақжарқын", "Светлый Рассвет", "Встречай каждое утро с чистым сердцем и гимном", "#16A085", "#0F2B2B", "#0E5A4B"));
        list.add(new StreakTier(5, 50, 59, "Сұңқар", "Сокол Степи", "50 дней стремительной реакции и верности Отчизне", "#1ABC9C", "#0B3037", "#137563"));
        list.add(new StreakTier(6, 60, 69, "Семсер", "Верный Клинок", "Два месяца несокрушимой дисциплины духа", "#2980B9", "#0E243A", "#1B4F72"));
        list.add(new StreakTier(7, 70, 79, "Қалқан", "Щит Отчизны", "Надежный защитник спокойствия родной земли", "#34495E", "#151F28", "#2C3E50"));
        list.add(new StreakTier(8, 80, 89, "Жолбарыс", "Снежный Барс", "Грация, сила и выдержка хозяина снежных гор", "#E67E22", "#2B1D12", "#7E3B09"));
        list.add(new StreakTier(9, 90, 99, "Алтын Адам", "Золотой Человек", "Квартал года! Символ исторического величия Казахстана", "#F1C40F", "#2A250B", "#7D6608"));

        list.add(new StreakTier(10, 100, 109, "Көкжал", "Степной Волк", "100 дней! Неукротимый дух древних кочевников", "#5DADE2", "#122A3C", "#2874A6"));
        list.add(new StreakTier(11, 110, 119, "Алаш", "Дух Алаш", "Преданность идеалам единства и чести народа", "#48C9B0", "#0D2E2B", "#117864"));
        list.add(new StreakTier(12, 120, 129, "Шаңырақ", "Священный Шанырак", "Символ домашнего очага, мира и процветания", "#F39C12", "#2E2108", "#875A0E"));
        list.add(new StreakTier(13, 130, 139, "Бәйтерек", "Древо Байтерек", "Корни в родной земле, крона устремлена к звездам", "#27AE60", "#0D2C1B", "#196F3D"));
        list.add(new StreakTier(14, 140, 149, "Тайқазан", "Священный Тайказан", "Символ достатка, гостеприимства и братства", "#D35400", "#2E170A", "#78281F"));
        list.add(new StreakTier(15, 150, 159, "Сарыарқа", "Златая Сарыарка", "Полгода верности утреннему гимну Казахстана", "#F5B041", "#332408", "#935116"));
        list.add(new StreakTier(16, 160, 169, "Алатау", "Величественный Алатау", "Выдержка и стойкость перед камерой как снежные пики", "#5499C7", "#132738", "#2471A3"));
        list.add(new StreakTier(17, 170, 179, "Қазыгурт", "Ковчег Казыгурт", "Легендарная вершина мудрости предков", "#A569BD", "#281733", "#5B2C6F"));
        list.add(new StreakTier(18, 180, 189, "Отырар", "Огни Отырара", "Половина года патриотического подвига!", "#E74C3C", "#33120E", "#78281F"));
        list.add(new StreakTier(19, 190, 199, "Түркістан", "Жемчужина Туркестан", "Духовная столица Великого Шелкового пути", "#1ABC9C", "#0D2F28", "#117A65"));

        list.add(new StreakTier(20, 200, 209, "Каспий", "Седой Каспий", "200 дней! Мощь бескрайних морских глубин", "#2E86C1", "#0C2336", "#1B4F72"));
        list.add(new StreakTier(21, 210, 219, "Ертіс", "Бурный Иртыш", "Неудержимый поток целеустремленности и силы", "#3498DB", "#0F283C", "#21618C"));
        list.add(new StreakTier(22, 220, 229, "Алтай", "Рудный Алтай", "Древняя колыбель тюркского мира", "#85929E", "#1F2327", "#34495E"));
        list.add(new StreakTier(23, 230, 239, "Ұлытау", "Сердце Улытау", "Центр единства казахских жузов и ханов", "#BA4A00", "#2B1607", "#6E2C00"));
        list.add(new StreakTier(24, 240, 249, "Көкшетау", "Синегорье Кокшетау", "Красота родного края в каждом рассвете", "#17A589", "#0B2B24", "#117864"));
        list.add(new StreakTier(25, 250, 259, "Мұзтау", "Ледяная Белуха", "250 дней безупречной дисциплины!", "#5DADE2", "#132A3C", "#2874A6"));
        list.add(new StreakTier(26, 260, 269, "Ақорда", "Сияние Акорды", "Государственная надежность и верность флагу", "#F4D03F", "#302B0A", "#7D6608"));
        list.add(new StreakTier(27, 270, 279, "Бөрте", "Мудрость Великой Степи", "Глубина традиций и уважение к истории", "#AF7AC5", "#2B1B36", "#633974"));
        list.add(new StreakTier(28, 280, 289, "Қобыланды", "Эпос Кобыланды", "Легендарная богатырская сила духа", "#DC7633", "#2C180B", "#6E2C00"));
        list.add(new StreakTier(29, 290, 299, "Томирис", "Царица Томирис", "Смелость и непреклонность перед трудностями", "#CB4335", "#301311", "#641E16"));

        list.add(new StreakTier(30, 300, 309, "Қаһарман", "Халық Қаһарманы", "300 дней! Высшая народная доблесть", "#E67E22", "#2F1B0B", "#7E3B09"));
        list.add(new StreakTier(31, 310, 319, "Сұңқар Қыран", "Беркут Небес", "Недосягаемая высота патриотизма", "#00B0FF", "#062238", "#006899"));
        list.add(new StreakTier(32, 320, 329, "Алтын Ту", "Золотой Стяг", "Гордо реющий флаг Казахстана в твоем сердце", "#FFD700", "#312803", "#8C7300"));
        list.add(new StreakTier(33, 330, 339, "Дала Дауысы", "Голос Великой Степи", "Связь поколений через утренний гимн", "#00E676", "#042A16", "#007A38"));
        list.add(new StreakTier(34, 340, 349, "Темір Қазық", "Полярная Звезда", "Неизменный ориентир в пути каждого патриота", "#00E5FF", "#052934", "#007F8F"));
        list.add(new StreakTier(35, 350, 359, "Тәуелсіздік", "Тәуелсіздік Тұғыры", "Священная Независимость Республики Казахстан", "#FFC400", "#332700", "#856400"));
        list.add(new StreakTier(36, 360, 9999, "Мәңгілік Ел", "Мәңгілік Ел — Вечная Нация", "365 ДНЕЙ СЕРИИ! Год безупречной верности гимну, флагу и народу Казахстана!", "#FFD700", "#041B33", "#004D73"));

        return list;
    }

    public static StreakTier getTierForDays(int days) {
        List<StreakTier> tiers = getAllTiers();
        for (StreakTier tier : tiers) {
            if (days >= tier.minDays && days <= tier.maxDays) {
                return tier;
            }
        }
        return tiers.get(tiers.size() - 1);
    }
}
