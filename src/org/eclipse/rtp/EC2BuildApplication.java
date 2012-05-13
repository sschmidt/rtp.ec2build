package org.eclipse.rtp;

public class EC2BuildApplication {
	public static void main(String[] args) throws Exception {
		EC2ImageBuilder builder = new EC2ImageBuilder();
		builder.build();
	}
}
