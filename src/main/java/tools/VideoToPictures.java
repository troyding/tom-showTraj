package tools;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Tom.fu on 21/11/2014.
 */
public class VideoToPictures {

    public static void main2(String[] args) {

        opencv_highgui.VideoCapture camera = new opencv_highgui.VideoCapture(0);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!camera.isOpened()) {
            System.out.println("Camera Error");
        } else {
            System.out.println("Camera OK?");
        }

        int generatedFrames = 0;
        int targetCount = 25 * 1000;
        int fps = 25;

        String windowName = "test-camera";
        long start = System.currentTimeMillis();
        long last = start;
        opencv_highgui.namedWindow(windowName, opencv_highgui.WINDOW_AUTOSIZE);

        try {
            while (++generatedFrames < targetCount) {
                opencv_core.IplImage fk = new opencv_core.IplImage();

                opencv_core.Mat frame = new opencv_core.Mat();
                camera.read(frame);


                opencv_highgui.imshow(windowName, frame);
                opencv_highgui.waitKey(1);

                if (generatedFrames % fps == 0) {
                    long current = System.currentTimeMillis();
                    long elapse = current - last;
                    long remain = 1000 - elapse;
                    if (remain > 0) {
                        Thread.sleep(remain);
                    }
                    last = System.currentTimeMillis();
                    System.out.println("Current: " + last + ", elapsed: " + (last - start)
                            + ",totalSend: " + generatedFrames + ", remain: " + remain);
                } else {
//                    long current = System.currentTimeMillis();
//                    long elapse = current - last;
//                    long remain = 1000 - elapse;
//                    int remainCnt = generatedFrames % fps;
//                    long wait = remain / remainCnt;
//                    if (wait > 0) {
//                        Thread.sleep(wait);
//                    }
                    //System.out.println("elapsed: " + (last - start)
                    //        + ",totalSend: " + generatedFrames + ", remain: " + remain+ ", remainCnt: " + remainCnt + ", wait: " + wait);
                }
            }

            camera.release();
            opencv_highgui.destroyWindow(windowName);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //System.out.println("Frame Obtained");


//        System.out.println("Captured Frame Width " + frame.width());
//
//
//        List<Serializable.Rect> rectList = new ArrayList<>();
//
//        rectList.add(new Serializable.Rect(0, 0, 5, 10));
//        rectList.add(null);
//        rectList.add(new Serializable.Rect(0, 5, 5, 10));
//        rectList.add(null);
//
//        System.out.println(rectList.size());

    }

    public static void main3(String[] args) {

        List<Serializable.Rect> rectList = new ArrayList<>();

        double fx = .25, fy = .25;
        double fsx = .5, fsy = .5;

        int W = 728, H = 408;
        int w = (int) (W * fx + .5), h = (int) (H * fy + .5);
        int dx = (int) (w * fsx + .5), dy = (int) (h * fsy + .5);
        int patchCount = 0;
        int xCnt = 0;
        int yCnt = 0;
        for (int x = 0; x + w <= W; x += dx) {
            xCnt++;
        }
        for (int y = 0; y + h <= H; y += dy) {
            yCnt++;
        }

        System.out.println("xCnt: " + xCnt + ", yCnt: " + yCnt);

        List<Integer> testList = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            testList.add(i);
        }

        List<Integer> getFiltered = testList.stream().filter(id -> id % 4 == 0).collect(Collectors.toList());

        System.out.println(getFiltered);

    }

    public static void main(String[] args) {
        System.out.println("Hello");
        int frameId = 0;
        int firstFrameId = 34000;
        int lastFrameId = 36000;
        String SOURCE_FILE = "C:\\Users\\Tom.fu\\Desktop\\VLD\\Video\\1.mp4";
        String picSaveFolder = "C:\\Users\\Tom.fu\\Desktop\\VLD\\picture2\\";
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(SOURCE_FILE);
        int index = 0;

        System.out.println("Created capture: " + SOURCE_FILE);

        String windowName = "test";

        opencv_highgui.namedWindow(windowName, opencv_highgui.WINDOW_AUTOSIZE);
        try {
            grabber.start();
            while (++frameId < firstFrameId) {
                grabber.grab();
                //opencv_core.IplImage tmp = grabber.grab();
                //opencv_core.Mat tmpMat = new opencv_core.Mat(tmp);
                //System.out.println("w, h: " + tmp.width() + "," + tmp.height() + ", mat w, h: " + tmpMat.cols() + ", " + tmpMat.rows());
                //return;
            }

            while (frameId < lastFrameId) {

                opencv_core.IplImage frame = grabber.grab();
                opencv_core.Mat matOrg = new opencv_core.Mat(frame);
//                opencv_highgui.imshow(windowName, mat);
//                opencv_highgui.waitKey(1);

                opencv_core.Mat matNew = new opencv_core.Mat();
                opencv_core.Size size = new opencv_core.Size(640, 480);
                opencv_imgproc.resize(matOrg, matNew, size);

                //BufferedImage bufferedImage = frame.getBufferedImage();
                BufferedImage bufferedImage = matNew.getBufferedImage();
                String fileName = picSaveFolder + String.format("frame%06d.jpg", (index + 1));
                File initialImage = new File(fileName);
                ImageIO.write(bufferedImage, "JPEG", initialImage);
                System.out.println("frameID: " + frameId);
                Thread.sleep(50);
                frameId++;
                index++;
            }

            opencv_highgui.destroyWindow(windowName);
            grabber.release();

        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}