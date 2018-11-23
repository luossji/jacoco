package org.jacoco.cli.internal.commands;

import com.kugou.sqlite.ProjectRecordDb;
import org.jacoco.cli.internal.Command;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The <code>report</code> command.
 */
public class KugouReport extends Command {
    ProjectRecordDb mDb = null;
	boolean mIsRunning = true;
	final BlockingQueue<String> mDBQueue = new LinkedBlockingQueue<String>();

	@Argument(usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
	List<File> execfiles = new ArrayList<File>();

	@Option(name = "--classDbFile", usage = "location of Java class files", metaVar = "<file>", required = true)
    File classDbFile;

	@Option(name = "--execListFile", usage = "location of Java class files", metaVar = "<file>", required = true)
	File execListFile;

	@Option(name = "--sourcefiles", usage = "location of the source files", metaVar = "<path>")
	List<File> sourcefiles = new ArrayList<File>();

	@Option(name = "--encoding", usage = "source file encoding (by default platform encoding is used)", metaVar = "<charset>")
	String encoding;

	@Option(name = "--tabwith", usage = "tab stop width for the source pages (default 4)", metaVar = "<n>")
	int tabwidth = 4;

	@Override
	public String description() { return "分析覆盖率文件."; }
	public class WriteDbCustomer extends Thread {
		@Override
		public void run() {
			try {
				List<String> sqlvalues = new ArrayList<String>();
				for (;;) {
					String sqlvalue = mDBQueue.poll(2, TimeUnit.SECONDS);
					if (null!=sqlvalue){
						if(sqlvalues.size() >= 800){
							mDb.appendCoverageResultRecord(String.join(",", sqlvalues));
							sqlvalues = new ArrayList<String>();
						} else {
							sqlvalues.add(sqlvalue);
						}
						continue;
					}
					if (!mIsRunning){
						if(sqlvalues.size()>0){
							mDb.appendCoverageResultRecord(String.join(",", sqlvalues));
						}
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int execute(final PrintWriter out, final PrintWriter err)
			throws IOException, ClassNotFoundException, SQLException {
        mDb = new ProjectRecordDb(classDbFile.getAbsolutePath());
        mDb.resetCoverageResultTable();
		final ExecFileLoader loader = loadExecutionData(out);
		ExecutionDataStore data = loader.getExecutionDataStore();
		HashMap<String, String> hm = mDb.getAllClassPath();
		System.out.println("[INFO] finish init");

		mIsRunning = true;
		WriteDbCustomer writedbthread = new WriteDbCustomer();
		writedbthread.start();

        final Analyzer analyzer = new Analyzer(data, new CoverageSaver());
		for(ExecutionData ed : data.getContents()){
		    String classFile = hm.get(String.valueOf(ed.getId()));
		    if (classFile != null){
		    	try {
					analyzer.analyzeAll(new File(classFile));
				} catch (Exception e){
		    		e.printStackTrace();
				}
            } else {
				System.out.println(String.format("[ERROR]can not find class file %s, %s", ed.getId(), ed.getName()));
            }
		}

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

	private ExecFileLoader loadExecutionData(final PrintWriter out)
			throws IOException {
		final ExecFileLoader loader = new ExecFileLoader();
		FileReader fr = new FileReader(execListFile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line=br.readLine())!=null) {
			if(line.length() == 0){
				continue;
			}
			System.out.println(String.format("[INFO] Loading execution data file %s.",  line));
			loader.load(new File(line));
		}
		br.close();
		fr.close();

		return loader;
	}

    private class CoverageSaver implements ICoverageVisitor {
        public void visitCoverage(final IClassCoverage coverage) {
        	String srcFile = String.format("%s/%s", coverage.getPackageName(), coverage.getSourceFileName());
            for (final Iterator<IMethodCoverage> i = coverage.getMethods()
                    .iterator(); i.hasNext();) {
                IMethodCoverage method = i.next();
				List covLines = new ArrayList<String>();
                for (int nr = method.getFirstLine(); nr <= method
                        .getLastLine(); nr++) {
                    final ILine line = method.getLine(nr);
                    int lineStatus = line.getStatus();
                    if(lineStatus != ICounter.EMPTY && lineStatus != ICounter.NOT_COVERED){
						covLines.add(String.valueOf(nr));
                    }
                }
                if (!covLines.isEmpty()){ // 有覆盖率才记录
					String sql = mDb.getAppendCoverageResultRecordValues(coverage.getId(), srcFile,method.getName()+method.getDesc(),
							method.getFirstLine(), method.getLastLine(), String.join(",", covLines));
					mDBQueue.offer(sql);
                }
            }

        }
    }
}
