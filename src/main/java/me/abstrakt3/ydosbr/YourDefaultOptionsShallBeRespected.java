package me.abstrakt3.ydosbr;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class YourDefaultOptionsShallBeRespected implements LanguageAdapter {
    public static final Logger LOGGER = LogManager.getLogger("YDOSBR");
    public static final File RUN_DIR = FabricLoader.getInstance().getGameDirectory();
    public static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDirectory();

    // Storing default options in Appdata
    public static final File DEFAULT_OPTIONS_DIR = new File(System.getenv("APPDATA"), "ydospr");

    /**
     * We are using pre-launch entrypoint here as we want to be faster than everyone.
     */
    public YourDefaultOptionsShallBeRespected() {
        LOGGER.info("(YDOSBR) Applying default options...");
        try {
            // The config directory for ydosbr
            File ydosbr = new File(CONFIG_DIR, "ydosbr");

            // Check if ydosbr exists in CONFIG_DIR
            if (!ydosbr.exists()) {
                LOGGER.warn("(YDOSBR) ydosbr config not found! Falling back to default options in {}", DEFAULT_OPTIONS_DIR.getAbsolutePath());

                // Directory for default options in %APPDATA%/ydosbr/defaultoptions
                File defaultOptions = new File(DEFAULT_OPTIONS_DIR, "defaultoptions");

                // Check if defaultoptions exists
                if (defaultOptions.exists()) {
                    LOGGER.info("(YDOSBR) Default options found! Copying default options to config/ydosbr...");
                    copyDefaultOptions(ydosbr, defaultOptions);
                } else {
                    LOGGER.warn("(YDOSBR) Default options not found! They will be created at: {}", DEFAULT_OPTIONS_DIR.getAbsolutePath());
                }
            }

            // Ensure ydosbr exists
            if (!ydosbr.exists() && !ydosbr.mkdirs()) {
                throw new IllegalStateException("Could not create directory: " + ydosbr.getAbsolutePath());
            }

            // Create default options and configs within ydosbr
            new File(ydosbr, "options.txt").createNewFile();
            File config = new File(ydosbr, "config");
            if (!config.exists() && !config.mkdirs()) {
                throw new IllegalStateException("Could not create directory: " + config.getAbsolutePath());
            }

            // Apply default options from ydosbr
            Files.walk(ydosbr.toPath()).forEach(path -> {
                File file = path.normalize().toAbsolutePath().normalize().toFile();
                if (!file.isFile()) return;
                try {
                    try {
                        Path configRelative = config.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize());
                        if (configRelative.startsWith("ydosbr"))
                            throw new IllegalStateException("Illegal default config file: " + file);
                        applyDefaultOptions(new File(CONFIG_DIR, configRelative.normalize().toString()), file);
                    } catch (IllegalArgumentException e) {
                        System.out.println(ydosbr.toPath().toAbsolutePath().normalize());
                        System.out.println(file.toPath().toAbsolutePath().normalize());
                        System.out.println(ydosbr.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()));
                        applyDefaultOptions(new File(RUN_DIR, ydosbr.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).normalize().toString()), file);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Saving contents of ydosbr to defaultoptions if it wasn't created yet
            File defaultOptions = new File(DEFAULT_OPTIONS_DIR, "defaultoptions");
            if (!defaultOptions.exists()) {
                LOGGER.info("(YDOSBR) Creating default options in {}", DEFAULT_OPTIONS_DIR.getAbsolutePath());
                if (!defaultOptions.mkdirs()) {
                    LOGGER.warn("(YDOSBR) Could not create defaultoptions directory!");
                } else {
                    copyDefaultOptions(defaultOptions, ydosbr);
                    LOGGER.info("(YDOSBR) Default options created!");
                }
            } else {
                LOGGER.info("(YDOSBR) Default options found at: {}", DEFAULT_OPTIONS_DIR.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply default options.", e);
        }
    }
    
    private void applyDefaultOptions(File file, File defaultFile) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + file.getParentFile().getAbsolutePath());
        }
        if (!defaultFile.getParentFile().exists() && !defaultFile.getParentFile().mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + defaultFile.getParentFile().getAbsolutePath());
        }
        if (!defaultFile.exists()) {
            defaultFile.createNewFile();
            return;
        }
        if (file.exists()) return;
        LOGGER.info("Applying default options for " + File.separator + RUN_DIR.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).normalize().toString() + " from " + File.separator +
                    RUN_DIR.toPath().toAbsolutePath().normalize().relativize(defaultFile.toPath().toAbsolutePath().normalize()).normalize().toString());
        Files.copy(defaultFile.toPath(), file.toPath());
    }

    /**
     * Copies default options from one directory to another.
     */
    private void copyDefaultOptions(File targetDir, File sourceDir) throws IOException {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + targetDir.getAbsolutePath());
        }

        Files.walk(sourceDir.toPath()).forEach(path -> {
            File sourceFile = path.toFile();
            if (!sourceFile.isFile()) return;

            try {
                Path relativePath = sourceDir.toPath().relativize(path);
                File targetFile = new File(targetDir, relativePath.toString());

                if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs()) {
                    throw new IllegalStateException("Could not create directory: " + targetFile.getParentFile().getAbsolutePath());
                }

                Files.copy(sourceFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("(YDOSBR) Copied file: " + targetFile.getName());
            } catch (IOException e) {
                LOGGER.error("(YDOSBR) Error copying file: " + sourceFile.getName(), e);
            }
        });
    }
    
    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        throw new IllegalStateException();
    }
}
