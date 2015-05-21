package tom; /**
 * Created by Tom.fu on 5/11/2014.
 */

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;

//import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_imgproc;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_highgui.cvDecodeImage;
import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import tools.Serializable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.Input;

public class ShowTraj {

    public static void main(String[] args) {
        // write your code here
//-Djava.library.path=C:\opencv\build\java\x64;C:\opencv\build\x64\vc12\bin;C:\opencvExtraLib

        //System.out.println("java.library.path: " + System.getProperty("java.library.path"));
        //System.out.println("java.class.path: " + System.getProperty("java.class.path"));

        String path = "C:\\Users\\Tom.fu\\Desktop\\fromPeiYong\\testdata2\\";

        //String file1 = path + "traj_bend_0001_trajectory_group_1.mat";
        //String file2 = path + "traj_bend_0001_trajectory_group_2.mat";
        String file3 = path + "group_ids_Chalearn_seq01.mat";
        //String trajFile = path + "traj_bend_0001.txt";
        //String trajFile = path + "traj_Seq01_color.txt";
        String trajFile = path + "output.txt";
        ArrayList<ArrayList<Float>> frameTraj = new ArrayList<ArrayList<Float>>();

        int sampleN = Integer.parseInt(args[0]);
        int seed = Integer.parseInt(args[1]);
        trajFile = path + args[2];

        try {
            /*
            MatFileReader mfr1 = new MatFileReader(file1);
            MLDouble mlData1 = (MLDouble) ((MLCell) mfr1.getMLArray("group_ids")).cells().get(0);
            ArrayList<Integer> group1 = getIndexArrayFromMLDouble(mlData1);

            MatFileReader mfr2 = new MatFileReader(file2);
            MLDouble mlData2 = (MLDouble) ((MLCell) mfr2.getMLArray("group_ids")).cells().get(0);
            ArrayList<Integer> group2 = getIndexArrayFromMLDouble(mlData2);

            int maxGroup1 = group1.stream().max(Integer::compare).get();
            ArrayList<Integer> group2New = (ArrayList<Integer>) group2.stream().map(item -> item + maxGroup1)
                    .collect(Collectors.toList());
            */
            ArrayList<Integer> groupIDs = new ArrayList<>();
            //groupIDs.addAll(group1);
            //groupIDs.addAll(group2);

            MatFileReader mfr3 = new MatFileReader(file3);
            MLDouble mlData3 = (MLDouble) ((MLCell) mfr3.getMLArray("group_ids")).cells().get(0);
            ArrayList<Integer> group3 = getIndexArrayFromMLDouble(mlData3, sampleN);
            groupIDs.addAll(group3);

            int maxGroupID = groupIDs.stream().max(Integer::compare).get();
            //System.out.println("maxGroup1: " + maxGroup1 + ", maxGroupID: " + maxGroupID);
            //System.out.println("group1Cnt: " + group1.size() + ", group2Cnt: "
            //        + group2.size() + ", groups: " + groupIDs.size());

            System.out.println("group3Cnt: " + group3.size() + ", groups: " + groupIDs.size());

            //ArrayList<int[]> groupColor = getRandomColor(maxGroupID, 3);
            ArrayList<int[]> groupColor = getPseudoRandomColor(maxGroupID, 3, seed);

            BufferedReader reader = new BufferedReader(new FileReader(trajFile));
            String rdLine= null;
            int lineCnt = 0;
            while ((rdLine = reader.readLine())!=null){
                if (lineCnt % sampleN == 0) {
                    ArrayList<String> rdLineResults = new ArrayList<> (Arrays.asList(rdLine.split(" ")));
                    ArrayList<Float> rdResults = (ArrayList<Float>) rdLineResults.stream().map(item -> Float.valueOf(item))
                            .collect(Collectors.toList());
                    frameTraj.add(rdResults);
                }
                lineCnt ++;
            }
            reader.close();

            int maxFrameID = frameTraj.stream().mapToInt(item->item.get(0).intValue()).reduce(Integer::max).getAsInt();

            System.out.println("frameTrajCnt: " + frameTraj.size() + ", maxFrameID: " + maxFrameID);

            Long startTime = System.currentTimeMillis();

            for (int i = 0; i < maxFrameID; i ++){
                System.out.println("frameID: " + i);
                //load image
                //img = cv2.imread('testdata/frame%06d.jpg' % i)
                //img = cv2.resize(img, (640, 480))
                //String fileName = path + "testdata\\" + String.format("frame%06d.jpg", (i+1));
                String fileName = path + "Seq01_color\\" + String.format("frame%06d.jpg", (i+1));
                File f = new File(fileName);
                if (f.exists() == false) {
                    System.out.println("File not exist");}
                System.out.println(fileName);


                ///Must call IplImage first!!!
                opencv_core.IplImage imageFK = cvLoadImage(fileName);
                opencv_core.Mat matOrg = opencv_highgui.imread(fileName, opencv_highgui.CV_LOAD_IMAGE_COLOR);

                Serializable.Mat sMatOrg = new Serializable.Mat(matOrg);
                byte[] data = sMatOrg.toByteArray();
                Serializable.Mat sMatDst = new Serializable.Mat(data);
//
//                BufferedImage bufferedImage = matOrg.getBufferedImage();
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ImageIO.write(bufferedImage, "JPEG", baos);
//
//                byte[] imgBytes = baos.toByteArray();
//                ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imgBytes));
//                BufferedImage img = ImageIO.read(iis);
//
//                opencv_core.IplImage image = opencv_core.IplImage.createFrom(img);

//                byte[] data = new byte[imageFK.arraySize()];
//                imageFK.getByteBuffer().get(data);
//
//                opencv_core.IplImage image = opencv_core.IplImage.create(imageFK.width(), imageFK.height(), imageFK.depth(), imageFK.nChannels());
//                image.getByteBuffer().put(data);


                //opencv_core.IplImage flow = cvCreateImage(cvGetSize(grey_temp), IPL_DEPTH_32F, 2);
                //opencv_core.IplImage image = cvDecodeImage(cvMat(1, imgBytes.length, CV_8UC1, new BytePointer(imgBytes)));

                //opencv_core.Mat mat = new opencv_core.Mat(image);
                opencv_core.Mat mat = sMatDst.toJavaCVMat();
                opencv_core.Mat matNew = new opencv_core.Mat();
                opencv_core.Size size = new opencv_core.Size(640, 480);
                opencv_imgproc.resize(matOrg, matNew, size);
                //opencv_imgproc.resize(mat, matNew, size);

                for (int j = 0; j < 15; j ++){
                    int endFrameID = i + j;
                    int duration = 16 - j;

                    ArrayList<Integer> selectedFrameIndex = (ArrayList<Integer>)frameTraj.stream()
                            .filter(item -> item.get(0).intValue() == endFrameID).map(item->frameTraj.indexOf(item))
                            .collect(Collectors.toList());

                    for (int k : selectedFrameIndex) {
                        int gid = groupIDs.get(k);

                        int x_prev = frameTraj.get(k).get(7).intValue();
                        int y_prev = frameTraj.get(k).get(8).intValue();

                        for (int m = 1; m < duration; m ++){
                            int x_curr = frameTraj.get(k).get(7 + m * 2).intValue();
                            int y_curr = frameTraj.get(k).get(8 + m * 2).intValue();

                            //opencv_core.line(finalImage, new opencv_core.Point((int) Q[i][0], (int) Q[i][1]),
                                    //new opencv_core.Point((int) Q[(i + 1) % 4][0], (int) Q[(i + 1) % 4][1]), color, 4, 4, 0);
                            opencv_core.line(matNew,
                                    new opencv_core.Point(x_prev, y_prev),
                                    new opencv_core.Point(x_curr, y_curr),
                                    new opencv_core.Scalar(
                                            groupColor.get(gid-1)[0],
                                            groupColor.get(gid-1)[1],
                                            groupColor.get(gid-1)[2], 0));
                            x_prev = x_curr;
                            y_prev = y_curr;
                        }
                    }
                }

                opencv_highgui.namedWindow( "test", opencv_highgui.WINDOW_AUTOSIZE );
                opencv_highgui.imshow("test", matNew);
                opencv_highgui.waitKey(1);
            }
            Long endTime = System.currentTimeMillis();
            System.out.println("startTime: " + startTime + ", endTime: " + endTime + ", duration: " + (endTime - startTime));

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    static ArrayList<Integer> getIndexArrayFromMLDouble(MLDouble input, int sampleN) {
        double[][] temp = input.getArray();
        ArrayList<Integer> ret = new ArrayList<>();
        for (int i = 0; i < temp.length; i++) {
            if (i % sampleN == 0) {
                ret.add((int) temp[i][0]);
            }
        }
        return ret;
    }

    static ArrayList<int[]> getRandomColor(int maxID, int dim) {
        Random rnd = new Random(System.currentTimeMillis());
        ArrayList<int[]> ret = new ArrayList<>();
        for (int i = 0; i < maxID; i++) {
            int[] rgbRnd = new int[dim];
            for (int j = 0; j < dim; j++) {
                rgbRnd[j] = (int) (255 * rnd.nextDouble());
            }
            ret.add(rgbRnd);
        }
        return ret;
    }

    static ArrayList<int[]> getPseudoRandomColor(int maxID, int dim, int seed) {
        //Random rnd = new Random(System.currentTimeMillis());
        Random rnd = new Random(seed);
        ArrayList<int[]> ret = new ArrayList<>();
        for (int i = 0; i < maxID; i++) {
            int[] rgbRnd = new int[dim];
            for (int j = 0; j < dim; j++) {
                rgbRnd[j] = (int) (255 * rnd.nextDouble());
                //rgbRnd[j] = (i + dim * i + i * 119 + 255 * j / dim) % 255;
            }
            ret.add(rgbRnd);
        }
        return ret;
    }

}
