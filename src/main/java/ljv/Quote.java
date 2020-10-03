package ljv;

public class Quote {

    private static final String quotable = "\"<>{}|";

    private static final String canAppearUnquotedInLabelChars = " $&*@#!-+()^%;[],;.=";

    private static boolean canAppearUnquotedInLabel(char c) {
        return canAppearUnquotedInLabelChars.indexOf(c) != -1
                || Character.isLetter(c)
                || Character.isDigit(c)
                ;
    }

    static String quote(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (quotable.indexOf(c) != -1)
                sb.append('\\').append(c);
            else if (canAppearUnquotedInLabel(c))
                sb.append(c);
            else
                sb.append("\\\\0u").append(Integer.toHexString((int) c));
        }
        return sb.toString();
    }
}
