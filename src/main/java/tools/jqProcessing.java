package tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * Created by Tom.fu on 21/4/2015.
 */
public class jqProcessing {

    public static void main(String[] args) {
        String inputFile = "C:\\Users\\Tom.fu\\Desktop\\VLD\\tmp.log";


        try {
            BufferedReader input = new BufferedReader(new FileReader(inputFile));

            String rdLine = null;
            String startPattern = "producerAdd: ";

            long lastSecond = -1;
            int cnt = 0;
            List<Integer> counts = new ArrayList<>();

            while ((rdLine = input.readLine()) != null) {
                JSONObject jo = new JSONObject(rdLine);

                String status = jo.getString("status");
                JSONObject jAllocation = jo.getJSONObject("currOptAllocation");

                //String t = ja.toString();
                System.out.println("Status: " + status);
                System.out.print("optAllo: ");
                printNode(jAllocation);
                JSONObject jContext = jo.getJSONObject("context");
                JSONObject jLatency = jContext.getJSONObject("latency");
                System.out.print("latency: ");
                printNode(jLatency);

                JSONObject jSpout = jContext.getJSONObject("spout");
                printNode(jSpout);
                JSONObject jBolt = jContext.getJSONObject("bolt");
                Set<String> boltKeys = jBolt.keySet();
                for (String boltKey : boltKeys) {
                    System.out.print(boltKey + "->");
                    printNode(jBolt.getJSONObject(boltKey));
                }
            }
            input.close();

        } catch (IOException ioE){
            ioE.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将Json对象转换成Map
     *
     * @return Map对象
     * @throws JSONException
     */
    public static void printNode(JSONObject obj) {
        obj.keySet().forEach(k-> System.out.print(k + ": " + obj.get(k).toString() + ", "));
        System.out.println();
    }
}
