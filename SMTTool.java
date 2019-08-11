package unletter;

import static java.util.stream.Collectors.joining;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class SMTTool {

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            String smtFormula = null;
            boolean shouldStop = false;
            while (!shouldStop) {
                String str = sc.nextLine();

                switch (str) {
                case "unlet":
                    smtFormula = unlet(smtFormula);
                    break;
                case "unand":
                    smtFormula = collapseAnd(smtFormula);
                    break;
                case "readFormula":
                    smtFormula = sc.nextLine();
                    break;
                case "writeFormula":
                    System.out.println(format(smtFormula));
                    break;
                case "all":
                    smtFormula = sc.nextLine();
                    smtFormula = unlet(smtFormula);
                    smtFormula = collapseAnd(smtFormula);
                    System.out.println(format(smtFormula));
                    break;
                case "exit":
                    shouldStop = true;
                    break;
                default:
                    System.out.println("no valid input: " + str);
                }
            }
        }
    }

    static String format(String str) {
        int openBrackets = 1;
        int index = 1;
        StringBuilder output = new StringBuilder("(");
        boolean openUnary = false;
        while (openBrackets != 0) {
            char c = str.charAt(index);
            boolean isUnary = checkIsUnary(str, index);
            openUnary = openUnary || isUnary;
            if (c == '(' && !isUnary) {
                indent(openBrackets, output);
                openBrackets++;
            } else if (c == ')') {
                if (!openUnary) {
                    openBrackets--;
                } else {
                    openUnary = false;
                }
                if (str.charAt(index - 1) == ')') {
                    indent(openBrackets, output);
                }
            }
            output.append(c);
            index++;
        }
        return output.toString();
    }

    private static void indent(int openBrackets, StringBuilder output) {
        output.append("\n");
        for (int i = 0; i < openBrackets; i++) {
            output.append("  ");
        }
    }

    static String all(String formula) {
        return format(collapseAnd(unlet(formula)));
    }

    static boolean checkIsUnary(String str, int index) {
        if (str.charAt(index) == '(') {
            boolean spaceFound = false;
            while (true) {
                char c = str.charAt(++index);
                if (c == ')') {
                    return true;
                } else if (c == '(' || (c == ' ' && spaceFound)) {
                    return false;
                } else if (c == ' ') {
                    spaceFound = true;
                }
            }
        }
        return false;
    }

    static String collapseAnd(String string) {
        while (true) {
            int startIndex;
            if ((startIndex = hasRecursiveAnd(string)) >= 0) {
                int index = startIndex;
                char[] str = string.toCharArray();
                List<String> conjuncts = getConjunctsRecursively(string.substring(startIndex));
                int openBrackets = 1;
                while (openBrackets > 0) {
                    char c = str[++index];
                    if (c == '(') {
                        openBrackets++;
                    } else if (c == ')') {
                        openBrackets--;
                    }
                }
                String prefix = string.substring(0, startIndex + 5); // including first and and whitespace
                String suffix = string.substring(index); // exclude last bracket as we remove the and

                string = prefix + conjuncts.stream().collect(joining(" ")) + suffix;
            } else {
                break;
            }
        }

        return string;
    }

    static int hasRecursiveAnd(String str) {
        int firstIndex = str.indexOf("(and");
        if (firstIndex == -1) {
            return -1;
        } else {
            char[] arr = str.toCharArray();
            while(true) {
                int index = firstIndex + 5; // includes offset for "(and "
    
                while (true) {
                    if (str.startsWith("(and", index)) {
                        return firstIndex;
                    }
                    int openBrackets = 1;
                    index ++; //includes offset for "("
                    while (openBrackets != 0) {
                        char c = arr[index++];
        
                        if (c == '(') {
                            openBrackets++;
                        } else if (c == ')') {
                            openBrackets--;
                        }
                    }
                    if (arr[index++] == ')') {
                        break;
                    }
                }
                
                firstIndex = str.indexOf("(and", firstIndex+4);
                if (firstIndex == -1) {
                    return -1;
                }
            }
        }
    }

    static List<String> getConjunctsRecursively(String string) {
        int index = 6; // offset for "(and "
        char[] str = string.toCharArray();
        List<String> conjuncts = new ArrayList<>();
        while (true) {
            StringBuilder conjunct = new StringBuilder("(");
            int openBrackets = 1;
            while (openBrackets != 0) {
                char c = str[index];
                conjunct.append(c);

                if (c == '(') {
                    openBrackets++;
                } else if (c == ')') {
                    openBrackets--;
                }
                index++;
            }
            conjuncts.add(conjunct.toString());

            if (str[index] == ')') {
                break;
            } else {
                index += 2;
            }
        }

        Iterator<String> it = conjuncts.iterator();
        List<String> allConjuncts = new ArrayList<>();
        while (it.hasNext()) {
            String b = it.next();
            if (b.startsWith("(and ")) {
                it.remove();
                allConjuncts.addAll(getConjunctsRecursively(b));
            }
        }
        allConjuncts.addAll(conjuncts);
        return allConjuncts;
    }

    static String unlet(String str) {
        while (str.contains("((.def_")) {
            Pair<String, String> defReplace = getNextDef(str);
            // remove definition itself
            str = str.replace("(let ((" + defReplace.first + " " + defReplace.second + "))", "");
            str = str.substring(0, str.length() - 1);

            // replace name with def
            str = str.replace(defReplace.first, defReplace.second);
        }
        return str;
    }

    static Pair<String, String> getNextDef(String string) {
        int index = string.indexOf("((.def_") + 7;
        String name = ".def_";
        char[] str = string.toCharArray();
        while (str[index] != ' ') {
            name += str[index++];
        }
        // step over the bracket
        index += 2;
        StringBuilder def = new StringBuilder("(");
        int openBrackets = 1;
        while (openBrackets != 0) {
            char c = str[index];
            def.append(c);

            if (c == '(') {
                openBrackets++;
            } else if (c == ')') {
                openBrackets--;
            }
            index++;
        }
        return Pair.of(name, def.toString());
    }

    static class Pair<A, B> implements Serializable {

        private static final long serialVersionUID = -8410959888808077296L;

        private final A first;
        private final B second;

        private Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>(first, second);
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        /**
         * Get the first parameter, crash if it is null.
         */
        public A getFirstNotNull() {
            return first;
        }

        /**
         * Get the second parameter, crash if it is null.
         */
        public B getSecondNotNull() {
            return second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof Pair<?, ?>) && Objects.equals(first, ((Pair<?, ?>) other).first)
                    && Objects.equals(second, ((Pair<?, ?>) other).second);
        }

        @Override
        public int hashCode() {
            if (first == null) {
                return (second == null) ? 0 : second.hashCode() + 1;
            } else if (second == null) {
                return first.hashCode() + 2;
            } else {
                return first.hashCode() * 17 + second.hashCode();
            }
        }
    }
}
