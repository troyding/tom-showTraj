package DenseTraj;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;

import javax.rmi.CORBA.Util;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;
import static org.bytedeco.javacpp.opencv_highgui.resizeWindow;

/**
 * Created by Tom.fu on 24/2/2015.
 */
public class DenseTrack {

    static int start_frame = 0;
    static int end_frame = Integer.MAX_VALUE;
    ///Notice the effective scale_num value is determined later, when the first frame is input, see function InitPry
    static int init_scale_num = 1;
    static int scale_num = 1;
    static float scale_stride = (float) Math.sqrt(2.0);

    // parameters for descriptors
    static int patch_size = 32;
    static int nxy_cell = 2;
    static int nt_cell = 3;
    static float epsilon = 0.05f;
    static float min_flow = 0.4f;

    // parameters for tracking
    static double quality = 0.001;
    static int min_distance = 5;
    static int init_gap = 1;
    static int track_length = 15;

    // parameters for rejecting trajectory
    static float min_var = (float) Math.sqrt(3.0);
    static float max_var = 50.0f;
    static float max_dis = 20.0f;

    public static void DenseTrackExtraction(
            int show_track, String sourceVideoFile, String outputTxtPath, int start_frame_no, int end_frame_no) {

        TrackInfo trackInfo = new TrackInfo(track_length, init_gap);
        DescInfo hogInfo = new DescInfo(8, false, patch_size, nxy_cell, nt_cell);
        DescInfo mbhInfo = new DescInfo(8, false, patch_size, nxy_cell, nt_cell);

        opencv_core.Mat image = null, prev_grey = null, grey = null;
        opencv_core.Mat[] prev_grey_pyr = null, grey_pyr = null, flow_pyr = null;
        opencv_core.Mat[] prev_poly_pyr = null, poly_pyr = null;

        float[] fscales = null;
        opencv_core.Size[] sizes = null;

        opencv_core.IplImage fk = new opencv_core.IplImage();

        List<LinkedList<Track>> xyScaleTracks = new ArrayList<>();
        int frameFileIndex = start_frame_no;
        int init_counter = 0;
        int frameNum = start_frame_no;
        try {
            BufferedWriter myfile = new BufferedWriter(new FileWriter(outputTxtPath));
            //grabber.start();
            //while ((frame = grabber.grab()) != null)
            while (frameFileIndex <= end_frame_no) {
                int i, j, c;

                String fileName = sourceVideoFile + "Seq01_color\\" + String.format("frame%06d.jpg", frameFileIndex);

                opencv_core.IplImage ppimage = cvLoadImage(fileName);
                opencv_core.Mat frame = new opencv_core.Mat(ppimage);

                if (frameNum == start_frame_no) {
                    image.create(frame.size(), opencv_core.CV_8UC3);
                    grey.create(frame.size(), opencv_core.CV_8UC1);
                    prev_grey.create(frame.size(), opencv_core.CV_8UC1);

                    Object[] initPryResults = UtilFunc.InitPry(frame, init_scale_num, patch_size, scale_stride);
                    scale_num = (int)initPryResults[0];
                    fscales = (float[])initPryResults[1];
                    sizes = (opencv_core.Size[])initPryResults[2];

                    prev_grey_pyr = UtilFunc.BuildPry(sizes, opencv_core.CV_8UC1);
                    grey_pyr = UtilFunc.BuildPry(sizes, opencv_core.CV_8UC1);

                    flow_pyr = UtilFunc.BuildPry(sizes, opencv_core.CV_32FC2);
                    prev_poly_pyr = UtilFunc.BuildPry(sizes, opencv_core.CV_32FC(5));
                    poly_pyr = UtilFunc.BuildPry(sizes, opencv_core.CV_32FC(5));

                    frame.copyTo(image);
                    opencv_imgproc.cvtColor(image, prev_grey, opencv_imgproc.CV_BGR2GRAY);

                    for (int iScale = 0; iScale < scale_num; iScale ++){
                        if (iScale == 0){
                            prev_grey.copyTo(prev_grey_pyr[0]);
                        } else {
                            opencv_imgproc.resize(
                                    prev_grey_pyr[iScale-1], prev_grey_pyr[iScale], prev_grey_pyr[iScale].size(), 0, 0, opencv_imgproc.INTER_LINEAR);
                        }

                        // dense sampling feature points
                        LinkedList<opencv_core.Point2f> points_in = new LinkedList<>();
                        LinkedList<opencv_core.Point2f> points_out = UtilFunc.DenseSample(prev_grey_pyr[iScale], points_in, quality, min_distance);

                        LinkedList<Track> tracks = new LinkedList<>();
                        for (i = 0; i < points_out.size(); i ++){
                            tracks.addLast(new Track(points_out.get(i), trackInfo, hogInfo, mbhInfo));
                        }
                        xyScaleTracks.add(tracks);
                    }

                    frameNum++;
                    continue;
                }

                init_counter++;
                frame.copyTo(image);
                opencv_imgproc.cvtColor(image, grey, opencv_imgproc.CV_BGR2GRAY);



            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
