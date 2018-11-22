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
package org.jacoco.core;

import static org.junit.Assert.assertNotNull;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.*;
import org.jacoco.report.html.HTMLFormatter;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Unit tests for {@link JaCoCo}.
 */
public class JaCoCoTest {
	private class Printer implements ICoverageVisitor {

		private final PrintWriter out;

		Printer(final PrintWriter out) {
			this.out = out;
			out.println("  INST   BRAN   LINE   METH   CXTY   ELEMENT");
		}

		public void visitCoverage(final IClassCoverage coverage) {
			final String desc = String.format("class 0x%016x %s",
					Long.valueOf(coverage.getId()), coverage.getName());
			printDetails(desc, coverage);
			for (final Iterator<IMethodCoverage> i = coverage.getMethods()
					.iterator(); i.hasNext();) {
				printMethod(i.next(), i.hasNext());
			}
		}


		private void printMethod(final IMethodCoverage method,
								 final boolean more) {
			final String desc = String.format("+- method %s%s",
					method.getName(), method.getDesc());
			printDetails(desc, method);

			for (int nr = method.getFirstLine(); nr <= method
					.getLastLine(); nr++) {
				printLine(method.getLine(nr), nr, more ? "| " : "  ");
			}
		}

		private void printLine(final ILine line, final int nr,
							   final String indent) {
			if (line.getStatus() != ICounter.EMPTY) {
				out.printf("%6s %6s                        %s +- line %d %d%n",
						total(line.getInstructionCounter()),
						total(line.getBranchCounter()), indent,nr,line.getStatus()
						);

			}
		}

		private void printDetails(final String description,
								  final ICoverageNode coverage) {
			out.printf("%6s %6s %6s %6s %6s   %s%n",
					total(coverage.getInstructionCounter()),
					total(coverage.getBranchCounter()),
					total(coverage.getLineCounter()),
					total(coverage.getMethodCounter()),
					total(coverage.getComplexityCounter()), description);
		}

		private String total(final ICounter counter) {
			return String.valueOf(counter.getTotalCount());
		}

	}
	@Test
	public void testDebug() throws IOException {
		// ExecFileLoader loader  = new ExecFileLoader();
		// loader.load(new File("C:\\Users\\sijiluo\\Desktop\\helloworld_ec\\20180306183008_92020973.ec"));
		// loader.save(new File("C:\\Users\\sijiluo\\Desktop\\helloworld_ec\\20180306183008_92020973.ecout"), true);
		// 分析覆盖率文件
		final ExecFileLoader loader = new ExecFileLoader();
		loader.load(new File("C:\\Users\\sijiluo\\Desktop\\helloworld_ec\\20180306183008_92020973.ec"));
		ExecutionDataStore data = loader.getExecutionDataStore();
		for(ExecutionData ed : data.getContents()){

		}
		final CoverageBuilder builder = new CoverageBuilder();
		PrintWriter printWriter = new PrintWriter(System.out,true);
		Printer p = new Printer(printWriter);
		final Analyzer analyzer = new Analyzer(data, p);
		analyzer.analyzeAll(new File("F:\\prj\\androidstudio\\helloworld\\app\\build\\intermediates\\classes\\debug\\com\\kugou\\helloworld\\RecordCoverageProvider.class"));

//		// 输出报告
//		final IBundleCoverage bundle = builder.getBundle("dd");
//		final List<IReportVisitor> visitors = new ArrayList<IReportVisitor>();
//		File out_html = new File("C:\\Users\\sijiluo\\Desktop\\helloworld_ec\\outhtml");
//		final HTMLFormatter formatter = new HTMLFormatter();
//		visitors.add(formatter.createVisitor(new FileMultiReportOutput(out_html)));
//		final IReportVisitor visitor =  new MultiReportVisitor(visitors);
//		final MultiSourceFileLocator multi = new MultiSourceFileLocator(4);
//		multi.add(new DirectorySourceFileLocator(new File("F:\\prj\\androidstudio\\helloworld\\app\\src\\main\\java"), null, 4));
//		visitor.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
//		visitor.visitBundle(bundle, multi);
//		visitor.visitEnd();
	}

	@Test
	public void testVERSION() {
		assertNotNull(JaCoCo.VERSION);
	}

	@Test
	public void testHOMEURL() {
		assertNotNull(JaCoCo.HOMEURL);
	}

	@Test
	public void testRUNTIMEPACKAGE() {
		assertNotNull(JaCoCo.RUNTIMEPACKAGE);
	}

}
