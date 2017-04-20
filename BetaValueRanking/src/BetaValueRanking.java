import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

public class BetaValueRanking {

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        // I. Count
        Job job = new Job();
        job.setJarByClass(BetaValueRanking.class);
        Path in = new Path(args[0]);
        // Here is the location of temp file
        Path temp = new Path(args[2]);

        Configuration conf = new Configuration();
        FileInputFormat.addInputPath(job, in);
        FileSystem fs1 = temp.getFileSystem(conf);
        if (fs1.exists(temp)) {
            fs1.delete(temp, true);
        }
        FileOutputFormat.setOutputPath(job, temp);

        job.setMapperClass(BetaValueAdderMapper.class);
        job.setReducerClass(BetaValueAdderReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        job.waitForCompletion(false);

        // II. Top K
        System.out.println("Top K: ");
        Job jobSort = new Job(conf, "TopK");
        jobSort.setJarByClass(BetaValueTopK.class);
        FileInputFormat.addInputPath(jobSort, temp);

        Path out = new Path(args[1]);
        FileSystem fs2 = out.getFileSystem(conf);
        if (fs2.exists(out)) {
            fs2.delete(out, true);
        }
        FileOutputFormat.setOutputPath(jobSort, out);

        jobSort.setMapperClass(BetaValueTopK.TopKMapper.class);
        jobSort.setReducerClass(BetaValueTopK.TopKReducer.class);
        jobSort.setNumReduceTasks(1);

        jobSort.setInputFormatClass(SequenceFileInputFormat.class);
        jobSort.setOutputKeyClass(DoubleWritable.class);
        jobSort.setOutputValueClass(Text.class);

        jobSort.setSortComparatorClass(DoubleWritable.Comparator.class);
        jobSort.waitForCompletion(false);
    }
} 
