/*******************************************************************************
 * Copyright (c) 2009, 2018 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import com.kugou.sqlite.ProjectRecordDb;
import org.jacoco.cli.internal.Command;
import org.jacoco.core.internal.ContentTypeDetector;
import org.jacoco.core.internal.InputStreams;
import org.jacoco.core.internal.data.CRC64;
import org.kohsuke.args4j.Option;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The <code>version</code> command.
 */
public class MakeClassDb extends Command {
    private ProjectRecordDb mDb = null;
	boolean mIsRunning = true;
	final BlockingQueue<String> mDBQueue = new LinkedBlockingQueue<String>();
	@Option(name = "--classfiles", usage = "location of Java class files", metaVar = "<path>", required = true)
	List<File> classfiles = new ArrayList<File>();


	@Option(name = "--out", usage = "output file for the DbFile", metaVar = "<file>")
	File outFile;

	@Override
	public String description() { return "Save class id to db."; }
	public class WriteDbCustomer extends Thread {
		@Override
		public void run() {
			try {
				List<String> sqlvalues = new ArrayList<String>();
				for (;;) {
					String sqlvalue = mDBQueue.poll(2, TimeUnit.SECONDS);
					if (null!=sqlvalue){
						if(sqlvalues.size() >= 800){
							mDb.appendClassRecord(String.join(",", sqlvalues));
							sqlvalues = new ArrayList<String>();
						} else {
							sqlvalues.add(sqlvalue);
						}
						continue;
					}
					if (!mIsRunning){
						if(sqlvalues.size()>0){
							mDb.appendClassRecord(String.join(",", sqlvalues));
						}
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private int parseClassFile(final File file) throws IOException {
		int count = 0;
		if (file.isDirectory()) {
			for (final File f : file.listFiles()) {
				count += parseClassFile(f);
			}
		} else {
			final InputStream in = new FileInputStream(file);
			try {
				count += parseClassFile(in, file.getPath());
			} finally {
				in.close();
			}
		}
		return count;
	}

	private int parseClassFile(final InputStream input, final String location) {
		final ContentTypeDetector detector;
		try {
			detector = new ContentTypeDetector(input);
			if (detector.getType() == ContentTypeDetector.CLASSFILE) {
				InputStream input_stream = detector.getInputStream();
				final byte[] buffer = InputStreams.readFully(input_stream);
				ClassReader reader = new ClassReader(buffer);
				long classid = CRC64.classId(reader.b);
				String className = reader.getClassName();
				mDBQueue.offer(mDb.getAppendClassRecordValues(classid, className, location));
				return 1;
			}
		} catch (final IOException e) {
			return 0;
		}
		return 0;
	}


	@Override
	public int execute(final PrintWriter out, final PrintWriter err)
			throws IOException, ClassNotFoundException, SQLException{
		mDb = new ProjectRecordDb(outFile.getAbsolutePath());
		mDb.resetProjectClassRecord();
		mIsRunning = true;
		MakeClassDb.WriteDbCustomer writedbthread = new MakeClassDb.WriteDbCustomer();
		writedbthread.start();

		int count = 0;
		for (final File file : classfiles) {
			count += parseClassFile(file);
		}
		out.println("parse class count=" + count);
		System.out.println("[INFO] begin wait write db.");
		try {
			mIsRunning = false;
			writedbthread.join(3 * 60 * 1000 );
		} catch (Exception  e){
			e.printStackTrace();
		}
		mDb.destroyed();
		System.out.println("[INFO] end wait write db.");
		return 0;
	}

}
