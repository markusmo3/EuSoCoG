package de.nxg.eusocog;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.concurrent.*;

/**
 * Base Class that all Problems should extend from.
 * @author markus.moser
 */
public abstract class EulerProblem {

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

    public static void start(Class<? extends EulerProblem> classToStart,
            IEulerConfig config) {
        try {
            EulerProblem problem = classToStart.newInstance();
            System.out.println("Solving " + classToStart.getSimpleName() + "...");

            long startTime = System.nanoTime();
            Object solveObject = problem.solve();

            long time = config.getFinishTimeUnit().convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            System.out.println("Finished in " + time + " " + config.getFinishTimeUnit().name().toLowerCase());

            if (solveObject == null) {
                System.out.println("The Result is null! Please return a valid String.");
                return;
            }

            String solve = solveObject.toString();
            System.out.println("Result: " + solve);
            if (config.shouldCopyToClipboard()) {
                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                systemClipboard.setContents(new StringSelection(solve), null);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
