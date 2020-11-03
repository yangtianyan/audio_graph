package work.ksprogram.audio_graph

import android.content.ContentValues.TAG
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import work.ksprogram.audio_graph.audio.AudioException
import work.ksprogram.audio_graph.models.*
import work.ksprogram.audio_graph.nodes.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.Iterable
import kotlin.collections.Iterator
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class AudioGradphBuilderPluginTest: MethodChannel.MethodCallHandler {
//    override fun prepareToPlay() {
//
//    }

    companion object {
        val mapper = jacksonObjectMapper()
    }

    fun getMap(jsonString: String?): HashMap<String, Any>? {
        val jsonObject: JSONObject
        try {
            jsonObject = JSONObject(jsonString)
            val keyIter: Iterator<String> = jsonObject.keys()
            var key: String
            var value: Any
            var valueMap = HashMap<String, Any>()
            while (keyIter.hasNext()) {
                key = keyIter.next()
                value = jsonObject[key] as Any
                valueMap[key] = value
            }
            return valueMap
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    private fun toMap(json:JSONObject) : Map<String,Any>
    {
        val dict: MutableMap<String, Any> = mutableMapOf();
        var keys = json.keys()
        while (keys.hasNext()){
            val key = keys.next() as String
            dict.put(key, json.get(key))
        }
        return dict;
    }

    private fun build(call: MethodCall, result: MethodChannel.Result){
        val jsonGraphData = (call.arguments as ArrayList<Any>)[0] as String
//        var graphModel: AudioGraphModel
        Log.e(TAG, "_build _build _build _build")
        var objc = JSONObject(jsonGraphData)
        var map = objc?.getJSONObject("connections")
        var list = objc?.getJSONArray("nodes")
//        var a : AudioGraphModel = mapper.readValue(jsonGraphData)
        val dict: Map<String, Int> = toMap(map) as Map<String, Int>
        
        val arr: MutableList<AudioNode> = mutableListOf()
        for (i in 0..(list.length() - 1)){
            var obj:JSONObject = list.get(i) as JSONObject
            val id : Int = obj.getInt("id")
            val volume : Double = obj.getDouble("volume")
            val name : String = obj.getString("name")
            val parameters:Map<String,String> = toMap(obj.getJSONObject("parameters")) as Map<String,String>
            val _inputs:JSONArray = obj.getJSONArray("inputs")
            val _outputs:JSONArray = obj.getJSONArray("outputs")

            val inputs:MutableList<InputPin> = mutableListOf()
            val outputs:MutableList<OutputPin> = mutableListOf()

            for (j in 0..(_inputs.length() - 1)) {

                var input:JSONObject = _inputs.get(j) as JSONObject
                Log.e(TAG, "_inputs _inputs _inputs _inputs  ${input}")
                val _format:JSONObject = input.getJSONObject("format")
                val format: AudioFormat = AudioFormat(channels = _format.getInt("channels"),sample_rate = _format.getInt("sample_rate"))
                val id: Int = input.getInt("id")
                val pin : InputPin = InputPin(id = id,format = format)
                inputs.add(pin)
            }
//
            for (j in 0..(_outputs.length() - 1)) {
                var output:JSONObject = _outputs.get(j) as JSONObject
                Log.e(TAG, "_outputs _outputs _outputs _outputs  ${output}")
//                val _format:JSONObject = output.getJSONObject("format")
//                var channels : Int = _format.getInt("channels")
//                var sample_rate : Int = _format.getInt("sample_rate")
//                channels = 2
//                sample_rate = 44100
                val format: AudioFormat = AudioFormat(channels = 2,sample_rate = 44100)
                val id: Int = output.getInt("id")
                val pin : OutputPin = OutputPin(id = id,format = format)
                outputs.add(pin)
            }
            val node: AudioNode = AudioNode(id = id, inputs = inputs, name = name, outputs = outputs, parameters = parameters, volume = volume)
            arr.add(node)
        }


        Log.e("build build build", "objc: ===> ${JSONObject(jsonGraphData)} ")
        var graphModel: AudioGraphModel =
                AudioGraphModel(connections = dict, nodes = arr as List<AudioNode>)
//                mapper.readValue(jsonGraphData)
//        graphModerl = a;

        //val graphModel: AudioGraphModel =
        val nodes: MutableList<AudioNativeNode> = mutableListOf()
        for (node in graphModel.nodes) {
            val nativeNode = when(node.name) {
                AudioFilePlayerNode.nodeName -> AudioFilePlayerNode(node.id, node.parameters["path"] as String)
                AudioMixerNode.nodeName -> AudioMixerNode(node.id)
                AudioDeviceOutputNode.nodeName -> AudioDeviceOutputNode(node.id)
                else -> throw Error("Unknown node name")
            }

            if (nativeNode is AudioOutputNode) {
                nativeNode.prepare()
            }

            print(nativeNode)
            nodes.add(nativeNode)
        }

        val connections = graphModel.connections.map { AudioNodeConnection(it.key.toInt(), it.value) }
        for (connection in connections) {
            var node = getNode(connection.input, graphModel.nodes, nodes)
            connection.inputNode = node

            if (connection.outputNode != null) {
                continue
            }

            node = getNode(connection.output, graphModel.nodes, nodes)
            connection.outputNode = node as AudioOutputNode
        }

        try {
            for (connection in connections) {
                if (connection.inputNode is AudioMultipleInputNode) {
                    val multipleInputNode = connection.inputNode as AudioMultipleInputNode
                    multipleInputNode.addInputNode(connection.outputNode!!)
                }

                if (connection.inputNode is AudioSingleInputNode) {
                    val singleInputNode = connection.inputNode as AudioSingleInputNode
                    singleInputNode.setInputNode(connection.outputNode!!)
                }
            }
        } catch (ex: AudioException) {
            result.error(ex.errorCode, ex.message, null)
            return
        }

        for (node in nodes) {
            AudioNativeNode.nodes[node.id] = node
        }

        val graph = AudioGraph(nodes)
        val id = AudioGraph.id.generateId()
        AudioGraph.graphs[id] = graph

        result.success(id)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.e("onMethodCall", "onMethodCall:dasdsadasdsadsadsdasdsadasdasdasd");
        when(call.method) {
            "build" -> build(call, result)
            "abcdefg" -> {
                val jsonGraphData = (call.arguments as ArrayList<Any>)[0] as String
                var graphModel: AudioGraphModel
                Log.e(TAG, "_build _build _build _build")
                graphModel = mapper.readValue(jsonGraphData)

                //val graphModel: AudioGraphModel =
                val nodes: MutableList<AudioNativeNode> = mutableListOf()
                for (node in graphModel.nodes) {
                    val nativeNode = when (node.name) {
                        AudioFilePlayerNode.nodeName -> AudioFilePlayerNode(node.id, node.parameters["path"] as String)
                        AudioMixerNode.nodeName -> AudioMixerNode(node.id)
                        AudioDeviceOutputNode.nodeName -> AudioDeviceOutputNode(node.id)
                        else -> throw Error("Unknown node name")
                    }

                    if (nativeNode is AudioOutputNode) {
                        nativeNode.prepare()
                    }

                    print(nativeNode)
                    nodes.add(nativeNode)
                }

                val connections = graphModel.connections.map { AudioNodeConnection(it.key.toInt(), it.value) }
                for (connection in connections) {
                    var node = getNode(connection.input, graphModel.nodes, nodes)
                    connection.inputNode = node

                    if (connection.outputNode != null) {
                        continue
                    }

                    node = getNode(connection.output, graphModel.nodes, nodes)
                    connection.outputNode = node as AudioOutputNode
                }

                try {
                    for (connection in connections) {
                        if (connection.inputNode is AudioMultipleInputNode) {
                            val multipleInputNode = connection.inputNode as AudioMultipleInputNode
                            multipleInputNode.addInputNode(connection.outputNode!!)
                        }

                        if (connection.inputNode is AudioSingleInputNode) {
                            val singleInputNode = connection.inputNode as AudioSingleInputNode
                            singleInputNode.setInputNode(connection.outputNode!!)
                        }
                    }
                } catch (ex: AudioException) {
                    result.error(ex.errorCode, ex.message, null)
                    return
                }

                for (node in nodes) {
                    AudioNativeNode.nodes[node.id] = node
                }

                val graph = AudioGraph(nodes)
                val id = AudioGraph.id.generateId()
                AudioGraph.graphs[id] = graph

                result.success(id)
            }
        }
    }

    private fun getNode(pinId: Int, nodes: Iterable<AudioNode>, nativeNodes: Iterable<AudioNativeNode>): AudioNativeNode {
        var pinParent: AudioNode? = null
        for (node in nodes) {
            for (pin in node.inputs) {
                if (pin.id == pinId) {
                    pinParent = node
                    break
                }
            }

            for (pin in node.outputs) {
                if (pin.id == pinId) {
                    pinParent = node
                    break
                }
            }
        }

        for (native in nativeNodes) {
            if (native.id == pinParent!!.id) {
                return native
            }
        }

        throw Exception("Invalid graph connections")
    }
}
