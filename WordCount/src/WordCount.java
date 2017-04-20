import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

    private static final Pattern p = Pattern.compile("[^a-zA-Z]");

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable>{

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String nextToken = itr.nextToken();
                if (!p.matcher(nextToken).find()) {// Valid word
                    if (nextToken.toLowerCase().startsWith("m") || nextToken.toLowerCase().startsWith("n") ||
                        nextToken.toLowerCase().startsWith("o") || nextToken.toLowerCase().startsWith("p") ||
                        nextToken.toLowerCase().startsWith("q")) {
                        Text word = new Text();
                        word.set(nextToken);
                        context.write(word, new IntWritable(1));
                    }
                }
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    // Partitioner Class
    public static class WordPartitioner extends Partitioner<Text, IntWritable> {
        @Override
        public int getPartition(Text key, IntWritable value, int numPartitions) {
            char prefix = key.toString().charAt(0);
            if (prefix == 'N' || prefix == 'n') {
                return 1 % numPartitions;
            } else if (prefix == 'O' || prefix == 'o') {
                return 2 % numPartitions;
            } else if (prefix == 'P' || prefix == 'p') {
                return 3 % numPartitions;
            } else if (prefix == 'Q' || prefix == 'q') {
                return 4 % numPartitions;
            }
            return 0;// 'M' || 'm'
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length < 2) {
            System.err.println("Usage: wordcount <in> [<in>...] <out>");
            System.exit(2);
        }
        // Define MapReduce job
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);

        // Set Mapper and Reducer classesCountNumUsersByStateDriver
        job.setPartitionerClass(WordPartitioner.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);

        // Output types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Set input and output location
        for (int i = 0; i < otherArgs.length - 1; ++i) {
            FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
        }
        FileOutputFormat.setOutputPath(job,
                new Path(otherArgs[otherArgs.length - 1]));

        // Submit job
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
