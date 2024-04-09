package buffer;

import jdk.jfr.Unsigned;

import java.util.Optional;

public interface Replacer {

    /**
     * Remove the victim frame as defined by the replacement policy.
     * @param[out] frame_id id of frame that was removed, nullptr if no victim was found
     * @return <del>true if a victim frame was found, false otherwise<del/> In this implementation, frame_id or null will return
     */
    Optional<Integer> victim();

    /**
     * Pins a frame, indicating that it should not be victimized until it is unpinned.
     * @param frame_id the id of the frame to pin
     */
    void pin(int frame_id);

    /**
     * Unpins a frame, indicating that it can now be victimized.
     * @param frame_id the id of the frame to unpin
     */
    void unpin(int frame_id);

    /** @return the number of elements in the replacer that can be victimized */
    int getSize();
}
