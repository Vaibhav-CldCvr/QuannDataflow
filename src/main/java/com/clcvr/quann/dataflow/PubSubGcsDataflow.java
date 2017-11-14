package com.clcvr.quann.dataflow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.repackaged.org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Charsets;
import com.google.common.io.ByteSource;

import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;

import org.apache.beam.sdk.transforms.windowing.FixedWindows;	
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.compress.utils.IOUtils;

import java.util.Base64;


/**
 * @author cloudcover-vaibhav
 *
 */
public class PubSubGcsDataflow {
    
	static final int WINDOW_SIZE = 5;
	private static final Logger LOG = LoggerFactory.getLogger(PubSubGcsDataflow.class);

	/**
	 * Options supported by {@link PubSubGcsDataflow}.
	 *
	 * <p>Defining your own configuration options. Here, you can add your own arguments
	 * to be processed by the command-line parser, and specify default values for them. You can then
	 * access the options values in your pipeline code.
	 *
	 * <p>Inherits standard configuration options.
	 */
	public interface Options extends PipelineOptions{

		/**
		 * Set this required option to specify where to write the output.
		 */
		@Description("Path of the file to write to")
		@Required
		@Default.String("gs://quann-logs/output/")
		String getOutput();
		void setOutput(String value);

		@Description("Fixed window duration, in minutes")
		@Required
		@Default.Integer(WINDOW_SIZE)
		Integer getWindowSize();
		void setWindowSize(Integer value);
		
		@Description("Read PubSub Topic")
		@Required
		@Default.String("projects/project-lms-182110/subscriptions/apache-nifi")
		String getReadSubscriptionName();
		void setReadSubscriptionName(String value);
		
		@Description("Read PubSub Topic")
		@Required
		@Default.String("projects/project-lms-182110/subscriptions/apache-nifi-netflow")
		String getNetflowSubscriptionName();
		void setNetflowSubscriptionName(String value);
		
		
		@Description("Write PubSub Topic")
		@Required
		@Default.String("projects/project-lms-182110/topics/logstash")
		String getWriteTopicName();
		void setWriteTopicName(String value);
		
		@Description("Path of the file to write to")
		@Required
		@Default.String("gs://quann-logs/netflow/")
		String getNetflowOutput();
		void setNetflowOutput(String value);
		
	}

	/**
	 * @param bytes
	 * @throws IOException 
	 */
	public static String decompress(byte[] bytes) throws IOException {
		/**
		 * This method is used to Decompress input PubSub Logs 
		 * to Human Readable format.
		 */
		LOG.info("Found UnCompressed PubSub Logs " + bytes);
		String outStr = "";
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			
			BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bais);
			InputStreamReader reader = new InputStreamReader(bzis);
			BufferedReader in = new BufferedReader(reader);
			String readed;		
			while ((readed = in.readLine()) != null) {
				LOG.info("Found Compressed Logs " + readed);
			    outStr += readed;
			    System.out.println("outStr"+outStr);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return outStr;
		
	}
	

	static class LogsFn extends DoFn<PubsubMessage, String> {
		/**
		 * This is DoFn function that process one element
		 * at a time from input PCollection
		 */
		private static final long serialVersionUID = 1L;

		@ProcessElement
		public void processElement(ProcessContext c) throws IOException {
			LOG.info("Found context " + c);
			// Get the input element from ProcessContext.
			PubsubMessage message = c.element();
			// Use ProcessContext.output to emit the output element.
			
			String part = "";
			// only for decompression
			//part = PubSubGcsDataflow.decompress(message.getPayload());
		
			part = new String(message.getPayload());
			c.output(part);
		}
	}
	
	
	static class ConvertBinaryToString extends DoFn<PubsubMessage, String> {
		/**
		 * This is DoFn function that process one element
		 * at a time from input PCollection
		 */
		private static final long serialVersionUID = 1L;

		@ProcessElement
		public void processElement(ProcessContext c) throws IOException {
			LOG.info("Found context " + c);
			// Get the input element from ProcessContext.
			PubsubMessage message = c.element();
			// Use ProcessContext.output to emit the output element.
			System.out.println("Without Conversion"+message);
			String part = "";
			part = new String(message.getPayload());
			System.out.println("After Conversion"+part);
			c.output(part);
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException , Exception{

		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
		Pipeline pipeline = Pipeline.create(options);	
		
		/**
		 * Read logs coming from Cloud Pub Sub 
		 */
		PCollection<PubsubMessage> inputCollection = pipeline
				.apply("Read PaloAlto Logs from Pubsub",PubsubIO.readMessages().fromSubscription((options.getReadSubscriptionName())));
		
		PCollection<String> netFlowCollection = pipeline
				.apply("Read NetFlow Logs from Pubsub",PubsubIO.readStrings().fromSubscription((options.getNetflowSubscriptionName())));
		/**
		 * Logs Transformation  
		 */
		PCollection<String> stringCollection = inputCollection.apply("Raw to String",ParDo.of(new LogsFn()));	
		//PCollection<String> stringNetFlowCollection = netFlowCollection.apply("Binary to String",ParDo.of(new ConvertBinaryToString()));	
		/**
		 * Write to Pub Sub Topic
		*/
		stringCollection.apply("Write PaloAlto Logs to Elk",PubsubIO.writeStrings().to(options.getWriteTopicName()));		
		/**
		 * Window into fixed windows. The fixed window size for 5 minutes
		 * minute.
		 */
		netFlowCollection.apply("Applying Window",Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))
		.apply("Write to NetFlow Bucket",TextIO.write().to(options.getNetflowOutput()).withWindowedWrites().withNumShards(1));
		
		stringCollection.apply("Applying Window",Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))
		/**
		 * Write log files to Google Cloud Storage bucket
		 */		
		.apply("Write to Output Bucket",TextIO.write().to(options.getOutput()).
		   withWindowedWrites().withNumShards(1));
		
		pipeline.run();	


	}


}
