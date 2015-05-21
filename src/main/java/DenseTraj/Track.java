package DenseTraj;

import org.bytedeco.javacpp.opencv_core;

/**
 * Created by Tom.fu on 24/2/2015.
 */
public class Track {

    public opencv_core.Point2f[] points;
    public float[] hog;
    public float[] mbhX;
    public float[] mbhY;
    public int index;

    public Track(opencv_core.Point2f point, TrackInfo trackInfo, DescInfo hogInfo, DescInfo mbhInfo){
        points = new opencv_core.Point2f[trackInfo.length +1];
        hog = new float[hogInfo.dim * trackInfo.length];
        mbhX = new float[mbhInfo.dim * trackInfo.length];
        mbhY = new float[mbhInfo.dim * trackInfo.length];

        index = 0;
        points[0].put(point);
    }

    void addPoint(opencv_core.Point2f point){
        index ++;
        points[index].put(point);
    }
}
