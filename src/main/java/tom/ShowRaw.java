package tom; /**
 * Created by Tom.fu on 5/11/2014.
 */

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_imgproc;
import tools.Serializable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;

//import org.bytedeco.javacpp.BytePointer;

public class ShowRaw {

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

        int maxFrameID = Integer.parseInt(args[0]);
        try {
            Long startTime = System.currentTimeMillis();

            for (int i = 0; i < maxFrameID; i++) {
                System.out.println("frameID: " + i);
                //load image
                //img = cv2.imread('testdata/frame%06d.jpg' % i)
                //img = cv2.resize(img, (640, 480))
                //String fileName = path + "testdata\\" + String.format("frame%06d.jpg", (i+1));
                String fileName = path + "Seq01_color\\" + String.format("frame%06d.jpg", (i + 1));
                File f = new File(fileName);
                if (f.exists() == false) {
                    System.out.println("File not exist");
                }
                System.out.println(fileName);


                ///Must call IplImage first!!!
                IplImage imageFK = cvLoadImage(fileName);
                Mat matOrg = opencv_highgui.imread(fileName, opencv_highgui.CV_LOAD_IMAGE_COLOR);

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
                Mat mat = sMatDst.toJavaCVMat();
                //Mat mat = sMatOrg.toJavaCVMat();
                Mat matNew = new Mat();
                Size size = new Size(640, 480);
                //opencv_imgproc.resize(matOrg, matNew, size);
                opencv_imgproc.resize(mat, matNew, size);


                opencv_highgui.namedWindow("test", opencv_highgui.WINDOW_AUTOSIZE);
                opencv_highgui.imshow("test", matNew);
                opencv_highgui.waitKey(1);
            }
            Long endTime = System.currentTimeMillis();
            System.out.println("startTime: " + startTime + ", endTime: " + endTime + ", duration: " + (endTime - startTime));

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }
}
