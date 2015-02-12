package de.nxg.eusocog.example;

import java.awt.*;
import java.io.*;
import java.nio.file.*;

import de.nxg.eusocog.*;

public class EulerExample {

    public static void main(String[] args) {
        // The path where the generated sources should be saved in */
        Path sourceDestinationFolder = Paths.get(System.getProperty("user.dir"));
        // The prefix for the generated classes (e.g. if prefix = "Euler", Euler001.java, Euler002.java, ...)
        String problemClassPrefix = "Euler";
        // The package to be used by the generated classes (e.g. "de.nxg.eulersolutions")
        String sourcePackage = "your.package.here";
        // If the generator should also create the directories associated with the generated package structure.
        boolean generatePackageStructure = true;
        // Prefix to be used for subpackge declaration (e.g <prefix>000-049, <prefix>050-099, ...)
        String subPackagePrefix = "x";

        EulerSourceGenerator generator = new EulerSourceGenerator(
                sourceDestinationFolder,
                problemClassPrefix,
                sourcePackage,
                generatePackageStructure,
                subPackagePrefix);

        boolean overwriteExisting = true;

        generator.generate(167, overwriteExisting);
//        generator.generateAll();

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(sourceDestinationFolder.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
