package DenseTraj;

/**
 * Created by Tom.fu on 28/11/2014.
 */
public class TrackInfo {
    public int length; // length of the trajectory
    public int gap; // initial gap for feature detection

    public TrackInfo(int track_length, int init_gap){
        this.length = track_length;
        this.gap = init_gap;
    }
}
