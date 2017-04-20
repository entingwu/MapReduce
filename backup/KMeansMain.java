import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class KMeansMain  {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        String inputFile = otherArgs[1];
        // file to store clustering results for each iteration.
        String basePath = otherArgs[2];
        int iterationCount = Integer.valueOf(otherArgs[3]);
        System.out.print("Output the path " + basePath);

        int iteration = 0;
        conf.set("num.iteration", iteration + "");

        Path in = new Path(inputFile);
        conf.set("CENPATH", basePath + "/clustering/depth_"
                + (iteration - 1) + "/" + "part-r-00000");

        Path out = new Path(basePath + "/clustering/depth_0");

        Job job = new Job(conf);
        job.setJobName("KMeansPrepare Clustering");

        job.setMapperClass(KMeansMapper.class);
        job.setReducerClass(KMeansReducer.class);
        job.setJarByClass(KMeansMain.class);

        FileInputFormat.addInputPath(job, in);
        FileSystem fs = FileSystem.get(conf);

        if (fs.exists(out))
            fs.delete(out, true);

        FileOutputFormat.setOutputPath(job, out);
        // job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(LongWritable.class);
        job.waitForCompletion(true);

        System.out.print("First job done");

        iteration++;
        while (iterationCount > 0)
        {
            conf = new Configuration();
            conf.set("CENPATH", basePath + "/clustering/depth_"
                    + (iteration - 1) + "/" + "part-r-00000");
            conf.set("num.iteration", iteration + "");
            //String nextPath="CENPATH", BASE_PATH + "/clustering/depth_" + (iteration - 1) + "/" + "part-r-00000/";
            //KMeansMapper.
            //conf.set("DICTPATH", DATA_PATH);
            job = new Job(conf);
            job.setJobName("KMeans Clustering " + iteration);

            job.setMapperClass(KMeansMapper.class);
            job.setReducerClass(KMeansReducer.class);
            job.setJarByClass(KMeansMain.class);

            in = new Path(inputFile);
            out = new Path(basePath + "/clustering/depth_" + iteration);

            FileInputFormat.addInputPath(job, in);
            if (fs.exists(out))
                fs.delete(out, true);

            FileOutputFormat.setOutputPath(job, out);
            if(iterationCount!=1)
            {
                job.setOutputFormatClass(SequenceFileOutputFormat.class);
            }
            job.setOutputKeyClass(IntWritable.class);
            job.setOutputValueClass(LongWritable.class);

            job.waitForCompletion(true);
            iterationCount--;
            iteration++;
        }
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
