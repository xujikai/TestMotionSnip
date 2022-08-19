package com.app.testmotionsnip.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.testmotionsnip.bean.MotionBean
import com.app.testmotionsnip.databinding.ActivityGltfBinding
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.google.android.filament.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis

class GltfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGltfBinding

    private lateinit var modelViewer: ModelViewer
    private val automation by lazy(LazyThreadSafetyMode.NONE) { AutomationEngine() }
    private val viewerContent by lazy(LazyThreadSafetyMode.NONE) { AutomationEngine.ViewerContent() }

    private var modelName = "qianyu"
    private var modelBgName = "venetian_crossroads_2k"
    private var motionFps = 0.033
    private var currentMotionFileName = "motions/666_girl.json"
    private var motionBean: MotionBean? = null

    private var isNeedUpdate = false
    private var mFrameIndex = 0
    private var mBodySimpleFloatArr = FloatArray(16) // 身体某一关节的数据

    companion object {
        init {
            Utils.init()
        }

        fun start(context: Context) {
            val intent = Intent(context, GltfActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGltfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initModel()

        lifecycleScope.launch {
            while (true) {
                val millis = measureTimeMillis { drawFrame() }
                delay((motionFps * 1000L - millis).toLong())
            }
        }

        binding.btnStart.setOnClickListener {
            mFrameIndex = 0
            isNeedUpdate = true
        }
        binding.btnStop.setOnClickListener {
            motionBean = null
            mFrameIndex = 0
        }
    }

    private fun initModel() {
        modelViewer = ModelViewer(binding.textureView)
        viewerContent.view = modelViewer.view
        viewerContent.sunlight = modelViewer.light
        viewerContent.lightManager = modelViewer.engine.lightManager
        viewerContent.scene = modelViewer.scene
        viewerContent.renderer = modelViewer.renderer

        binding.textureView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
            true
        }
        loadGltfModel()
        loadGltfBg()
    }

    private fun loadGltfModel() {
        val buffer = readCompressedAsset("${modelName}/${modelName}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readCompressedAsset("${modelName}/${uri}") }
        updateRootTransform()
    }

    private fun loadGltfBg() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        readCompressedAsset("envs/${modelBgName}/${modelBgName}_ibl.ktx").let {
            scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }
        readCompressedAsset("envs/${modelBgName}/${modelBgName}_skybox.ktx").let {
            scene.skybox = KTX1Loader.createSkybox(engine, it)
        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = this.assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }

    private fun drawFrame() {
        if (modelViewer.progress == 1.0f) {
            modelViewer.animator?.apply {
                if (isNeedUpdate) {
                    isNeedUpdate = false
                    readMotionJson(currentMotionFileName)
                    motionBean?.apply {
                        motionFps = totalTime / curves[0].rotas.size
                    }
                }
                setBodyMorph()
                updateBoneMatrices() // 更新
            }
        }
        modelViewer.render((motionFps * 1_000_000_000L).toLong())
    }

    private fun readMotionJson(fileName: String) {
        val res = ResourceUtils.readAssets2String(fileName)
        motionBean = GsonUtils.fromJson(res, MotionBean::class.java)
    }

    /**
     * 设置身体形变
     */
    private fun setBodyMorph() {
        val manager = modelViewer.engine.transformManager
        val asset = modelViewer.asset

        motionBean?.curves?.forEachIndexed { _, it ->
            if (mFrameIndex >= it.rotas.size) mFrameIndex = 0
            val rota = it.rotas[mFrameIndex]
            val x = rota.x
            val y = rota.y
            val z = rota.z
            val w = rota.w

            val m00 = 1 - 2 * pow(y, 2f) - 2 * pow(z, 2f);
            val m01 = 2 * x * y - 2 * w * z;
            val m02 = 2 * x * z + 2 * w * y;

            val m10 = 2 * x * y + 2 * w * z;
            val m11 = 1 - 2 * pow(x, 2f) - 2 * pow(z, 2f);
            val m12 = 2 * y * z - 2 * w * x;

            val m20 = 2 * x * z - 2 * w * y;
            val m21 = 2 * y * z + 2 * w * x;
            val m22 = 1 - 2 * pow(x, 2f) - 2 * pow(y, 2f);

            val entity = asset?.getFirstEntityByName(it.name) ?: return@forEachIndexed
            manager.getTransform(entity - 1, mBodySimpleFloatArr)
            mBodySimpleFloatArr[0] = m00
            mBodySimpleFloatArr[1] = -m10
            mBodySimpleFloatArr[2] = -m20
            mBodySimpleFloatArr[4] = -m01
            mBodySimpleFloatArr[5] = m11
            mBodySimpleFloatArr[6] = m21
            mBodySimpleFloatArr[8] = -m02
            mBodySimpleFloatArr[9] = m12
            mBodySimpleFloatArr[10] = m22

            manager.setTransform(entity - 1, mBodySimpleFloatArr)
        }

        mFrameIndex++
    }

}