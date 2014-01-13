package org.anarres.gradle.plugin.sablecc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some things don't have to be pretty.
 *
 * @author shevek
 */
public class SableCCPostProcessor {

    public static final String LEXER_NAME = "Lexer.java";
    public static final String PARSER_NAME = "Parser.java";
    public static final String DEPTH_FIRST_ADAPTER_NAME = "DepthFirstAdapter.java";
    public static final String ANALYSIS_ADAPTER_NAME = "AnalysisAdapter.java";
    public static final String NODE_NAME = "Node.java";

	private static List<String> readLines(File file) throws IOException {
		FileReader fr = new FileReader(file);
		try {
			BufferedReader br = new BufferedReader(fr);
			List<String> out = new ArrayList<String>();
			for (;;) {
				String line = br.readLine();
				if (line == null)
					break;
				out.add(line);
			}
			return out;
		} finally {
			fr.close();
		}
	}

	private static void writeLines(File file, List<? extends String> lines) throws IOException {
		StringBuilder buf = new StringBuilder();
		for (String line : lines)
			buf.append(line).append("\n");
		FileWriter fw = new FileWriter(file);
		fw.write(buf.toString());
		fw.close();
	}

	private static String getBaseName(String name) {
		name = name.substring(name.lastIndexOf(File.separator) + 1);
		int index = name.lastIndexOf('.');
		if (index > 0)
			name = name.substring(0, index);
		return name;
	}

	public static void processFile(File file) throws IOException {
		if (LEXER_NAME.equals(file.getName())) {
			processLexer(file);
		} else if (PARSER_NAME.equals(file.getName())) {
			processParser(file);
		} else if (DEPTH_FIRST_ADAPTER_NAME.equals(file.getName())) {
			processClone(file);
		} else if (NODE_NAME.equals(file.getName())) {
			processNode(file);
		} else if (ANALYSIS_ADAPTER_NAME.equals(file.getName())) {
			processAnalysisAdapter(file);
		}
	}

    private static void processLexer(File file) throws IOException {
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> lines = readLines(file);
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("package ")) {
                out.add(line);
            } else if (line.startsWith("import ")) {
                out.add(line);
            } else if (line.startsWith("public class Lexer")) {
                lines.set(i, "public class Lexer implements LexerInterface");
            }
        }
        writeLines(file, lines);

        out.addAll(Arrays.asList(
                "public interface LexerInterface {",
                "    public Token peek() throws LexerException, IOException;",
                "    public Token next() throws LexerException, IOException;",
                "}"));
        File ifile = new File(file.getParentFile(), "LexerInterface.java");
        writeLines(ifile, out);
    }

    private static void processParser(File file) throws IOException {
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            line = line.replace(" Lexer ", " LexerInterface ");
            lines.set(i, line);
        }
        writeLines(file, lines);
    }

    private static String getField(String line) {
        int start;
        for (start = 0; start < line.length(); start++)
            if (Character.isUpperCase(line.charAt(start)))
                break;
        int end;
        for (end = start; end < line.length(); end++)
            if (!Character.isJavaIdentifierPart(line.charAt(end)))
                break;
        return line.substring(start, end - 1);
    }
    private static final Pattern GETTER = Pattern.compile("\\s*public\\s+([\\w<>]+)\\s+get(\\w+)\\(\\)");
    private static final String[] IGNORE = {"Node", "NodeAccessor", "Switch", "Switchable", "Token"};  // Sorted

    private static void processCloneNode(File file, List<String> body) throws IOException {
        String name = getBaseName(file.getName());
        if (name.startsWith("P"))
            return;
        if (Arrays.binarySearch(IGNORE, name) >= 0)
            return;
        body.add("");
        body.add("\t@Override");
        body.add("\tpublic void case" + name + "(" + name + " node) {");

        if (name.startsWith("T")) {
            body.add("\t\tsetClone(node, (" + name + ") node.clone());");
        } else {

            Map<String, String> fields = new LinkedHashMap<String, String>();

            @SuppressWarnings({"rawtypes", "unchecked"})
            List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = GETTER.matcher(line);
                if (m.find()) {
                    String type = m.group(1);
                    type = type.replace("LinkedList<", "List<");
                    String field = m.group(2);
                    fields.put(field, type);
                    body.add("\t\t" + type + " _" + field + "_ = clone(node.get" + field + "());");
                }
            }

            StringBuilder buf = new StringBuilder("\t\tsetClone(node, new ");
            buf.append(name).append('(');
            boolean b = false;
            for (String field : fields.keySet()) {
                if (b)
                    buf.append(", ");
                else
                    b = true;
                buf.append("_").append(field).append("_");
            }
            buf.append("));");
            body.add(buf.toString());
        }

        body.add("\t}");
    }

    private static void processClone(File file) throws IOException {
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> lines = readLines(file);
        List<String> header = new ArrayList<String>();
        List<String> body = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trim = line.trim();
            if (trim.startsWith("package ")) {
                header.add(line);
                header.add("");
            } else if (trim.startsWith("import ")) {
                header.add(line);
            }
        }

        File dir = new File(file.getParent(), "../node");
        for (File node : dir.listFiles()) {
            processCloneNode(node, body);
        }

        List<String> out = new ArrayList<String>();
        out.addAll(header);
        out.addAll(Arrays.asList("",
                "public class CloneVisitor extends AnalysisAdapter {",
                "",
                "\tprivate Object clone;",
                "",
                "\tprivate <T extends Node> List<T> clone(List<T> in) {",
                "\t\tif (in == null)",
                "\t\t\treturn null;",
                "\t\tList<T> out = new ArrayList<T>(in.size());",
                "\t\tfor (T node : in)",
                "\t\t\tout.add(clone(node));",
                "\t\treturn out;",
                "\t}",
                "",
                "\t@SuppressWarnings({\"unchecked\"})",
                "\tpublic <T extends Node> T clone(T node) {",
                "\t\tif (node == null)",
                "\t\t\treturn null;",
                "\t\tnode.apply(this);",
                "\t\treturn (T) clone;",
                "\t}",
                "",
                "\tprivate <T extends Node> void setClone(T prev, T repl) {",
                "\t\tthis.clone = repl;",
                "\t\tfireClone(prev, repl);",
                "\t}",
                "",
                "\tprotected <T extends Node> void fireClone(T prev, T repl) {",
                "\t}",
                "",
                "\t@Override",
                "\tpublic void defaultCase(Node node) {",
                "\t\tthrow new IllegalStateException();",
                "\t}"));
        out.addAll(body);
        out.add("}");
        for (int i = 0; i < out.size(); i++)
            out.set(i, out.get(i).replace("\t", "    "));
        File cfile = new File(file.getParentFile(), "CloneVisitor.java");
        writeLines(cfile, out);

    }

    private static void processNode(File file) throws IOException {
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> lines = readLines(file);
        List<String> header = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trim = line.trim();
            if (trim.startsWith("package ")) {
                header.add(line);
            }
        }

        List<String> out = new ArrayList<String>(header);
        out.addAll(Arrays.asList("",
                "import javax.annotation.CheckForNull;",
                "import javax.annotation.Nonnull;",
                "",
                "public class NodeAccessor {",
                "",
                "\tprivate NodeAccessor() {",
                "\t}",
                "",
                "\tpublic static void setParent(@Nonnull Node node, @CheckForNull Node parent) {",
                "\t\tnode.parent(parent);",
                "\t}",
                "",
                "\tpublic static void removeChild(@Nonnull Node parent, @Nonnull Node child) {",
                "\t\tparent.removeChild(child);",
                "\t}",
                "}"));
        for (int i = 0; i < out.size(); i++)
            out.set(i, out.get(i).replace("\t", "    "));
        File cfile = new File(file.getParentFile(), "NodeAccessor.java");
        writeLines(cfile, out);
    }

    private static void processAnalysisAdapter(File file) throws IOException {
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> lines = readLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            line = line.replace("private Hashtable", "private Map");
            line = line.replace("new Hashtable", "new WeakHashMap");
            line = line.replace("public void case", "@Override\n    public void case");
            line = line.replace("public Object get", "@Override\n    public Object get");
            line = line.replace("public void set", "@Override\n    public void set");
            lines.set(i, line);
        }
        writeLines(file, lines);
    }
}
