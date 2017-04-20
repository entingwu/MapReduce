import java.io.IOException;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;

public class DistributionCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String line = value.toString();
        // Extract info
        String[] tokens = line.split("\\s");
        // Filter out the head line..
        // int addValue;
        if (!tokens[0].subSequence(0, 2).equals("cg")) return;
        // if(key.get()==0)
        //     return;

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        // Beta value
        if (tokens[1].equals("NA")) return;
        // divide [0-1] for 100 intervals, The interval falling in is just multiply by 100.
        double numDouble = Double.parseDouble(tokens[1]);
        int interval = (int) (numDouble * 100.0);
        context.write(new Text(Integer.toString(interval)), new IntWritable(1));
    }
}
