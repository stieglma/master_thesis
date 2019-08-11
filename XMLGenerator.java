package xmlGen;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XMLGenerator {

    private static Set<String> fileSets = new HashSet<>();

    private static String base1;
    private static String base2;
    private static String invBase;
    private static String outputNameBase;
    private static String inputDir;

    public static void main(String... args) throws IOException {
        base1 = args[0];
        base2 = args[1];
        invBase = args[2];
        outputNameBase = args[3];
        inputDir = args[4];

        fileSets.add("Loops_true");
        fileSets.add("Loops_false");
        fileSets.add("Other_true");
        fileSets.add("Other_false");
        fileSets.add("x64_true");
        fileSets.add("x64_false");

        makeXmlFile("all");

        fileSets.clear();
        fileSets.add("Loops_true");
        fileSets.add("Loops_false");
        makeXmlFile("loops");

        fileSets.clear();
        fileSets.add("Loops_true");
        makeXmlFile("loops_true");

        fileSets.clear();
        fileSets.add("Loops_false");
        makeXmlFile("loops_false");

        fileSets.clear();
        fileSets.add("Other_true");
        fileSets.add("Other_false");
        makeXmlFile("other");

        fileSets.clear();
        fileSets.add("Other_true");
        makeXmlFile("other_true");

        fileSets.clear();
        fileSets.add("Other_false");
        makeXmlFile("other_false");

        fileSets.clear();
        fileSets.add("x64_true");
        fileSets.add("x64_false");
        makeXmlFile("x64");

        fileSets.clear();
        fileSets.add("x64_true");
        makeXmlFile("x64_true");

        fileSets.clear();
        fileSets.add("x64_false");
        makeXmlFile("x64_false");
    }

    private static void makeXmlFile(String finalName) throws IOException {
        StringBuilder xmlBuilder = new StringBuilder();
        writeHeader(xmlBuilder);

        xmlBuilder.append("  <!-- baseline results -->\n");
        writeContent(xmlBuilder, computeBaseline(base1));
        writeContent(xmlBuilder, computeBaseline(base2));

        xmlBuilder.append("\n  <!-- inv results -->\n");
        for (List<String> parts : computeInvUnion()) {
            writeContent(xmlBuilder, parts);
        }
        writeFooter(xmlBuilder);

        try (FileWriter w = new FileWriter(inputDir + "/" + outputNameBase + finalName + ".xml")) {
            w.write(xmlBuilder.toString());
        }
    }

    private static List<List<String>> computeInvUnion() {
        List<List<String>> unions = new ArrayList<>();
        for (String fileSet : fileSets) {
            List<String> tmp = new ArrayList<>();
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(inputDir),
                    invBase + "*" + fileSet + ".xml.bz2")) {
                dirStream.forEach(f -> tmp.add(f.getFileName().toString()));
            } catch (IOException e) {
                throw new RuntimeException();
            }
            Collections.sort(tmp);

            int counter = 0;
            boolean createUnions = unions.size() == 0;
            for (String file : tmp) {
                // if we haven't added something we need to create the lists on
                // the fly
                if (createUnions) {
                    unions.add(new ArrayList<String>());
                    unions.get(counter++).add(file);
                } else {
                    unions.get(counter++).add(file);
                }
            }
        }
        return unions;
    }

    private static List<String> computeBaseline(String baseName) {
        List<String> union = new ArrayList<>();
        for (String fileSet : fileSets) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(inputDir),
                    baseName + fileSet + ".xml.bz2")) {
                dirStream.forEach(f -> union.add(f.getFileName().toString()));
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        return union;
    }

    private static void writeHeader(StringBuilder builder) {
        builder.append("<?xml version=\"1.0\" ?>\n")
                .append("<!DOCTYPE table PUBLIC \"+//IDN sosy-lab.org//DTD BenchExec table 1.6//EN\" \"http://www.sosy-lab.org/benchexec/table-1.6.dtd\">\n")
                .append("<table>\n\n")
                .append("  <!-- <column> can be used here to override the global set of columns.  -->\n")
                .append("  <column title=\"status\"/>\n").append("  <column title=\"cputime\"/>\n")
                .append("  <column title=\"memUsage\" displayUnit=\"MB\" scaleFactor=\"0.000001\"/>\n\n");
    }

    private static void writeContent(StringBuilder builder, List<String> parts) {
        builder.append("  <union>\n");
        for (String part : parts) {
            builder.append("    <result filename=\"" + part + "\"></result>\n");
        }
        builder.append("  </union>\n");
    }

    private static void writeFooter(StringBuilder builder) {
        builder.append("</table>");
    }
}
