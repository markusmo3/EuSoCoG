package de.nxg.eusocog;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Class generates the source for all Euler Problems available at
 * <a href="http://www.projecteuler.net">http://www.projecteuler.net</a>
 *
 * @see #generate(problemNo)
 * @see #generateAll()
 */
public final class EulerSourceGenerator {

    // #########################################################################
    // ########################## STATIC FINAL FIELDS ##########################
    // #########################################################################

    private static final String HEADING_FORMAT = "<a href=\"https://projecteuler.net/problem=%1$d\"><b>Problem %1$d</b></a></br>";

    /**
     * Need to use https because the webpage sends a "moved temporarily" response when
     * using http only
     */
    private static final String URL_PREFIX = "https://projecteuler.net/problem=";

    /* Search strings to identify website with a valid Problem on it */
    private static final String SEARCH_BEGIN = "<div class=\"problem_content\" role=\"problem\">";
    private static final String DIV_REGEX = "(?<begin><div[^<>]*>)|(?<end></div[^<>]*>)";

    /*
     * Replace internal website links with absolute links, that way all the images get
     * correctly presented inside the IDE
     */
    private static final String IMAGE_REGEX = "<img src=(?<qm>\\\"|')?project\\/images\\/(?<file>[^\\\">]+)(?:\\\\k<qm>)?[^>]+>";
    private static final String IMAGE_REGEX_REPLACEMENT = "<img src=https://projecteuler.net/project/images/${file}>";

    private static final String PACKAGE_FORMAT = "%1$s%2$03d_%3$03d";

    private static final String CLASS_NAME_FORMAT = "%1$s%2$03d";

    private static final String CLASS_FORMAT = ""
            + "%1$s\n"
            + "\n"
            + "import de.nxg.eusocog.*;\n"
            + "import %4$s.EulerConfig;\n"
            + "\n"
            + "%2$s\n"
            + "public class %3$s extends EulerProblem {\n"
            + "\n"
            + "    @Override\n"
            + "    public Object solve() {\n"
            + "        return null;\n"
            + "    }\n"
            + "\n"
            + "    public static void main(String[] args) {\n"
            + "        start(%3$s.class, EulerConfig.getInstance());\n"
            + "    }\n"
            + "\n"
            + "}\n";

    private static final String CONFIG_CLASS_FORMAT = ""
            + "%1$s\n"
            + "\n"
            + "import java.util.concurrent.*;\n"
            + "import de.nxg.eusocog.*;\n"
            + "\n"
            + "public class EulerConfig implements IEulerConfig {\n"
            + "\n"
            + "    private static final EulerConfig instance = new EulerConfig();\n"
            + "\n"
            + "    private EulerConfig() {};\n"
            + "\n"
            + "    public static EulerConfig getInstance() {\n"
            + "        return instance;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public boolean shouldCopyToClipboard() {\n"
            + "        // TODO Should copy to Clipboard ? Currently false\n"
            + "        return false;\n"
            + "    }\n"
            + "\n"
            + "    @Override\n"
            + "    public TimeUnit getFinishTimeUnit() {\n"
            + "        // TODO finishTimeUnit ? Currently milliseconds (ms)\n"
            + "        return TimeUnit.MILLISECONDS;\n"
            + "    }\n"
            + "\n"
            + "}\n";

    /** the amount of Problems to be pulled in parallel */
    private static final int GENERATE_ALL_BATCH_SIZE = 50;
    /** the amount of thread that should pull problems */
    private static final int THREAD_COUNT = 4;
    /** max amount of classes per subpackage */
    private static final int SUBPACKAGE_SIZE = 50;

    // #########################################################################
    // ############################# CLASS FIELDS ##############################
    // #########################################################################

    private Path sourceDestinationFolder;
    private String problemClassPrefix;
    private String sourcePackage;
    private String subPackagePrefix;

    /**
     * Creates a new SourceGenerator Object
     *
     * @param sourceDestinationFolder
     *            the path where the generated sources should be saved in
     * @param sourcePackage
     *            The package to be used by the generated classes (e.g.
     *            "de.nxg.eulersolutions")
     * @param generatePackageStructure
     *            If the generator should also create the directories associated with the
     *            generated package structure.
     */
    public EulerSourceGenerator(Path sourceDestinationFolder, String sourcePackage,
            boolean generatePackageStructure) {
        this(sourceDestinationFolder, "Euler", sourcePackage, generatePackageStructure, "x");
    }

    /**
     * Creates a new SourceGenerator Object
     *
     * @param sourceDestinationFolder
     *            the path where the generated sources should be saved in
     * @param problemClassPrefix
     *            The prefix for the generated classes (e.g. if prefix = "Euler",
     *            Euler001.java, Euler002.java, ...)
     * @param sourcePackage
     *            The package to be used by the generated classes (e.g.
     *            "de.nxg.eulersolutions")
     * @param generatePackageStructure
     *            If the generator should also create the directories associated with the
     *            generated package structure.
     */
    public EulerSourceGenerator(Path sourceDestinationFolder, String problemClassPrefix,
            String sourcePackage, boolean generatePackageStructure) {
        this(sourceDestinationFolder, problemClassPrefix, sourcePackage, generatePackageStructure, "x");
    }

    /**
     * Creates a new SourceGenerator Object
     *
     * @param sourceDestinationFolder
     *            the path where the generated sources should be saved in
     * @param problemClassPrefix
     *            The prefix for the generated classes (e.g. if prefix = "Euler",
     *            Euler001.java, Euler002.java, ...)
     * @param sourcePackage
     *            The package to be used by the generated classes (e.g.
     *            "de.nxg.eulersolutions")
     * @param generatePackageStructure
     *            If the generator should also create the directories associated with the
     *            generated package structure.
     * @param subPackagePrefix
     *            Prefix to be used for subpackge declaration  (e.g &ltprefix>000-049, &ltprefix>050-099, ...)
     */
    public EulerSourceGenerator(Path sourceDestinationFolder, String problemClassPrefix, String sourcePackage,
            boolean generatePackageStructure, String subPackagePrefix) {
        this.sourceDestinationFolder = sourceDestinationFolder;
        this.problemClassPrefix = problemClassPrefix;
        this.sourcePackage = sourcePackage;
        if (generatePackageStructure) {
            Path newPackagePath = sourceDestinationFolder.resolve(sourcePackage.replaceAll("\\.", "/"));
            this.sourceDestinationFolder = newPackagePath;
        }
        this.subPackagePrefix = subPackagePrefix;
    }

    // #########################################################################
    // ############################## PUBLIC API ###############################
    // #########################################################################

    /**
     * Generates a single Java File (.java) for the problemNo. Also generates a
     * EulerConfig.java File if it doesnt allready exist.
     *
     * Internally calls {@link #generate(problemNo, overwriteExisting)} with
     * overwriteExisting = false
     *
     * @param problemNo
     *            the distinct number for one problem as found on the webpage.
     * @return {@link SourceGenResult} additional information about the conversion. The
     *         {@link #generateAll()} method uses it.
     */
    public SourceGenResult generate(int problemNo) {
        generateEulerConfig();
        return _generate(problemNo, false);
    }

    /**
     * Generates a single Java File (.java) for the problemNo. Also generates a
     * EulerConfig.java File if it doesnt allready exist.
     *
     * @param problemNo
     *            the distinct number for one problem as found on the webpage.
     * @param overwriteExisting
     *            if the Java File for the problem should be overwritten when allready
     *            existant
     * @return {@link SourceGenResult} additional information about the conversion. The
     *         {@link #generateAll()} method uses it.
     */
    public SourceGenResult generate(int problemNo, boolean overwriteExisting) {
        generateEulerConfig();
        return _generate(problemNo, overwriteExisting);
    }

    /**
     * Generates source filse for all problems found on the webpage. Uses Multithreading
     * to speed up the Process
     */
    public void generateAll() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        int counter = 0;
        boolean continueGenerating = true;
        List<SourceGenCallable> callableList = new ArrayList<>(GENERATE_ALL_BATCH_SIZE);

        generateEulerConfig();
        while (continueGenerating) {
            callableList.clear();
            int lastGenerated = 0;
            int from = counter * GENERATE_ALL_BATCH_SIZE + 1;
            int to = (counter + 1) * GENERATE_ALL_BATCH_SIZE;
            for (int i = from; i <= to; i++) {
                callableList.add(new SourceGenCallable(this, i));
            }

            try {
                List<Future<SourceGenResult>> invokeAll = executorService.invokeAll(callableList);
                while (invokeAll.size() != 0) {
                    Iterator<Future<SourceGenResult>> iterator = invokeAll.iterator();
                    while (iterator.hasNext()) {
                        Future<SourceGenResult> next = iterator.next();
                        if (!next.isDone()) {
                            continue;
                        } else {
                            iterator.remove();
                            SourceGenResult nextResult = next.get();
                            if (nextResult != null) {
                                if (nextResult.continueGen) {
                                    lastGenerated = Math.max(lastGenerated, nextResult.problemNo);
                                } else {
                                    continueGenerating = false;
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            System.out.println("Generated " + from + " to " + lastGenerated);
            counter++;
        }
        executorService.shutdown();
        System.out.println("Finished!");
    }

    // #########################################################################
    // ############################## PRIVATE API ##############################
    // #########################################################################

    private SourceGenResult _generate(int problemNo, boolean overwriteExisting) {
        StringBuffer sb = getWebpageAsString(URL_PREFIX + problemNo);

        if (sb.toString().trim().isEmpty()) {
            System.out.println("Webpage for problem " + problemNo + " is empty!");
        } else if (sb.indexOf("problems_table_page") != -1) {
            System.out.println("Problem " + problemNo + " is not accessible!");
        } else {
            int subPackageIndex = problemNo / SUBPACKAGE_SIZE;
            String subPackage = String.format(PACKAGE_FORMAT, subPackagePrefix,
                    subPackageIndex * SUBPACKAGE_SIZE,
                    (subPackageIndex + 1) * SUBPACKAGE_SIZE - 1);
            String packageString = "";
            if (sourcePackage != null) {
                packageString = "package " + sourcePackage + "." + subPackage + ";";
            }

            String javaDoc = toJavaDoc(problemNo, extractProblem(sb));
            String generatedClassName = String.format(CLASS_NAME_FORMAT,
                    problemClassPrefix, problemNo);
            String generatedClass = String.format(CLASS_FORMAT, packageString,
                    javaDoc, generatedClassName, sourcePackage);

            try {
                Path classSavePath = sourceDestinationFolder.resolve(subPackage);
                if (!Files.exists(classSavePath)) {
                    Files.createDirectories(classSavePath);
                }
                Path problemFilePath = classSavePath.resolve(generatedClassName + ".java");
                if (!Files.exists(problemFilePath) || overwriteExisting) {
                    Files.write(problemFilePath, generatedClass.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                }
                return new SourceGenResult(problemNo, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new SourceGenResult(problemNo, false);
    }

    private void generateEulerConfig() {
        try {
            Path eulerConfigPath = sourceDestinationFolder.resolve("EulerConfig.java");
            if (!Files.exists(eulerConfigPath)) {
                if (!Files.exists(sourceDestinationFolder)) {
                    Files.createDirectories(sourceDestinationFolder);
                }
                String eulerConfigClass = String.format(CONFIG_CLASS_FORMAT,
                        "package " + sourcePackage + ";");
                Files.write(eulerConfigPath, eulerConfigClass.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String toJavaDoc(int problemNo, String problem) {
        StringBuffer sb = new StringBuffer();
        sb.append("/**\n");
        sb.append(" * ").append(String.format(HEADING_FORMAT, problemNo)).append("\n");
        for (String line : problem.split("\n")) {
            sb.append(" * ").append(line).append("\n");
        }
        sb.append(" */");
        return sb.toString();
    }

    private static StringBuffer getWebpageAsString(String url) {
        StringBuffer sb = new StringBuffer();
        try {
            URL problemUrl = new URL(url);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(problemUrl.openStream()));

            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

    private static String extractProblem(StringBuffer sb) {
        String substring = null;

        int indexOf = sb.indexOf(SEARCH_BEGIN);
        if (indexOf != -1) {
            int nextIndexOf = -1;
            Matcher matcher = Pattern.compile(DIV_REGEX)
                    .matcher(sb.toString().substring(indexOf));
            int count = 0;
            while (matcher.find() && !matcher.hitEnd()) {
                String begin = matcher.group("begin");
                String end = matcher.group("end");
                if (begin != null) {
                    count++;
                } else if (end != null) {
                    count--;
                }
                if (count == 0) {
                    nextIndexOf = matcher.start();
                    break;
                }
            }

            if (nextIndexOf != -1) {
                substring = sb.substring(indexOf + SEARCH_BEGIN.length(), indexOf + nextIndexOf).trim();
            }
        }
        if (substring != null) {
            substring = substring.replaceAll(IMAGE_REGEX, IMAGE_REGEX_REPLACEMENT);
        }
        return substring;
    }

    public static void main(String[] args) throws IOException {
        Path sourceDestination = Files.createTempDirectory("EuSoCoGTest");
        String problemClassPrefix = "Euler";
        String sourcePackage = "de.nxg.eulersolution";
        boolean generatePackageStructure = true;

        EulerSourceGenerator generator = new EulerSourceGenerator(sourceDestination, problemClassPrefix,
                sourcePackage, generatePackageStructure);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(sourceDestination.toFile());
        }

        generator.generateAll();
    }

    // #########################################################################
    // ############################ INNER CLASSES ##############################
    // #########################################################################

    public static final class SourceGenCallable implements Callable<SourceGenResult> {

        private EulerSourceGenerator generator;
        private int problemNo;

        public SourceGenCallable(EulerSourceGenerator generator, int problemNo) {
            this.generator = generator;
            this.problemNo = problemNo;
        }

        @Override
        public SourceGenResult call() throws Exception {
            return generator._generate(problemNo, false);
        }

    }

    public static final class SourceGenResult {

        /** The Problem that got generated */
        public final int problemNo;
        /** If the generator should continue, will be set to false if an error happened */
        public final boolean continueGen;

        protected SourceGenResult(int problemNo, boolean continueGen) {
            this.problemNo = problemNo;
            this.continueGen = continueGen;
        }

    }
}
