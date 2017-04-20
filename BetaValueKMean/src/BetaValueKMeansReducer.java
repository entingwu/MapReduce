import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class BetaValueKMeansReducer extends Reducer<IntWritable, LongWritable, IntWritable, LongWritable> {
    @Override
    protected void reduce(IntWritable key, Iterable<LongWritable> values, Context context)
            throws IOException, InterruptedException {
        long center = 0;
        int count = 0;
        for (LongWritable value : values) {
            center += value.get();
            count += 1;
        }
        if (count == 0) return;
        center = center / count;
        context.write(key, new LongWritable(center));
    }
}