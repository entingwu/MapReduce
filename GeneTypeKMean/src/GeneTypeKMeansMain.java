import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class GeneTypeKMeansMain {

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        String inputFile = otherArgs[0];
        // File to store clustering results for each iteration.
        String basePath = otherArgs[1];

        int iterationCount = Integer.valueOf(otherArgs[2]);
        System.out.print("Output the path " + basePath);
        int iteration = 0;

        Job job = null;
        while (iterationCount > 0) {
            conf = new Configuration();
            conf.set("CENPATH", basePath + "/clustering/depth_" + (iteration - 1) + "/");
            conf.set("TOP_TEN_GENE_TYPES",
                    "PRDM16;RPS6KA2;MYT1L;TRIM26;TRIO;B3GNTL1;OPCML;TAP2;CTBP1;BRSK2;OSBPL5;HERC2;CCDC88C;SPG7;HIPK2;SPTBN1;TUBGCP2;PRDM15");

            job = new Job(conf);
            job.setJobName("KMeans Clustering");
            job.setMapperClass(GeneTypeKMeansMapper.class);
            job.setReducerClass(GeneTypeKMeansReducer.class);
            job.setJarByClass(GeneTypeKMeansMain.class);

            Path in = new Path(inputFile);
            Path out = new Path(basePath + "/clustering/depth_" + iteration);
            FileInputFormat.addInputPath(job, in);

            FileSystem fs = out.getFileSystem(conf);
            if (fs.exists(out)) {
                fs.delete(out, true);
            }
            FileOutputFormat.setOutputPath(job, out);
            //if(iterationCount!=1) {
            //    job.setOutputFormatClass(SequenceFileOutputFormat.class);
            //}
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(LongWritable.class);
            job.waitForCompletion(true);
            iterationCount--;
            iteration++;
        }
        if (job != null) {
            System.exit(job.waitForCompletion(true)? 0 : 1);
        }
    }
}
