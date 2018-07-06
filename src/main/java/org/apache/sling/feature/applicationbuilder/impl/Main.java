/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.applicationbuilder.impl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ApplicationBuilder;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.io.ArtifactHandler;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.io.json.ApplicationJSONWriter;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    private static Logger LOGGER;

    private static String output;

    private static String filesInput;

    private static String dirsInput;

    private static String repoUrls;

    private static String propsFile;

    private static String frameworkVersion;

    private static File cacheDir;

    /**
     * Parse the command line parameters and update a configuration object.
     * @param args Command line parameters
     * @return Configuration object.
     */
    private static void parseArgs(final String[] args) throws Exception {
        final Option repoOption =  Option.builder("u").hasArg().argName("Set repository url")
                .desc("repository url").build();
        final Option filesOption =  new Option("f", true, "Set feature files (comma separated)");
        final Option dirsOption = new Option("d", true, "Set feature file dirs (comma separated)");
        final Option propsOption =  new Option("p", true, "sling.properties file");
        final Option frameworkOption = new Option("fv", true, "Set felix framework version");
        final Option outputOption = Option.builder("o").hasArg().argName("Set output file")
                .desc("output file").build();

        final Option cacheOption = new Option("c", true, "Set cache dir");
        final Option debugOption = new Option("v", false, "Verbose");
        debugOption.setArgs(0);

        final Options options = new Options();
        options.addOption(repoOption);
        options.addOption(filesOption);
        options.addOption(dirsOption);
        options.addOption(outputOption);
        options.addOption(propsOption);
        options.addOption(frameworkOption);
        options.addOption(cacheOption);
        options.addOption(debugOption);

        final CommandLineParser parser = new DefaultParser();
        try {
            final CommandLine cl = parser.parse(options, args);

            if ( cl.hasOption(repoOption.getOpt()) ) {
                repoUrls = cl.getOptionValue(repoOption.getOpt());
            }
            if ( cl.hasOption(filesOption.getOpt()) ) {
                filesInput = cl.getOptionValue(filesOption.getOpt());
            }
            if ( cl.hasOption(dirsOption.getOpt()) ) {
                dirsInput = cl.getOptionValue(dirsOption.getOpt());
            }
            if ( cl.hasOption(outputOption.getOpt()) ) {
                output = cl.getOptionValue(outputOption.getOpt());
            }
            if ( cl.hasOption(propsOption.getOpt()) ) {
                propsFile = cl.getOptionValue(propsOption.getOpt());
            }
            if (cl.hasOption(frameworkOption.getOpt())) {
                frameworkVersion = cl.getOptionValue(frameworkOption.getOpt());
            }
            if (cl.hasOption(cacheOption.getOpt())) {
                cacheDir = new File(cl.getOptionValue(cacheOption.getOpt()));
            }
            if ( cl.hasOption(debugOption.getOpt()) ) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            }
        } catch ( final ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("applicationbuilder", options);

            throw new Exception("Unable to parse command line: {}", pe);
        }
        if ( filesInput == null && dirsInput == null) {
            throw new Exception("Required argument missing: model files or directory");
        }
    }

    private static ArtifactManager getArtifactManager() {
        final ArtifactManagerConfig amConfig = new ArtifactManagerConfig();
        if ( repoUrls != null ) {
            amConfig.setRepositoryUrls(repoUrls.split(","));
        }
        if (cacheDir != null) {
            amConfig.setCacheDirectory(cacheDir);
        }
        try {
            return ArtifactManager.getArtifactManager(amConfig);
        } catch ( IOException ioe) {
            LOGGER.error("Unable to create artifact manager " + ioe.getMessage(), ioe);
            System.exit(1);
        }
        // we never reach this, but have to keep the compiler happy
        return null;
    }

    public static void main(final String[] args) {
        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        Exception clException = null;
        try {
            parseArgs(args);
        }
        catch (Exception ex) {
            clException = ex;
        }

        LOGGER = LoggerFactory.getLogger("applicationbuilder");

        LOGGER.info("Apache Sling Feature Application Builder");
        LOGGER.info("");

        if (clException != null) {
            LOGGER.error(clException.getMessage(), clException);
            System.exit(1);
        }

        final ArtifactManager am = getArtifactManager();

        final String[] files =
                Stream.concat(
                    Stream.of(filesInput != null ? filesInput.split(",") : new String[0]),
                    Stream.of(dirsInput != null ? dirsInput.split(",") : new String[0])
                        .map(path -> new File(path))
                        .filter(File::isDirectory)
                        .flatMap(dir ->
                            Stream.of(dir.listFiles()))
                        .filter(file -> !file.getName().startsWith("."))
                        .map(File::getAbsolutePath))
                .toArray(String[]::new);

        if (files.length == 0) {
            LOGGER.error("No feature files found.");
            System.exit(1);
        }

        try {

            writeApplication(buildApplication(assembleApplication(null, am, files)), output == null ? "application.json" : output);

        } catch ( final IOException ioe) {
            LOGGER.error("Unable to read feature/application files " + ioe.getMessage(), ioe);
            System.exit(1);
        } catch ( final Exception e) {
            LOGGER.error("Problem generating application", e);
            System.exit(1);
        }
    }

    private static Application assembleApplication(
        Application app,
        final ArtifactManager artifactManager,
        final String... featureFiles)
        throws IOException {
        if ( featureFiles == null || featureFiles.length == 0 ) {
            throw new IOException("No features found.");
        }

        List<Feature> features = new ArrayList<>();

        for (final String initFile : featureFiles)
        {
            try
            {
                final Feature f = IOUtils.getFeature(initFile, artifactManager, FeatureJSONReader.SubstituteVariables.RESOLVE);
                features.add(f);
            }
            catch (Exception ex)
            {
                throw new IOException("Error reading feature: " + initFile, ex);
            }
        }

        Collections.sort(features);

        app = ApplicationBuilder.assemble(app, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {
                try {
                    final ArtifactHandler handler = artifactManager.getArtifactHandler(id.toMvnUrl());
                    try (final FileReader r = new FileReader(handler.getFile())) {
                        final Feature f = FeatureJSONReader.read(r, handler.getUrl(), FeatureJSONReader.SubstituteVariables.RESOLVE);
                        return f;
                    }

                } catch (final IOException e) {
                    // ignore
                }
                return null;
            }
        }), features.toArray(new Feature[0]));

        // check framework
        if ( app.getFramework() == null ) {
            // use hard coded Apache Felix
            app.setFramework(IOUtils.getFelixFrameworkId(null));
        }

        return app;
    }

    private static Application buildApplication(final Application app) {
        final org.apache.sling.feature.Artifact a = new org.apache.sling.feature.Artifact(ArtifactId.parse("org.apache.sling/org.apache.sling.launchpad.api/1.2.0"));
        a.getMetadata().put(org.apache.sling.feature.Artifact.KEY_START_ORDER, "1");
        app.getBundles().add(a);
        // sling.properties (TODO)
        if ( propsFile == null ) {
            app.getFrameworkProperties().put("org.osgi.framework.bootdelegation", "sun.*,com.sun.*");
        } else {

        }
        // felix framework hard coded for now
        app.setFramework(IOUtils.getFelixFrameworkId(frameworkVersion));
        return app;
    }

    private static void writeApplication(final Application app, final String out) {
        LOGGER.info("Writing application: " + out);
        final File file = new File(out);
        try ( final FileWriter writer = new FileWriter(file)) {
            ApplicationJSONWriter.write(writer, app);
        } catch ( final IOException ioe) {
            LOGGER.error("Unable to write application to {} : {}", out, ioe.getMessage(), ioe);
            System.exit(1);
        }
    }
}
