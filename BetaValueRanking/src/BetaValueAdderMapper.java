import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class BetaValueAdderMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String text = value.toString();
        //Extract info
        String[] tokens = text.split("\t");
        //Filter out the head line..
        if (!tokens[0].subSequence(0, 2).equals("cg"))
            return;

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        if (tokens[1].equals("NA")) return;
        if (tokens[5].equals(".")) return;
        double numDouble = Double.parseDouble(tokens[1]);
        String geneSymbol = tokens[5].split(";")[0];
        System.out.println("@: " + geneSymbol);
        context.write(new Text(geneSymbol), new DoubleWritable(numDouble));
    }
}