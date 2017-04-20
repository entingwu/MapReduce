import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class BetaValueKMeansMain {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        String inputFile = otherArgs[0];
        System.out.print("Input File: " + inputFile + "\n");
        // File to store clustering results for each iteration.
        String basePath = otherArgs[1];

        int iterationCount = Integer.valueOf(otherArgs[2]);
        System.out.print("Iteration: " + iterationCount + "\n");
        System.out.print("Output the path: " + basePath);
        int iteration = 0;

        Job job = null;
        while (iterationCount > 0) {
            conf = new Configuration();
            conf.set("CENPATH", basePath + "/clustering/depth_" + (iteration - 1) + "/");
            job = new Job(conf);
            job.setJobName("KMeans Clustering " + iteration);
            job.setMapperClass(BetaValueKMeansMapper.class);
            job.setReducerClass(BetaValueKMeansReducer.class);
            job.setJarByClass(BetaValueKMeansMain.class);

            Path in = new Path(inputFile);
            Path out = new Path(basePath + "/clustering/depth_" + iteration);

            FileInputFormat.addInputPath(job, in);

            FileSystem fs2 = out.getFileSystem(conf);// FileSystem.get(out.toUri().getPath(), conf);
            if (fs2.exists(out)) {
                System.out.print("file exists deleting \n");
                fs2.delete(out, true);
            }
            FileOutputFormat.setOutputPath(job, out);
            job.setOutputKeyClass(IntWritable.class);
            job.setOutputValueClass(LongWritable.class);
            job.waitForCompletion(true);
            iterationCount--;
            iteration++;
        }
        if (job != null) {
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        }
    }
}
