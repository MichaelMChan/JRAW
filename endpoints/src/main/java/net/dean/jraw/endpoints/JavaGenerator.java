package net.dean.jraw.endpoints;

import net.dean.jraw.Endpoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class generates an enum called Endpoints.java.
 */
public class JavaGenerator extends AbstractEndpointGenerator {
    private static final String INDENT = "    ";
    private static final String COMMENT_WARNING =
            "/* This class is updated by running ./gradlew endpoints:update. Do not modify directly */";
    private static final String JAVADOC_CLASS =
            "/** This class is an automatically generated enumeration of Reddit's API endpoints */";
    private static final String JAVADOC_ENUM =
            "/** Represents the endpoint \"<a href=\"%s\">{@code %s}</a>\" in the \"%s\" category */";
    private static final String JAVADOC_GET_ENDPOINT = "Gets the Endpoint object associated with this enumeration";
    private static final String JAVADOC_GET_ENDPOINT_RETURN = "The Endpoint object";
    private static final Map<String, String> PREFIX_SUBSTITUTIONS;
    private static final Map<String, String> POSTFIX_SUBSTITUTIONS;

    /**
     * Instantiates a new JavaGenerator
     *
     * @param endpoints A map of endpoints where the key is the category and the value is a list of endpoints in that category
     */
    public JavaGenerator(NavigableMap<String, List<Endpoint>> endpoints) {
        super(endpoints, true);
    }


    @Override
    protected void _generate(File dest, BufferedWriter bw) throws IOException {
        write(bw, "package net.dean.jraw;");
        newLine(bw);
        write(bw, COMMENT_WARNING);
        write(bw, JAVADOC_CLASS);
        write(bw, "@SuppressWarnings(\"unused\")");
        write(bw, "public enum Endpoints {");

        NavigableMap<String, List<String>> duplicateUris = findDuplicateUris(endpoints);

        int catCounter = 0;
        for (Map.Entry<String, List<Endpoint>> entry : endpoints.entrySet()) {
            List<String> duplicates = duplicateUris.get(entry.getKey());

            bw.write('\n');
            write(bw, 1, "///////// " + entry.getKey() + " /////////");

            int endpointCounter = 0;
            for (Endpoint endpoint : entry.getValue()) {
                write(bw, 1, String.format(JAVADOC_ENUM, getRedditDocUrl(endpoint), endpoint.getRequestDescriptor(), endpoint.getCategory().replace("&", "&amp;")));
                write(bw, 1, String.format("%s(\"%s\")%s", generateEnumName(endpoint, duplicates.contains(endpoint.getUri())), endpoint.getRequestDescriptor(),
                        catCounter == endpoints.size() - 1 && endpointCounter == entry.getValue().size() - 1 ? ";" : ","));

                endpointCounter++;
            }

            catCounter++;
        }

        newLine(bw);
        write(bw, 1, "private final net.dean.jraw.Endpoint endpoint;");
        newLine(bw);
        write(bw, 1, "private Endpoints(String requestDescriptor) {");
        write(bw, 2, "this.endpoint = new Endpoint(requestDescriptor);");
        write(bw, 1, "}");
        newLine(bw);
        write(bw, 1, "/**");
        write(bw, 1, "  * " + JAVADOC_GET_ENDPOINT);
        write(bw, 1, "  * @return " + JAVADOC_GET_ENDPOINT_RETURN);
        write(bw, 1, "  */");
        write(bw, 1, "public final net.dean.jraw.Endpoint getEndpoint() {");
        write(bw, 2, "return endpoint;");
        write(bw, 1, "}");

        newLine(bw);

        write(bw, 1, "@Override");
        write(bw, 1, "public java.lang.String toString() {");
        write(bw, 2, "return endpoint.toString();");
        write(bw, 1, "}");
        write(bw, "}");
    }

    private String generateEnumName(Endpoint ep, boolean isDuplicate) {
        String enumName = ep.getUri();

        // Replace prefixes
        for (Map.Entry<String, String> substitutionEntry : PREFIX_SUBSTITUTIONS.entrySet()) {
            String key = substitutionEntry.getKey();
            String val = substitutionEntry.getValue();
            if (enumName.startsWith(key)) {
                enumName = enumName.replaceFirst(key, val);
            }
        }

        // Replace postfixes
        // TODO: Could be done more accurately with substring()
        for (Map.Entry<String, String> substitutionEntry : POSTFIX_SUBSTITUTIONS.entrySet()) {
            String key = substitutionEntry.getKey();
            String val = substitutionEntry.getValue();
            if (enumName.endsWith(key)) {
                enumName = enumName.replace(key, val);
            }
        }

        enumName = enumName.toUpperCase()
                .replace("/", "_")
                .replace("{", "")
                .replace("}", "");

        if (isDuplicate) {
            enumName += "_" + ep.getVerb();
        }

        return enumName;
    }

    private NavigableMap<String, List<String>> findDuplicateUris(NavigableMap<String, List<Endpoint>> endpoints) {
        TreeMap<String, List<String>> dupes = new TreeMap<>();
        for (Map.Entry<String, List<Endpoint>> entry : endpoints.entrySet()) {

            List<String> uris = entry.getValue().stream().map(Endpoint::getUri).collect(Collectors.toList());
            dupes.put(entry.getKey(), findDuplicates(uris));
        }

        return dupes;
    }

    private <T> List<T> findDuplicates(List<T> list) {
        Set<T> duplicates = new LinkedHashSet<>();
        Set<T> uniques = new HashSet<>();

        for (T obj : list) {
            if (!uniques.add(obj)) {
                duplicates.add(obj);
            }
        }

        List<T> returnList = new ArrayList<>(duplicates.size());

        returnList.addAll(duplicates.stream().collect(Collectors.toList()));

        return returnList;
    }

    private void write(BufferedWriter bw, int indents, String msg) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indents; i++) {
            sb.append(INDENT);
        }
        sb.append(msg);
        sb.append('\n');

        bw.write(sb.toString());
    }

    private void write(BufferedWriter bw, String msg) throws IOException {
        write(bw, 0, msg);
    }

    private void newLine(BufferedWriter bw) throws IOException {
        bw.write('\n');
    }

    static {
        PREFIX_SUBSTITUTIONS = new LinkedHashMap<>();
        // Put in order of priority
        PREFIX_SUBSTITUTIONS.put("/api/v1", "OAUTH");
        PREFIX_SUBSTITUTIONS.put("/api/", "");
        PREFIX_SUBSTITUTIONS.put("/r/", "");
        PREFIX_SUBSTITUTIONS.put("/", "");

        POSTFIX_SUBSTITUTIONS = new LinkedHashMap<>();
        POSTFIX_SUBSTITUTIONS.put(".json", "");
    }
}
