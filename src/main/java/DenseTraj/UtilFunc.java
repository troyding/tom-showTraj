package DenseTraj;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_video;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * Created by Tom.fu on 24/2/2015.
 */
public class UtilFunc {

    public static RectInfo getRect(opencv_core.Point2f point, int width, int height, DescInfo descInfo) {
        int x_min = descInfo.width / 2;
        int y_min = descInfo.height / 2;
        int x_max = width - descInfo.width;
        int y_max = height - descInfo.height;

        int x = Math.min(Math.max(opencv_core.cvRound(point.x()) - x_min, 0), x_max);
        int y = Math.min(Math.max(opencv_core.cvRound(point.y()) - y_min, 0), y_max);

        return new RectInfo(x, y, descInfo.width, descInfo.height);
    }

    public static Object[] InitPry(opencv_core.Mat frame, int init_scale_num, int patch_size, float scale_stride) {
        int rows = frame.rows();
        int cols = frame.cols();
        float min_size = Math.min(rows, cols);

        int nlayers = 0;
        while (min_size >= patch_size) {
            min_size /= scale_stride;
            nlayers++;
        }

        if (nlayers == 0) {
            nlayers = 1;
        }

        int sNum = Math.min(init_scale_num, nlayers);
        float[] scales = new float[sNum];
        opencv_core.Size[] sizes = new opencv_core.Size[sNum];

        scales[0] = 1.0f;
        sizes[0] = new opencv_core.Size(cols, rows);

        for (int i = 1; i < sNum; i++) {
            scales[i] = scales[i - 1] * scale_stride;
            sizes[i] = new opencv_core.Size(opencv_core.cvRound(cols / scales[i]), opencv_core.cvRound(rows / scales[i]));
        }

        return new Object[]{sNum, scales, sizes};
    }

    public static opencv_core.Mat[] BuildPry(opencv_core.Size[] sizes, int type) {
        int nlayers = sizes.length;
        opencv_core.Mat[] pyr = new opencv_core.Mat[nlayers];

        for (int i = 0; i < nlayers; i++) {
            pyr[i].create(sizes[i], type);
        }

        return pyr;
    }

    public static LinkedList<opencv_core.Point2f> DenseSample(
            opencv_core.Mat grey,
            LinkedList<opencv_core.Point2f> points_in,
            double quality,
            int min_distance) {
        LinkedList<opencv_core.Point2f> points_out = new LinkedList<>();

        int width = grey.cols() / min_distance;
        int height = grey.rows() / min_distance;

        opencv_core.Mat eig = null;
        ///cornerMinEigenVal(Mat src, Mat dst, int blockSize, int ksize, int borderType)
        opencv_imgproc.cornerMinEigenVal(grey, eig, 3, 3, opencv_imgproc.BORDER_DEFAULT);

        double[] maxVal = new double[1];
        maxVal[0] = 0.0;

        //TODO: here need to be careful check with original c++ code.
        //minMaxLoc(eig, 0, &maxVal);
        opencv_core.minMaxLoc(eig, null, maxVal, null, null, null);
        //cvMinMaxLoc(eig, 0, &maxVal, 0, 0, 0);
        double threshold = maxVal[0] * quality;

        int[] counters = new int[width * height];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
        }

        int x_max = min_distance * width;
        int y_max = min_distance * height;

        for (int i = 0; i < points_in.size(); i++) {
            opencv_core.Point2f point = points_in.get(i);

            int x = opencv_core.cvFloor(point.x());
            int y = opencv_core.cvFloor(point.y());

            if (x >= x_max || y >= y_max) {
                //System.out.println("skip: i: " + i + ", ywx: " + ywx);
                continue;
            }

            x /= min_distance;
            y /= min_distance;

            int ywx = y * width + x;
            counters[ywx]++;
        }

        int index = 0;
        int offset = min_distance / 2;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++, index++) {
                if (counters[index] == 0) {
                    int x = j * min_distance + offset;
                    int y = i * min_distance + offset;

                    FloatBuffer floatBuffer = eig.createBuffer(y * eig.arrayWidth());
                    float ve = floatBuffer.get(x);
                    if (ve > threshold) {
                        points_out.add(new Point2f(x, y));
                    }
                }
            }
        }
        return points_out;
    }
}
