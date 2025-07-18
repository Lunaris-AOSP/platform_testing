/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.layoutlib.bridge.intensive;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.view.Choreographer;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.resources.ResourceRepository;
import com.android.internal.lang.System_Delegate;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.RenderParamsFlags;
import com.android.layoutlib.bridge.intensive.setup.ConfigGenerator;
import com.android.layoutlib.bridge.intensive.setup.LayoutLibTestCallback;
import com.android.layoutlib.bridge.intensive.setup.LayoutPullParser;
import com.android.layoutlib.bridge.intensive.util.ImageUtils;
import com.android.layoutlib.bridge.intensive.util.ModuleClassLoader;
import com.android.layoutlib.bridge.intensive.util.SessionParamsBuilder;
import com.android.layoutlib.bridge.intensive.util.TestAssetRepository;
import com.android.resources.aar.AarSourceResourceRepository;
import com.android.resources.aar.FrameworkResourceRepository;
import com.android.utils.ILogger;

import com.google.android.collect.Lists;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Base class for render tests. The render tests load all the framework resources and a project
 * checked in this test's resources. The main dependencies
 * are:
 * 1. Fonts directory.
 * 2. Framework Resources.
 * 3. App resources.
 * 4. build.prop file
 * <p>
 * These are configured by two variables set in the system properties.
 * <p>
 * 1. platform.dir: This is the directory for the current platform in the built SDK
 * (.../sdk/platforms/android-<version>).
 * <p>
 * The fonts are platform.dir/data/fonts.
 * The Framework resources are platform.dir/data/res.
 * build.prop is at platform.dir/build.prop.
 * <p>
 * 2. test_res.dir: This is the directory for the resources of the test. If not specified, this
 * falls back to getClass().getProtectionDomain().getCodeSource().getLocation()
 * <p>
 * The app resources are at: test_res.dir/testApp/MyApplication/app/src/main/res
 */
public class RenderTestBase {

    /**
     * Listener for render process.
     */
    public interface RenderSessionListener {

        /**
         * Called before session is disposed after rendering.
         */
        void beforeDisposed(RenderSession session);
    }

    private static final String NATIVE_LIB_PATH_PROPERTY = "native.lib.path";
    private static final String FONT_DIR_PROPERTY = "font.dir";
    private static final String ICU_DATA_PATH_PROPERTY = "icu.data.path";
    private static final String HYPHEN_DATA_DIR_PROPERTY = "hyphen.data.dir";
    private static final String KEYBOARD_DIR_PROPERTY = "keyboard.dir";
    private static final String PLATFORM_DIR_PROPERTY = "platform.dir";
    private static final String RESOURCE_DIR_PROPERTY = "test_res.dir";

    private static final String NATIVE_LIB_DIR_PATH;
    private static final String FONT_DIR;
    private static final String ICU_DATA_PATH;
    private static final String HYPHEN_DATA_DIR;
    private static final String KEYBOARD_DIR;
    protected static final String PLATFORM_DIR;
    private static final String TEST_RES_DIR;
    /** Location of the app to test inside {@link #TEST_RES_DIR} */
    protected static final String APP_TEST_DIR = "testApp/MyApplication";
    /** Location of the app's res dir inside {@link #TEST_RES_DIR} */
    private static final String APP_TEST_RES = APP_TEST_DIR + "/src/main/res";
    /** Location of the app's asset dir inside {@link #TEST_RES_DIR} */
    private static final String APP_TEST_ASSET = APP_TEST_DIR + "/src/main/assets/";
    private static final String APP_CLASSES_LOCATION =
            APP_TEST_DIR + "/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/";
    protected static Bridge sBridge;
    /** List of log messages generated by a render call. It can be used to find specific errors */
    protected static ArrayList<String> sRenderMessages = Lists.newArrayList();
    private static ILayoutLog sLayoutLibLog;
    private static FrameworkResourceRepository sFrameworkRepo;
    private static ResourceRepository sProjectResources;
    private static ILogger sLogger;

    static {
        // Test that System Properties are properly set.
        PLATFORM_DIR = getPlatformDir();
        if (PLATFORM_DIR == null) {
            fail(String.format("System Property %1$s not properly set. The value is %2$s",
                    PLATFORM_DIR_PROPERTY, System.getProperty(PLATFORM_DIR_PROPERTY)));
        }

        NATIVE_LIB_DIR_PATH = getNativeLibDirPath();
        FONT_DIR = getFontDir();
        ICU_DATA_PATH = getIcuDataPath();
        HYPHEN_DATA_DIR = getHyphenDataDir();
        KEYBOARD_DIR = getKeyboardDir();

        TEST_RES_DIR = getTestResDir();
        if (TEST_RES_DIR == null) {
            fail(String.format("System property %1$s.dir not properly set. The value is %2$s",
                    RESOURCE_DIR_PROPERTY, System.getProperty(RESOURCE_DIR_PROPERTY)));
        }
    }

    @Rule
    public TestWatcher sRenderMessageWatcher = new TestWatcher() {
        @Override
        protected void succeeded(Description description) {
            // We only check error messages if the rest of the test case was successful.
            if (!sRenderMessages.isEmpty()) {
                fail(description.getMethodName() + " render error message: " +
                        sRenderMessages.get(0));
            }
        }
    };

    @Rule
    public TestWatcher sMemoryLeakChecker = new TestWatcher() {
        @Override
        protected void succeeded(Description description) {
            for (int i = Choreographer.CALLBACK_INPUT; i <= Choreographer.CALLBACK_COMMIT; ++i) {
                if (Choreographer.getInstance().mCallbackQueues[i].mHead != null) {
                    fail("Memory leak: leftover frame callbacks are detected in Choreographer");
                }
            }
        }
    };

    protected ClassLoader mDefaultClassLoader;

    private static String getNativeLibDirPath() {
        String nativeLibDirPath = System.getProperty(NATIVE_LIB_PATH_PROPERTY);
        if (nativeLibDirPath != null) {
            File nativeLibDir = new File(nativeLibDirPath);
            if (nativeLibDir.isDirectory()) {
                nativeLibDirPath = nativeLibDir.getAbsolutePath();
            } else {
                nativeLibDirPath = null;
            }
        }
        if (nativeLibDirPath == null) {
            nativeLibDirPath = PLATFORM_DIR + "/../../../../../lib64/";
        }
        return nativeLibDirPath;
    }

    private static String getFontDir() {
        String fontDir = System.getProperty(FONT_DIR_PROPERTY);
        if (fontDir == null) {
            // The fonts are built into out/host/common/obj/PACKAGING/fonts_intermediates
            // as specified in build/make/core/layoutlib_data.mk, and PLATFORM_DIR is
            // out/host/[arch]/sdk/sdk*/android-sdk*/platforms/android*
            fontDir = PLATFORM_DIR +
                    "/../../../../../../common/obj/PACKAGING/fonts_intermediates";
        }
        return fontDir;
    }

    private static String getIcuDataPath() {
        String icuDataPath = System.getProperty(ICU_DATA_PATH_PROPERTY);
        if (icuDataPath == null) {
            icuDataPath = PLATFORM_DIR + "/../../../../../com.android.i18n/etc/icu/icudt71l.dat";
        }
        return icuDataPath;
    }

    private static String getHyphenDataDir() {
        String hyphenDataDir = System.getProperty(HYPHEN_DATA_DIR_PROPERTY);
        if (hyphenDataDir == null) {
            hyphenDataDir =
                    PLATFORM_DIR + "/../../../../../../common/obj/PACKAGING/hyphen_intermediates";
        }
        return hyphenDataDir;
    }

    private static String getKeyboardDir() {
        String keyboardDir = System.getProperty(KEYBOARD_DIR_PROPERTY);
        if (keyboardDir == null) {
            // The keyboard files are built into
            // out/host/common/obj/PACKAGING/keyboards_intermediates
            // as specified in build/make/core/layoutlib_data.mk, and PLATFORM_DIR is
            // out/host/[arch]/sdk/sdk*/android-sdk*/platforms/android*
            keyboardDir = PLATFORM_DIR +
                    "/../../../../../../common/obj/PACKAGING/keyboards_intermediates";
        }
        return keyboardDir;
    }    

    private static String getPlatformDir() {
        String platformDir = System.getProperty(PLATFORM_DIR_PROPERTY);
        if (platformDir != null && !platformDir.isEmpty() && new File(platformDir).isDirectory()) {
            return platformDir;
        }
        // System Property not set. Try to find the directory in the build directory.
        String androidHostOut = System.getenv("ANDROID_HOST_OUT");
        if (androidHostOut != null) {
            platformDir = getPlatformDirFromHostOut(new File(androidHostOut));
            if (platformDir != null) {
                return platformDir;
            }
        }
        String workingDirString = System.getProperty("user.dir");
        File workingDir = new File(workingDirString);
        // Test if workingDir is android checkout root.
        platformDir = getPlatformDirFromRoot(workingDir);
        if (platformDir != null) {
            return platformDir;
        }

        // Test if workingDir is platform/frameworks/base/tools/layoutlib/bridge.
        File currentDir = workingDir;
        if (currentDir.getName().equalsIgnoreCase("bridge")) {
            currentDir = currentDir.getParentFile();
        }

        // Find frameworks/layoutlib
        while (currentDir != null && !"layoutlib".equals(currentDir.getName())) {
            currentDir = currentDir.getParentFile();
        }

        if (currentDir == null ||
                currentDir.getParentFile() == null ||
                !"frameworks".equals(currentDir.getParentFile().getName())) {
            return null;
        }

        // Test if currentDir is  platform/frameworks/layoutlib. That is, root should be
        // workingDir/../../ (2 levels up)
        for (int i = 0; i < 2; i++) {
            if (currentDir != null) {
                currentDir = currentDir.getParentFile();
            }
        }
        return currentDir == null ? null : getPlatformDirFromRoot(currentDir);
    }

    private static String getPlatformDirFromRoot(File root) {
        if (!root.isDirectory()) {
            return null;
        }
        File out = new File(root, "out");
        if (!out.isDirectory()) {
            return null;
        }
        File host = new File(out, "host");
        if (!host.isDirectory()) {
            return null;
        }
        File[] hosts = host.listFiles(path -> path.isDirectory() &&
                (path.getName().startsWith("linux-") ||
                        path.getName().startsWith("darwin-")));
        assert hosts != null;
        for (File hostOut : hosts) {
            String platformDir = getPlatformDirFromHostOut(hostOut);
            if (platformDir != null) {
                return platformDir;
            }
        }

        return null;
    }

    private static String getPlatformDirFromHostOut(File out) {
        if (!out.isDirectory()) {
            return null;
        }
        File sdkDir = new File(out, "sdk");
        if (!sdkDir.isDirectory()) {
            return null;
        }
        File[] sdkDirs = sdkDir.listFiles(path -> {
            // We need to search for $TARGET_PRODUCT (usually, sdk_phone_armv7)
            return path.isDirectory() && path.getName().startsWith("sdk");
        });
        assert sdkDirs != null;
        for (File dir : sdkDirs) {
            String platformDir = getPlatformDirFromHostOutSdkSdk(dir);
            if (platformDir != null) {
                return platformDir;
            }
        }
        return null;
    }

    private static String getPlatformDirFromHostOutSdkSdk(File sdkDir) {
        File[] possibleSdks = sdkDir.listFiles(
                path -> path.isDirectory() && path.getName().contains("android-sdk"));
        assert possibleSdks != null;
        for (File possibleSdk : possibleSdks) {
            File platformsDir = new File(possibleSdk, "platforms");
            File[] platforms = platformsDir.listFiles(
                    path -> path.isDirectory() && path.getName().startsWith("android-"));
            if (platforms == null || platforms.length == 0) {
                continue;
            }
            Arrays.sort(platforms, (o1, o2) -> {
                final int MAX_VALUE = 1000;
                String suffix1 = o1.getName().substring("android-".length());
                String suffix2 = o2.getName().substring("android-".length());
                int suff1, suff2;
                try {
                    suff1 = Integer.parseInt(suffix1);
                } catch (NumberFormatException e) {
                    suff1 = MAX_VALUE;
                }
                try {
                    suff2 = Integer.parseInt(suffix2);
                } catch (NumberFormatException e) {
                    suff2 = MAX_VALUE;
                }
                if (suff1 != MAX_VALUE || suff2 != MAX_VALUE) {
                    return suff2 - suff1;
                }
                return suffix2.compareTo(suffix1);
            });
            return platforms[0].getAbsolutePath();
        }
        return null;
    }

    private static String getTestResDir() {
        String resourceDir = System.getProperty(RESOURCE_DIR_PROPERTY);
        if (resourceDir != null && !resourceDir.isEmpty() && new File(resourceDir).isDirectory()) {
            return resourceDir;
        }
        // TEST_RES_DIR not explicitly set. Fallback to the class's source location.
        try {
            URL location = RenderTestBase.class.getProtectionDomain().getCodeSource().getLocation();
            return new File(location.getPath()).exists() ? location.getPath() : null;
        } catch (NullPointerException e) {
            // Prevent a lot of null checks by just catching the exception.
            return null;
        }
    }

    /**
     * Initialize the bridge and the resource maps.
     */
    @BeforeClass
    public static void beforeClass() {
        File data_dir = new File(PLATFORM_DIR, "data");
        File res = new File(data_dir, "res");
        sFrameworkRepo =
                FrameworkResourceRepository.create(
                        res.getAbsoluteFile().toPath(), Collections.emptySet(), null, false);

        File projectRes = new File(TEST_RES_DIR + "/" + APP_TEST_RES);
        sProjectResources =
                AarSourceResourceRepository.create(
                        projectRes.getAbsoluteFile().toPath(), "Application");

        File fontLocation = new File(FONT_DIR);
        File buildProp = new File(PLATFORM_DIR, "build.prop");
        File attrs = new File(res, "values" + File.separator + "attrs.xml");
        
        String[] keyboardPaths = new String[] { KEYBOARD_DIR + "/Generic.kcm" };
        sBridge = new Bridge();
        sBridge.init(
                ConfigGenerator.loadProperties(buildProp),
                fontLocation,
                NATIVE_LIB_DIR_PATH,
                ICU_DATA_PATH,
                HYPHEN_DATA_DIR,
                keyboardPaths,
                ConfigGenerator.getEnumMap(attrs),
                getLayoutLog());
        Bridge.getLock().lock();
        try {
            Bridge.setLog(getLayoutLog());
        } finally {
            Bridge.getLock().unlock();
        }
    }

    @AfterClass
    public static void tearDown() {
        sLayoutLibLog = null;
        sFrameworkRepo = null;
        sProjectResources = null;
        sLogger = null;
        sBridge = null;
    }

    @NonNull
    protected static RenderResult render(com.android.ide.common.rendering.api.Bridge bridge,
            SessionParams params,
            long frameTimeNanos) {
        return render(bridge, params, frameTimeNanos, null);
    }

    @NonNull
    protected static RenderResult render(com.android.ide.common.rendering.api.Bridge bridge,
            SessionParams params,
            long frameTimeNanos,
            @Nullable RenderSessionListener listener) {
        // TODO: Set up action bar handler properly to test menu rendering.
        // Create session params.
        System_Delegate.setBootTimeNanos(TimeUnit.MILLISECONDS.toNanos(871732800000L));
        System_Delegate.setNanosTime(TimeUnit.MILLISECONDS.toNanos(871732800000L));
        RenderSession session = bridge.createSession(params);

        try {
            if (frameTimeNanos != -1) {
                session.setElapsedFrameTimeNanos(frameTimeNanos);
            }

            if (!session.getResult().isSuccess()) {
                getLogger().error(session.getResult().getException(),
                        session.getResult().getErrorMessage());
            }
            else {
                // Render the session with a timeout of 50s.
                Result renderResult = session.render(50000);
                if (!renderResult.isSuccess()) {
                    getLogger().error(session.getResult().getException(),
                            session.getResult().getErrorMessage());
                }
            }
            if (listener != null) {
                listener.beforeDisposed(session);
            }

            return RenderResult.getFromSession(session);
        } finally {
            session.dispose();
        }
    }

    /**
     * Compares the golden image with the passed image
     */
    protected static void verify(@NonNull String goldenImageName, @NonNull BufferedImage image) {
        try {
            String goldenImagePath = APP_TEST_DIR + "/golden/" + goldenImageName;
            ImageUtils.requireSimilar(goldenImagePath, image);
        } catch (IOException e) {
            getLogger().error(e, e.getMessage());
        }
    }

    /**
     * Create a new rendering session and test that rendering the given layout doesn't throw any
     * exceptions and matches the provided image.
     * <p>
     * If frameTimeNanos is >= 0 a frame will be executed during the rendering. The time indicates
     * how far in the future is.
     */
    @Nullable
    protected static RenderResult renderAndVerify(SessionParams params, String goldenFileName,
            long frameTimeNanos) throws ClassNotFoundException {
        RenderResult result = RenderTestBase.render(sBridge, params, frameTimeNanos);
        assertNotNull(result.getImage());
        verify(goldenFileName, result.getImage());

        return result;
    }

    /**
     * Create a new rendering session and test that rendering the given layout doesn't throw any
     * exceptions and matches the provided image.
     */
    @Nullable
    protected static RenderResult renderAndVerify(SessionParams params, String goldenFileName)
            throws ClassNotFoundException {
        return RenderTestBase.renderAndVerify(params, goldenFileName, TimeUnit.SECONDS.toNanos(2));
    }

    protected static ILayoutLog getLayoutLog() {
        if (sLayoutLibLog == null) {
            sLayoutLibLog = new ILayoutLog() {
                @Override
                public void warning(@Nullable String tag, @NonNull String message, @Nullable Object viewCookie,
                        @Nullable Object data) {
                    System.out.println("Warning " + tag + ": " + message);
                    failWithMsg(message);
                }

                @Override
                public void fidelityWarning(@Nullable String tag, String message,
                        Throwable throwable, Object cookie, Object data) {

                    System.out.println("FidelityWarning " + tag + ": " + message);
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                    failWithMsg(message == null ? "" : message);
                }

                @Override
                public void error(@Nullable String tag, @NonNull String message, @Nullable Object viewCookie,
                        @Nullable Object data) {
                    System.out.println("Error " + tag + ": " + message);
                    failWithMsg(message);
                }

                @Override
                public void error(@Nullable String tag, @NonNull String message, @Nullable Throwable throwable,
                        @Nullable Object viewCookie, @Nullable Object data) {
                    System.out.println("Error " + tag + ": " + message);
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                    failWithMsg(message);
                }

                @Override
                public void logAndroidFramework(int priority, String tag, String message) {
                    System.out.println("Android framework message " + tag + ": " + message);
                }
            };
        }
        return sLayoutLibLog;
    }

    protected static void ignoreAllLogging() {
        sLayoutLibLog = new ILayoutLog() {};
        sLogger = new ILogger() {
            @Override
            public void error(Throwable t, String msgFormat, Object... args) {
            }

            @Override
            public void warning(String msgFormat, Object... args) {
            }

            @Override
            public void info(String msgFormat, Object... args) {
            }

            @Override
            public void verbose(String msgFormat, Object... args) {
            }
        };
    }

    protected static ILogger getLogger() {
        if (sLogger == null) {
            sLogger = new ILogger() {
                @Override
                public void error(Throwable t, @Nullable String msgFormat, Object... args) {
                    if (t != null) {
                        t.printStackTrace();
                    }
                    failWithMsg(msgFormat == null ? "" : msgFormat, args);
                }

                @Override
                public void warning(@NonNull String msgFormat, Object... args) {
                    failWithMsg(msgFormat, args);
                }

                @Override
                public void info(@NonNull String msgFormat, Object... args) {
                    // pass.
                }

                @Override
                public void verbose(@NonNull String msgFormat, Object... args) {
                    // pass.
                }
            };
        }
        return sLogger;
    }

    private static void failWithMsg(@NonNull String msgFormat, Object... args) {
        sRenderMessages.add(args == null ? msgFormat : String.format(msgFormat, args));
    }

    @Before
    public void beforeTestCase() {
        // Default class loader with access to the app classes
        mDefaultClassLoader = new ModuleClassLoader(APP_CLASSES_LOCATION, getClass().getClassLoader());
        sRenderMessages.clear();
    }

    @NonNull
    protected LayoutPullParser createParserFromPath(String layoutPath)
            throws FileNotFoundException {
        return LayoutPullParser.createFromPath(APP_TEST_RES + "/layout/" + layoutPath);
    }

    /**
     * Create a new rendering session and test that rendering the given layout on nexus 5
     * doesn't throw any exceptions and matches the provided image.
     */
    @Nullable
    protected RenderResult renderAndVerify(String layoutFileName, String goldenFileName,
            boolean decoration)
            throws ClassNotFoundException, FileNotFoundException {
        return renderAndVerify(layoutFileName, goldenFileName, ConfigGenerator.NEXUS_5, decoration);
    }

    /**
     * Create a new rendering session and test that rendering the given layout on given device
     * doesn't throw any exceptions and matches the provided image.
     */
    @Nullable
    protected RenderResult renderAndVerify(String layoutFileName, String goldenFileName,
            ConfigGenerator deviceConfig, boolean decoration) throws ClassNotFoundException,
            FileNotFoundException {
        SessionParams params = createSessionParams(layoutFileName, deviceConfig);
        if (!decoration) {
            params.setForceNoDecor();
        }
        return renderAndVerify(params, goldenFileName);
    }

    protected SessionParams createSessionParams(String layoutFileName, ConfigGenerator deviceConfig)
            throws ClassNotFoundException, FileNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = createParserFromPath(layoutFileName);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        // TODO: Set up action bar handler properly to test menu rendering.
        // Create session params.
        return getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(deviceConfig)
                .setCallback(layoutLibCallback)
                .build();
    }

    /**
     * Returns a pre-configured {@link SessionParamsBuilder} for target API 22, Normal rendering
     * mode, AppTheme as theme and Nexus 5.
     */
    @NonNull
    protected SessionParamsBuilder getSessionParamsBuilder() {
        return new SessionParamsBuilder()
                .setLayoutLog(getLayoutLog())
                .setFrameworkResources(sFrameworkRepo)
                .setConfigGenerator(ConfigGenerator.NEXUS_5)
                .setProjectResources(sProjectResources)
                .setTheme("AppTheme", true)
                .setRenderingMode(RenderingMode.NORMAL)
                .setTargetSdk(28)
                .setFlag(RenderParamsFlags.FLAG_DO_NOT_RENDER_ON_CREATE, true)
                .setAssetRepository(new TestAssetRepository(TEST_RES_DIR + "/" + APP_TEST_ASSET));
    }
}
