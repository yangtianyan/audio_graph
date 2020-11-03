package work.ksprogram.audio_graph

import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.ArrayList

/** AudioGraphPlugin */
class AudioGraphPlugin: FlutterPlugin, MethodCallHandler {
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val fileChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "audio_graph/file")
    fileChannel.setMethodCallHandler(AudioFilePlugin())

    val graphBuilderChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "audio_graph/graph_builderaaaa")
    graphBuilderChannel.setMethodCallHandler(AudioGradphBuilderPluginTest())

    val nodeChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "audio_graph/node")
    nodeChannel.setMethodCallHandler(AudioNodePlugin())

    val graphChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "audio_graph/graph")
    graphChannel.setMethodCallHandler(AudioGraphPlugin())
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when(call.method) {
      "dispose" -> {
        val id = (call.arguments as ArrayList<Any>)[0] as Int
        AudioGraph.graphs[id]?.dispose()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
  }
}
