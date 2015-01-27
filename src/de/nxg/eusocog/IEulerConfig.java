package de.nxg.eusocog;

import java.util.concurrent.*;

/**
 * Interface for configurability
 * @author markus.moser
 */
public interface IEulerConfig {

    /**
     * @return if the the solved String should be copied into Clipboard
     */
    public boolean shouldCopyToClipboard();

    /**
     * @return the {@link TimeUnit} to be used for the finish time
     */
    public TimeUnit getFinishTimeUnit();

}
