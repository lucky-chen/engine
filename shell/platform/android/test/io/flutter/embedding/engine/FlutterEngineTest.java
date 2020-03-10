package test.io.flutter.embedding.engine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterJNI;
import io.flutter.embedding.engine.loader.FlutterLoader;
import io.flutter.embedding.engine.FlutterEngine.EngineLifecycleListener;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class FlutterEngineTest {
  @Test
  public void itDoesNotCrashIfGeneratedPluginRegistrantIsUnavailable() {
    FlutterJNI flutterJNI = mock(FlutterJNI.class);
    when(flutterJNI.isAttached()).thenReturn(true);

    FlutterEngine flutterEngine = new FlutterEngine(
        RuntimeEnvironment.application,
        mock(FlutterLoader.class),
        flutterJNI,
        new String[] {},
        true
    );
    // The fact that the above constructor executed without error means that
    // it dealt with a non-existent GeneratedPluginRegistrant.
  }
  @Test
  public void itRunSuccessWhenAsyncInitEngine(){
    EngineLifecycleListener listener = new EngineLifecycleListener() {
      @Override
      public void onPreEngineRestart() {

      }

      @Override
      public void onEngineInit() {
        FlutterEngine engine = FlutterEngineCache.getInstance().get("my_engine_id");
        engine.getDartExecutor().executeDartEntrypoint(
            DartEntrypoint.createDefault()
        );
      }
    };
    FlutterEngine engine = new FlutterEngine(
        this,
        FlutterLoader.getInstance(),
        new FlutterJNI(),
        null,
        true,
        listener
    );
    FlutterEngineCache.getInstance().put("my_engine_id", engine);
  }
}
