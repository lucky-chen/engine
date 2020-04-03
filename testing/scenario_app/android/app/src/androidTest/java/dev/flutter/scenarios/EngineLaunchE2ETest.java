// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.flutter.scenarios;

import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.junit4.statement.UiThreadStatement;
import androidx.test.runner.AndroidJUnit4;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngine.EngineLifecycleListener;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EngineLaunchE2ETest {
  @Test
  public void smokeTestEngineLaunch() throws Throwable {
    Context applicationContext = InstrumentationRegistry.getTargetContext();
    // Specifically, create the engine without running FlutterMain first.
    final AtomicReference<FlutterEngine> engine = new AtomicReference<>();

    // Run the production under test on the UI thread instead of annotating the whole test
    // as @UiThreadTest because having the message handler and the CompletableFuture both being
    // on the same thread will create deadlocks.
    UiThreadStatement.runOnUiThread(() -> engine.set(new FlutterEngine(applicationContext)));
    CompletableFuture<Boolean> statusReceived = new CompletableFuture<>();

    // The default Dart main entrypoint sends back a platform message on the "scenario_status"
    // channel. That will be our launch success assertion condition.
    engine
        .get()
        .getDartExecutor()
        .setMessageHandler(
            "scenario_status", (byteBuffer, binaryReply) -> statusReceived.complete(Boolean.TRUE));

    // Launching the entrypoint will run the Dart code that sends the "scenario_status" platform
    // message.
    UiThreadStatement.runOnUiThread(
        () ->
            engine
                .get()
                .getDartExecutor()
                .executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault()));

    try {
      Boolean result = statusReceived.get(10, TimeUnit.SECONDS);
      if (!result) {
        fail("expected message on scenario_status not received");
      }
    } catch (ExecutionException e) {
      fail(e.getMessage());
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (TimeoutException e) {
      fail("timed out waiting for engine started signal");
    }
    // If it gets to here, statusReceived is true.
  }

  @Test
  public void smokeTestEngineAsyncLaunch() {
    final CompletableFuture<Boolean> statusReceived = new CompletableFuture<>();
    UiThreadStatement.runOnUiThread(
        () -> {
          FlutterEngine engine = new FlutterEngine();
          engine.initAsync(
              this,
              new EngineLifecycleListener() {
                @Override
                public void onPreEngineRestart() {}

                @Override
                public void onAsyncAttachEnd(boolean success) {}

                @Override
                public void onAsyncCreateEngineEnd(boolean success) {
                  if (!success) {
                    Log.e("flutter", "========> init error ! <=======================");

                  } else {
                    engine.getDartExecutor().executeDartEntrypoint(DartEntrypoint.createDefault());
                  }
                  statusReceived.complete(success);
                }
              });
        });
    try {
      Boolean result = statusReceived.get(10, TimeUnit.SECONDS);
      if (!result) {
        fail("flutterEngien async init failed");
      }
    } catch (ExecutionException e) {
      fail(e.getMessage());
    } catch (InterruptedException e) {
      fail(e.getMessage());
    } catch (TimeoutException e) {
      fail("timed out waiting for EngineAsyncLaunch");
    }
  }
}
