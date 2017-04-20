import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class GeneTypeKMeansReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
    @Override
    protected void reduce(Text key, Iterable<LongWritable> values, Context context)
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
