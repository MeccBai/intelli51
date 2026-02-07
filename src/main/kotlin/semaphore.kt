package com.intelli51.intelli51

import com.sun.jna.*
import com.sun.jna.win32.*

interface WinKernel32 : StdCallLibrary {
    companion object {
        val INSTANCE: WinKernel32 = Native.load("kernel32", WinKernel32::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }

    fun CreateSemaphoreW(attr: Pointer?, initial: Int, max: Int, name: WString): Pointer?
    fun WaitForSingleObject(handle: Pointer, ms: Int): Int
    fun ReleaseSemaphore(handle: Pointer, count: Int, prevCount: IntArray?): Boolean
    fun CloseHandle(handle: Pointer): Boolean
    fun GetLastError(): Int
}

class NamedSemaphore(val name: String, initialCount: Int = 0, maxCount: Int = 1) {
    private val hSemaphore: Pointer
    private val wName = WString(name)

    init {
        // 创建信号量
        hSemaphore = WinKernel32.INSTANCE.CreateSemaphoreW(null, initialCount, maxCount, wName)
            ?: throw RuntimeException("无法创建信号量，错误码: ${WinKernel32.INSTANCE.GetLastError()}")
    }

    /**
     * 阻塞当前线程，直到信号量计数 > 0
     * @param timeoutMs 等待时间，默认永久等待
     */
    fun lock(timeoutMs: Int = -1): Boolean {
        val result = WinKernel32.INSTANCE.WaitForSingleObject(hSemaphore, timeoutMs)
        return result == 0 // WAIT_OBJECT_0
    }

    /**
     * 释放信号量（手动增加计数）
     */
    fun unlock() {
        WinKernel32.INSTANCE.ReleaseSemaphore(hSemaphore, 1, null)
    }

    /**
     * 关闭句柄（销毁内核对象引用）
     */
    fun close() {
        WinKernel32.INSTANCE.CloseHandle(hSemaphore)
    }
}

fun semExample() {
    // 建议：如果不需要跨 Session 通信，去掉 Global\ 会减少权限带来的不确定性
    val sem = NamedSemaphore("Global\\MySharedSemaphore", initialCount = 0)

    println("--- Kotlin 信号量服务已启动 ---")
    println("状态: 已创建计数为 0 的信号量，正在原地阻塞等待 C++...")

    try {
        // 阻塞在这里，直到 C++ 调用了释放操作
        if (sem.lock()) {
            println("【唤醒成功】C++ 进程已获取信号量并释放了资源！")

            // 执行你的后续逻辑
            println("正在处理 C++ 传递后的任务...")
        }
    } finally {
        sem.close()
        println("资源已释放。")
    }
}