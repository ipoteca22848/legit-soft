package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AnarchyFinder extends Module {
    public static final String MODE_KEYWORD = "режим";
    private static final Pattern ONLINE_PAIR_PATTERN = Pattern.compile("([0-9][0-9\\s]*)\\s*/\\s*([0-9][0-9\\s]*)", 66);
    private static final Pattern ONLINE_MODE_JOIN_PATTERN = Pattern.compile("(?is)онлайн\\s*режим[а-я]*\\s*:?\\s*([0-9][0-9\\s]*)\\s*/\\s*([0-9][0-9\\s]*)");
    private static final Pattern ONLINE_ANY_PAIR_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)", 66);
    private static final int ANARCHY_REGEX_FLAGS = 66;
    private static final Pattern ANARCHY_ID_PATTERN = Pattern.compile("(?:анархия|анархии|анарх\\S*|anarchy)\\s*[-–—:#№\\s]*(?:#|№)?\\s*(\\d{1,6})", 66);
    private static final Pattern ANARCHY_DASH_ID_PATTERN = Pattern.compile("(?:анархия|анархии|anarchy)\\s*-\\s*(\\d{1,6})", 66);
    private static final Pattern ANARCHY_HASH_FALLBACK_PATTERN = Pattern.compile("#\\s*(\\d{1,6})");
    private static final Pattern ANARCHY_CONTEXT_PATTERN = Pattern.compile("анарх|anarchy", 66);
    private static final int[] FIRST_G0 = rangeSlots(28, 33);
    private static final int[] FIRST_G1 = rangeSlots(28, 47);
    private static final int[] FIRST_G2 = rangeSlots(28, 34);
    private static final int[] FIRST_G3 = rangeSlots(28, 31);
    private static final int[] FIRST_G4 = rangeSlots(28, 30);
    private static final int[] SECOND_G0 = rangeSlots(28, 41);
    private static final int[] SECOND_G1 = rangeSlots(19, 52);
    private static final int[] SECOND_G2 = rangeSlots(19, 21);
    private static final int[] SECOND_G3 = rangeSlots(28, 50);
    private static final int[] SECOND_G4 = rangeSlots(28, 41);
    private static final int[] SECOND_G5 = rangeSlots(28, 31);
    private boolean scanInProgress = false;
    private int scanStage = 0;
    private long nextActionAtMs = 0L;
    private int stageSlotIndex = 0;
    private boolean stageTransitioning = false;
    private int transitionClickIndex = 0;
    private int analyzeRetries = 0;
    private int firstMinOnline = Integer.MAX_VALUE;
    private boolean hasFirstOnline = false;
    private String firstAnarchyId = null;
    private int secondMinOnline = Integer.MAX_VALUE;
    private boolean hasSecondOnline = false;
    private String secondAnarchyId = null;
    private boolean resultReady = false;
    private long resultUntilMs = 0L;

    public AnarchyFinder() {
        super("AnarchyFinder", "Ищет анархию с наименьшем онлайном", Category.MISC);
    }

    public static boolean isModeContainerTitle(Text title) {
        return title == null ? false : title.getString().toLowerCase(Locale.ROOT).contains("режим");
    }

    public void startScan() {
        if (!this.scanInProgress) {
            if (this.mc.player != null) {
                if (this.mc.currentScreen instanceof HandledScreen screen) {
                    if (isModeContainerTitle(screen.getTitle())) {
                        this.scanInProgress = true;
                        this.scanStage = 0;
                        this.stageSlotIndex = 0;
                        this.stageTransitioning = false;
                        this.transitionClickIndex = 0;
                        this.analyzeRetries = 0;
                        this.firstMinOnline = Integer.MAX_VALUE;
                        this.hasFirstOnline = false;
                        this.firstAnarchyId = null;
                        this.secondMinOnline = Integer.MAX_VALUE;
                        this.hasSecondOnline = false;
                        this.secondAnarchyId = null;
                        this.resultReady = false;
                        this.nextActionAtMs = 0L;
                    }
                }
            }
        }
    }

    public boolean isScanInProgress() {
        return this.scanInProgress;
    }

    public boolean isResultReady() {
        if (!this.resultReady) {
            return false;
        } else if (System.currentTimeMillis() > this.resultUntilMs) {
            this.resultReady = false;
            return false;
        } else {
            return true;
        }
    }

    public int getFirstMinOnline() {
        return this.hasFirstOnline ? this.firstMinOnline : -1;
    }

    public int getSecondMinOnline() {
        return this.hasSecondOnline ? this.secondMinOnline : -1;
    }

    public String getFirstAnarchyId() {
        return this.firstAnarchyId;
    }

    public String getSecondAnarchyId() {
        return this.secondAnarchyId;
    }

    @Override
    public void onTick() {
        if (this.scanInProgress) {
            if (this.mc.player != null && this.mc.interactionManager != null) {
                long now = System.currentTimeMillis();
                if (now >= this.nextActionAtMs) {
                    if (!(this.mc.currentScreen instanceof HandledScreen screen)) {
                        this.nextActionAtMs = now + 75L;
                    } else {
                        int slotCount = screen.getScreenHandler().slots.size();
                        switch (this.scanStage) {
                            case 0:
                                if (slotCount <= 12) {
                                    this.nextActionAtMs = now + 75L;
                                    return;
                                }

                                this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 12, 0, SlotActionType.PICKUP, this.mc.player);
                                this.scanStage = 1;
                                this.nextActionAtMs = now + 224L;
                                return;
                            case 1:
                                if (slotCount <= 20) {
                                    this.nextActionAtMs = now + 75L;
                                    return;
                                }

                                this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 20, 0, SlotActionType.PICKUP, this.mc.player);
                                this.scanStage = 2;
                                this.resetStageProgress();
                                this.nextActionAtMs = now + 288L;
                                return;
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                                int[] slotsToAnalyze = this.getAnalyzeSlotsForStage(this.scanStage);
                                int[] transitionClicks = this.getTransitionClicksForStage(this.scanStage);
                                if (slotsToAnalyze.length == 0) {
                                    this.scanInProgress = false;
                                    this.resultReady = true;
                                    this.resultUntilMs = now + 18000L;
                                    return;
                                } else if (this.stageTransitioning) {
                                    if (this.transitionClickIndex < transitionClicks.length) {
                                        int clickSlot = transitionClicks[this.transitionClickIndex];
                                        if (clickSlot >= 0 && clickSlot < slotCount) {
                                            this.mc
                                                .interactionManager
                                                .clickSlot(screen.getScreenHandler().syncId, clickSlot, 0, SlotActionType.PICKUP, this.mc.player);
                                        }

                                        this.transitionClickIndex++;
                                        this.nextActionAtMs = now + 360L;
                                        return;
                                    } else if (this.scanStage == 6) {
                                        this.scanStage = 7;
                                        this.resetStageProgress();
                                        this.nextActionAtMs = now + 72L;
                                        return;
                                    } else {
                                        if (this.scanStage >= 14) {
                                            this.scanInProgress = false;
                                            this.resultReady = true;
                                            this.resultUntilMs = now + 18000L;
                                            return;
                                        }

                                        this.scanStage++;
                                        this.resetStageProgress();
                                        this.nextActionAtMs = now + 64L;
                                        return;
                                    }
                                } else if (this.stageSlotIndex >= slotsToAnalyze.length) {
                                    this.stageTransitioning = true;
                                    this.transitionClickIndex = 0;
                                    this.nextActionAtMs = now + 1L;
                                    return;
                                } else {
                                    int batchLimit = 8;
                                    int i = 0;

                                    for (; i < batchLimit; i++) {
                                        if (this.stageSlotIndex >= slotsToAnalyze.length) {
                                            this.stageTransitioning = true;
                                            this.transitionClickIndex = 0;
                                            this.nextActionAtMs = now + 1L;
                                            return;
                                        }

                                        int slotId = slotsToAnalyze[this.stageSlotIndex];
                                        if (slotId >= 0 && slotId < slotCount) {
                                            ItemStack stack = screen.getScreenHandler().getSlot(slotId).getStack();
                                            String anarchyId = this.extractAnarchyIdFromStack(stack);
                                            Integer online = this.extractOnlineMinFromStack(stack);
                                            if (online != null && online > 0) {
                                                this.storeOnlineForCurrentPass(this.scanStage, online, anarchyId);
                                                this.analyzeRetries = 0;
                                                this.stageSlotIndex++;
                                            } else {
                                                if (!stack.isEmpty() && this.analyzeRetries < 1) {
                                                    this.analyzeRetries++;
                                                    this.nextActionAtMs = now + 1L;
                                                    return;
                                                }

                                                this.analyzeRetries = 0;
                                                this.stageSlotIndex++;
                                            }
                                        } else {
                                            this.stageSlotIndex++;
                                            this.analyzeRetries = 0;
                                        }
                                    }

                                    this.nextActionAtMs = now + 1L;
                                    return;
                                }
                            case 7:
                                if (slotCount <= 14) {
                                    this.nextActionAtMs = now + 75L;
                                    return;
                                }

                                this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 14, 0, SlotActionType.PICKUP, this.mc.player);
                                this.scanStage = 8;
                                this.nextActionAtMs = now + 256L;
                                return;
                            case 8:
                                if (slotCount <= 20) {
                                    this.nextActionAtMs = now + 75L;
                                    return;
                                }

                                this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 20, 0, SlotActionType.PICKUP, this.mc.player);
                                this.scanStage = 9;
                                this.resetStageProgress();
                                this.nextActionAtMs = now + 288L;
                                return;
                        }
                    }
                }
            }
        }
    }

    private static int[] rangeSlots(int from, int toInclusive) {
        int size = Math.max(0, toInclusive - from + 1);
        int[] arr = new int[size];

        for (int i = 0; i < size; i++) {
            arr[i] = from + i;
        }

        return arr;
    }

    private void resetStageProgress() {
        this.stageSlotIndex = 0;
        this.stageTransitioning = false;
        this.transitionClickIndex = 0;
        this.analyzeRetries = 0;
    }

    private int[] getAnalyzeSlotsForStage(int stage) {
        return switch (stage) {
            case 2 -> FIRST_G0;
            case 3 -> FIRST_G1;
            case 4 -> FIRST_G2;
            case 5 -> FIRST_G3;
            case 6 -> FIRST_G4;
            default -> new int[0];
            case 9 -> SECOND_G0;
            case 10 -> SECOND_G1;
            case 11 -> SECOND_G2;
            case 12 -> SECOND_G3;
            case 13 -> SECOND_G4;
            case 14 -> SECOND_G5;
        };
    }

    private int[] getTransitionClicksForStage(int stage) {
        int[] nArray;
        switch (stage) {
            case 2:
                int[] nArray2 = new int[1];
                nArray = nArray2;
                nArray2[0] = 21;
                break;
            case 3:
                int[] nArray3 = new int[1];
                nArray = nArray3;
                nArray3[0] = 22;
                break;
            case 4:
                int[] nArray4 = new int[1];
                nArray = nArray4;
                nArray4[0] = 23;
                break;
            case 5:
                int[] nArray5 = new int[1];
                nArray = nArray5;
                nArray5[0] = 24;
                break;
            case 6:
            case 7:
            case 8:
            default:
                nArray = new int[0];
                break;
            case 9:
                int[] nArray6 = new int[1];
                nArray = nArray6;
                nArray6[0] = 21;
                break;
            case 10:
                int[] nArray7 = new int[1];
                nArray = nArray7;
                nArray7[0] = 53;
                break;
            case 11:
                int[] nArray8 = new int[1];
                nArray = nArray8;
                nArray8[0] = 13;
                break;
            case 12:
                int[] nArray9 = new int[1];
                nArray = nArray9;
                nArray9[0] = 23;
                break;
            case 13:
                int[] nArray10 = new int[1];
                nArray = nArray10;
                nArray10[0] = 24;
        }

        return nArray;
    }

    private boolean storeOnlineForCurrentPass(int stage, int online, String anarchyId) {
        if (stage >= 2 && stage <= 6) {
            if (online < this.firstMinOnline) {
                this.firstMinOnline = online;
                this.hasFirstOnline = true;
                this.firstAnarchyId = anarchyId;
                return true;
            } else {
                return false;
            }
        } else if (stage < 9 || stage > 14) {
            return false;
        } else if (online < this.secondMinOnline) {
            this.secondMinOnline = online;
            this.hasSecondOnline = true;
            this.secondAnarchyId = anarchyId;
            return true;
        } else {
            return false;
        }
    }

    private Integer extractOnlineMinFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (this.mc.player == null) {
            return null;
        }

        try {
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            Integer fromLoreLines;
            if (lore != null && (fromLoreLines = this.extractOnlineCandidateFromLines(lore.lines())) != null && fromLoreLines > 0) {
                return fromLoreLines;
            }
        } catch (Throwable var6) {
        }

        TooltipContext ctx = this.mc.world != null ? TooltipContext.create(this.mc.world) : TooltipContext.DEFAULT;
        List tooltipBasic = stack.getTooltip(ctx, this.mc.player, TooltipType.BASIC);
        List tooltipAdvanced = stack.getTooltip(ctx, this.mc.player, TooltipType.ADVANCED);
        Integer candidate = this.extractOnlineCandidateFromLines(tooltipBasic);
        if (candidate != null && candidate > 0) {
            return candidate;
        }

        candidate = this.extractOnlineCandidateFromLines(tooltipAdvanced);
        return candidate != null && candidate > 0 ? candidate : null;
    }

    private String extractAnarchyIdFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (this.mc.player == null) {
            return null;
        }

        try {
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            String fromLore;
            if (lore != null && (fromLore = this.extractAnarchyIdFromLines(lore.lines())) != null) {
                return fromLore;
            }
        } catch (Throwable var7) {
        }

        String fromName = this.extractAnarchyIdFromText(stack.getName().getString());
        if (fromName != null) {
            return fromName;
        }

        TooltipContext ctx = this.mc.world != null ? TooltipContext.create(this.mc.world) : TooltipContext.DEFAULT;
        List tooltipBasic = stack.getTooltip(ctx, this.mc.player, TooltipType.BASIC);
        String fromTooltip = this.extractAnarchyIdFromLines(tooltipBasic);
        if (fromTooltip != null) {
            return fromTooltip;
        }

        List tooltipAdvanced = stack.getTooltip(ctx, this.mc.player, TooltipType.ADVANCED);
        return this.extractAnarchyIdFromLines(tooltipAdvanced);
    }

    private String extractAnarchyIdFromLines(List<Text> lines) {
        if (lines != null && !lines.isEmpty()) {
            String joined = this.joinStrippedTooltip(lines);
            String fromJoined = this.extractAnarchyIdFromText(joined);
            if (fromJoined != null) {
                return fromJoined;
            }

            for (Text line : lines) {
                String id;
                if (line != null && (id = this.extractAnarchyIdFromText(Formatting.strip(line.getString()))) != null) {
                    return id;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private String extractAnarchyIdFromText(String text) {
        if (text != null && !text.isEmpty()) {
            String stripped = Formatting.strip(text);
            if (stripped != null && !stripped.isEmpty()) {
                Matcher matcher = ANARCHY_ID_PATTERN.matcher(stripped);
                String digits3;
                if (matcher.find() && !(digits3 = digitsOnly(matcher.group(1))).isEmpty()) {
                    return digits3;
                }

                Matcher dashMatcher = ANARCHY_DASH_ID_PATTERN.matcher(stripped);
                String digits2;
                if (dashMatcher.find() && !(digits2 = digitsOnly(dashMatcher.group(1))).isEmpty()) {
                    return digits2;
                }

                String digits;
                Matcher hashMatcher;
                return ANARCHY_CONTEXT_PATTERN.matcher(stripped).find()
                        && (hashMatcher = ANARCHY_HASH_FALLBACK_PATTERN.matcher(stripped)).find()
                        && !(digits = digitsOnly(hashMatcher.group(1))).isEmpty()
                    ? digits
                    : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static String digitsOnly(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    private Integer extractOnlineCandidateFromLines(List<Text> tooltipLines) {
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            for (Text line : tooltipLines) {
                String s;
                Integer candidate;
                String lowered;
                if (line != null
                    && (s = Formatting.strip(line.getString())) != null
                    && ((lowered = s.toLowerCase(Locale.ROOT)).contains("онлайн") || lowered.contains("режим"))
                    && (candidate = this.tryParseOnlineCandidateFromText(s)) != null
                    && candidate > 0) {
                    return candidate;
                }
            }

            for (Text line : tooltipLines) {
                String s;
                Integer candidate;
                if (line != null
                    && (s = Formatting.strip(line.getString())) != null
                    && s.contains("/")
                    && (candidate = this.tryParseOnlineCandidateFromText(s)) != null
                    && candidate > 0) {
                    return candidate;
                }
            }

            String joined = this.joinStrippedTooltip(tooltipLines);
            if (joined != null && !joined.isEmpty()) {
                Matcher pairMatcher = ONLINE_ANY_PAIR_PATTERN.matcher(joined);
                if (pairMatcher.find()) {
                    return this.trySelectCandidateFromPair(pairMatcher.group(1), pairMatcher.group(2));
                } else {
                    Matcher modeMatcher = ONLINE_MODE_JOIN_PATTERN.matcher(joined);
                    if (modeMatcher.find()) {
                        String leftRaw = modeMatcher.group(1);
                        String rightRaw = modeMatcher.group(2);
                        return this.trySelectCandidateFromPair(leftRaw, rightRaw);
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String joinStrippedTooltip(List<Text> tooltipLines) {
        if (tooltipLines != null && !tooltipLines.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for (Text line : tooltipLines) {
                String s;
                if (line != null && (s = Formatting.strip(line.getString())) != null && !s.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }

                    sb.append(s);
                }
            }

            return sb.toString();
        } else {
            return null;
        }
    }

    private Integer tryParseOnlineCandidateFromText(String strippedText) {
        if (strippedText != null && !strippedText.isEmpty()) {
            Matcher m = ONLINE_ANY_PAIR_PATTERN.matcher(strippedText);
            return !m.find() ? null : this.trySelectCandidateFromPair(m.group(1), m.group(2));
        } else {
            return null;
        }
    }

    private Integer trySelectCandidateFromPair(String leftRaw, String rightRaw) {
        if (leftRaw != null && rightRaw != null) {
            String leftDigits = leftRaw.replaceAll("[^0-9]", "");
            String rightDigits = rightRaw.replaceAll("[^0-9]", "");
            if (leftDigits.isEmpty() && rightDigits.isEmpty()) {
                return null;
            }

            try {
                int left = leftDigits.isEmpty() ? 0 : Integer.parseInt(leftDigits);
                int right = rightDigits.isEmpty() ? 0 : Integer.parseInt(rightDigits);
                if (left > 0) {
                    return left;
                } else {
                    return right > 0 ? right : null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void onDisable() {
        this.scanInProgress = false;
        this.scanStage = 0;
        this.stageSlotIndex = 0;
        this.stageTransitioning = false;
        this.transitionClickIndex = 0;
        this.analyzeRetries = 0;
        this.firstMinOnline = Integer.MAX_VALUE;
        this.hasFirstOnline = false;
        this.firstAnarchyId = null;
        this.secondMinOnline = Integer.MAX_VALUE;
        this.hasSecondOnline = false;
        this.secondAnarchyId = null;
        this.resultReady = false;
        this.resultUntilMs = 0L;
        this.nextActionAtMs = 0L;
    }
}
