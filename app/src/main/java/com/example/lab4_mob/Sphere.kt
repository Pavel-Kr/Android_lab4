package com.example.lab4_mob

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun getErrors(){
    var error = GLES20.glGetError()
    while (error != GLES20.GL_NO_ERROR){
        when(error){
            GLES20.GL_INVALID_ENUM -> println("ERROR: Invalid enum")
            GLES20.GL_INVALID_VALUE -> println("ERROR: Invalid value")
            GLES20.GL_INVALID_OPERATION -> println("ERROR: Invalid operation")
            GLES20.GL_INVALID_FRAMEBUFFER_OPERATION -> println("ERROR: Invalid framebuffer operation")
            GLES20.GL_OUT_OF_MEMORY -> println("ERROR: Out of memory")
        }
    }
}

fun vecLength(x: Double, y: Double, z: Double): Double{
    return sqrt(x*x + y*y + z*z)
}

class Sphere(cX: Float, cY: Float, cZ: Float, radius: Float, private val shaderProgram: Int) {
    private val vertices: MutableList<Float> = mutableListOf()
    private val indices: MutableList<Short> = mutableListOf()
    private val normCoords: MutableList<Float> = mutableListOf()

    private val stackCount: Int = 26
    private val sectorCount: Int = 26

    private val vertexBuffer: FloatBuffer
    private val normBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private val modelMatrix = FloatArray(16)
    private val transformationMatrix = FloatArray(16)
    private var originMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var lightMatrix = FloatArray(16)

    private val vertexStride = 3 * Float.SIZE_BYTES
    private val normStride = 3 * Float.SIZE_BYTES
    private val color = floatArrayOf(0.5607843f, 0.843137f, 0.858823f, 1.0f)
    private val lightColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private val lightOrigin = floatArrayOf(0f, 0f, 0f, 1.0f)
    private val lightPos = floatArrayOf(0f, 0f, 0f, 1.0f)
    private var viewPos = floatArrayOf(0f, 0f, 20f, 1f)

    private val positionHandle: Int
    private val normalHandle: Int
    private val colorHandle: Int
    private val lightColorHandle: Int
    private val lightPosHandle: Int
    private val viewPosHandle: Int
    private val mvpMatrixLocation: Int
    private val modelMatrixHandle: Int

    init {
        val stackStep = Math.PI / stackCount
        val sectorStep = 2 * Math.PI / sectorCount
        for(i in 0 .. stackCount){
            val stackAngle = Math.PI / 2 - i * stackStep
            val xz = radius * cos(stackAngle)
            val y = radius * sin(stackAngle)
            for(j in 0 .. sectorCount){
                val sectorAngle = j * sectorStep
                val x = xz * sin(sectorAngle)
                val z = xz * cos(sectorAngle)
                vertices.add((x + cX).toFloat())
                vertices.add((y + cY).toFloat())
                vertices.add((z + cZ).toFloat())

                var normX = x - cX
                var normY = y - cY
                var normZ = z - cZ

                val len = vecLength(normX, normY, normZ)
                normX /= len
                normY /= len
                normZ /= len

                normCoords.add(normX.toFloat())
                normCoords.add(normY.toFloat())
                normCoords.add(normZ.toFloat())
            }
        }

        for(i in 0 until stackCount){
            var k1 = i * (sectorCount + 1)
            var k2 = k1 + sectorCount + 1
            for(j in 0 until sectorCount){
                if(i != 0){
                    indices.add(k1.toShort())
                    indices.add(k2.toShort())
                    indices.add((k1 + 1).toShort())
                }
                if((i + 1) != stackCount){
                    indices.add((k1 + 1).toShort())
                    indices.add(k2.toShort())
                    indices.add((k2 + 1).toShort())
                }
                k1++
                k2++
            }
        }

        vertexBuffer =
            ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices.toFloatArray())
                    position(0)
                }
            }

        normBuffer =
            ByteBuffer.allocateDirect(normCoords.size * Float.SIZE_BYTES).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(normCoords.toFloatArray())
                    position(0)
                }
            }

        indexBuffer =
            ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(indices.toShortArray())
                    position(0)
                }
            }

        GLES20.glUseProgram(shaderProgram)
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )

        normalHandle = GLES20.glGetAttribLocation(shaderProgram, "normal")
        GLES20.glVertexAttribPointer(
            normalHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            normStride,
            normBuffer
        )

        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "objectColor")
        lightColorHandle = GLES20.glGetUniformLocation(shaderProgram, "lightColor")
        lightPosHandle = GLES20.glGetUniformLocation(shaderProgram, "lightPos")
        viewPosHandle = GLES20.glGetUniformLocation(shaderProgram, "viewPos")
        mvpMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        modelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "model")
        GLES20.glUseProgram(0)

        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.setIdentityM(originMatrix, 0)
        Matrix.setIdentityM(rotationMatrix, 0)
    }

    fun draw(vpMatrix: FloatArray){
        GLES20.glUseProgram(shaderProgram)

        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )

        GLES20.glVertexAttribPointer(
            normalHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            normStride,
            normBuffer
        )

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)

        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glUniform4fv(lightColorHandle, 1, lightColor, 0)
        Matrix.multiplyMV(lightPos, 0, lightMatrix, 0, lightOrigin, 0)
        GLES20.glUniform4fv(lightPosHandle, 1, lightPos, 0)
        GLES20.glUniform4fv(viewPosHandle, 1, viewPos, 0)

        Matrix.multiplyMM(modelMatrix, 0, originMatrix, 0, transformationMatrix, 0)
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixLocation, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glUseProgram(0)
        //getErrors()
    }
    fun translate(x: Float, y: Float, z: Float){
        Matrix.translateM(transformationMatrix, 0, x, y, z)
    }

    fun rotate(angle: Float, x: Float, y: Float, z: Float){
        Matrix.rotateM(transformationMatrix, 0, angle, x, y, z)
    }

    fun rotateSelf(angle: Float, x: Float, y: Float, z: Float){
        Matrix.rotateM(rotationMatrix, 0, angle, x, y, z)
    }

    fun scale(x: Float, y: Float, z: Float){
        Matrix.scaleM(transformationMatrix, 0, x, y, z)
    }

    fun setRotate(angle: Float, x: Float, y: Float, z: Float){
        Matrix.setRotateM(transformationMatrix, 0, angle, x, y, z)
    }

    fun getTransformationMatrix(): FloatArray {
        return transformationMatrix
    }

    fun applyOriginTransformation(origin: FloatArray){
        originMatrix = origin
    }

    fun setLightMatrix(light: FloatArray){
        lightMatrix = light
    }

    fun setViewPos(vPos: FloatArray){
        viewPos = vPos
    }
}