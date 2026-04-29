package com.ab25cq.keibajr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String PREFS = "keiba_jr_state";
    private static final int RACE_MS = 14000;
    private static final int MIN_TRAININGS_REQUIRED = 1;
    private static final int MAX_TRAININGS_REQUIRED = 5;
    private static final int TRAINING_DAYS = 7;
    private static final int POST_RACE_DAYS = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN);

    private HorseStore store;
    private LinearLayout setupPanel;
    private EditText horseNameEdit;
    private Spinner stallionSpinner;
    private Button createHorseButton;
    private TextView calendarView;
    private TextView horseView;
    private TextView trainingView;
    private TextView raceInfoView;
    private TextView raceLogView;
    private Button trainingButton;
    private Button pastureButton;
    private Button raceButton;
    private Button nextHorseButton;
    private Button retireButton;
    private Spinner trainingSpinner;
    private Spinner durationSpinner;
    private Spinner raceSpinner;
    private ArrayAdapter<String> raceAdapter;
    private final List<RaceData> availableRaces = new ArrayList<>();
    private int availableRaceWeekKey = -1;
    private Spinner jockeySpinner;
    private Spinner tacticSpinner;
    private boolean raceRunning;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            boolean changed = store.finishTrainingIfReady();
            refreshUi();
            handler.postDelayed(this, changed ? 300 : 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("競馬育成Jr");
        store = new HorseStore(this);
        store.ensureHorse();
        buildLayout();
        refreshUi();
        handler.post(ticker);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void buildLayout() {
        int pad = dp(16);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(246, 242, 232));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad * 2);
        scrollView.addView(root);

        TextView title = text("競馬育成Jr", 27, Color.rgb(33, 45, 38), true);
        TextView subtitle = text("指示だけ出して、実時間で調教。準備ができたらJRA 2026 G1/G2/G3へ出走。", 14, Color.rgb(83, 84, 75), false);
        root.addView(title);
        root.addView(subtitle);

        root.addView(sectionTitle("新しい競走馬"));
        setupPanel = panel();
        horseNameEdit = new EditText(this);
        horseNameEdit.setSingleLine(true);
        horseNameEdit.setHint("馬名");
        horseNameEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        stallionSpinner = spinner(Stallion.names());
        createHorseButton = button("この馬で育成開始");
        createHorseButton.setOnClickListener(v -> createHorseFromInput());
        setupPanel.addView(label("馬名"));
        setupPanel.addView(horseNameEdit);
        setupPanel.addView(label("種牡馬"));
        setupPanel.addView(stallionSpinner);
        setupPanel.addView(createHorseButton);
        root.addView(setupPanel);

        calendarView = panelText();
        root.addView(sectionTitle("カレンダー"));
        root.addView(calendarView);

        horseView = panelText();
        root.addView(sectionTitle("厩舎"));
        root.addView(horseView);

        root.addView(sectionTitle("調教"));
        LinearLayout trainingPanel = panel();
        trainingSpinner = spinner(new String[]{"強め", "弱め", "馬なり"});
        durationSpinner = spinner(new String[]{"10秒", "1分", "10分", "1時間", "3時間", "8時間"});
        trainingPanel.addView(label("調教指示"));
        trainingPanel.addView(trainingSpinner);
        trainingPanel.addView(label("現実の調教時間"));
        trainingPanel.addView(durationSpinner);
        trainingButton = button("調教開始");
        trainingButton.setOnClickListener(v -> startTraining());
        pastureButton = button("放牧する");
        pastureButton.setOnClickListener(v -> startPasture());
        trainingView = text("", 15, Color.rgb(48, 61, 52), false);
        trainingPanel.addView(trainingButton);
        trainingPanel.addView(pastureButton);
        trainingPanel.addView(trainingView);
        root.addView(trainingPanel);

        root.addView(sectionTitle("出走"));
        LinearLayout racePanel = panel();
        raceSpinner = new Spinner(this);
        raceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        raceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        raceSpinner.setAdapter(raceAdapter);
        jockeySpinner = spinner(new String[]{"若手ジョッキー", "ベテランジョッキー", "逃げ名人", "差し名人", "外国人ジョッキー"});
        tacticSpinner = spinner(new String[]{"お任せ", "逃げ", "普通", "追い込み"});
        raceInfoView = text("", 14, Color.rgb(74, 75, 68), false);
        raceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshRaceInfo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        racePanel.addView(label("レース"));
        racePanel.addView(raceSpinner);
        racePanel.addView(raceInfoView);
        racePanel.addView(label("ジョッキー"));
        racePanel.addView(jockeySpinner);
        racePanel.addView(label("指示"));
        racePanel.addView(tacticSpinner);
        raceButton = button("出馬する");
        raceButton.setOnClickListener(v -> startRace());
        racePanel.addView(raceButton);
        root.addView(racePanel);

        raceLogView = panelText();
        root.addView(raceLogView);

        retireButton = button("この馬を引退させる");
        retireButton.setOnClickListener(v -> {
            store.retireCurrentHorse();
            raceLogView.setText("この馬を引退させました。馬名と種牡馬を選んで次の競走馬を作れます。");
            refreshUi();
        });
        root.addView(retireButton);

        nextHorseButton = button("次の競走馬を作る");
        nextHorseButton.setOnClickListener(v -> {
            horseNameEdit.requestFocus();
            raceLogView.setText("馬名と種牡馬を選んでください。");
            refreshUi();
        });
        root.addView(nextHorseButton);

        setContentView(scrollView);
    }

    private void createHorseFromInput() {
        String name = horseNameEdit.getText().toString().trim();
        if (name.length() == 0) {
            name = HorseStore.defaultName(store.nextGeneration());
        }
        Stallion stallion = Stallion.ALL[stallionSpinner.getSelectedItemPosition()];
        store.createHorse(name, stallion);
        availableRaceWeekKey = -1;
        raceLogView.setText(name + "が入厩しました。父は" + stallion.name + "です。");
        horseNameEdit.setText("");
        refreshUi();
    }

    private void startTraining() {
        if (!store.hasHorse()) {
            raceLogView.setText("先に馬名と種牡馬を決めて競走馬を作ってください。");
            return;
        }
        if (store.isRetired()) {
            raceLogView.setText("この馬は引退済みです。次の競走馬を迎えてください。");
            return;
        }
        if (store.isTraining()) {
            return;
        }
        int type = trainingSpinner.getSelectedItemPosition();
        long durationMs = durationFromSelection(durationSpinner.getSelectedItemPosition());
        store.startTraining(type, durationMs);
        raceLogView.setText("調教を開始しました。完了まではアプリを閉じても実時間で進みます。");
        refreshUi();
    }

    private void startPasture() {
        if (!store.hasHorse()) {
            raceLogView.setText("先に馬名と種牡馬を決めて競走馬を作ってください。");
            return;
        }
        if (store.isRetired()) {
            raceLogView.setText("この馬は引退済みです。次の競走馬を迎えてください。");
            return;
        }
        if (store.isTraining() || raceRunning) {
            raceLogView.setText("今は放牧できません。調教やレースが終わってからにしましょう。");
            return;
        }
        store.applyPasture();
        availableRaceWeekKey = -1;
        raceLogView.setText("1か月放牧に出しました。疲れが抜けて、馬体も戻ってきました。");
        refreshUi();
    }

    private void startRace() {
        if (!store.hasHorse()) {
            raceLogView.setText("先に馬名と種牡馬を決めて競走馬を作ってください。");
            return;
        }
        store.finishTrainingIfReady();
        if (!store.isReadyForRace()) {
            raceLogView.setText("まだ準備OKではありません。必要な調教回数を完了すると出走できます。");
            refreshUi();
            return;
        }
        if (store.isRetired() || raceRunning) {
            return;
        }

        RaceData race = selectedRace();
        if (race == null) {
            raceLogView.setText("今週出走できる重賞はありません。調教で週を進めて次の番組を待ちましょう。");
            refreshUi();
            return;
        }
        String jockey = (String) jockeySpinner.getSelectedItem();
        String tactic = (String) tacticSpinner.getSelectedItem();
        int raceGameDay = store.currentGameDay();
        String raceDate = GameCalendar.weekText(raceGameDay);
        RaceResult result = RaceEngine.simulate(store.snapshot(), race, jockey, tactic, raceGameDay, raceDate);
        store.applyRace(result);
        launchRaceScreen(result, store.isRetired());
        refreshUi();
    }

    private void launchRaceScreen(RaceResult result, boolean retiredAfterRace) {
        int size = result.runners.size();
        String[] names = new String[size];
        int[] ranks = new int[size];
        float[] paces = new float[size];
        boolean[] players = new boolean[size];
        for (int i = 0; i < size; i++) {
            Runner runner = result.runners.get(i);
            names[i] = runner.name;
            ranks[i] = runner.rank;
            paces[i] = runner.visualPace;
            players[i] = runner.player;
        }

        Intent intent = new Intent(this, RaceActivity.class);
        intent.putExtra("raceName", result.race.name);
        intent.putExtra("summary", result.summary(retiredAfterRace));
        intent.putExtra("runnerNames", names);
        intent.putExtra("runnerRanks", ranks);
        intent.putExtra("runnerPaces", paces);
        intent.putExtra("runnerPlayers", players);
        startActivity(intent);
    }

    private void refreshUi() {
        store.finishTrainingIfReady();
        if (!store.hasHorse()) {
            setupPanel.setVisibility(View.VISIBLE);
            calendarView.setText("馬名と種牡馬を決めると、2026年1月第1週から育成が始まります。");
            horseView.setText("まだ競走馬がいません。");
            availableRaces.clear();
            availableRaceWeekKey = -1;
            raceAdapter.clear();
            raceAdapter.add("競走馬を作成してください");
            raceAdapter.notifyDataSetChanged();
            trainingButton.setEnabled(false);
            raceButton.setEnabled(false);
            raceSpinner.setEnabled(false);
            pastureButton.setEnabled(false);
            retireButton.setVisibility(View.GONE);
            nextHorseButton.setVisibility(View.GONE);
            trainingView.setText("馬を作成すると調教できます。");
            raceInfoView.setText("出走できるレースはカレンダーの週に合わせて表示されます。");
            return;
        }
        Horse horse = store.snapshot();
        calendarView.setText(store.calendarText());
        horseView.setText(horse.statusText());
        updateAvailableRaces(horse.gameDay);

        boolean training = store.isTraining();
        boolean retired = store.isRetired();
        setupPanel.setVisibility(retired ? View.VISIBLE : View.GONE);
        trainingButton.setEnabled(!training && !retired && !raceRunning);
        pastureButton.setEnabled(!training && !retired && !raceRunning);
        raceButton.setEnabled(store.isReadyForRace() && !retired && !raceRunning && !availableRaces.isEmpty());
        raceSpinner.setEnabled(!availableRaces.isEmpty());
        retireButton.setVisibility(!retired ? View.VISIBLE : View.GONE);
        nextHorseButton.setVisibility(retired ? View.VISIBLE : View.GONE);
        trainingView.setText(store.trainingText(timeFormat));
        refreshRaceInfo();
    }

    private void refreshRaceInfo() {
        if (raceInfoView == null || raceSpinner == null) {
            return;
        }
        RaceData race = selectedRace();
        if (race == null) {
            raceInfoView.setText(GameCalendar.weekText(store.currentGameDay())
                    + "\n今週出走できるG1/G2/G3はありません。");
            return;
        }
        raceInfoView.setText(GameCalendar.weekText(store.currentGameDay())
                + " / " + race.course + " / " + race.surface + race.distance + "m / " + race.grade
                + "\n1着賞金 " + race.prizeWin + "万円");
    }

    private void updateAvailableRaces(int gameDay) {
        int weekKey = GameCalendar.weekKey(gameDay);
        if (weekKey == availableRaceWeekKey) {
            return;
        }
        availableRaceWeekKey = weekKey;
        availableRaces.clear();
        availableRaces.addAll(RaceData.forGameWeek(gameDay));
        raceAdapter.clear();
        if (availableRaces.isEmpty()) {
            raceAdapter.add("今週の重賞はありません");
        } else {
            for (RaceData race : availableRaces) {
                raceAdapter.add(race.grade + " " + race.name);
            }
        }
        raceAdapter.notifyDataSetChanged();
    }

    private RaceData selectedRace() {
        int position = raceSpinner == null ? 0 : raceSpinner.getSelectedItemPosition();
        if (position < 0 || position >= availableRaces.size()) {
            return null;
        }
        return availableRaces.get(position);
    }

    private long durationFromSelection(int position) {
        long[] values = {
                10_000L,
                60_000L,
                10L * 60_000L,
                60L * 60_000L,
                3L * 60L * 60_000L,
                8L * 60L * 60_000L
        };
        return values[position];
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 20, Color.rgb(33, 45, 38), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(18);
        params.bottomMargin = dp(8);
        view.setLayoutParams(params);
        return view;
    }

    private TextView panelText() {
        TextView view = text("", 15, Color.rgb(35, 45, 40), false);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackgroundColor(Color.rgb(255, 252, 244));
        return view;
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(12), dp(14), dp(14));
        layout.setBackgroundColor(Color.rgb(255, 252, 244));
        return layout;
    }

    private TextView label(String value) {
        TextView view = text(value, 13, Color.rgb(83, 84, 75), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        view.setLayoutParams(params);
        return view;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class Horse {
        String name;
        int generation;
        int careerMonths;
        int speed;
        int stamina;
        int power;
        int guts;
        int gate;
        int condition;
        int fatigue;
        int races;
        int wins;
        int prize;
        int gameDay;
        int trainingProgress;
        int trainingRequired;
        String stallionName;
        String growthType;
        String recordRank;
        String retireReason;
        String lastTrainingComment;
        boolean peakForm;
        boolean ready;
        boolean retired;

        String statusText() {
            return name + "  " + ageText() + "  第" + generation + "世代"
                    + "\n父: " + stallionName + " / 成長: " + growthType
                    + "\n戦績: " + races + "戦 " + wins + "勝 / 獲得賞金 " + prize + "万円"
                    + "\n調教師コメント:\n" + trainerComment();
        }

        String ageText() {
            int age = 3 + careerMonths / 12;
            int month = careerMonths % 12;
            return age + "歳" + (month == 0 ? "" : month + "か月");
        }

        private String trainerComment() {
            if (retired) {
                if ("fracture".equals(retireReason)) {
                    return "強めの調教が続いて脚元に無理が出ました。骨折のため、この馬は引退です。";
                }
                return "ここまでよく走ってくれました。そろそろ引退させて、次の馬を見ましょう。";
            }

            String conditionComment;
            if (condition >= 82 && fatigue <= 25) {
                conditionComment = "体調は万全です。毛ヅヤも良く、動きに余裕があります。";
            } else if (condition >= 65 && fatigue <= 45) {
                conditionComment = "状態は悪くありません。もう少し整えばさらに良くなります。";
            } else if (fatigue >= 70) {
                conditionComment = "かなり疲れが見えます。無理せず軽めにしておきたいですね。";
            } else if (fatigue >= 50) {
                conditionComment = "少し疲れが残っています。馬なりで様子を見るのがよさそうです。";
            } else {
                conditionComment = "まだ本調子ではありませんが、調教を重ねれば上向いてきます。";
            }

            String readyComment;
            if (ready) {
                readyComment = "レースに出れます。相手関係を見て出走を決めましょう。";
            } else {
                readyComment = "まだレースに使うには早いですね。もう少し乗り込んでからにしましょう。";
            }
            if (lastTrainingComment != null && lastTrainingComment.length() > 0) {
                return lastTrainingComment + "\n" + conditionComment + "\n" + readyComment;
            }
            return conditionComment + "\n" + readyComment;
        }
    }

    private static class HorseStore {
        private static final String[] NAMES = {
                "ミドリノキセキ", "ハヤテノサクラ", "ツキノブレイブ", "アサヒノクラウン",
                "ホクトノレガリア", "シロガネランナー", "ナナイロスター", "カゼノグランプリ"
        };
        private final SharedPreferences prefs;

        HorseStore(Context context) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }

        void ensureHorse() {
            if (!prefs.contains("name")) {
                return;
            }
            SharedPreferences.Editor editor = prefs.edit();
            boolean needsApply = false;
            if (!prefs.contains("gameDay")) {
                editor.putInt("gameDay", prefs.getInt("careerMonths", 0) * 30);
                needsApply = true;
            }
            if (!prefs.contains("trainingProgress")) {
                editor.putInt("trainingProgress", 0).putBoolean("ready", false);
                needsApply = true;
            }
            if (!prefs.contains("trainingRequired")) {
                editor.putInt("trainingRequired", randomTrainingRequired());
                needsApply = true;
            }
            if (!prefs.contains("stallionName")) {
                Stallion stallion = Stallion.ALL[0];
                editor.putString("stallionName", stallion.name)
                        .putString("growthType", stallion.growth)
                        .putString("recordRank", stallion.recordRank);
                needsApply = true;
            }
            if (needsApply) {
                editor.apply();
            }
        }

        boolean hasHorse() {
            return prefs.contains("name");
        }

        Horse snapshot() {
            Horse horse = new Horse();
            horse.name = prefs.getString("name", NAMES[0]);
            horse.generation = prefs.getInt("generation", 1);
            horse.gameDay = prefs.getInt("gameDay", prefs.getInt("careerMonths", 0) * 30);
            horse.careerMonths = horse.gameDay / 30;
            horse.speed = prefs.getInt("speed", 44);
            horse.stamina = prefs.getInt("stamina", 44);
            horse.power = prefs.getInt("power", 42);
            horse.guts = prefs.getInt("guts", 40);
            horse.gate = prefs.getInt("gate", 40);
            horse.condition = prefs.getInt("condition", 74);
            horse.fatigue = prefs.getInt("fatigue", 10);
            horse.races = prefs.getInt("races", 0);
            horse.wins = prefs.getInt("wins", 0);
            horse.prize = prefs.getInt("prize", 0);
            horse.trainingProgress = prefs.getInt("trainingProgress", 0);
            horse.trainingRequired = prefs.getInt("trainingRequired", 3);
            horse.stallionName = prefs.getString("stallionName", "ノーザンレガシー");
            horse.growthType = prefs.getString("growthType", "普通");
            horse.recordRank = prefs.getString("recordRank", "B");
            horse.retireReason = prefs.getString("retireReason", "");
            horse.lastTrainingComment = prefs.getString("lastTrainingComment", "");
            horse.peakForm = prefs.getBoolean("peakForm", false);
            horse.ready = prefs.getBoolean("ready", false);
            horse.retired = prefs.getBoolean("retired", false);
            return horse;
        }

        static String defaultName(int generation) {
            return NAMES[(generation - 1) % NAMES.length];
        }

        int nextGeneration() {
            int generation = prefs.getInt("generation", 0) + 1;
            return generation;
        }

        void createHorse(String name, Stallion stallion) {
            int generation = nextGeneration();
            Random random = new Random(System.currentTimeMillis() + generation * 97L + stallion.name.hashCode());
            int speed = inheritedAbility(stallion.speedRank, random);
            int stamina = inheritedAbility(stallion.staminaRank, random);
            int recordBase = rankBase(stallion.recordRank);
            int growthAdjust = "早熟".equals(stallion.growth) ? 8 : "晩成".equals(stallion.growth) ? -6 : 0;
            prefs.edit()
                    .clear()
                    .putString("name", name)
                    .putInt("generation", generation)
                    .putString("stallionName", stallion.name)
                    .putString("growthType", stallion.growth)
                    .putString("recordRank", stallion.recordRank)
                    .putInt("speed", clamp(speed + growthAdjust, 25, 98))
                    .putInt("stamina", clamp(stamina + growthAdjust, 25, 98))
                    .putInt("power", clamp((speed + stamina) / 2 + random.nextInt(15) - 7, 25, 96))
                    .putInt("guts", clamp(recordBase + random.nextInt(23) - 11, 25, 96))
                    .putInt("gate", clamp(recordBase + random.nextInt(25) - 12, 25, 96))
                    .putInt("condition", 78)
                    .putInt("fatigue", 8)
                    .putInt("gameDay", 0)
                    .putInt("trainingProgress", 0)
                    .putInt("trainingRequired", trainingRequiredFor(stamina, random))
                    .putInt("hardTrainingStreak", 0)
                    .putBoolean("ready", false)
                    .apply();
        }

        void retireCurrentHorse() {
            if (!hasHorse()) {
                return;
            }
            prefs.edit()
                    .putBoolean("retired", true)
                    .putString("retireReason", "manual")
                    .putBoolean("ready", false)
                    .remove("trainingType")
                    .remove("trainingStartAt")
                    .remove("trainingEndAt")
                    .apply();
        }

        boolean isTraining() {
            return prefs.getLong("trainingEndAt", 0L) > System.currentTimeMillis();
        }

        boolean isReadyForRace() {
            finishTrainingIfReady();
            return prefs.getBoolean("ready", false) && !prefs.getBoolean("retired", false);
        }

        boolean isRetired() {
            return prefs.getBoolean("retired", false);
        }

        void startTraining(int type, long durationMs) {
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putInt("trainingType", type)
                    .putLong("trainingStartAt", now)
                    .putLong("trainingEndAt", now + durationMs)
                    .putBoolean("ready", false)
                    .apply();
        }

        boolean finishTrainingIfReady() {
            long endAt = prefs.getLong("trainingEndAt", 0L);
            if (endAt == 0L || endAt > System.currentTimeMillis()) {
                return false;
            }
            int type = prefs.getInt("trainingType", 0);
            long startAt = prefs.getLong("trainingStartAt", endAt);
            long minutes = Math.max(1, (endAt - startAt) / 60_000L);
            int gain = Math.min(10, 2 + (int) Math.sqrt(minutes));
            Horse horse = snapshot();

            int speed = horse.speed;
            int stamina = horse.stamina;
            int power = horse.power;
            int guts = horse.guts;
            int gate = horse.gate;
            int condition = horse.condition;
            int fatigue = horse.fatigue;
            int trainingProgress = Math.min(horse.trainingRequired, horse.trainingProgress + 1);
            int gameDay = horse.gameDay + TRAINING_DAYS;
            boolean retired = gameDay >= GameCalendar.YEAR_DAYS * 4;
            String retireReason = retired ? "age" : "";
            int hardTrainingStreak = type == 0 ? prefs.getInt("hardTrainingStreak", 0) + 1 : 0;
            boolean peakForm = false;
            int preTrainingFatigue = fatigue;

            if (type == 0) {
                int fractureRisk = Math.max(0, fatigue - 45) + hardTrainingStreak * 9;
                if (fatigue >= 35 && new Random().nextInt(100) < fractureRisk) {
                    prefs.edit()
                            .putInt("gameDay", gameDay)
                            .putInt("fatigue", 100)
                            .putBoolean("ready", false)
                            .putBoolean("retired", true)
                            .putString("retireReason", "fracture")
                            .putInt("hardTrainingStreak", hardTrainingStreak)
                            .remove("trainingType")
                            .remove("trainingStartAt")
                            .remove("trainingEndAt")
                            .apply();
                    return true;
                }
                speed += gain + 2;
                stamina += gain / 2;
                power += gain;
                guts += 1;
                fatigue += Math.min(24, 12 + gain);
                condition -= 3;
                peakForm = preTrainingFatigue <= 18 && new Random().nextInt(100) < 30;
                if (peakForm) {
                    condition = Math.max(condition, 90);
                    fatigue = Math.min(fatigue, 24);
                }
            } else if (type == 1) {
                speed += Math.max(1, gain / 2);
                stamina += Math.max(1, gain / 2);
                power += gain / 2;
                guts += 1;
                gate += 1;
                fatigue += Math.min(14, 5 + gain / 2);
            } else {
                speed += 1;
                stamina += 1;
                power += 1;
                guts += 1;
                gate += 1;
                fatigue = Math.max(0, fatigue - 10);
                condition += 3;
            }

            condition = clamp(condition - Math.max(0, fatigue - 40) / 5, 35, 100);
            String trainingComment = trainingConditionComment(condition, fatigue, type, peakForm);
            prefs.edit()
                    .putInt("speed", clamp(speed, 1, 100))
                    .putInt("stamina", clamp(stamina, 1, 100))
                    .putInt("power", clamp(power, 1, 100))
                    .putInt("guts", clamp(guts, 1, 100))
                    .putInt("gate", clamp(gate, 1, 100))
                    .putInt("condition", clamp(condition, 1, 100))
                    .putInt("fatigue", clamp(fatigue, 0, 100))
                    .putInt("trainingProgress", trainingProgress)
                    .putInt("gameDay", gameDay)
                    .putInt("hardTrainingStreak", hardTrainingStreak)
                    .putString("lastTrainingComment", trainingComment)
                    .putBoolean("peakForm", peakForm)
                    .putBoolean("ready", trainingProgress >= horse.trainingRequired && !retired)
                    .putBoolean("retired", retired)
                    .putString("retireReason", retireReason)
                    .remove("trainingType")
                    .remove("trainingStartAt")
                    .remove("trainingEndAt")
                    .apply();
            return true;
        }

        String trainingText(SimpleDateFormat format) {
            if (isRetired()) {
                return "引退済みです。次の競走馬を育成できます。";
            }
            long endAt = prefs.getLong("trainingEndAt", 0L);
            if (endAt > System.currentTimeMillis()) {
                long remaining = Math.max(0, endAt - System.currentTimeMillis());
                return "調教中: " + trainingName(prefs.getInt("trainingType", 0))
                        + "\n完了予定: " + format.format(new Date(endAt))
                        + "\n残り: " + remainingText(remaining);
            }
            return prefs.getBoolean("ready", false)
                    ? "レースに出れます。ジョッキーと指示を選んでください。"
                    : "調教指示を選んで開始してください。";
        }

        void applyPasture() {
            Horse horse = snapshot();
            int newGameDay = horse.gameDay + 28;
            boolean retired = newGameDay >= GameCalendar.YEAR_DAYS * 4;
            prefs.edit()
                    .putInt("gameDay", newGameDay)
                    .putInt("careerMonths", newGameDay / 30)
                    .putInt("condition", clamp(horse.condition + 18, 1, 100))
                    .putInt("fatigue", Math.max(0, horse.fatigue - 55))
                    .putInt("hardTrainingStreak", 0)
                    .putString("lastTrainingComment", "放牧で疲れはかなり抜けました。馬体もふっくらしてきました。")
                    .putBoolean("peakForm", false)
                    .putBoolean("retired", retired)
                    .putString("retireReason", retired ? "age" : "")
                    .apply();
        }

        void applyRace(RaceResult result) {
            Horse horse = snapshot();
            int prize = result.rank == 1 ? result.race.prizeWin
                    : result.rank == 2 ? result.race.prizeWin / 2
                    : result.rank == 3 ? result.race.prizeWin / 4
                    : 0;
            int newGameDay = Math.max(horse.gameDay, result.raceGameDay) + POST_RACE_DAYS;
            boolean retired = newGameDay >= GameCalendar.YEAR_DAYS * 4 || horse.races + 1 >= 30;
            prefs.edit()
                    .putInt("races", horse.races + 1)
                    .putInt("wins", horse.wins + (result.rank == 1 ? 1 : 0))
                    .putInt("prize", horse.prize + prize)
                    .putInt("gameDay", newGameDay)
                    .putInt("careerMonths", newGameDay / 30)
                    .putInt("trainingProgress", 0)
                    .putInt("trainingRequired", trainingRequiredFor(horse.stamina, new Random()))
                    .putInt("hardTrainingStreak", 0)
                    .putInt("condition", clamp(horse.condition - 12, 25, 100))
                    .putInt("fatigue", clamp(horse.fatigue + 22, 0, 100))
                    .putString("lastTrainingComment", "")
                    .putBoolean("peakForm", false)
                    .putBoolean("ready", false)
                    .putBoolean("retired", retired)
                    .apply();
        }

        private String trainingName(int type) {
            return new String[]{"強め", "弱め", "馬なり"}[type];
        }

        private String trainingConditionComment(int condition, int fatigue, int type, boolean peakForm) {
            if (peakForm) {
                return "絶好調です。いい感じに仕上がりました。今ならレースでいい結果を期待できます。";
            }
            if (fatigue >= 78) {
                return "疲れが見えます。ここで無理をすると脚元に響きそうです。";
            }
            if (condition <= 45 && fatigue <= 35) {
                return "少し太り気味です。動きに重さがありますね。";
            }
            if (condition >= 88 && fatigue <= 18) {
                return "絶好調です。反応も鋭く、文句ない仕上がりです。";
            }
            if (condition >= 75 && fatigue <= 35) {
                return "いい感じに仕上がりました。レースでも力を出せそうです。";
            }
            if (type == 0 && fatigue >= 55) {
                return "強めにやったぶん疲れが残っています。次は軽めで様子を見たいですね。";
            }
            if (type == 2 && fatigue <= 30) {
                return "馬なりで気分良く走れていました。雰囲気は悪くありません。";
            }
            return "順調に乗り込めています。大きな問題はありません。";
        }

        String calendarText() {
            Horse horse = snapshot();
            return "現在: " + GameCalendar.weekText(horse.gameDay)
                    + "\n育成期間: " + (horse.gameDay / 7 + 1) + "週目 / 引退目安 " + GameCalendar.weekText(GameCalendar.YEAR_DAYS * 4)
                    + "\n調教完了で1週、レース後は1週、放牧では1か月進みます。";
        }

        int nextRaceGameDay(RaceData race) {
            return GameCalendar.nextRaceGameDay(snapshot().gameDay, race);
        }

        int currentGameDay() {
            return snapshot().gameDay;
        }

        private int randomTrainingRequired() {
            return MIN_TRAININGS_REQUIRED + new Random().nextInt(MAX_TRAININGS_REQUIRED - MIN_TRAININGS_REQUIRED + 1);
        }

        private int trainingRequiredFor(int stamina, Random random) {
            int base;
            if (stamina >= 82) {
                base = 1 + random.nextInt(2);
            } else if (stamina >= 66) {
                base = 2 + random.nextInt(2);
            } else if (stamina >= 50) {
                base = 3 + random.nextInt(2);
            } else {
                base = 4 + random.nextInt(2);
            }
            return clamp(base, MIN_TRAININGS_REQUIRED, MAX_TRAININGS_REQUIRED);
        }

        private int inheritedAbility(String rank, Random random) {
            return clamp(rankBase(rank) + random.nextInt(31) - 15, 25, 98);
        }

        private int rankBase(String rank) {
            if ("A".equals(rank)) {
                return 82;
            }
            if ("B".equals(rank)) {
                return 66;
            }
            return 48;
        }
    }

    private static class Stallion {
        static final Stallion[] ALL = {
                new Stallion("ノーザンレガシー", "A", "普通", "A", "B"),
                new Stallion("サクラブレイブ", "A", "早熟", "A", "C"),
                new Stallion("グランドステイヤー", "A", "晩成", "B", "A"),
                new Stallion("ミラクルロード", "B", "普通", "B", "B"),
                new Stallion("ライトニングベル", "B", "早熟", "A", "B"),
                new Stallion("ロングリバー", "B", "晩成", "C", "A"),
                new Stallion("ターフマーチ", "C", "普通", "B", "C"),
                new Stallion("ダートワーカー", "C", "晩成", "C", "B"),
                new Stallion("スモールチャンス", "C", "早熟", "C", "C")
        };

        final String name;
        final String recordRank;
        final String growth;
        final String speedRank;
        final String staminaRank;

        Stallion(String name, String recordRank, String growth, String speedRank, String staminaRank) {
            this.name = name;
            this.recordRank = recordRank;
            this.growth = growth;
            this.speedRank = speedRank;
            this.staminaRank = staminaRank;
        }

        static String[] names() {
            String[] names = new String[ALL.length];
            for (int i = 0; i < ALL.length; i++) {
                Stallion stallion = ALL[i];
                names[i] = stallion.name
                        + " 実績" + stallion.recordRank
                        + " " + stallion.growth
                        + " スピード" + stallion.speedRank
                        + " スタミナ" + stallion.staminaRank;
            }
            return names;
        }
    }

    private static class RaceData {
        static final RaceData[] ALL = {
                new RaceData(1, 4, "中山金杯", "中山", "芝", 2000, "G3", 4300),
                new RaceData(1, 4, "京都金杯", "京都", "芝", 1600, "G3", 4300),
                new RaceData(1, 18, "日経新春杯", "京都", "芝", 2400, "G2", 5700),
                new RaceData(1, 25, "アメリカジョッキークラブカップ", "中山", "芝", 2200, "G2", 6200),
                new RaceData(1, 25, "プロキオンステークス", "京都", "ダート", 1800, "G2", 5500),
                new RaceData(2, 1, "根岸ステークス", "東京", "ダート", 1400, "G3", 4000),
                new RaceData(2, 1, "シルクロードステークス", "京都", "芝", 1200, "G3", 4100),
                new RaceData(2, 10, "東京新聞杯", "東京", "芝", 1600, "G3", 4100),
                new RaceData(2, 15, "京都記念", "京都", "芝", 2200, "G2", 6200),
                new RaceData(2, 22, "フェブラリーステークス", "東京", "ダート", 1600, "G1", 15000),
                new RaceData(3, 1, "中山記念", "中山", "芝", 1800, "G2", 6700),
                new RaceData(3, 8, "弥生賞ディープインパクト記念", "中山", "芝", 2000, "G2", 5400),
                new RaceData(3, 15, "金鯱賞", "中京", "芝", 2000, "G2", 6700),
                new RaceData(3, 22, "阪神大賞典", "阪神", "芝", 3000, "G2", 6700),
                new RaceData(3, 28, "日経賞", "中山", "芝", 2500, "G2", 6700),
                new RaceData(3, 29, "高松宮記念", "中京", "芝", 1200, "G1", 17000),
                new RaceData(4, 5, "大阪杯", "阪神", "芝", 2000, "G1", 30000),
                new RaceData(4, 11, "ニュージーランドトロフィー", "中山", "芝", 1600, "G2", 5400),
                new RaceData(4, 12, "桜花賞", "阪神", "芝", 1600, "G1", 14000),
                new RaceData(4, 19, "皐月賞", "中山", "芝", 2000, "G1", 20000),
                new RaceData(4, 25, "青葉賞", "東京", "芝", 2400, "G2", 5400),
                new RaceData(4, 26, "フローラステークス", "東京", "芝", 2000, "G2", 5200),
                new RaceData(4, 26, "マイラーズカップ", "京都", "芝", 1600, "G2", 5900),
                new RaceData(5, 2, "京王杯スプリングカップ", "東京", "芝", 1400, "G2", 5900),
                new RaceData(5, 2, "ユニコーンステークス", "京都", "ダート", 1900, "G3", 3700),
                new RaceData(5, 3, "天皇賞（春）", "京都", "芝", 3200, "G1", 30000),
                new RaceData(5, 9, "エプソムカップ", "東京", "芝", 1800, "G3", 4300),
                new RaceData(5, 9, "京都新聞杯", "京都", "芝", 2200, "G2", 5400),
                new RaceData(5, 10, "NHKマイルカップ", "東京", "芝", 1600, "G1", 13000),
                new RaceData(5, 17, "ヴィクトリアマイル", "東京", "芝", 1600, "G1", 15000),
                new RaceData(5, 24, "優駿牝馬（オークス）", "東京", "芝", 2400, "G1", 15000),
                new RaceData(5, 31, "東京優駿（日本ダービー）", "東京", "芝", 2400, "G1", 30000),
                new RaceData(5, 31, "目黒記念", "東京", "芝", 2500, "G2", 5700),
                new RaceData(6, 7, "安田記念", "東京", "芝", 1600, "G1", 18000),
                new RaceData(6, 13, "函館スプリントステークス", "函館", "芝", 1200, "G3", 4100),
                new RaceData(6, 14, "宝塚記念", "阪神", "芝", 2200, "G1", 30000),
                new RaceData(6, 28, "ラジオNIKKEI賞", "福島", "芝", 1800, "G3", 4100),
                new RaceData(6, 28, "函館記念", "函館", "芝", 2000, "G3", 4300),
                new RaceData(7, 5, "北九州記念", "小倉", "芝", 1200, "G3", 4100),
                new RaceData(7, 12, "七夕賞", "福島", "芝", 2000, "G3", 4300),
                new RaceData(7, 26, "関屋記念", "新潟", "芝", 1600, "G3", 4100),
                new RaceData(8, 2, "アイビスサマーダッシュ", "新潟", "芝", 1000, "G3", 4100),
                new RaceData(8, 8, "エルムステークス", "札幌", "ダート", 1700, "G3", 3800),
                new RaceData(8, 16, "札幌記念", "札幌", "芝", 2000, "G2", 7000),
                new RaceData(8, 30, "新潟記念", "新潟", "芝", 2000, "G3", 4300),
                new RaceData(9, 6, "紫苑ステークス", "中山", "芝", 2000, "G2", 5200),
                new RaceData(9, 6, "セントウルステークス", "阪神", "芝", 1200, "G2", 5900),
                new RaceData(9, 13, "セントライト記念", "中山", "芝", 2200, "G2", 5400),
                new RaceData(9, 13, "ローズステークス", "阪神", "芝", 1800, "G2", 5200),
                new RaceData(9, 20, "オールカマー", "中山", "芝", 2200, "G2", 6700),
                new RaceData(9, 21, "神戸新聞杯", "阪神", "芝", 2400, "G2", 5400),
                new RaceData(9, 27, "スプリンターズステークス", "中山", "芝", 1200, "G1", 17000),
                new RaceData(10, 4, "毎日王冠", "東京", "芝", 1800, "G2", 6700),
                new RaceData(10, 4, "京都大賞典", "京都", "芝", 2400, "G2", 6700),
                new RaceData(10, 12, "スワンステークス", "京都", "芝", 1400, "G2", 5900),
                new RaceData(10, 17, "富士ステークス", "東京", "芝", 1600, "G2", 5900),
                new RaceData(10, 18, "秋華賞", "京都", "芝", 2000, "G1", 11000),
                new RaceData(10, 25, "菊花賞", "京都", "芝", 3000, "G1", 20000),
                new RaceData(11, 1, "天皇賞（秋）", "東京", "芝", 2000, "G1", 30000),
                new RaceData(11, 8, "アルゼンチン共和国杯", "東京", "芝", 2500, "G2", 5700),
                new RaceData(11, 8, "みやこステークス", "京都", "ダート", 1800, "G3", 4000),
                new RaceData(11, 14, "武蔵野ステークス", "東京", "ダート", 1600, "G3", 4000),
                new RaceData(11, 15, "エリザベス女王杯", "京都", "芝", 2200, "G1", 15000),
                new RaceData(11, 22, "マイルチャンピオンシップ", "京都", "芝", 1600, "G1", 18000),
                new RaceData(11, 29, "ジャパンカップ", "東京", "芝", 2400, "G1", 50000),
                new RaceData(11, 29, "京阪杯", "京都", "芝", 1200, "G3", 4100),
                new RaceData(12, 5, "ステイヤーズステークス", "中山", "芝", 3600, "G2", 6200),
                new RaceData(12, 6, "チャンピオンズカップ", "中京", "ダート", 1800, "G1", 15000),
                new RaceData(12, 13, "カペラステークス", "中山", "ダート", 1200, "G3", 3800),
                new RaceData(12, 20, "朝日杯フューチュリティステークス", "阪神", "芝", 1600, "G1", 8000),
                new RaceData(12, 26, "ホープフルステークス", "中山", "芝", 2000, "G1", 8000),
                new RaceData(12, 26, "阪神カップ", "阪神", "芝", 1400, "G2", 6700),
                new RaceData(12, 27, "有馬記念", "中山", "芝", 2500, "G1", 50000)
        };

        final int month;
        final int day;
        final int dayOfYear;
        final String name;
        final String course;
        final String surface;
        final int distance;
        final String grade;
        final int prizeWin;

        RaceData(int month, int day, String name, String course, String surface, int distance, String grade, int prizeWin) {
            this.month = month;
            this.day = day;
            this.dayOfYear = GameCalendar.dayOfYear(month, day);
            this.name = name;
            this.course = course;
            this.surface = surface;
            this.distance = distance;
            this.grade = grade;
            this.prizeWin = prizeWin;
        }

        static String[] names() {
            String[] names = new String[ALL.length];
            for (int i = 0; i < ALL.length; i++) {
                names[i] = GameCalendar.monthWeekText(ALL[i].month, ALL[i].day) + " " + ALL[i].grade + " " + ALL[i].name;
            }
            return names;
        }

        static List<RaceData> forGameWeek(int gameDay) {
            List<RaceData> races = new ArrayList<>();
            int month = GameCalendar.monthOf(gameDay);
            int week = GameCalendar.weekOfMonth(gameDay);
            for (RaceData race : ALL) {
                if (race.month == month && GameCalendar.weekOfMonthForDay(race.day) == week) {
                    races.add(race);
                }
            }
            return races;
        }
    }

    private static class RaceEngine {
        private static final List<String> RIVALS = Arrays.asList(
                "サンライズホープ", "グランドミラージュ", "レッドテンペスト", "キングスロード",
                "ブルームフェザー", "ファイナルベル", "スターリーミント"
        );

        static RaceResult simulate(Horse horse, RaceData race, String jockey, String tactic, int raceGameDay, String raceDate) {
            Random random = new Random((horse.name + race.name + jockey + tactic + horse.races).hashCode());
            List<Runner> runners = new ArrayList<>();
            double base = horse.speed * shortFactor(race.distance)
                    + horse.stamina * longFactor(race.distance)
                    + horse.power * 0.62
                    + horse.guts * 0.54
                    + horse.gate * 0.32
                    + conditionScore(horse);
            base += jockeyBonus(jockey, tactic);
            base += tacticBonus(tactic, race.distance, horse);
            base += maturityBonus(horse);
            base += recordBonus(horse.recordRank, race.grade);
            if (horse.peakForm) {
                base += 36;
            }
            runners.add(new Runner(horse.name, base + random.nextGaussian() * 5, tactic, true));

            for (int i = 0; i < RIVALS.size(); i++) {
                double rival = rivalBase(race.grade) + random.nextInt(36) + random.nextGaussian() * 7;
                String rivalTactic = new String[]{"逃げ", "普通", "追い込み"}[random.nextInt(3)];
                if (race.distance >= 2400 && "追い込み".equals(rivalTactic)) {
                    rival += 5;
                }
                runners.add(new Runner(RIVALS.get(i), rival, rivalTactic, false));
            }
            runners.sort((a, b) -> Double.compare(b.score, a.score));
            int rank = 1;
            for (int i = 0; i < runners.size(); i++) {
                runners.get(i).rank = i + 1;
                if (runners.get(i).player) {
                    rank = i + 1;
                }
            }
            for (Runner runner : runners) {
                runner.visualPace = visualPaceFor(runner.rank);
            }
            runners.sort((a, b) -> Boolean.compare(!a.player, !b.player));
            return new RaceResult(race, runners, rank, jockey, tactic, raceGameDay, raceDate);
        }

        private static double shortFactor(int distance) {
            return distance <= 1600 ? 1.3 : distance <= 2200 ? 1.0 : 0.78;
        }

        private static double longFactor(int distance) {
            return distance >= 2500 ? 1.35 : distance >= 2000 ? 1.05 : 0.78;
        }

        private static double jockeyBonus(String jockey, String tactic) {
            if (jockey.contains("逃げ") && "逃げ".equals(tactic)) {
                return 12;
            }
            if (jockey.contains("差し") && "追い込み".equals(tactic)) {
                return 12;
            }
            if (jockey.contains("ベテラン") || jockey.contains("外国人")) {
                return 8;
            }
            return 4;
        }

        private static double tacticBonus(String tactic, int distance, Horse horse) {
            if ("お任せ".equals(tactic)) {
                return 6 + horse.guts * 0.12;
            }
            if ("逃げ".equals(tactic)) {
                return horse.gate * 0.35 + (distance <= 1800 ? 8 : -4);
            }
            if ("追い込み".equals(tactic)) {
                return horse.stamina * 0.22 + horse.guts * 0.24 + (distance >= 2200 ? 7 : -3);
            }
            return 5 + horse.condition * 0.08;
        }

        private static double conditionScore(Horse horse) {
            double score = (horse.condition - 55) * 1.15 - horse.fatigue * 1.25;
            if (horse.condition >= 85 && horse.fatigue <= 25) {
                score += 28;
            } else if (horse.condition <= 45 || horse.fatigue >= 70) {
                score -= 42;
            } else if (horse.fatigue >= 50) {
                score -= 20;
            }
            return score;
        }

        private static double maturityBonus(Horse horse) {
            int age = 3 + horse.careerMonths / 12;
            if ("早熟".equals(horse.growthType)) {
                if (age <= 3) {
                    return 48;
                }
                if (age == 4) {
                    return 30;
                }
                if (age == 5) {
                    return -18;
                }
                return -58;
            }
            if ("晩成".equals(horse.growthType)) {
                if (age <= 3) {
                    return -42;
                }
                if (age == 4) {
                    return -12;
                }
                if (age == 5) {
                    return 28;
                }
                return 52;
            }
            return age >= 4 && age <= 6 ? 16 : -4;
        }

        private static double recordBonus(String recordRank, String grade) {
            if ("A".equals(recordRank)) {
                return "G1".equals(grade) ? 22 : 14;
            }
            if ("B".equals(recordRank)) {
                return "G1".equals(grade) ? 8 : 10;
            }
            return "G3".equals(grade) ? 2 : -8;
        }

        private static double rivalBase(String grade) {
            if ("G1".equals(grade)) {
                return 295;
            }
            if ("G2".equals(grade)) {
                return 258;
            }
            return 228;
        }

        private static float visualPaceFor(int rank) {
            return 0.80f + (8 - rank) * 0.026f;
        }
    }

    private static class RaceResult {
        final RaceData race;
        final List<Runner> runners;
        final int rank;
        final String jockey;
        final String tactic;
        final int raceGameDay;
        final String raceDate;

        RaceResult(RaceData race, List<Runner> runners, int rank, String jockey, String tactic, int raceGameDay, String raceDate) {
            this.race = race;
            this.runners = runners;
            this.rank = rank;
            this.jockey = jockey;
            this.tactic = tactic;
            this.raceGameDay = raceGameDay;
            this.raceDate = raceDate;
        }

        String summary(boolean retired) {
            StringBuilder builder = new StringBuilder();
            builder.append(raceDate).append(" ").append(race.name).append(" 結果\n");
            builder.append("ジョッキー: ").append(jockey).append(" / 指示: ").append(tactic).append("\n");
            builder.append("着順: ").append(rank).append("着\n\n");
            List<Runner> sorted = new ArrayList<>(runners);
            sorted.sort((a, b) -> Integer.compare(a.rank, b.rank));
            for (Runner runner : sorted) {
                builder.append(runner.rank).append("着  ").append(runner.name);
                if (runner.player) {
                    builder.append("  *育成馬");
                }
                builder.append("\n");
            }
            if (retired) {
                builder.append("\n7歳前後まで走り切ったため、この馬は引退します。次の競走馬を育成できます。");
            }
            return builder.toString();
        }
    }

    private static class Runner {
        final String name;
        final double score;
        final String tactic;
        final boolean player;
        float visualPace;
        int rank;

        Runner(String name, double score, String tactic, boolean player) {
            this.name = name;
            this.score = score;
            this.tactic = tactic;
            this.player = player;
        }
    }

    private static class GameCalendar {
        static final int BASE_YEAR = 2026;
        static final int YEAR_DAYS = 365;
        private static final int[] MONTH_LENGTHS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        static int dayOfYear(int month, int day) {
            int total = 0;
            for (int i = 0; i < month - 1; i++) {
                total += MONTH_LENGTHS[i];
            }
            return total + day - 1;
        }

        static int nextRaceGameDay(int currentGameDay, RaceData race) {
            int seasonStart = (currentGameDay / YEAR_DAYS) * YEAR_DAYS;
            int candidate = seasonStart + race.dayOfYear;
            if (candidate < currentGameDay) {
                candidate += YEAR_DAYS;
            }
            return candidate;
        }

        static String weekText(int gameDay) {
            int year = BASE_YEAR + gameDay / YEAR_DAYS;
            int month = monthOf(gameDay);
            int week = weekOfMonth(gameDay);
            return year + "年" + month + "月第" + week + "週";
        }

        static String monthWeekText(int month, int day) {
            int week = weekOfMonthForDay(day);
            return month + "月第" + week + "週";
        }

        static int monthOf(int gameDay) {
            int dayInYear = gameDay % YEAR_DAYS;
            int month = 1;
            while (month <= MONTH_LENGTHS.length && dayInYear >= MONTH_LENGTHS[month - 1]) {
                dayInYear -= MONTH_LENGTHS[month - 1];
                month++;
            }
            return month;
        }

        static int weekOfMonth(int gameDay) {
            int dayInYear = gameDay % YEAR_DAYS;
            int month = 1;
            while (month <= MONTH_LENGTHS.length && dayInYear >= MONTH_LENGTHS[month - 1]) {
                dayInYear -= MONTH_LENGTHS[month - 1];
                month++;
            }
            return dayInYear / 7 + 1;
        }

        static int weekOfMonthForDay(int day) {
            return (day - 1) / 7 + 1;
        }

        static int weekKey(int gameDay) {
            return (BASE_YEAR + gameDay / YEAR_DAYS) * 1000 + monthOf(gameDay) * 10 + weekOfMonth(gameDay);
        }
    }

    private class RaceTrackView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RaceResult result;
        private long startAt;

        RaceTrackView(Context context) {
            super(context);
            setBackgroundColor(Color.rgb(60, 108, 71));
        }

        void startRace(RaceResult result) {
            this.result = result;
            this.startAt = System.currentTimeMillis();
            invalidate();
        }

        void clearRace() {
            this.result = null;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            drawCourse(canvas, width, height);
            if (result == null) {
                drawIdle(canvas, width, height);
                return;
            }
            float elapsed = Math.min(1f, (System.currentTimeMillis() - startAt) / (float) RACE_MS);
            drawRunners(canvas, width, height, elapsed);
            if (elapsed < 1f) {
                invalidate();
            }
        }

        private void drawCourse(Canvas canvas, int width, int height) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(60, 108, 71));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.rgb(206, 176, 107));
            RectF track = new RectF(dp(18), dp(48), width - dp(18), height - dp(30));
            canvas.drawRoundRect(track, dp(28), dp(28), paint);
            paint.setColor(Color.rgb(236, 217, 166));
            canvas.drawRoundRect(new RectF(track.left + dp(10), track.top + dp(10), track.right - dp(10), track.bottom - dp(10)),
                    dp(22), dp(22), paint);
            paint.setColor(Color.rgb(255, 252, 244));
            canvas.drawRect(width - dp(42), track.top - dp(10), width - dp(36), track.bottom + dp(8), paint);
            paint.setTextSize(dp(14));
            paint.setColor(Color.WHITE);
            canvas.drawText(result == null ? "Race Watch" : result.race.name, dp(18), dp(28), paint);
        }

        private void drawIdle(Canvas canvas, int width, int height) {
            paint.setColor(Color.rgb(33, 45, 38));
            paint.setTextSize(dp(16));
            canvas.drawText("準備OKになったら出馬できます", dp(26), height / 2f, paint);
        }

        private void drawRunners(Canvas canvas, int width, int height, float elapsed) {
            float laneTop = dp(72);
            float laneHeight = (height - dp(128)) / 8f;
            for (int i = 0; i < result.runners.size(); i++) {
                Runner runner = result.runners.get(i);
                float progress = progressFor(runner, elapsed);
                float x = dp(34) + progress * (width - dp(86));
                float y = laneTop + laneHeight * i + laneHeight * 0.5f;

                paint.setColor(i % 2 == 0 ? Color.rgb(190, 159, 95) : Color.rgb(181, 146, 82));
                canvas.drawRect(dp(28), y + dp(11), width - dp(46), y + dp(13), paint);
                paint.setColor(runner.player ? Color.rgb(201, 47, 54) : Color.rgb(35, 45, 40));
                canvas.drawOval(new RectF(x - dp(12), y - dp(9), x + dp(12), y + dp(9)), paint);
                paint.setColor(runner.player ? Color.rgb(255, 232, 87) : Color.rgb(236, 236, 226));
                canvas.drawCircle(x + dp(7), y - dp(11), dp(5), paint);
                paint.setColor(Color.rgb(33, 45, 38));
                paint.setTextSize(dp(10));
                canvas.drawText(String.valueOf(runner.rank), x - dp(3), y + dp(4), paint);
                if (runner.player) {
                    paint.setTextSize(dp(12));
                    canvas.drawText(runner.name, dp(30), y - dp(12), paint);
                }
            }
        }

        private float progressFor(Runner runner, float elapsed) {
            float progress = elapsed * runner.visualPace;
            if (elapsed >= 1f) {
                progress = 0.88f + (8 - runner.rank) * 0.014f;
            }
            return Math.max(0.02f, Math.min(0.98f, progress));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String remainingText(long ms) {
        long seconds = ms / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainSeconds = seconds % 60L;
        if (hours > 0) {
            return hours + "時間" + minutes + "分";
        }
        if (minutes > 0) {
            return minutes + "分" + remainSeconds + "秒";
        }
        return remainSeconds + "秒";
    }
}
