package org.eclipse.rtp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class UbuntuConfigurator {

	public void configure(String publicDNS) throws Exception {
		Session session = connectToInstance(publicDNS);
		executeShellScripts(session);

		// TODO: install rtp
		// TODO: rename user
		// TODO: create instance image and upload to ec2
	}

	private void executeShellScripts(Session session) throws IOException,
			JSchException {
		Channel channel = session.openChannel("shell");
		File shellScript = new File("scripts/ubuntu-scripts.txt");
		FileInputStream fin = new FileInputStream(shellScript);
		byte fileContent[] = new byte[(int) shellScript.length()];
		fin.read(fileContent);
		InputStream in = new ByteArrayInputStream(fileContent);
		channel.setInputStream(in);
		channel.setOutputStream(System.out);
		channel.connect();
	}

	private Session connectToInstance(String publicDNS) throws Exception {
		JSch jsch = new JSch();
		jsch.addIdentity("rtp.pem");
		Session session = jsch.getSession("ubuntu", publicDNS);
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();
		return session;
	}
}
