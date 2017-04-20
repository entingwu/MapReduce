/*
import com.opencsv.CSVParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;

public class HPopulateR {

    private static final CSVParser parser = new CSVParser(',', '"');
    private static final String TABLE_NAME = "AirlineDelay";
    private static final String COLUMN_FAMILY = "Airline";
    private static final String CANCELLED = "cancelled";
    private static final String AIR_DELAY_MINUTES = "airDelayMinutes";
    private static final String YEAR = "year";
    private static final String DIV = "$";

    public static class HPopulateMapper extends
            Mapper<Object, Text, ImmutableBytesWritable, Writable> {

        private HTable table = null;
        public void setup(Context context) throws IOException {
            Configuration hbConf = HBaseConfiguration.create();
            table = new HTable(hbConf, TABLE_NAME);
            table.setAutoFlush(false);
            table.setWriteBufferSize(204800);
        }

        public void map(Object object, Text value, Context context)
                throws IOException, InterruptedException {
            String[] entry = parser.parseLine(value.toString());

            if (entry.length >= 1) {
                // airlineName month origin flightNum
                String rowKey = entry[6] + DIV + entry[2] + DIV + entry[11] + DIV + entry[10];
                String cancelled = entry[41];
                String year = entry[0];
                double airDelayMinutes = Double.parseDouble(entry[37]);

                byte[] rowBytes = Bytes.toBytes(rowKey);
                Put putEntry = new Put(rowBytes);
                putEntry.add(COLUMN_FAMILY.getBytes(), CANCELLED.getBytes(), cancelled.getBytes());
                putEntry.add(COLUMN_FAMILY.getBytes(), AIR_DELAY_MINUTES.getBytes(),
                        String.valueOf(airDelayMinutes).getBytes());
                putEntry.add(COLUMN_FAMILY.getBytes(), YEAR.getBytes(), year.getBytes());
                table.put(putEntry);
                System.out.print("added one row \n");
            }
        }

        public void cleanup(Context context) throws IOException, InterruptedException {
            table.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (args.length < 2) {
            System.err.println("Usage: HPopulate <in> <out>");
            System.exit(2);
        }

        // Create table
        HBaseAdmin admin = new HBaseAdmin(HBaseConfiguration.create());
        if (admin.tableExists(TABLE_NAME)) {
            System.out.print("table exsits \n");
            admin.disableTable(TABLE_NAME);
            admin.deleteTable(TABLE_NAME);
        }

        HTableDescriptor htd = new HTableDescriptor(TABLE_NAME);
        HColumnDescriptor hcd = new HColumnDescriptor(COLUMN_FAMILY);
        htd.addFamily(hcd);
        admin.createTable(htd);
        admin.close();

        System.out.print("table created \n");

        // Define MapReduce job
        Job job = new Job(conf, "HPopulate");
        job.setJarByClass(HPopulateR.class);
        job.setNumReduceTasks(0);

        // Set Mapper and Output types
        job.setMapperClass(HPopulateMapper.class);
        job.setOutputKeyClass(ImmutableBytesWritable.class);
        job.setOutputValueClass(Put.class);
        job.setOutputFormatClass(TableOutputFormat.class);
        job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, TABLE_NAME);

        // Submit job
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
*/
