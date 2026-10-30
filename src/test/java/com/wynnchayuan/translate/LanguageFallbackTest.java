package com.wynnchayuan.translate;

import java.nio.file.Path;
import java.util.List;

public final class LanguageFallbackTest {

    private static int failures = 0;

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    static String pickOnlyTw(TranslationStore tw, TranslationStore cn) {
        for (String key : new java.util.TreeSet<>(tw.sourceKeys())) {
            if (key.length() >= 20 && Character.isLetter(key.charAt(0)) && key.indexOf(' ') > 0
                    && key.indexOf('{') < 0 && key.indexOf('\n') < 0
                    && tw.lookup(key) != null && !cn.hasTranslation(key)) {
                return key;
            }
        }
        return null;
    }

    private static final String BOTH = "Combat Level";

    private static final String LONG = "Earth Main Attack Damage:";

    public static void main(String[] args) throws Exception {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve("zh_tw"));
        int alone = tw.size();

        Path cnDir = ROOT.resolve("zh_cn");
        TranslationStore cn = new TranslationStore();
        cn.loadAll(cnDir);
        String onlyTw = pickOnlyTw(tw, cn);
        if (onlyTw == null) {
            onlyTw = pickOnlyTw(tw, new TranslationStore());
            cnDir = com.wynnchayuan.CorpusGap.without(cnDir, onlyTw);
            cn = new TranslationStore();
            cn.loadAll(cnDir);
        }
        check("找得到一句繁體有、簡體沒翻的台詞（" + onlyTw + "）", onlyTw != null);
        String twLine = onlyTw == null ? null : tw.lookup(onlyTw);
        check("繁體本來就有那一句（" + alone + " 條）", twLine != null);
        check("簡體還沒翻那一句", onlyTw != null && cn.lookup(onlyTw) == null);

        check("★ 簡體預設不墊任何語言", Languages.underlay("", "zh_cn") == null);
        List<Path> cnLayers = new java.util.ArrayList<>();
        String under = Languages.underlay("", "zh_cn");
        if (under != null) {
            cnLayers.add(ROOT.resolve(under));
        }
        cnLayers.add(cnDir);
        TranslationStore cnGame = new TranslationStore();
        cnGame.loadAll(cnLayers);
        check("★ 簡體只載簡體那一層：繁體有、簡體沒翻的那一句留英文（拿到 "
                        + (onlyTw == null ? null : cnGame.lookup(onlyTw)) + "）",
                onlyTw != null && cnGame.lookup(onlyTw) == null);
        check("簡體的條目數跟單載簡體一樣（" + cnGame.size() + " / " + cn.size() + "）",
                cnGame.size() == cn.size());
        TranslationStore twGame = new TranslationStore();
        String twUnder = Languages.underlay("", "zh_tw");
        check("繁體預設也不墊", twUnder == null);
        twGame.loadAll(List.of(ROOT.resolve("zh_tw")));
        check("繁體不受影響（" + twGame.size() + " 條）",
                twGame.size() == alone && twLine != null && twLine.equals(twGame.lookup(onlyTw)));
        String twLabel = tw.lookup(BOTH);
        String cnLabel = cn.lookup(BOTH);
        String twLong = tw.lookupFlat(LONG);
        String cnLong = cn.lookupFlat(LONG);
        check("簡體翻好了介面標籤（" + cnLabel + "）", cnLabel != null);
        check("兩種語言的介面標籤翻得不一樣（" + twLabel + " / " + cnLabel + "）",
                twLabel != null && !twLabel.equals(cnLabel));
        check("兩種語言的長句翻得不一樣（" + twLong + " / " + cnLong + "）",
                twLong != null && cnLong != null && !twLong.equals(cnLong));

        TranslationStore both = new TranslationStore();
        both.loadAll(List.of(ROOT.resolve("zh_tw"), cnDir));

        check("★ 疊完之後，繁體那一層還在（先前這裡是 null）",
                twLine != null && twLine.equals(both.lookup(onlyTw)));
        check("★ 簡體蓋過繁體，不是反過來（拿到 "
                        + both.lookup(BOTH) + "）",
                cnLabel != null && cnLabel.equals(both.lookup(BOTH)));
        check("疊完的條目數應該接近繁體那一層（疊完 " + both.size()
                        + "、繁體 " + alone + "）",
                both.size() >= alone);

        TranslationStore flipped = new TranslationStore();
        flipped.loadAll(List.of(cnDir, ROOT.resolve("zh_tw")));
        check("順序反過來就換繁體勝出（拿到 " + flipped.lookup(BOTH) + "）",
                twLabel != null && twLabel.equals(flipped.lookup(BOTH)));

        check("★ 長句也要簡體勝出（flat 索引，拿到 "
                        + both.lookupFlat(LONG) + "）",
                cnLong != null && cnLong.equals(both.lookupFlat(LONG)));
        check("順序反過來時長句換繁體勝出（拿到 "
                        + flipped.lookupFlat(LONG) + "）",
                twLong != null && twLong.equals(flipped.lookupFlat(LONG)));
        check("長句在單層時本來就查得到（" + cnLong + "）", cnLong != null);

        TranslationStore one = new TranslationStore();
        one.loadAll(List.of(ROOT.resolve("zh_tw")));
        check("單層的結果跟舊的單參數版本一樣", one.size() == alone);

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "LanguageFallback: 全部通過"
                : "LanguageFallback: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
