package org.eclipse.rtp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class EC2ImageBuilder {

	private static final String AMAZON_ENDPOINT = "ec2.us-east-1.amazonaws.com";
	private static final String CONFIG_FILE = "credentials.properties";
	private AmazonEC2 ec2Client = null;
	private static Log LOG = LogFactory.getLog(EC2ImageBuilder.class);

	public void build() throws Exception {
		connectToEC2();

		Instance instance = createUbuntuInstance();
		waitUntilRunning(instance);

		String publicDNS = getPublicDNS(instance);

		UbuntuConfigurator configurator = new UbuntuConfigurator();
		configurator.configure(publicDNS);

		terminateInstance(instance);
	}

	private void terminateInstance(Instance instance) {
		TerminateInstancesRequest stopInstances = new TerminateInstancesRequest();
		List<String> instances = new ArrayList<String>();
		instances.add(instance.getInstanceId());
		stopInstances.setInstanceIds(instances);
		ec2Client.terminateInstances(stopInstances);
	}

	private String getPublicDNS(Instance instance) {
		DescribeInstancesRequest requestId = new DescribeInstancesRequest();
		List<String> instanceIds = new ArrayList<String>();
		instanceIds.add(instance.getInstanceId());
		requestId.setInstanceIds(instanceIds);

		DescribeInstancesResult describeInstances = ec2Client
				.describeInstances(requestId);
		return describeInstances.getReservations().get(0).getInstances().get(0)
				.getPublicDnsName();
	}

	private void connectToEC2() {
		AWSCredentials credentials = null;
		try {
			File propertiesFile = new File(CONFIG_FILE);
			credentials = new PropertiesCredentials(propertiesFile);
		} catch (IOException e1) {
			LOG.fatal("Credentials were not properly entered into "
					+ CONFIG_FILE); //$NON-NLS-1$
			System.exit(-1);
		}

		ec2Client = new AmazonEC2Client(credentials);
		ec2Client.setEndpoint(AMAZON_ENDPOINT);
	}

	private void waitUntilRunning(Instance instance)
			throws InterruptedException {
		boolean started = false;

		do {
			Thread.sleep(30000);
			DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();

			List<String> instanceIds = new ArrayList<String>();
			instanceIds.add(instance.getInstanceId());
			describeInstanceStatusRequest.setInstanceIds(instanceIds);

			DescribeInstanceStatusResult describeInstanceStatus = ec2Client
					.describeInstanceStatus(describeInstanceStatusRequest);

			if (describeInstanceStatus.getInstanceStatuses().size() == 1) {
				InstanceState instanceState = describeInstanceStatus
						.getInstanceStatuses().get(0).getInstanceState();

				if (instanceState.getCode() == 16) {
					started = true;
				}
			}
		} while (!started);
	}

	private Instance createUbuntuInstance() {
		RunInstancesRequest request = new RunInstancesRequest();
		request.setMinCount(1);
		request.setMaxCount(1);
		request.setInstanceType("m1.small");
		List<String> securityGroups = new ArrayList<String>();
		securityGroups.add("sg-7ccc5115");
		request.setSecurityGroupIds(securityGroups);
		request.setKeyName("rtp");
		request.setImageId("ami-b89842d1"); // TODO: make configurable

		RunInstancesResult runInstances = ec2Client.runInstances(request);
		return runInstances.getReservation().getInstances().get(0);
	}
}
