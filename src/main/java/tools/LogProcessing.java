package tools;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom.fu on 21/4/2015.
 */
public class LogProcessing {

    public static void main(String[] args) {
        String inputFile = "C:\\Users\\Tom.fu\\Desktop\\VLD\\w1.txt";
        String outputFile = "C:\\Users\\Tom.fu\\Desktop\\VLD\\w1.xls";

        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
            BufferedReader input = new BufferedReader(new FileReader(inputFile));

            String rdLine = null;
            String startPattern = "producerAdd: ";

            long lastSecond = -1;
            int cnt = 0;
            List<Integer> counts = new ArrayList<>();

            while ((rdLine = input.readLine()) != null) {
                int startIndex = rdLine.indexOf(startPattern) + startPattern.length();
                int endIndex = startIndex + 10;
                String secondStr = rdLine.substring(startIndex, endIndex);
                String frameIDStr = rdLine.substring(startIndex + 14);

                long second = Long.valueOf(secondStr);
                int fID = Integer.valueOf(frameIDStr);

                if (second > lastSecond){
                    output.write(lastSecond + "\t" + cnt);
                    output.newLine();
                    counts.add(cnt);
                    cnt = 0;
                    lastSecond = second;
                }
                cnt ++;
            }
            output.write(lastSecond + "\t" + cnt);
            output.newLine();
            counts.add(cnt);

            int totalSec = 0;
            double sum = 0.0;
            double sum_2 = 0.0;
            for (int i = 11; i < counts.size() - 2; i ++){
                sum += counts.get(i);
                sum_2 += counts.get(i) * counts.get(i);
                totalSec ++;
            }

            double avg = sum / totalSec;
            double avg_2 = sum_2 / totalSec;
            double var = avg_2 - avg * avg;
            double std = Math.sqrt(var);
            System.out.println("totalSec: " + counts.size() + ", secCnt: " + totalSec + ", avg: " + avg + ", avg_2: " + avg_2
                    + ", var: " + var + ", std: " + std);

            input.close();
            output.close();
        } catch (IOException ioE){
            ioE.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
