/*
 * Copyright 2011-2013 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility class to look for new things specified by "since xxx" in Java sources.
 * @author karnokd, 2011.02.21.
 */
public final class WhatsNew {
	/** Utility class. */
	private WhatsNew() { }
	/** The output. */
	static PrintWriter out;
	/**
	 * @param args no arguments
	 * @throws Exception ignored
	 */
	public static void main(String[] args) throws Exception {
		out = new PrintWriter(new BufferedWriter(new FileWriter("whatsnew_java7.txt")));
		try {
			ZipFile zip = new ZipFile("c:\\Program Files\\Java\\jdk1.7.0\\src.zip");
			Enumeration<? extends ZipEntry> zes1 = zip.entries();
			
			List<ZipEntry> zes = new ArrayList<ZipEntry>();
			while (zes1.hasMoreElements()) {
				ZipEntry ze = zes1.nextElement();
				zes.add(ze);
			}			
			Collections.sort(zes, new Comparator<ZipEntry>() {
				@Override
				public int compare(ZipEntry o1, ZipEntry o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (ZipEntry ze : zes) {
				if (ze.getName().toLowerCase().endsWith(".java") && !ze.getName().startsWith("com/sun")) {
					BufferedReader bin = new BufferedReader(new InputStreamReader(zip.getInputStream(ze)));
					try {
						StringBuilder b = new StringBuilder();
						for (;;) {
							String line = bin.readLine();
							if (line == null) {
								break;
							}
							b.append(line).append("\n");
						}
						parse(ze.getName(), b.toString());
					} finally {
						bin.close();
					}
				}
			}
		} finally {
			out.close();
		}
	}
	/**
	 * Look for the since tag in the file, then locate the subsequent method specification.
	 * @param name the full class name
	 * @param java the java file
	 */
	static void parse(String name, String java) {
		boolean first = true;
		int idx = 0; 
		for (;;) {
			int newIdx = java.indexOf("@since", idx);
			if (newIdx < 0) {
				break;
			}
			int lineEnd = java.indexOf('\n', newIdx);
			if (lineEnd < 0) {
				break;
			}
			String version = java.substring(newIdx + 6, lineEnd).trim();
			if (!version.contains("1.7")) {
				idx = newIdx + 6;
				continue;
			}
			if (first) {
				String n = String.format("%n--------------------------------%n%s%n", name.replaceAll("\\.java", ""));
				System.out.print(n);
				out.print(n);
				first = false;
			}
			
			int commentEnd = java.indexOf("*/", newIdx + 6);
			if (commentEnd < 0) {
				idx = newIdx + 6;
				continue;
			}
			int bodyStart = java.indexOf("{", commentEnd + 2);
			int bodyStart2 = java.indexOf(";", commentEnd + 2);
			if (bodyStart < 0 && bodyStart2 < 0) {
				idx = newIdx + 6;
				continue;
			}
			bodyStart = bodyStart < 0 ? bodyStart2 : (bodyStart2 < 0 ? bodyStart : Math.min(bodyStart, bodyStart2));
			
			String declaration = java.substring(commentEnd + 2, bodyStart)
			.replace('\n', ' ').replaceAll("\\s+", " ").trim();
			
			while (declaration.matches("@.+\\s*\\(\\s*")) {
				bodyStart = java.indexOf("{", bodyStart + declaration.length());
				bodyStart2 = java.indexOf(";", bodyStart + declaration.length());
				if (bodyStart < 0 && bodyStart2 < 0) {
					idx = newIdx + 6;
					continue;
				}
				bodyStart = bodyStart < 0 ? bodyStart2 : (bodyStart2 < 0 ? bodyStart : Math.min(bodyStart, bodyStart2));
				declaration = java.substring(commentEnd + 2, bodyStart)
				.replace('\n', ' ').replaceAll("\\s+", " ").trim();
			}
			
			if (declaration.startsWith("package")) {
				idx = newIdx + 6;
				continue;
			}
			out.printf("\t%d\t%s%n", countLines(java, commentEnd + 2), declaration);
			
			idx = newIdx + 6;
		}
	}
	/**
	 * Counts the number of newlines till the given character index.
	 * @param java the source string
	 * @param endIndex the end index
	 * @return the line count
	 */
	static int countLines(String java, int endIndex) {
		int c = 1;
		for (int i = 0; i < java.length() && i < endIndex; i++) {
			if (java.charAt(i) == '\n') {
				c++;
			}
		}
		return c;
	}
}
