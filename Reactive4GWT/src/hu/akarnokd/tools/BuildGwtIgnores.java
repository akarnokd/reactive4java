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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Traverses each java file in the Reactive4Java project
 * and creates entries in the GWT override directory
 * which has sections marked with // #GWT-IGNORE-START
 * and // #GWT-IGNORE-END parts
 * @author karnok, 2013.01.17.
 */
public final class BuildGwtIgnores {
	/** Utility class. */
	private BuildGwtIgnores() { }
	/**
	 * Main entry point
	 * @param args no arguments
	 */
	public static void main(String[] args) throws Exception {
		final Path root = Paths.get("../Reactive4Java/src");
		final Path override = Paths.get("gwt-overrides");
		if (!Files.exists(root)) {
			throw new Exception("Missing?");
		}
		final Set<String> ignores = new HashSet<String>();
		
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Path fileName = file.getFileName();
				if (fileName.toString().endsWith(".java")) {
					List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
					boolean skip = false;
					boolean uncomment = false;
					List<String> filtered = new ArrayList<String>(lines.size());
					boolean changed = false;
					
					for (String line : lines) {
						if (line.contains("// #GWT-IGNORE-FILE")) {
							ignores.add(fileName.toString());
							return FileVisitResult.CONTINUE;
						} else
						if (line.contains("// #GWT-ACCEPT-START")) {
							skip = false;
							uncomment = true;
							filtered.add("");
						} else
						if (line.contains("// #GWT-ACCEPT-END")) {
							uncomment = false;
							filtered.add("");
						} else
						if (line.contains("// #GWT-IGNORE-START")) {
							skip = true;
							changed = true;
							filtered.add("");
						} else
						if (line.contains("// #GWT-IGNORE-END")) {
							skip = false;
							changed = true;
							filtered.add("");
						} else
						if (!skip) {
							if (uncomment) {
								if (line.matches("\\s*//.*")) {
									line = line.replaceFirst("//", "");
								}
							}
							filtered.add(line);
						} else {
							filtered.add("");
						}
					}

					Path rel = root.relativize(file.getParent());
					
					Path overridePack = override
							.resolve(rel)
							.resolve("gwt")
							.resolve(rel)
							.resolve(fileName);
					
					if (changed) {
						
						Files.createDirectories(overridePack.getParent());
						
						Files.write(overridePack, filtered, Charset.forName("UTF-8"));
						
						ignores.add(fileName.toString());
					} else {
						Files.deleteIfExists(overridePack);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		BuildJarGWT.main(ignores.toArray(new String[0]));
	}
}
