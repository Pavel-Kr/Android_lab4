package com.example.lab4_mob

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

class MyGLRenderer(private val context: Context): GLSurfaceView.Renderer {
    private val vpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewPos = floatArrayOf(0f, 0f, 20f, 1f)

    private val shaderHandler = ShaderHandler()

    private var sphereShader by Delegates.notNull<Int>()
    private var lightShader by Delegates.notNull<Int>()

    private lateinit var sphere: Sphere
    private lateinit var lightSource: Sphere

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        sphereShader = shaderHandler.loadShaders(context, R.raw.vertex, R.raw.fragment)
        lightShader = shaderHandler.loadShaders(context, R.raw.vertex, R.raw.fragment_light)

        sphere = Sphere(0f, 0f, 0f, 1f, sphereShader)
        sphere.setViewPos(viewPos)
        lightSource = Sphere(0f, 0f, 0f, 0.3f, lightShader)
        lightSource.translate(2f, 2f, -2f);
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, viewPos[0], viewPos[1], viewPos[2], 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        lightSource.translate(-2f, -2f, 2f)
        lightSource.rotate(1f, 0f, 1f, 0f)
        lightSource.translate(2f, 2f, -2f)

        lightSource.draw(vpMatrix)
        sphere.setLightMatrix(lightSource.getTransformationMatrix())
        sphere.draw(vpMatrix)
    }

}