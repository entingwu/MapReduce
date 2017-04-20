import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;

import java.io.IOException;
import java.util.*;

/*public class BetaValueTopK extends Configured implements Tool {

    public static final int K = 20;
    private static final String NA = "NA";
    private static final String CG = "cg";
    private static final String CHR = "chr";
    private static final String X = "X";
    private static final String Y = "Y";

    public static class TopKMapper extends Mapper<LongWritable, Text, NullWritable, Text> {

        private TreeMap<Integer, Text> topKMap = new TreeMap<>();
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            // Extract info
            String[] tokens = value.toString().split("\t");
            // Filter out the head line..
            if (!tokens[0].subSequence(0, 2).equals("cg"))
                return;

            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].trim();
            }
            if (tokens[1].equals(NA)) return;
            if (tokens[5].equals(".")) return;
            // Beta value
            double numDouble = Double.parseDouble(tokens[1]);
            String geneSymbol = tokens[5].split(";")[0];
            context.write(new Text(geneSymbol), new DoubleWritable(numDouble));
        }
    }

    public static class TopKReducer extends Reducer<Text, Text, Text, Text> {

    }
}*/
