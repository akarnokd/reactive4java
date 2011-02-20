/*
 * Copyright 2011 David Karnok
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class to traverse a directory containing java source files,
 * extract javadoc, single- and multiline comments and build a large
 * single file. The generated file can then be syntax checked by external means.
 * @author akarnokd, 2011.02.04.
 */
public final class JavaCommentsCollector {
	/** Utility class. */
	private JavaCommentsCollector() {
		
	}
	/**
	 * @param args no arguments
	 * @throws Exception ignored
	 */
	public static void main(String[] args) throws Exception {
		
		final Path p = Paths.get("..\\Reactive4Java\\src");
		
		final PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("..\\Reactive4Java\\docs\\javadoc\\flat.txt"), 1024 * 1024));
		try {
			Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (file.getName().toString().toLowerCase().endsWith(".java")) {
						visitAFile(file, p, out);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} finally {
			out.close();
		}
	}
	/**
	 * Parse the given Java file.
	 * @param theFile the concrete file.
	 * @param base the base directory
	 * @param out the output writer where to place stuff
	 */
	static void visitAFile(Path theFile, Path base, PrintWriter out) {
		String t = String.format("File: %s%n", theFile);
		out.print(t);
		for (int i = 0; i < t.length(); i++) {
			out.print("-");
		}
		out.println();
		out.println();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(theFile.newInputStream()));
			try {
				StringBuilder b = new StringBuilder();
				String line = null;
				while ((line = in.readLine()) != null) {
					b.append(line).append("\n");
				}
				parse(b.toString(), out);
			} finally {
				in.close();
			}
		} catch (IOException ex) {
			out.printf("!!!File access error: %s%s%n");
		}
	}
	/**
	 * Parse the file content for comments.
	 * @param file the file contents
	 * @param out the output
	 */
	static void parse(String file, PrintWriter out) {
		// the tuple of type: string, line_start: int, content: string, line_end: int optional
		List<Object[]> byLine = new ArrayList<Object[]>();
		// find single line comments
		int index = 0;
		do {
			index = file.indexOf("//", index);
			if (index >= 0) {
				int lineEnd = file.indexOf('\n', index);
				int c = countLines(file, index);
				byLine.add(new Object[] {"//", c, file.substring(index, lineEnd >= 0 ? lineEnd : file.length())});
				index = lineEnd;
			}
		} while (index > 0);
		// find single line comments
		index = 0;
		do {
			index = file.indexOf("/*", index);
			if (index >= 0) {
				int lineEnd = file.indexOf("*/", index);
				if (lineEnd < 0) {
					lineEnd = file.length();
				}
				int c = countLines(file, index);
				int c1 = countLines(file, lineEnd);
				
				String cs = file.substring(index, lineEnd);
				
				if (cs.startsWith("/**")) {
					byLine.add(new Object[] {"/**", c, cs, c1});
				} else {
					byLine.add(new Object[] {"/*", c, cs, c1});
				}
				index = lineEnd;
			}
		} while (index > 0);
		
		Collections.sort(byLine, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o1, Object[] o2) {
				return ((Integer)o1[1]).compareTo((Integer)o2[1]);
			}
		});
		
		for (Object[] o : byLine) {
			
			String s = strip((String)o[2]);
			if (s.length() > 0) {
				out.printf("Line: %d%n%n", o[1]);
				out.println(s);
				out.println();
			}
			
		}
	}
	/**
	 * Counts the line breaks up to the given character index.
	 * @param s the string
	 * @param index the index limit, exclusive
	 * @return the line count, starts with 1
	 */
	static int countLines(String s, int index) {
		int count = 1;
		for (int i = 0; i < index && i < s.length(); i++) {
			if (s.charAt(i) == '\n') {
				count++;
			}
		}
		return count;
	}
	/**
	 * Removes any comment start/end indicator from the lines of source.
	 * @param source the source string
	 * @return the stripped text
	 */
	static String strip(String source) {
		String[] lines = source.split("\n");
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String s = lines[i].trim();
			if (s.startsWith("//")) {
				s = s.substring(2);
			} else
			if (s.startsWith("/**")) {
				s = s.substring(3);
			} else
			if (s.startsWith("/*")) {
				s = s.substring(2);
			} else
			if (s.startsWith("*/")) {
				s = s.substring(2);
			} else
			if (s.startsWith("*")) {
				s = s.substring(1);
			}
			b.append(s.trim()).append(" ");
		}
		String u = b.toString().trim();
		if (u.toLowerCase().contains("apache")) {
			return "";
		}
		return b.toString().trim();
	}
}
