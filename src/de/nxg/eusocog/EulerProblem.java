package de.nxg.eusocog;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Base Class that all Problems should extend from.
 * @author markus.moser
 */
public abstract class EulerProblem {

    private IEulerConfig config;
    private long startTime;
    private long lastTimedPrint;

    /**
     * No-Arg-Constructor
     * calls the {@link #init()} method.
     * Is needed for instantiating every EulerProblem with reflection.
     */
    protected EulerProblem() {
        init();
    }

    /**
     * Overwrite if needed. Gets called from the constructor.
     */
    public void init() {

    }

    /**
     * Solve the Problem
     * @return the solved Object. toString() is called on the object.
     */
    public abstract Object solve();

    /**
     * When called this method will print the current time since the start of the program.
     * Usefull when profiling.<br/>
     * <code>"&ltstring> took &lttime> since the start"</code>
     * @param string, will be printed as the cause
     */
    public void time(String string) {
        System.out.println(string + " took " + getTimeDisplayString() + " since the start");
    }

    /**
     * When called this method will print the current time since the start of the program.
     * Usefull when profiling.<br/>
     * <code>"Something took &lttime> since the start"</code>
     */
    public void time() {
        time("Something");
    }

    public void timedPrint(Object obj, long milliseconds) {
        long now = System.nanoTime();
        if (now - lastTimedPrint > milliseconds * 1_000_000) {
            lastTimedPrint = now;
            System.out.println("[" + (now - startTime) / 1_000_000 + " ms] " + objectToString(obj));
        }
    }

    private String getTimeDisplayString() {
        long time = config.getFinishTimeUnit().convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        return  time + " "+ config.getFinishTimeUnit().name().toLowerCase();
    }

    public static void start(Class<? extends EulerProblem> classToStart,
            IEulerConfig config) {
        try {
            EulerProblem problem = classToStart.newInstance();
            problem.config = config;

            System.out.println("Solving " + classToStart.getSimpleName() + "...");
            problem.startTime = System.nanoTime();
            Object solveObject = problem.solve();

            System.out.println("Finished in " + problem.getTimeDisplayString());

            if (solveObject == null) {
                System.out.println("The Result is null! Please return a valid String.");
                return;
            }

            String solve = null;
            solve = objectToString(solveObject);
            System.out.println("Result: " + solve);
            if (config.shouldCopyToClipboard()) {
                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                systemClipboard.setContents(new StringSelection(solve), null);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static String objectToString(Object obj) {
        if (obj instanceof Object[]) {
            return Arrays.deepToString((Object[]) obj);
        } else {
            return obj.toString();
        }
    }
}
