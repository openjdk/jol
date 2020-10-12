package ljv;

final class Quote {

    private Quote(){

    }

    private static final String quotable = "\"<>{}|";

    private static final String canAppearUnquotedInLabelChars = " $&*@#!-+()^%;[],;.=";

    private static boolean canAppearUnquotedInLabel(char c) {
        return canAppearUnquotedInLabelChars.indexOf(c) != -1
                || Character.isLetter(c)
                || Character.isDigit(c)
                ;
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (quotable.indexOf(c) != -1)
                sb.append('\\').append(c);
            else if (canAppearUnquotedInLabel(c))
                sb.append(c);
            else
                sb.append("\\\\0u").append(Integer.toHexString(c));
        }
        return sb.toString();
    }
}
