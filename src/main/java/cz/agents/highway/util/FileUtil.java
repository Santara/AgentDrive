package cz.agents.highway.util;


import cz.agents.highway.storage.Pair;

import java.util.*;
import java.io.*;


/**
 * Created by david on 10.3.15.
 */
public class FileUtil {
    private FileUtil(){}

    public void writeDistancesToFile(Map<Integer, Queue<Pair<Long,Float>>> distances)
    {
        String file_name = "list";
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        try {
            FileWriter fstream = new FileWriter(file_name);
            BufferedWriter out = new BufferedWriter(fstream);
            int index = 0;
            String addstring = "";
            Random rand = new Random();
            for (Map.Entry<Integer, Queue<Pair<Long,Float>>> entry : distances.entrySet())
            {
                if(index > 25)
                {
                    addstring +="A";
                    index = 0;
                }
                out.write(addstring + alphabet[index] + " = [");
                Queue<Pair<Long,Float>> way =  entry.getValue();
                Iterator<Pair<Long,Float>> itr = way.iterator();
                while (itr.hasNext()) {
                    Float element = (Float) itr.next().getValue();
                    out.write(element + " ");
                }
                out.write("]");
                out.newLine();
                out.write(addstring + alphabet[index] + "time" + " = [");
                itr = way.iterator();
                while (itr.hasNext()) {
                    Long element = itr.next().getKey();
                    out.write(element + " ");
                }
                out.write("]");
                index++;
                out.newLine();
            }
            out.write("figure;");
            out.newLine();
            out.write("hold on;");
            out.newLine();
            index = 0;
            addstring = "";
            for (Map.Entry<Integer, Queue<Pair<Long,Float>>> entry : distances.entrySet())
            {
                if(index > 25)
                {
                    addstring +="A";
                    index = 0;
                }
                float r = rand.nextFloat();
                float g = rand.nextFloat();
                float b = rand.nextFloat();
                out.write("plot("+addstring + alphabet[index] + "time" + "," + addstring + alphabet[index++] + ",'Color'," +"[" + r +" " + g + " " + b + "])");
                out.newLine();
            }
            out.write("title('Car distances to the junction')");
            out.newLine();
            out.write("ylabel('distance')");
            out.newLine();
            out.write("xlabel('Simulation time')");
            out.newLine();
            out.write("hold off;");
            out.close();
            System.out.println("File created successfully.");

        } catch (Exception e) { // TODO Improve this
            System.out.println(e.getMessage());
        }
    }

    public static FileUtil getInstance() {
        return FileUtilHolder.INSTANCE;
    }
    private static class FileUtilHolder {
        private static final FileUtil INSTANCE = new FileUtil();
    }
}

