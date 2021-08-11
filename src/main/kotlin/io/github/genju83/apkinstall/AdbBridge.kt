package io.github.genju83.apkinstall

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.InstallException
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.util.concurrent.Executors

private val executorService = Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("AdbBridge-%d").build())

object AdbBridge {

    object Install : BridgeMethod<AnActionEvent>, Notifier by ToastNotifier {

        override fun checkCondition(eventType: AnActionEvent): ReadyState<AnActionEvent> {
            val virtualFile = eventType.getData(PlatformDataKeys.VIRTUAL_FILE)
            val capturedProject: Project? = eventType.project
            val capturedFile: VirtualFile? = virtualFile
            val capturedFilePath: String? = virtualFile?.canonicalPath

            return when {
                capturedProject == null -> ReadyState.NotReady(RuntimeException("Unknown project error occurs"))
                capturedFile == null -> ReadyState.NotReady(RuntimeException("Invalid apk file"))
                capturedFilePath == null || !capturedFilePath.endsWith(".apk") -> ReadyState.NotReady(RuntimeException("Invalid apk"))
                else -> ReadyState.Ready(eventType)
            }
        }

        override fun doIfReady(eventType: AnActionEvent, androidBridge: AndroidDebugBridge) {
            runCatching {
                val virtualFile = requireNotNull(eventType.getData(PlatformDataKeys.VIRTUAL_FILE))
                val virtualFilePath = requireNotNull(virtualFile.path)

                if (androidBridge.isConnected && androidBridge.devices.isNotEmpty()) {
                    info("${virtualFile.name} install begins")
                    androidBridge.devices.first().installPackage(virtualFilePath, true, "-d", "-t")
                    info("${virtualFile.name} installed")
                } else {
                    throw InstallException("Invalid conditions")
                }
            }.onFailure {
                error(it)
            }
        }

        override fun doIfNotReady(throwable: Throwable) {
            error(throwable)
        }
    }
}

sealed interface BridgeMethod<EventType> {

    fun checkCondition(eventType: EventType): ReadyState<EventType>

    fun doIfReady(eventType: EventType, androidBridge: AndroidDebugBridge)

    fun doIfNotReady(throwable: Throwable)

    fun execute(project: Project?, eventType: EventType) {
        val givenProject = requireNotNull(project)
        val androidBridge = requireNotNull(AndroidSdkUtils.getDebugBridge(givenProject))

        when (val checkCondition = checkCondition(eventType)) {
            is ReadyState.Ready -> {
                ProgressManager.getInstance().run(object : Task.Backgroundable(givenProject, "progress") {
                    override fun run(progressIndicator: ProgressIndicator) {
                        progressIndicator.isIndeterminate = true
                        executorService.submit { doIfReady(checkCondition.eventType, androidBridge) }.get()
                        progressIndicator.isIndeterminate = false
                        progressIndicator.fraction = 1.0
                    }
                })
            }
            is ReadyState.NotReady -> doIfNotReady(checkCondition.throwable)
        }
    }
}

sealed class ReadyState<EventType> {

    class Ready<EventType>(val eventType: EventType) : ReadyState<EventType>()
    class NotReady<EventType>(val throwable: Throwable) : ReadyState<EventType>()
}
