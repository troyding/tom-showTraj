package GenerateTraj;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

//import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacpp.opencv_video;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_highgui.*;

import javax.imageio.ImageIO;
import javax.swing.text.Utilities;

/**
 * Created by Tom.fu on 28/11/2014.
 */
public class FeatureExtra {
    static int patch_size = 32;
    static int nxy_cell = 2;
    static int nt_cell = 3;
    static boolean fullOrientation = true;
    static float epsilon = 0.05f;
    static float min_flow = 0.4f * 0.4f;

    static int start_frame = 0;
    static int end_frame = 1000000;
    static double quality = 0.001;
    static double min_distance = 10;
    static int init_gap = 3;
    static int track_length = 15; //15; update (bingbing longer traj)

    static float min_var = (float) Math.sqrt(3.0);
    static float max_var = 50.0f;
    static float max_dis = 20.0f;

    //static int scale_num = 2;
    static int scale_num = 1;
    static float scale_stride = (float) Math.sqrt(2.0);


    public static void main(String[] args) {
        String sourceVideoFile = "C:\\Users\\Tom.fu\\Desktop\\fromPeiYong\\testdata2\\";
        String outputTxtPath = "C:\\Users\\Tom.fu\\Desktop\\fromPeiYong\\testdata2\\newResults2.txt";
        int start_frame_no = 1;
        int end_frame_no = 100;

        min_distance = Double.parseDouble(args[0]);
        scale_num = Integer.parseInt(args[1]);
        track_length = Integer.parseInt(args[2]);
        //end_frame_no = Integer.parseInt(args[3]);

        long startTime = System.currentTimeMillis();
        System.out.println("new test, start at: " + startTime);
        FeatureDetectTrackingExtraction(1, sourceVideoFile, outputTxtPath, start_frame_no, end_frame_no);
        long endTime = System.currentTimeMillis();
        System.out.println("finished with duration: " + (endTime - startTime));
    }

    public static void FeatureDetectTrackingExtraction(
            int show_track, String sourceVideoFile, String outputTxtPath, int start_frame_no, int end_frame_no) {

        opencv_core.IplImage frame = null;
        opencv_core.IplImage image = null;
        opencv_core.IplImage prev_image = null;

        opencv_core.IplImage grey = null;
        opencv_core.IplImage prev_grey = null;
        IplImagePyramid grey_pyramid = null;
        IplImagePyramid prev_grey_pyramid = null;
        IplImagePyramid eig_pyramid = null;

        float[] fscales = new float[scale_num];
        List<LinkedList<Track>> xyScaleTracks = new ArrayList<>();

        DescInfo hogInfo = new DescInfo(8, 0, 1, patch_size, nxy_cell, nt_cell, min_flow);
        DescInfo mbhInfo = new DescInfo(8, 0, 1, patch_size, nxy_cell, nt_cell, min_flow);
        //InitTrackerInfo(&tracker, track_length, init_gap);
        TrackerInfo tracker = new TrackerInfo(track_length, init_gap);
        int nChannel = 3;
        int nDepth = 8;
        int inHeight = 480;
        int inWidth = 640;

        //grabber = new FFmpegFrameGrabber(sourceVideoFile);
        int frameFileIndex = start_frame_no;
        int init_counter = 0;
        int frameNum = 0;
        try {
            BufferedWriter myfile = new BufferedWriter(new FileWriter(outputTxtPath));
            //grabber.start();
            //while ((frame = grabber.grab()) != null)
            while (frameFileIndex <= end_frame_no) {
                //TODO: caution on the change of the frameNumber
                //frameNum = capture.getFrameIndex();
                frameNum++;
                int i, j, c;

                String fileName = sourceVideoFile + "Seq01_color\\" + String.format("frame%06d.jpg", frameFileIndex);
                IplImage ppimage = cvLoadImage(fileName);
                frame = cvCreateImage(cvSize(inWidth, inHeight), nDepth, nChannel);
                opencv_imgproc.cvResize(ppimage, frame, opencv_imgproc.CV_INTER_AREA);

                if (frameNum >= start_frame && frameNum <= end_frame) {
                    if (image == null) {
                        image = cvCreateImage(cvGetSize(frame), 8, 3);
                        image.origin(frame.origin());
                        prev_image = cvCreateImage(cvGetSize(frame), 8, 3);
                        prev_image.origin(frame.origin());

                        grey = cvCreateImage(cvGetSize(frame), 8, 1);
                        grey_pyramid = new IplImagePyramid(scale_stride, scale_num, cvGetSize(frame), 8, 1);
                        prev_grey = cvCreateImage(cvGetSize(frame), 8, 1);
                        prev_grey_pyramid = new IplImagePyramid(scale_stride, scale_num, cvGetSize(frame), 8, 1);
                        eig_pyramid = new IplImagePyramid(scale_stride, scale_num, cvGetSize(frame), 32, 1);

                        cvCopy(frame, image, null);
                        opencv_imgproc.cvCvtColor(image, grey, opencv_imgproc.CV_BGR2GRAY);
                        grey_pyramid.rebuild(grey);

                        for (int ixyScale = 0; ixyScale < scale_num; ++ixyScale) {
                            LinkedList<Track> tracks = new LinkedList<>();
                            fscales[ixyScale] = (float) Math.pow(scale_stride, ixyScale);

                            IplImage grey_temp = cvCloneImage(grey_pyramid.getImage(ixyScale));
                            IplImage eig_temp = cvCloneImage(eig_pyramid.getImage(ixyScale));

                            ///TODO: note a different implementation approach
                            LinkedList<CvPoint2D32f> points = cvDenseSample(grey_temp, eig_temp, quality, min_distance);

                            for (i = 0; i < points.size(); i++) {
                                Track track = new Track(tracker.trackLength);
                                PointDesc point = new PointDesc(mbhInfo, points.get(i));
                                track.addPointDesc(point);
                                tracks.addLast(track);
                            }
                            xyScaleTracks.add(tracks);
                            cvReleaseImage(grey_temp);
                            cvReleaseImage(eig_temp);
                        }
                    }

                    cvCopy(frame, image, null);
                    opencv_imgproc.cvCvtColor(image, grey, opencv_imgproc.CV_BGR2GRAY);
                    grey_pyramid.rebuild(grey);

                    //TODO: bug finding!!! be careful of this iTrack = tracks.erase(iTrack)
                    if (frameNum > 0) {
                        init_counter++;
                        System.out.println("Step1_fID: " + frameFileIndex + ", init_counter: " + init_counter);
                        for (int ixyScale = 0; ixyScale < scale_num; ixyScale++) {
                            LinkedList<CvPoint2D32f> points_in = new LinkedList<>();
                            LinkedList<Track> tracks = xyScaleTracks.get(ixyScale);

                            for (int tempI = 0; tempI < tracks.size(); tempI ++) {
                                Track iTrack = tracks.get(tempI);
                                //TODO: double check!!!
                                //CvPoint2D32f point = iTrack.pointDescs.getLast().point;
                                CvPoint2D32f point = new CvPoint2D32f(iTrack.pointDescs.getLast().point);
                                //CvPoint2D32f point = new CvPoint2D32f();
                                //point.x(iTrack.pointDescs.getLast().point.x());
                                //point.y(iTrack.pointDescs.getLast().point.y());

                                points_in.addLast(point);
//                                if (tempI > 137 && tempI < 144 && ixyScale < 0){
//                                    System.out.println("Step1_i: " + tempI + ", s: " + ixyScale +", tSize: " + tracks.size()
//                                            + ", iTPS: " + iTrack.pointDescs.size()
//                                            + ", p0: " + iTrack.pointDescs.getFirst().point
//                                            + ", p1: " + iTrack.pointDescs.getLast().point);
//                                }
                            }

                            //int count = points_in.size();
                            IplImage prev_grey_temp = cvCloneImage(prev_grey_pyramid.getImage(ixyScale));
                            IplImage grey_temp = cvCloneImage(grey_pyramid.getImage(ixyScale));
                            //opencv_core.Mat prev_grey_mat = cvarrToMat(pre_grey_temp);
                            //opencv_core.Mat grey_mat = cvarrToMat(grey_temp);

                            IplImage flow = cvCreateImage(cvGetSize(grey_temp), IPL_DEPTH_32F, 2);
                            opencv_video.cvCalcOpticalFlowFarneback(prev_grey_temp, grey_temp, flow,
                                    Math.sqrt(2.0) / 2.0, 5, 10, 2, 7, 1.5, opencv_video.OPTFLOW_FARNEBACK_GAUSSIAN);

//                            cvShowImage("DenseTrack", flow);
//                            c = cvWaitKey(3);

                            Object[] pOutWithStatus = OpticalFlowTrackerSimple(flow, points_in);
                            LinkedList<CvPoint2D32f> points_out = (LinkedList<CvPoint2D32f>) pOutWithStatus[0];
                            int[] status = (int[]) pOutWithStatus[1];


                            //System.out.println("Step2_fID: " + frameFileIndex
                            //        + ", s: " + ixyScale
                            //        + ", tSize: " + tracks.size()
                            //        + ", pin.size(): " + points_in.size()
                            //        + ", pout.size(): " + points_out.size());

//                            if (ixyScale < 0) {
//                                for (int tempI = 138; tempI < 144; tempI ++) {
//                                    System.out.println("step2_i: " + tempI
//                                            + ", s: " + ixyScale
//                                            + points_in.get(tempI).toString() + ","
//                                            + points_out.get(tempI).toString() + ", "
//                                            + status[tempI]);
//                                }
//                            }

                            int width = grey_temp.width();
                            int height = grey_temp.height();

                            ///DescMat mbhMatX = new DescMat(height, width, mbhInfo.nBins);
                            ///DescMat mbhMatY = new DescMat(height, width, mbhInfo.nBins);
                            DescMat[] mbhMatXY = MbhComp(flow, mbhInfo, width, height);
                            DescMat mbhMatX = mbhMatXY[0];
                            DescMat mbhMatY = mbhMatXY[1];

                            int tSize = tracks.size();
                            int tSizeCnt = 0;
                            for (i = 0; i < tSize; i++) {
                                Track iTrack = tracks.get(tSizeCnt);
                                if (status[i] == 1) {
                                    CvPoint2D32f prev_point = points_in.get(i);
                                    CvScalar rect = getRect(prev_point, cvSize(width, height), hogInfo);

                                    ///Note, here change the information of  iTrack.pointDescs.lastNode!
                                    PointDesc pointDesc = iTrack.pointDescs.getLast();
                                    pointDesc.mbhX = getDesc(mbhMatX, rect, mbhInfo);
                                    pointDesc.mbhY = getDesc(mbhMatY, rect, mbhInfo);

                                    PointDesc point = new PointDesc(mbhInfo, points_out.get(i));
                                    iTrack.addPointDesc(point);

                                    if (show_track == 1) {
                                        float length = iTrack.pointDescs.size();

                                        float point0_x = fscales[ixyScale] * iTrack.pointDescs.get(0).point.x();
                                        float point0_y = fscales[ixyScale] * iTrack.pointDescs.get(0).point.y();
                                        CvPoint2D32f point0 = new CvPoint2D32f();
                                        point0.x(point0_x);
                                        point0.y(point0_y);

                                        float jIndex = 0;
                                        for (int jj = 1; jj < length; jj++, jIndex++) {
                                            float point1_x = fscales[ixyScale] * iTrack.pointDescs.get(jj).point.x();
                                            float point1_y = fscales[ixyScale] * iTrack.pointDescs.get(jj).point.y();
                                            CvPoint2D32f point1 = new CvPoint2D32f();
                                            point1.x(point1_x);
                                            point1.y(point1_y);


                                            //TODO: note different to C++code:
                                            //cvLine(image, cvPointFrom32f(point0), cvPointFrom32f(point1),
                                            //CV_RGB(0, cvFloor(255.0 * (j + 1.0) / length), 0), 0.5, 8, 0);
                                            cvLine(image, cvPointFrom32f(point0), cvPointFrom32f(point1),
                                                    CV_RGB(0, cvFloor(255.0 * (jIndex + 1.0) / length), 0), 1, 8, 0);
                                            point0 = point1;
                                        }
                                    }
                                    tSizeCnt++;
                                } else {
                                    tracks.remove(iTrack);
                                    //System.out.println("remove: fID: " + frameFileIndex
                                    //        + ", s: " + ixyScale
                                    //        + ", tSize: " + tracks.size()
                                    //        + ", pin.size(): " + points_in.size()
                                    //        + ", pout.size(): " + points_out.size());
                                }
                            }
                            //ReleDescMat(hogMat);
                            //ReleDescMat(hofMat);
                            //ReleDescMat(mbhMatX);
                            //ReleDescMat(mbhMatY);
                            cvReleaseImage(prev_grey_temp);
                            cvReleaseImage( grey_temp );
                            cvReleaseImage( flow );
                        }

                        //output
                        for (int ixyScale = 0; ixyScale < scale_num; ixyScale++) {
                            LinkedList<Track> tracks = xyScaleTracks.get(ixyScale);
                            int tSizeBefore = tracks.size();
                            int tSizeCnt = 0;
                            int indCnt = 0;
                            int removeSize = 0;
                            for (i = 0; i < tSizeBefore; i++) {
                                Track iTrack = tracks.get(tSizeCnt);
                                // if the trajectory achieves the length we want
                                if (iTrack.pointDescs.size() >= tracker.trackLength + 1) {
                                    CvPoint2D32f[] trajectory = new CvPoint2D32f[tracker.trackLength + 1];
                                    for (int count = 0; count <= tracker.trackLength; count++) {
                                        PointDesc iDesc = iTrack.pointDescs.get(count);
                                        trajectory[count] = new CvPoint2D32f();
                                        trajectory[count].x(iDesc.point.x() * fscales[ixyScale]);
                                        trajectory[count].y(iDesc.point.y() * fscales[ixyScale]);

//                                        if (i < 5 && count < 5){
//                                            System.out.println("OP_fID: " + frameFileIndex + ", i: " + i + ", s: " + ixyScale
//                                                    + ", cnt: " + count + ", trajPT: " + trajectory[count]);
//                                        }
                                    }

                                    //return new Object[]{bk2, new float[]{mean_x, mean_y, var_x, var_y, length}, 1};
                                    Object[] ret = isValid(trajectory);
                                    CvPoint2D32f[] trajectory_backup = (CvPoint2D32f[]) ret[0];
                                    float[] vals = (float[]) ret[1];
                                    int indicator = (int) ret[2];

                                    if (indicator == 1) {
                                        indCnt ++;
                                        //System.out.println("Processing track in frame " + frameFileIndex);

                                        float[] Track_Info = new float[]{
                                                (float) frameNum, vals[0], vals[1], vals[2], vals[3], vals[4], fscales[ixyScale]};

                                        //System.out.println("OP_fID: " + frameFileIndex + ", i: " + i + ", s: " + ixyScale
                                        //        + ", mx: " + vals[0] + ", my: " + vals[1]
                                        //        + ", vx: " + vals[2] + ", vy: " + vals[3]
                                        //        + ", len: " + vals[4] + ", fs: " + fscales[ixyScale]);

                                        float[] XYs = new float[(tracker.trackLength + 1) * 2];
                                        for (int count = 0; count < tracker.trackLength + 1; count++) {
                                            XYs[count * 2] = trajectory[count].x();
                                            XYs[count * 2 + 1] = trajectory[count].y();

//                                            if (count < 5) {
//                                                System.out.println("Ind_fID: " + frameFileIndex + ", i: " + i + ", s: "
//                                                        + ixyScale + ", cnt: " + count + ", XYs[cnt*2]: " + XYs[count * 2] + ", XYs[cnt*2+1]: " + XYs[count * 2 + 1]);
//                                            }
                                        }

                                        int t_stride = cvFloor(tracker.trackLength / hogInfo.ntCells);

                                        List<Float> mbhdescr = new ArrayList<>();
                                        //float[] mbhdescr = new float[mbhInfo.dim + mbhInfo.dim];
                                        float[] trajdescr = new float[(tracker.trackLength - 1 + 1) * 2];

                                        float geodes_traj_length = 0;
                                        for (int count = 0; count < tracker.trackLength - 1 + 1; count++) {
                                            trajdescr[count * 2] = trajectory[count + 1].x() - trajectory[count].x();
                                            trajdescr[count * 2 + 1] = trajectory[count + 1].y() - trajectory[count].y();
                                            geodes_traj_length += Math.sqrt(
                                                    trajdescr[count * 2] * trajdescr[count * 2] + trajdescr[count * 2 + 1] * trajdescr[count * 2 + 1]);

//                                            if (count < 5) {
//                                                System.out.println("Ind_fID: " + frameFileIndex + ", i: " + i + ", s: "
//                                                        + ixyScale + ", cnt: " + count + ", trajdescr[cnt*2]: " + trajdescr[count * 2]
//                                                        + ", trajdescr[cnt*2+1]: " + trajdescr[count * 2 + 1] + ", geodes: " + geodes_traj_length);
//                                            }
                                        }

                                        for (int h = 0; h < trajdescr.length; h++) {
                                            trajdescr[h] = trajdescr[h] / (geodes_traj_length + 0.000001f);

//                                            if (h < 5) {
//                                                System.out.println("Ind_Fid: " + frameFileIndex + ", i: " + i + ", s: "
//                                                        + ixyScale + ", h: " + h + ", trajdescr[h]: " + trajdescr[h]);
//                                            }
                                        }

                                        //TODO: caution, different to the c++ code
                                        t_stride = cvFloor(tracker.trackLength / mbhInfo.ntCells);
                                        int iDescIndex = 0;
                                        for (int n = 0; n < mbhInfo.ntCells; n++) {
                                            float[] vec = new float[mbhInfo.dim];
                                            for (int m = 0; m < mbhInfo.dim; m++) {
                                                vec[m] = 0;
                                            }
                                            for (int t = 0; t < t_stride; t++, iDescIndex++) {
                                                PointDesc iDesc = iTrack.pointDescs.get(iDescIndex);
                                                for (int m = 0; m < mbhInfo.dim; m++) {
                                                    vec[m] += iDesc.mbhX[m];
                                                }
                                            }
                                            for (int m = 0; m < mbhInfo.dim; m++) {
                                                mbhdescr.add(vec[m] / (float)t_stride);
                                            }
                                        }

                                        //TODO: caution, this is a bug, we miss the Y part!!!
                                        t_stride = cvFloor(tracker.trackLength / mbhInfo.ntCells);
                                        iDescIndex = 0;
                                        for (int n = 0; n < mbhInfo.ntCells; n++) {
                                            float[] vec = new float[mbhInfo.dim];
                                            for (int m = 0; m < mbhInfo.dim; m++) {
                                                vec[m] = 0;
                                            }
                                            for (int t = 0; t < t_stride; t++) {
                                                PointDesc iDesc = iTrack.pointDescs.get(iDescIndex );
                                                for (int m = 0; m < mbhInfo.dim; m++) {
                                                    vec[m] += iDesc.mbhY[m];
                                                }
                                            }
                                            for (int m = 0; m < mbhInfo.dim; m++) {
                                                mbhdescr.add(vec[m] / (float)t_stride);
                                            }
                                        }

                                        WriteTrajFeature2Txt(myfile, Track_Info, XYs, mbhdescr, trajdescr);
                                        //System.out.println("\n");
                                    }
                                    tracks.remove(iTrack);
                                    removeSize++;
                                }else {
                                    //iTracker++;
                                    tSizeCnt++;
                                }
                            }
//                            System.out.println("OP_FID: " + frameFileIndex
//                                    + ", tSizeB: " + tSizeBefore + ", tSizeA: " + tracks.size()
//                                    + ", indSize: " + indCnt + ", removeSize: " + removeSize);
                        }

                        if (init_counter == tracker.initGap) {
//                            System.out.println("Step3_fID:: " + frameFileIndex + ", init_counter == tracker.gap = " + init_counter);
                            init_counter = 0;
                            for (int ixyScale = 0; ixyScale < scale_num; ixyScale++) {
                                LinkedList<Track> tracks = xyScaleTracks.get(ixyScale);
                                LinkedList<CvPoint2D32f> points_in = new LinkedList<>();
//                                System.out.println("Step3_fID: " + frameFileIndex + ", s: " + ixyScale + ", tracks.size(): " + tracks.size());
                                //int tracksLength = tracks.size();
                                for (i = 0; i < tracks.size(); i++) {
                                    Track iTrack = tracks.get(i);
                                    CvPoint2D32f point = new CvPoint2D32f(iTrack.pointDescs.getLast().point);
                                    //CvPoint2D32f point = new CvPoint2D32f();
                                    //point.x(iTrack.pointDescs.getLast().point.x());
                                    //point.y(iTrack.pointDescs.getLast().point.y());

                                    points_in.addLast(point);
//                                    if (ixyScale < 0) {
//                                        if (i > 137 && i < 144) {
//                                            System.out.println("init_i: " + i
//                                                    + ", s: " + ixyScale
//                                                    + ", iTPS: " + iTrack.pointDescs.size()
//                                                    + ", p0: " + iTrack.pointDescs.getFirst().point
//                                                    + ", p1: " + iTrack.pointDescs.getLast().point);
//                                        }
//                                    }
                                }

                                IplImage grey_temp = cvCloneImage(grey_pyramid.getImage(ixyScale));
                                IplImage eig_temp = cvCloneImage(eig_pyramid.getImage(ixyScale));

                                LinkedList<CvPoint2D32f> points_out =
                                        cvDenseSample(grey_temp, eig_temp, points_in, quality, min_distance);

                                for (i = 0; i < points_out.size(); i++) {
                                    Track track = new Track(tracker.trackLength);
                                    PointDesc point = new PointDesc(mbhInfo, points_out.get(i));
                                    track.addPointDesc(point);
                                    tracks.addLast(track);
                                }

//                                System.out.println("Step3_fID: " + frameFileIndex
//                                        + ", s: " + ixyScale
//                                        + ", tracks.size(): " + tracks.size()
//                                        + ", points_in.size(): " + points_in.size()
//                                        + ", points_out.size(): " + points_out.size());

                                cvReleaseImage(grey_temp);
                                cvReleaseImage(eig_temp);
                            }
                        }
                    }

                    cvCopy(frame, prev_image, null);
                    opencv_imgproc.cvCvtColor(prev_image, prev_grey, opencv_imgproc.CV_BGR2GRAY);
                    prev_grey_pyramid.rebuild(prev_grey);
                }

                if (show_track == 1) {
                    cvShowImage("DenseTrack", image);
                    c = cvWaitKey(3);
                    if ((char) c == 27) {
                        break;
                    }
                }
                frameFileIndex++;
            }
            myfile.close();

            if (show_track == 1) {
                cvDestroyWindow("DenseTrack");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //We have re-organized the input and output to the oringal c++ version
    //i.e, we make points to be return values instead of an input.
    public static LinkedList<CvPoint2D32f> cvDenseSample(IplImage grey, IplImage eig,
                                                         double quality, double min_distance) {

        LinkedList<CvPoint2D32f> points = new LinkedList<>();

        int width = cvFloor(grey.width() / min_distance);
        int height = cvFloor(grey.height() / min_distance);

        double[] maxVal = new double[1];
        maxVal[0] = 0.0;
        opencv_imgproc.cvCornerMinEigenVal(grey, eig, 3, 3);
        //TODO: here need to be careful check with original c++ code.
        //cvMinMaxLoc(eig, 0, &maxVal, 0, 0, 0);
        cvMinMaxLoc(eig, null, maxVal, null, null, null);
        double threshold = maxVal[0] * quality;


        opencv_core.Mat eigMat = new opencv_core.Mat(eig);

        int offset = cvFloor(min_distance / 2.0);
        //TODO:: is the calculation of "ve" correct?
        //FloatBuffer floatBuffer = eig.getFloatBuffer();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int x = cvFloor(j * min_distance + offset);
                int y = cvFloor(i * min_distance + offset);
                //int index = j * eig.widthStep() + eig.nChannels() * i;
                //int index = y * eig.widthStep() + x;
                //TODO:: caution, as explained by Peiyong, each row width is not dividable by size of float (4 bytes)
                //There fore, we need to go to the target row first, then get the col.

                //FloatBuffer floatBuffer = eigMat.createBuffer(y * eigMat.arrayWidth());

                FloatBuffer floatBuffer = eig.getByteBuffer(y * eig.widthStep()).asFloatBuffer();
                float ve = floatBuffer.get(x);

                if (ve > threshold) {
                    points.addLast(cvPoint2D32f(x, y));
                }
            }
        }

        return points;
    }

    //We have re-organized the input and output to the oringal c++ version
    //i.e, we make points to be return values instead of an input.
    public static LinkedList<CvPoint2D32f> cvDenseSample(IplImage grey, IplImage eig,
                                                         LinkedList<CvPoint2D32f> points_in,
                                                         double quality, double min_distance) {
        LinkedList<CvPoint2D32f> points_out = new LinkedList<>();
        int width = cvFloor(grey.width() / min_distance);
        int height = cvFloor(grey.height() / min_distance);

        double[] maxVal = new double[1];
        maxVal[0] = 0.0;
        opencv_imgproc.cvCornerMinEigenVal(grey, eig, 3, 3);
        //TODO: here need to be careful check with original c++ code.
        //cvMinMaxLoc(eig, 0, &maxVal, 0, 0, 0);
        cvMinMaxLoc(eig, null, maxVal, null, null, null);
        double threshold = maxVal[0] * quality;

//        System.out.println("cvDenseSample, w: " + width + ", h: " + height
//                + ", th: " + threshold + ", md: " + min_distance + ", pin.Size: " + points_in.size());

        int[] counters = new int[width * height];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
        }
        for (int i = 0; i < points_in.size(); i++) {
            CvPoint2D32f point = points_in.get(i);

            int x = cvFloor(point.x() / min_distance);
            int y = cvFloor(point.y() / min_distance);
            int ywx = y * width + x;

            if (point.x() >= min_distance * width || point.y() >= min_distance * height) {
                //System.out.println("skip: i: " + i + ", ywx: " + ywx);
                continue;
            }
            counters[y * width + x]++;

        }

        opencv_core.Mat eigMat = new opencv_core.Mat(eig);


        int totalSampled = 0;
        int removed = 0;
        int remained = 0;
        int index = 0;
        int offset = cvFloor(min_distance / 2);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++, index++) {
//                if (counters[index] == 0) {
                    int x = cvFloor(j * min_distance + offset);
                    int y = cvFloor(i * min_distance + offset);

                    //FloatBuffer floatBuffer = eigMat.createBuffer(y * eigMat.arrayWidth());

                    FloatBuffer floatBuffer = eig.getByteBuffer(y * eig.widthStep()).asFloatBuffer();
                    float ve = floatBuffer.get(x);
                    if (ve > threshold) {
                        totalSampled ++;
                        if (counters[index] == 0){
                            points_out.addLast(cvPoint2D32f(x, y));
                            remained ++;
                        } else {
                            removed ++;
                        }
                    }
//                }
            }
        }
        System.out.println("totalSampled: " + totalSampled + ", removed: " + removed + ", remained: " + remained);
        return points_out;
    }

    public static Object[] OpticalFlowTracker(IplImage flow, LinkedList<CvPoint2D32f> points_in) {

        LinkedList<CvPoint2D32f> points_out = new LinkedList<>();
        int[] status = new int[points_in.size()];

        int width = flow.width();
        int height = flow.height();
        for (int i = 0; i < points_in.size(); i++) {

            CvPoint2D32f point_in = points_in.get(i);
            LinkedList<Float> xs = new LinkedList<>();
            LinkedList<Float> ys = new LinkedList<>();
            int x = cvFloor(point_in.x());
            int y = cvFloor(point_in.y());
            for (int m = x - 1; m <= x + 1; m++) {
                for (int n = y - 1; n <= y + 1; n++) {
                    int p = Math.min(Math.max(m, 0), width - 1);
                    int q = Math.min(Math.max(n, 0), height - 1);

                    FloatBuffer floatBuffer = flow.getByteBuffer(q * flow.widthStep()).asFloatBuffer();
                    int xsIndex = 2 * p;
                    int ysIndex = 2 * p + 1;

                    xs.addLast(floatBuffer.get(xsIndex));
                    ys.addLast(floatBuffer.get(ysIndex));
//                    if (i < 0){
//                        System.out.println("i: " + i
//                                + ", x: " + x + ", y: " + y
//                                + ", m: " + m + ", n: " + n
//                                + ", p: " + p + ", q: " + q
//                                + ", xs: " + floatBuffer.get(xsIndex) + ", ys: " + floatBuffer.get(ysIndex)
//                        );
//                    }
                }
            }
            xs.sort(Float::compare);
            ys.sort(Float::compare);
            int size = xs.size() / 2;
            for (int m = 0; m < size; m++) {
                xs.removeLast();
                ys.removeLast();
            }

            CvPoint2D32f offset = new CvPoint2D32f();
            offset.x(xs.getLast());
            offset.y(ys.getLast());

            CvPoint2D32f point_out = new CvPoint2D32f();
            point_out.x(point_in.x() + offset.x());
            point_out.y(point_in.y() + offset.y());

            //TODO: note we used a different approach than the c++ version
            points_out.addLast(point_out);
            if (point_out.x() > 0 && point_out.x() < width && point_out.y() > 0 && point_out.y() < height) {
                status[i] = 1;
//                if ((i > 137 && i < 144) || i < 6){
//                    System.out.println("s(1)_i: " + i + ", pin: " + point_in + ", pout: " + point_out + ", off: " + offset);
//                }
            } else {
                status[i] = -1;
//                System.out.println("s(-1)_i: " + i + ", w: " + width + ", h: " + height
//                        + ", pin: " + point_in + ", pout: " + point_out + ", off: " + offset);
            }
        }
        return new Object[]{points_out, status};
    }

    public static Object[] OpticalFlowTrackerSimple(IplImage flow, LinkedList<CvPoint2D32f> points_in) {

        LinkedList<CvPoint2D32f> points_out = new LinkedList<>();
        int[] status = new int[points_in.size()];

        int width = flow.width();
        int height = flow.height();
        for (int i = 0; i < points_in.size(); i++) {

            CvPoint2D32f point_in = points_in.get(i);

            int p = Math.min(Math.max(cvFloor(point_in.x()), 0), width - 1);
            int q = Math.min(Math.max(cvFloor(point_in.y()), 0), height - 1);



            FloatBuffer floatBuffer = flow.getByteBuffer(q * flow.widthStep()).asFloatBuffer();
            float[] fData = new float[width*2];
            floatBuffer.get(fData);


            ByteBuffer bb = flow.getByteBuffer(q * flow.widthStep());
            //byte[] data = new byte[flow.widthStep()];
            //bb.get(data);
//
//            byte[] newData = new byte[data.length];
//            newData = Arrays.copyOf(data, data.length);
//
//
//            FloatBuffer floatBuffer = ByteBuffer.wrap(newData).asFloatBuffer();

//            FloatBuffer floatBuffer = bb.asFloatBuffer();
            //FloatBuffer floatBuffer = flow.getByteBuffer(q * flow.widthStep()).asFloatBuffer();
            int xsIndex = 2 * p;
            int ysIndex = 2 * p + 1;

            CvPoint2D32f point_out = new CvPoint2D32f();
//            point_out.x(point_in.x() + floatBuffer.get(xsIndex));
//            point_out.y(point_in.y() + floatBuffer.get(ysIndex));

            point_out.x(point_in.x() + fData[xsIndex]);
            point_out.y(point_in.y() + fData[ysIndex]);


            //TODO: note we used a different approach than the c++ version
            points_out.addLast(point_out);
            if (point_out.x() > 0 && point_out.x() < width && point_out.y() > 0 && point_out.y() < height) {
                status[i] = 1;
            } else {
                status[i] = -1;
            }
        }
        return new Object[]{points_out, status};
    }

    //We have re-organized the input and output to the oringal c++ version
    public static DescMat[] MbhComp(IplImage flow, DescInfo descInfo,
                                    int width, int height) {
        //int width = descMatX.width;
        //int height = descMatX.height;
        IplImage flowX = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);
        IplImage flowY = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);
        IplImage flowXdX = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);
        IplImage flowXdY = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);
        IplImage flowYdX = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);
        IplImage flowYdY = cvCreateImage(cvSize(width, height), IPL_DEPTH_32F, 1);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                FloatBuffer floatBuffer = flow.getByteBuffer(i * flow.widthStep()).asFloatBuffer();
                FloatBuffer floatBufferX = flowX.getByteBuffer(i * flowX.widthStep()).asFloatBuffer();
                FloatBuffer floatBufferY = flowY.getByteBuffer(i * flowY.widthStep()).asFloatBuffer();

                int fXIndex = j;
                int fYIndex = j;
                int fIndexForX = 2 * j;
                int fIndexForY = 2 * j + 1;

                floatBufferX.put(fXIndex, 100 * floatBuffer.get(fIndexForX));
                floatBufferY.put(fYIndex, 100 * floatBuffer.get(fIndexForY));
            }
        }

        opencv_imgproc.cvSobel(flowX, flowXdX, 1, 0, 1);
        opencv_imgproc.cvSobel(flowX, flowXdY, 0, 1, 1);
        opencv_imgproc.cvSobel(flowY, flowYdX, 1, 0, 1);
        opencv_imgproc.cvSobel(flowY, flowYdY, 0, 1, 1);

        DescMat[] retVal = new DescMat[2];
        retVal[0] = BuildDescMat(flowXdX, flowXdY, descInfo, width, height);//descMatX
        retVal[1] = BuildDescMat(flowYdX, flowYdY, descInfo, width, height);//descMatY

        cvReleaseImage(flowX);
        cvReleaseImage(flowY);
        cvReleaseImage(flowXdX);
        cvReleaseImage(flowXdY);
        cvReleaseImage(flowYdX);
        cvReleaseImage(flowYdY);

        return retVal;
    }

    //We have re-organized the input and output to the oringal c++ version
    public static DescMat BuildDescMat(IplImage xComp, IplImage yComp, DescInfo descInfo, int width, int height) {

        DescMat descMat = new DescMat(height, width, descInfo.nBins);
        // whether use full orientation or not
        float fullAngle = descInfo.fullOrientation > 0 ? 360 : 180;
        // one additional bin for hof
        int nBins = descInfo.flagThre > 0 ? descInfo.nBins - 1 : descInfo.nBins;

        float angleBase = fullAngle / (float) nBins;
        int histDim = descInfo.nBins;
        int index = 0;

        for (int i = 0; i < height; i++) {
            // the histogram accumulated in the current line
            float[] sum = new float[histDim];
            for (int j = 0; j < sum.length; j++) {
                sum[j] = 0;
            }
            for (int j = 0; j < width; j++, index++) {
                FloatBuffer floatBufferX = xComp.getByteBuffer(i * xComp.widthStep()).asFloatBuffer();
                FloatBuffer floatBufferY = yComp.getByteBuffer(i * yComp.widthStep()).asFloatBuffer();

                int xIndex = j;
                int yIndex = j;

                float shiftX = floatBufferX.get(xIndex);
                float shiftY = floatBufferY.get(yIndex);
                float magnitude0 = (float) Math.sqrt(shiftX * shiftX + shiftY * shiftY);
                float magnitude1 = magnitude0;
                int bin0, bin1;

                // for the zero bin of hof
                if (descInfo.flagThre == 1 && magnitude0 <= descInfo.threshold) {
                    bin0 = nBins; // the zero bin is the last one
                    magnitude0 = 1.0f;
                    bin1 = 0;
                    magnitude1 = 0;
                } else {
                    float orientation = cvFastArctan(shiftY, shiftX);
                    if (orientation > fullAngle) {
                        orientation -= fullAngle;
                    }

                    // split the magnitude to two adjacent bins
                    float fbin = orientation / angleBase;
                    bin0 = ((int) Math.floor(fbin + 0.5)) % nBins;
                    bin1 = ((fbin - bin0) > 0 ? (bin0 + 1) : (bin0 - 1 + nBins)) % nBins;

                    float weight0 = 1 - Math.min(Math.abs(fbin - bin0), nBins - fbin);
                    float weight1 = 1 - weight0;

                    magnitude0 *= weight0;
                    magnitude1 *= weight1;
                }

                sum[bin0] += magnitude0;
                sum[bin1] += magnitude1;

                int temp0 = index * descMat.nBins;
                if (i == 0) {
                    // for the 1st line
                    for (int m = 0; m < descMat.nBins; m++) {
                        descMat.desc[temp0++] = sum[m];
                    }
                } else {
                    int temp1 = (index - width) * descMat.nBins;
                    for (int m = 0; m < descMat.nBins; m++) {
                        descMat.desc[temp0++] = descMat.desc[temp1++] + sum[m];
                    }
                }
            }
        }
        return descMat;
    }

    public static CvScalar getRect(CvPoint2D32f point, CvSize size, DescInfo descInfo) {
        int x_min = descInfo.blockWidth / 2;
        int y_min = descInfo.blockHeight / 2;
        int x_max = size.width() - descInfo.blockWidth;
        int y_max = size.height() - descInfo.blockHeight;

        float tmp_x = point.x() - x_min;
        float temp_x = Math.min(Math.max(tmp_x, 0.0f), (float) x_max);

        float tmp_y = point.y() - y_min;
        float temp_y = Math.min(Math.max(tmp_y, 0.0f), (float) y_max);

        CvScalar rect = new CvScalar();
        rect.setVal(0, temp_x);
        rect.setVal(1, temp_y);
        rect.setVal(2, descInfo.blockWidth);
        rect.setVal(3, descInfo.blockHeight);

        return rect;
    }

    public static float[] getDesc(DescMat descMat, CvScalar rect, DescInfo descInfo) {
        int descDim = descInfo.dim;
        int height = descMat.height;
        int width = descMat.width;

        float[] vec = new float[descDim];
        int xOffset = (int) rect.getVal(0);
        int yOffset = (int) rect.getVal(1);
        int xStride = (int) (rect.getVal(2)/descInfo.ntCells);
        int yStride = (int) (rect.getVal(3)/descInfo.ntCells);

        int iDesc = 0;
        for (int iX = 0; iX < descInfo.nxCells; iX++) {
            for (int iY = 0; iY < descInfo.nyCells; iY++) {
                int left = xOffset + iX * xStride - 1;
                int right = Math.min(left + xStride, width - 1);
                int top = yOffset + iY * yStride - 1;
                int bottom = Math.min(top + yStride, height - 1);

                int topLeft = (top * width + left) * descInfo.nBins;
                int topRight = (top * width + right) * descInfo.nBins;
                int bottomLeft = (bottom * width + left) * descInfo.nBins;
                int bottomRight = (bottom * width + right) * descInfo.nBins;

                for (int i = 0; i < descInfo.nBins; i++, iDesc++) {
                    double sumTopLeft = 0.0;
                    double sumTopRight = 0.0;
                    double sumBottomLeft = 0.0;
                    double sumBottomRight = 0.0;

                    if (top >= 0) {
                        if (left >= 0) {
                            sumTopLeft = descMat.desc[topLeft + i];
                        }
                        if (right >= 0) {
                            sumTopRight = descMat.desc[topRight + i];
                        }
                    }
                    if (bottom >= 0) {
                        if (left >= 0) {
                            sumBottomLeft = descMat.desc[bottomLeft + i];
                        }
                        if (right >= 0) {
                            sumBottomRight = descMat.desc[bottomRight + i];
                        }
                    }
                    float temp = (float) (sumBottomRight + sumTopLeft - sumBottomLeft - sumTopRight);
                    vec[iDesc] = Math.max(temp, 0.0f) + 0.05f;
                }
            }
        }

        if (descInfo.norm == 1) {
            return norm_1(vec);
        } else {
            return norm_2(vec);
        }
    }

    public static float[] norm_1(float[] input) {
        float[] retVal = new float[input.length];

        double sum = 0.0;
        for (int i = 0; i < input.length; i++) {
            sum += input[i];
        }
        sum += 0.0000000001;

        for (int i = 0; i < input.length; i++) {
            retVal[i] = (float) (input[i] / sum);
        }
        return retVal;
    }

    public static float[] norm_2(float[] input) {
        float[] retVal = new float[input.length];

        double sum = 0.0;
        for (int i = 0; i < input.length; i++) {
            sum += (input[i] * input[i]);
        }
        sum += 0.0000000001;
        sum = Math.sqrt(sum);

        for (int i = 0; i < input.length; i++) {
            retVal[i] = (float) (input[i] / sum);
        }
        return retVal;
    }

    public static Object[] isValid(CvPoint2D32f[] track) {
        float min_var = 1.732f;
        float max_var = 50;
        float max_dis = 20;

        float mean_x = 0;
        float mean_y = 0;
        float var_x = 0;
        float var_y = 0;
        float length = 0;

        int size = track.length;
        CvPoint2D32f[] bk = new CvPoint2D32f[size];
        for (int i = 0; i < size; i++) {
            bk[i] = new CvPoint2D32f();
            //mean_x += track[i].x();
            //mean_y += track[i].y();
            bk[i].x(track[i].x());
            bk[i].y(track[i].y());
            mean_x += bk[i].x();
            mean_y += bk[i].y();
        }

        mean_x /= size;
        mean_y /= size;

        for (int i = 0; i < size; i++) {
            float tmpX = bk[i].x();
            bk[i].x(tmpX - mean_x);
            var_x += (bk[i].x() * bk[i].x());

            float tmpY = bk[i].y();
            bk[i].y(tmpY - mean_y);
            var_y += (bk[i].y() * bk[i].y());
        }

        var_x /= size;
        var_y /= size;
        var_x = (float) Math.sqrt(var_x);
        var_y = (float) Math.sqrt(var_y);

        if (var_x < min_var && var_y < min_var) {
            return new Object[]{null, null, 0};
        }

        if (var_x > max_var || var_x > max_var) {
            return new Object[]{null, null, 0};
        }

        for (int i = 1; i < size; i++) {
            float temp_x = bk[i].x() - bk[i - 1].x();
            float temp_y = bk[i].y() - bk[i - 1].y();
            length += Math.sqrt(temp_x * temp_x + temp_y * temp_y);
            bk[i - 1].x(temp_x);
            bk[i - 1].y(temp_y);
        }

        float len_thre = length * 0.7f;
        for (int i = 0; i < size - 1; i++) {
            float temp_x = bk[i].x();
            float temp_y = bk[i].y();
            float temp_dis = (float) Math.sqrt(temp_x * temp_x + temp_y * temp_y);
            if (temp_dis > max_dis && temp_dis > len_thre) {
                return new Object[]{null, null, 0};
            }
        }

        CvPoint2D32f[] bk2 = Arrays.copyOfRange(bk, 0, size - 1);
        for (int i = 0; i < size - 1; i++) {
            float tmpX = bk2[i].x() / length;
            bk2[i].x(tmpX);

            float tmpY = bk2[i].y() / length;
            bk2[i].y(tmpY);
        }



        return new Object[]{bk2, new float[]{mean_x, mean_y, var_x, var_y, length}, 1};

    }

    public static void WriteTrajFeature2Txt(
            BufferedWriter oStream, float[] Track_Info, float[] XYs, List<Float> MBHfeature, float[] TRJfeature) throws IOException {
        for (int i = 0; i < Track_Info.length; i++) {
            oStream.write(Track_Info[i] + " ");
        }
        for (int i = 0; i < XYs.length; i++) {
            oStream.write(XYs[i] + " ");
        }
        for (int i = 0; i < MBHfeature.size(); i++) {
            oStream.write(MBHfeature.get(i) + " ");
        }
        for (int i = 0; i < TRJfeature.length; i++) {
            oStream.write(TRJfeature[i] + " ");
        }
        oStream.newLine();
    }
}
