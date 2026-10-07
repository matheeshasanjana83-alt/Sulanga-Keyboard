// SPDX-License-Identifier: GPL-3.0-only
// Sulanga Keyboard - Singlish (Sinhala phonetic) transliteration engine.
package helium314.keyboard.event;

/**
 * Converts a latin "Singlish" string into Sinhala Unicode.
 * Stateless: the whole composing buffer is converted on every keystroke
 * (longest match first), so backspace and corrections stay consistent.
 *
 * Scheme summary
 *   vowels   a aa A(ae) AA(aae) i ii(I) u uu(U) e ee(E) o oo(O) ai au R(ru-sign)
 *   cons.    k K(kh) g G(gh) ch Ch(chh) j J(jh) ny t T d D th Th dh Dh n N p P(ph)
 *            b B(bh) m y r l L w v sh Sh s h f
 *            prenasal: zg=ඟ zj=ඦ zd=ඬ zdh=ඳ zb=ඹ ; X=ඞ ; kn=ඥ
 *   signs    x=ං (anusvara)  H=ඃ (visarga)
 *   rules    consonant without vowel -> hal (්)
 *            consonant + r + vowel   -> rakaransaya  (ක්‍ර)
 *            consonant + y + vowel   -> yansaya      (ක්‍ය)
 *            "\" in front of a letter -> that latin letter is kept as is
 */
public final class SinglishEngine {
    private static final char HAL = '\u0DCA';
    private static final char ZWJ = '\u200D';

    // consonants, longest keys first
    private static final String[][] CONS = {
            {"zdh", "ඳ"},
            {"chh", "ඡ"},
            {"Ch", "ඡ"}, {"ch", "ච"},
            {"kh", "ඛ"}, {"gh", "ඝ"}, {"jh", "ඣ"},
            {"Th", "ථ"}, {"th", "ත"}, {"Dh", "ධ"}, {"dh", "ද"},
            {"ph", "ඵ"}, {"bh", "භ"},
            {"Sh", "ෂ"}, {"sh", "ශ"},
            {"ny", "ඤ"}, {"kn", "ඥ"},
            {"zg", "ඟ"}, {"zj", "ඦ"}, {"zd", "ඬ"}, {"zb", "ඹ"},
            {"k", "ක"}, {"K", "ඛ"}, {"g", "ග"}, {"G", "ඝ"},
            {"j", "ජ"}, {"J", "ඣ"},
            {"t", "ට"}, {"T", "ඨ"}, {"d", "ඩ"}, {"D", "ඪ"},
            {"n", "න"}, {"N", "ණ"},
            {"p", "ප"}, {"P", "ඵ"}, {"b", "බ"}, {"B", "භ"}, {"m", "ම"}, {"M", "ම"},
            {"y", "ය"}, {"Y", "ය"}, {"r", "ර"}, {"l", "ල"}, {"L", "ළ"},
            {"w", "ව"}, {"v", "ව"}, {"W", "ව"}, {"V", "ව"},
            {"s", "ස"}, {"S", "ෂ"}, {"h", "හ"}, {"f", "ෆ"}, {"F", "ෆ"},
            {"X", "ඞ"},
            {"c", "ච"}, {"C", "ඡ"}, {"q", "ක"}, {"Q", "ක"}, {"z", "ස"}, {"Z", "ස"}
    };

    // vowels: {latin, independent, dependent sign ("" = inherent a)}
    private static final String[][] VOW = {
            {"aae", "ඈ", "ෑ"}, {"aee", "ඈ", "ෑ"},
            {"AA", "ඈ", "ෑ"}, {"Aa", "ඈ", "ෑ"},
            {"aa", "ආ", "ා"}, {"ae", "ඇ", "ැ"}, {"ai", "ඓ", "ෛ"}, {"au", "ඖ", "ෞ"},
            {"ii", "ඊ", "ී"}, {"uu", "ඌ", "ූ"}, {"ee", "ඒ", "ේ"}, {"oo", "ඕ", "ෝ"},
            {"RR", "ඎ", "ෲ"},
            {"A", "ඇ", "ැ"}, {"a", "අ", ""},
            {"I", "ඊ", "ී"}, {"i", "ඉ", "ි"},
            {"U", "ඌ", "ූ"}, {"u", "උ", "ු"},
            {"E", "ඒ", "ේ"}, {"e", "එ", "ෙ"},
            {"O", "ඕ", "ෝ"}, {"o", "ඔ", "ො"},
            {"R", "ඍ", "ෘ"}
    };

    private SinglishEngine() {}

    private static String[] matchCons(String s, int i) {
        for (String[] c : CONS) if (s.startsWith(c[0], i)) return c;
        return null;
    }

    private static String[] matchVow(String s, int i) {
        for (String[] v : VOW) if (s.startsWith(v[0], i)) return v;
        return null;
    }

    public static String convert(String in) {
        if (in == null || in.isEmpty()) return "";
        StringBuilder out = new StringBuilder(in.length() * 2);
        int i = 0, n = in.length();
        while (i < n) {
            char ch = in.charAt(i);
            if (ch == '\\' && i + 1 < n) { // escape: keep next char latin
                out.append(in.charAt(i + 1));
                i += 2;
                continue;
            }
            if (ch == 'x') { out.append('ං'); i++; continue; }
            if (ch == 'H') { out.append('ඃ'); i++; continue; }

            String[] c = matchCons(in, i);
            if (c != null) {
                i += c[0].length();
                out.append(c[1]);
                // rakaransaya / yansaya: C + r|y + vowel
                if (i < n && (in.charAt(i) == 'r' || in.charAt(i) == 'y')) {
                    char ry = in.charAt(i);
                    String[] vAfter = matchVow(in, i + 1);
                    boolean nyCase = ry == 'y' && c[0].equals("n"); // "ny" already taken above
                    if (vAfter != null && !nyCase) {
                        out.append(HAL).append(ZWJ).append(ry == 'r' ? 'ර' : 'ය');
                        i += 1;
                    }
                }
                String[] v = matchVow(in, i);
                if (v != null) {
                    out.append(v[2]);
                    i += v[0].length();
                } else {
                    out.append(HAL);
                }
                continue;
            }
            String[] v = matchVow(in, i);
            if (v != null) {
                out.append(v[1]);
                i += v[0].length();
                continue;
            }
            out.append(ch); // digits, punctuation, anything else
            i++;
        }
        return out.toString();
    }
}
