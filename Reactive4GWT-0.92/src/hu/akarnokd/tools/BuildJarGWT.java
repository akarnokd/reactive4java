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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates a GWT JAR fire from the src and gwt-override directories.
 * @author akarnokd, 2011.02.07.
 */
public final class BuildJarGWT {

	/**
	 * 
	 */
	private BuildJarGWT() {
	}
	/**
	 * Process the contents of the given directory.
	 * @param baseDir the base directory
	 * @param currentDir the current directory
	 * @param zout the output stream
	 * @param filter the optional file filter
	 * @throws IOException on error
	 */
	static void processDirectory(String baseDir, String currentDir, ZipOutputStream zout,
			FilenameFilter filter) throws IOException {
		File[] files = new File(currentDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return !pathname.isHidden();
			}
		});
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					processDirectory(baseDir, f.getPath(), zout, filter);
				} else {
					String fpath = f.getPath();
					String fpath2 = fpath.substring(baseDir.length());
					
					if (filter == null || filter.accept(f.getParentFile(), f.getName())) {
						System.out.printf("Adding %s as %s%n", fpath, fpath2);
						ZipEntry ze = new ZipEntry(fpath2.replace('\\', '/'));
						ze.setSize(f.length());
						ze.setTime(f.lastModified());
						zout.putNextEntry(ze);
						
						zout.write(IOUtils.load(f));
					}
				}
			}
		}
	}
	/**
	 * Add file to the zip.
	 * @param entryName the target entry name
	 * @param file the source file name
	 * @param zout the zip output stream
	 * @throws IOException on error
	 */
	static void addFile(String entryName, String file, ZipOutputStream zout) throws IOException {
		ZipEntry mf = new ZipEntry(entryName);
		File mfm = new File(file);
		mf.setSize(mfm.length());
		mf.setTime(mfm.lastModified());
		zout.putNextEntry(mf);
		zout.write(IOUtils.load(mfm));
	}
	/**
	 * Main program, no arguments.
	 * @param args no arguments
	 * @throws Exception ignored
	 */
	public static void main(String[] args) throws Exception {
		
		String baseProject = "..\\Reactive4Java\\";
		String baseProject2 = ".\\";
		String targetJar = "reactive4java-gwt";
		String targetJar2 = "reactive4java";
		String version = "0.92";
		
		ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(
				new FileOutputStream(baseProject2 + targetJar + "-" + version + ".jar"), 1024 * 1024));
		zout.setLevel(9);
		try {
			processDirectory(baseProject + ".\\src\\", baseProject + ".\\src", zout, new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					String path = dir.getAbsolutePath().replace('\\', '/');
					return !name.equals(".svn") 
					&& path.contains("hu/akarnokd/reactive4java") 
					&& !path.contains("hu/akarnokd/reactive4java/swing")
					&& !path.contains("hu/akarnokd/reactive4java/test")
					&& !name.equals("Reactive.java")
					&& !name.equals("Interactive.java")
					&& !name.equals("DefaultScheduler.java")
					;
				}
			});
			processDirectory(baseProject2 + ".\\gwt-overrides\\", baseProject2 + ".\\gwt-overrides", zout, new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.equals(".svn");
				}
			});
			
			addFile("META-INF/MANIFEST.MF", baseProject + "META-INF/MANIFEST.MF", zout);
			addFile("LICENSE.txt", baseProject + "LICENSE.txt", zout);
		} finally {
			zout.close();
		}
		
		zout = new ZipOutputStream(new BufferedOutputStream(
				new FileOutputStream(baseProject2 + targetJar2 + "-" + version + ".jar"), 1024 * 1024));
		zout.setLevel(9);
		try {
			processDirectory(baseProject + ".\\bin\\", baseProject + ".\\bin", zout, new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.equals(".svn");
				}
			});
			processDirectory(baseProject + ".\\src\\", baseProject + ".\\src", zout, new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.equals(".svn");
				}
			});
			
			addFile("META-INF/MANIFEST.MF", baseProject + "META-INF/MANIFEST.MF", zout);
			addFile("LICENSE.txt", baseProject + "LICENSE.txt", zout);
		} finally {
			zout.close();
		}

	}
}
