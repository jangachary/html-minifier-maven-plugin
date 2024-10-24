package com.js.maven;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.yahoo.platform.yui.compressor.CssCompressor;

@Mojo(name = "minify-html", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class HtmlMinifierMojo extends AbstractMojo {

	@Parameter(property = "minifyHtml.sourceDirectory", defaultValue = "${project.basedir}/src/main/resources/templates")
	private File sourceDirectory;

	@Parameter(property = "minifyHtml.targetDirectory", defaultValue = "${project.build.directory}/minified")
	private File targetDirectory;

	@Parameter(property = "minifyHtml.overwrite", defaultValue = "true")
	private boolean overwrite;

	@Parameter(property = "minifyHtml.minifyInlineScripts", defaultValue = "false")
	private boolean minifyInlineScripts;

	@Parameter(property = "minifyHtml.minifyInlineStyles", defaultValue = "false")
	private boolean minifyInlineStyles;

	@Override
	public void execute() throws MojoExecutionException {
		if (!sourceDirectory.exists()) {
			throw new MojoExecutionException("Source directory does not exist: " + sourceDirectory.getAbsolutePath());
		}

		try {
			// Create target directory if not exist
			if (!targetDirectory.exists()) {
				targetDirectory.mkdirs();
			}

			HtmlCompressor compressor = new HtmlCompressor();
			compressor.setRemoveComments(true);
			compressor.setRemoveIntertagSpaces(true);

			File[] htmlFiles = sourceDirectory.listFiles((dir, name) -> name.endsWith(".html"));
			if (htmlFiles != null) {
				for (File htmlFile : htmlFiles) {
					getLog().info("Minifying file: " + htmlFile.getName());
					FileReader reader = new FileReader(htmlFile);
					String content = readContent(reader);
					reader.close();

					// Compress the HTML content
					String minifiedContent = compressor.compress(content);

					// If minifyInlineScripts is true, minify inline <script> tags
					if (minifyInlineScripts) {
						minifiedContent = minifyInlineScripts(minifiedContent);
					}

					// If minifyInlineStyles is true, minify inline <style> tags
					if (minifyInlineStyles) {
						minifiedContent = minifyInlineStyles(minifiedContent);
					}
					// Write to target directory
					File targetFile = new File(targetDirectory, htmlFile.getName());
					FileWriter writer = new FileWriter(targetFile);
					writer.write(minifiedContent);
					writer.close();
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error during HTML minification", e);
		}
	}

	private String readContent(FileReader reader) throws IOException {
		StringBuilder content = new StringBuilder();
		char[] buffer = new char[1024];
		int numCharsRead;
		while ((numCharsRead = reader.read(buffer)) > 0) {
			content.append(buffer, 0, numCharsRead);
		}
		return content.toString();
	}

	// Basic HTML minification function (remove comments and whitespace)
	private String minifyHtml(String content) {
		return content.replaceAll("(?s)<!--.*?-->", "") // Remove comments
				.replaceAll("\\s{2,}", " ") // Collapse whitespace
				.replaceAll(">\\s+<", "><"); // Remove spaces between tags
	}

	// Function to minify inline <script> content using Google Closure Compiler
//	private String minifyInlineScripts(String content) throws MojoExecutionException {
////		Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
////		Pattern scriptPattern = Pattern.compile("<script(?![^>]*\\bsrc\\b)[^>]*>(.*?)</script>", Pattern.DOTALL);
//		Pattern scriptPattern = Pattern.compile("<script(?![^>]*(\\bsrc\\b|th:[^\\s=]+))[^>]*>(.*?)</script>",
//				Pattern.DOTALL);
//
//		Matcher matcher = scriptPattern.matcher(content);
//		StringBuffer minifiedContent = new StringBuffer();
//
//		while (matcher.find()) {
//			String scriptContent = matcher.group(1);
//			String minifiedScript = compileJavaScript(scriptContent);
//			matcher.appendReplacement(minifiedContent,
//					"<script>" + Matcher.quoteReplacement(minifiedScript) + "</script>");
//		}
//
//		matcher.appendTail(minifiedContent);
//		return minifiedContent.toString();
//	}
	private String minifyInlineScripts(String content) throws MojoExecutionException {
		// Pattern to match <script> tags that do not have a 'src' attribute or
		// attributes starting with 'th:'
		Pattern scriptPattern = Pattern.compile("<script(?![^>]*(\\bsrc\\b|th:[^\\s=]+))[^>]*>(.*?)</script>",
				Pattern.DOTALL);

		Matcher matcher = scriptPattern.matcher(content);
		StringBuffer minifiedContent = new StringBuffer();

		while (matcher.find()) {
			String scriptContent = matcher.group(2); // Use group(2) for the actual script content
			String minifiedScript = compileJavaScript(scriptContent);
			matcher.appendReplacement(minifiedContent,
					"<script>" + Matcher.quoteReplacement(minifiedScript) + "</script>");
		}

		matcher.appendTail(minifiedContent);
		return minifiedContent.toString();
	}

	// Function to minify inline <style> content using Google Closure Compiler
	private String minifyInlineStyles(String content) throws MojoExecutionException {
		Pattern stylePattern = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL);
		Matcher matcher = stylePattern.matcher(content);
		StringBuffer minifiedContent = new StringBuffer();

		while (matcher.find()) {
			String styleContent = matcher.group(1);
			String minifiedStyle = compileCSS(styleContent);
			matcher.appendReplacement(minifiedContent,
					"<style>" + Matcher.quoteReplacement(minifiedStyle) + "</style>");
		}

		matcher.appendTail(minifiedContent);
		return minifiedContent.toString();
	}

	// Compile JavaScript using Google Closure Compiler

	private String compileJavaScript(String js) throws MojoExecutionException {
		try {
			// Create a new compiler instance
			com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();

			// Set up the compiler options
			CompilerOptions options = new CompilerOptions();
//			options.setWarningLevel(DiagnosticGroups.UNUSED_LOCAL_VARIABLE, CheckLevel.WARNING);
//			options.setWarningLevel(DiagnosticGroups.UNUSED_LOCAL_VARIABLE, CheckLevel.WARNING);

			// Create a SourceFile from the JS string
			SourceFile sourceFile = SourceFile.fromCode("input.js", js); // This method should exist // Compile the JS
																			// code
			Result result = compiler.compile(SourceFile.fromCode("externs.js", ""), sourceFile, options);

			// Check for compilation errors
			if (result.errors != null && result.errors.size() > 0) {
				throw new MojoExecutionException("JavaScript compilation errors: " + result.errors.get(0).toString());
			}
			String compiledJs = compiler.toSource();
			compiledJs = compiledJs.replaceAll("'use strict';", "").replaceAll("\"use strict\";", "");
			// Return the compiled JavaScript code
			return compiledJs;
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to compile JavaScript", e);
		}
	}

	private String compileCSS(String css) throws MojoExecutionException {
		try {
			StringReader stringReader = new StringReader(css);
			StringWriter stringWriter = new StringWriter();
			CssCompressor compressor = new CssCompressor(stringReader);

			// Minify the CSS and write to stringWriter
			compressor.compress(stringWriter, -1);

			return stringWriter.toString();
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to minify CSS", e);
//			return css;
		}
	}
}
