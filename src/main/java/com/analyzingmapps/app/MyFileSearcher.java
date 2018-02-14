package com.analyzingmapps.app;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyFileSearcher extends SimpleFileVisitor<Path> {
    private static String MANIFEST_FILE = "AndroidManifest.xml";
    private Analyzer Analyzer;

    //Variable to search unique File name
    Path foundPath;
    private String fileName;

    // Variables for searching multiple files
    private PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.java");
    private Set<Path> foundManifests = new HashSet<>();
    private Set<MyFileParserVisitor> foundJavaFiles = new HashSet<>();

    MyFileSearcher(Analyzer an) {
        this.Analyzer = an;
    }

    MyFileSearcher(String fileName) {
        this.fileName = fileName;
    }

    private boolean shouldSkip(Path path) {
        return path.getFileName().toString().matches("^.*?(bin|androidTest|test|lib|debug|fdroid|build|gradle|res).*$");
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (shouldSkip(dir)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        //System.out.println("DIR -> " + dir.toAbsolutePath());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
        if (fileName != null && fileName.equals(filePath.getFileName())) {
            //System.out.println("ONE FILE -> " + filePath.getFileName());
            foundPath = filePath;
            return FileVisitResult.TERMINATE;

        } else if (matcher.matches(filePath.getFileName())) { //Analyze all *.java

            //System.out.println("JAVA FILE -> " + filePath.getFileName());
            //Analyzer.this.analyzeSourceCode(filePath);
            checkPermissionUsage(filePath.toFile());

        } else if (filePath.getFileName().toString().equals(MANIFEST_FILE)) {

            //System.out.println("XML FILE -> " + filePath.getFileName());
            foundManifests.add(filePath);

        }
        return FileVisitResult.CONTINUE;
    }

    private void checkPermissionUsage(File file) {

        MyFileParserVisitor currentFileVisitor = new MyFileParserVisitor();
        try {
            FileInputStream in = new FileInputStream(file);
            CompilationUnit cu;
            cu = JavaParser.parse(in);
            in.close();

            currentFileVisitor.compilationUnit = cu;
            currentFileVisitor.file = file;
            currentFileVisitor.visit(cu, file.getName());

            if (currentFileVisitor.isFileUsingPermissions()) {
                foundJavaFiles.add(currentFileVisitor);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong parsing " + file.getPath());
            Analyzer.logError(file.getPath(), e.getMessage(), "checkPermissionUsage");
        }
    }

    List<Path> getFoundManifests() {
        return new ArrayList<>(foundManifests);
    }
    List<MyFileParserVisitor> getJavaFiles() {
        return new ArrayList<>(foundJavaFiles);
    }

}