package GenerateTraj;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * Created by Tom.fu on 28/11/2014.
 */
public class IplImagePyramid implements Cloneable {

    LinkedList<opencv_core.IplImage> imagePyramidList;
    double scaleFactor;
    int numScales;
    int depth;
    int numChannels;
    opencv_core.CvSize initSize;

    public IplImagePyramid(){
        this.scaleFactor = 0.0;
        this.numChannels = 0;
        this.numScales = 0;
        this.depth = 0;
        this.initSize.height(0);
        this.initSize.width(0);
        this.imagePyramidList = new LinkedList<>();
    }

    public IplImagePyramid(double scaleFactor, int numScales, opencv_core.CvSize initialSize, int depth, int numChannels){
        this.scaleFactor = scaleFactor;
        this.numScales = numScales;
        this.initSize = initialSize;
        this.depth = depth;
        this.numChannels = numChannels;

        if (this.imagePyramidList == null){
            this.imagePyramidList = new LinkedList<>();
        }
        this.imagePyramidList.addLast(cvCreateImage(initialSize, depth, numChannels));

        for (int i = 1; i < numScales; i ++){
            double newScaleFactor = Math.pow(scaleFactor, i);
            int newWidth = (int)Math.floor(initialSize.width() / newScaleFactor);
            int newHeight = (int)Math.floor(initialSize.height() / newScaleFactor);
            opencv_core.CvSize newSize = cvSize(newWidth, newHeight);
            this.imagePyramidList.addLast(cvCreateImage(newSize, depth, numChannels));
        }
    }

    @Override
    public IplImagePyramid clone(){
        IplImagePyramid c = new IplImagePyramid();
        c.scaleFactor = this.scaleFactor;
        c.numChannels = this.numChannels;
        c.numScales = this.numScales;
        c.depth = this.depth;
        c.initSize = this.initSize;
        this.imagePyramidList.forEach(e->{
            opencv_core.IplImage tmp = cvCloneImage(e);
            c.imagePyramidList.addLast(tmp);
        });
        return c;
    }

    public opencv_core.IplImage getImage(int index){
        return this.imagePyramidList.get(index);
    }

    public void rebuild(opencv_core.IplImage img){
        cvCopy(img, this.imagePyramidList.get(0));
        for (int i = 0; i < this.numScales; i ++){
            opencv_imgproc.cvResize(img, this.imagePyramidList.get(i), opencv_imgproc.CV_INTER_AREA);
        }
    }
}
